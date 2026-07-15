package com.stylekeyboard.app.data.repository

import com.stylekeyboard.app.data.db.dao.BigramDao
import com.stylekeyboard.app.data.db.dao.TrigramDao
import com.stylekeyboard.app.data.db.dao.UserDictionaryDao
import com.stylekeyboard.app.data.db.dao.WordFrequencyDao
import com.stylekeyboard.app.data.db.entity.BigramEntity
import com.stylekeyboard.app.data.db.entity.TrigramEntity
import com.stylekeyboard.app.data.db.entity.UserDictionaryEntity
import com.stylekeyboard.app.data.db.entity.WordFrequencyEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the local predictive-text engine. The keyboard service calls
 * [recordConfirmedWord] on each word boundary and [suggest] during typing.
 *
 * Ranking merges:
 *   - prefix matches from [WordFrequencyDao] (frequency + recency)
 *   - "next word" matches from [BigramDao] / [TrigramDao] given the last 1-2 words
 *   - always-included entries from [UserDictionaryDao]
 */
class PredictionRepository(
    private val wordDao: WordFrequencyDao,
    private val bigramDao: BigramDao,
    private val trigramDao: TrigramDao,
    private val userDictDao: UserDictionaryDao
) {
    fun observeTopWords(limit: Int = 100): Flow<List<WordFrequencyEntity>> =
        wordDao.observeTop(limit)

    fun observeTopBigrams(limit: Int = 50): Flow<List<BigramEntity>> =
        bigramDao.observeTop(limit)

    fun observeUserDictionary(): Flow<List<UserDictionaryEntity>> =
        userDictDao.observeAll()

    /**
     * Returns up to [slots] suggestions. If [partial] is non-empty we are mid-word
     * (prefix matching dominates). If [previousWords] is non-empty we just completed
     * a word (next-word prediction via bigrams/trigrams also contributes).
     */
    suspend fun suggest(
        partial: String,
        previousWords: List<String>,
        slots: Int = 3
    ): List<String> {
        val results = LinkedHashMap<String, Int>() // word -> score

        if (partial.isNotEmpty()) {
            val prefixHits = wordDao.suggestByPrefix(partial.lowercase(), slots * 4)
            prefixHits.forEach { w ->
                results[w.word] = (results[w.word] ?: 0) + w.frequency * 10 + recencyBonus(w.lastUsed)
            }
            val userHits = userDictDao.suggestByPrefix(partial.lowercase())
            userHits.forEach { u ->
                results[u.word] = (results[u.word] ?: 0) + 5000 // always surface user dict
            }
        }

        if (previousWords.isNotEmpty()) {
            val prev = previousWords.last().lowercase()
            if (previousWords.size >= 2) {
                val prevPrev = previousWords[previousWords.size - 2].lowercase()
                val tri = trigramDao.suggestNext(prevPrev, prev, slots)
                tri.forEach { t ->
                    results[t.thirdWord] = (results[t.thirdWord] ?: 0) + t.frequency * 25 + recencyBonus(t.lastUsed)
                }
            }
            val bi = bigramDao.suggestNext(prev, slots)
            bi.forEach { b ->
                results[b.secondWord] = (results[b.secondWord] ?: 0) + b.frequency * 15 + recencyBonus(b.lastUsed)
            }
        }

        return results.entries
            .sortedByDescending { it.value }
            .take(slots)
            .map { it.key }
    }

    private fun recencyBonus(lastUsed: Long): Int {
        val ageMs = System.currentTimeMillis() - lastUsed
        val days = ageMs / (1000L * 60 * 60 * 24)
        return when {
            days < 1 -> 200
            days < 7 -> 100
            days < 30 -> 50
            else -> 0
        }
    }

    /**
     * Called on a word boundary (space/punctuation/enter). Updates unigram + the
     * bigram/trigram leading into this word. Intended for a background coroutine.
     */
    suspend fun recordConfirmedWord(word: String, previousWords: List<String>) {
        val w = word.lowercase().trim()
        if (w.isEmpty() || w.any { it.isDigit() && w.length > 32 }) return
        val now = System.currentTimeMillis()

        // Unigram
        val existing = wordDao.get(w)
        if (existing != null) {
            wordDao.increment(w, now)
        } else {
            wordDao.upsert(WordFrequencyEntity(word = w, frequency = 1, lastUsed = now))
        }

        // Bigram
        if (previousWords.isNotEmpty()) {
            val prev = previousWords.last().lowercase()
            val bigram = bigramDao.get(prev, w)
            if (bigram != null) {
                bigramDao.increment(prev, w, now)
            } else {
                bigramDao.upsert(BigramEntity(prev, w, 1, now))
            }
        }

        // Trigram
        if (previousWords.size >= 2) {
            val prevPrev = previousWords[previousWords.size - 2].lowercase()
            val prev = previousWords.last().lowercase()
            val trigram = trigramDao.get(prevPrev, prev, w)
            if (trigram != null) {
                trigramDao.increment(prevPrev, prev, w, now)
            } else {
                trigramDao.upsert(TrigramEntity(prevPrev, prev, w, 1, now))
            }
        }
    }

    suspend fun addUserWord(word: String) {
        val w = word.lowercase().trim()
        if (w.isEmpty()) return
        userDictDao.insert(UserDictionaryEntity(w, System.currentTimeMillis()))
        if (wordDao.get(w) == null) {
            wordDao.upsert(WordFrequencyEntity(word = w, frequency = 50, lastUsed = System.currentTimeMillis(), isUserAdded = true))
        }
    }

    suspend fun removeUserWord(word: String) = userDictDao.deleteByWord(word)

    /**
     * Nightly maintenance pass: down-weight stale unigrams, drop dead rows.
     */
    suspend fun runDecay() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 // 30 days
        wordDao.decayOldEntries(cutoff)
        wordDao.pruneStale(cutoff)
    }

    /**
     * "Clear History" — wipe learned frequencies, keep only the seeded base +
     * user-added entries.
     */
    suspend fun clearLearnedHistory() {
        wordDao.clearLearned()
        bigramDao.clearAll()
        trigramDao.clearAll()
    }

    suspend fun seedBaseDictionary(words: List<String>) {
        val now = System.currentTimeMillis()
        words.forEach { w ->
            val cleaned = w.lowercase().trim()
            if (cleaned.isNotEmpty() && wordDao.get(cleaned) == null) {
                wordDao.upsert(
                    WordFrequencyEntity(
                        word = cleaned,
                        frequency = 10,
                        lastUsed = now,
                        isUserAdded = false
                    )
                )
            }
        }
    }
}
