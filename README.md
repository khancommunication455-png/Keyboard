# StyleKeyboard — Font Style Converter & Smart Custom Keyboard (Android)

A single Android app with two parts:

1. **Host App** — dashboard for managing Presets, Emoji Shortcuts, Appearance,
   Smart Typing, Sounds, and the Auto Message Sender.
2. **Custom Keyboard Extension (IME)** — a system-wide keyboard that replaces
   Gboard and applies font conversion, emoji shortcuts, sounds, and visuals in
   real time as the user types in any app (WhatsApp, Instagram, etc.).

**Core promise**: Type → the keyboard instantly outputs stylized/converted
text, emoji shortcuts, sound, and visuals — all configured from the Host App.

---

## Tech Stack

| Layer        | Choice                                             |
|--------------|----------------------------------------------------|
| Language     | Kotlin 1.9.24                                       |
| UI           | Jetpack Compose (BOM 2024.06), Material 3           |
| Persistence  | Room 2.6.1, JSON-serialized compound fields (Moshi) |
| Async        | Coroutines + Flow                                   |
| Keyboard     | `InputMethodService` + Compose `ComposeView`        |
| Media        | SoundPool (key sounds), ExoPlayer (video bg), `Movie` (GIF bg) |
| Automation   | Foreground `Service` + optional `AccessibilityService` |
| Min SDK      | 26 (Android 8.0)                                    |
| Target SDK   | 34                                                  |
| Build        | Gradle 8.7, AGP 8.5.2, JDK 17                       |

---

## Project Structure

```
StyleKeyboardApp/
├── .github/workflows/android.yml          # CI: JDK 17 + gradle assembleDebug + artifact upload
├── gradlew / gradle/wrapper/              # Wrapper
├── build.gradle                           # Root
├── settings.gradle
├── gradle.properties
├── .gitignore
└── app/
    ├── build.gradle                       # Module: app
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/stylekeyboard/app/
        │   ├── app/
        │   │   ├── StyleApp.kt            # Application: first-launch seeding, notif channels
        │   │   └── ServiceLocator.kt      # Manual DI for repositories + DB
        │   ├── data/
        │   │   ├── db/
        │   │   │   ├── StyleDatabase.kt   # Room database, 8 entities
        │   │   │   ├── entity/            # Preset, Shortcut, AppConfig, WordFrequency,
        │   │   │   │                      #   Bigram, Trigram, UserDictionary, AutoSenderLog
        │   │   │   └── dao/               # 8 DAOs
        │   │   ├── model/JsonCodec.kt     # GlintConfig, SoundConfig, AutoSenderScript + JSON
        │   │   └── repository/            # Preset, Shortcut, AppConfig, Prediction,
        │   │                              #   AutoSenderLog repositories
        │   ├── ui/
        │   │   ├── MainActivity.kt        # Drawer + NavHost
        │   │   ├── Screen.kt              # Drawer routes
        │   │   ├── theme/                 # Color, Theme, Typography (dark, #0A0A0A, purple/cyan/pink)
        │   │   ├── components/            # GradientOutlinePanel, SectionCard, GlowingButton, …
        │   │   └── screens/
        │   │       ├── home/              # Font converter home (input + carousel + preview + actions)
        │   │       ├── presets/           # Preset manager + editor with monospaced mapping table
        │   │       ├── emojilab/          # Shortcut list + add/edit dialog + import/export JSON
        │   │       ├── smarttyping/       # Top words/bigrams, user dict, Clear History
        │   │       ├── appearance/        # GIF bg, glint, key shape, sound pack, haptics
        │   │       ├── autosender/        # Script editor + Start/Pause/Stop + run log
        │   │       └── enablekeyboard/    # 3-step IME enable wizard
        │   ├── keyboard/
        │   │   ├── StyleKeyboardService.kt # InputMethodService entry point
        │   │   ├── KeyboardController.kt   # State holder: mapping, suggestions, shortcuts
        │   │   ├── KeyboardScreen.kt       # Compose canvas: bg + scrim + suggestions + QWERTY + glint
        │   │   ├── KeyboardBackground.kt   # GIF (Movie) + video (ExoPlayer) backgrounds
        │   │   ├── KeyboardLayout.kt       # QWERTY rows + Key model
        │   │   └── KeySoundManager.kt      # SoundPool + Vibrator
        │   ├── autosender/
        │   │   ├── AutoSenderService.kt            # Foreground service + loop logic
        │   │   ├── AutoSenderManager.kt            # Intent front-door
        │   │   ├── AutoSenderAccessibilityService.k # Fallback for non-share-intent apps
        │   │   └── SenderStrategy.kt                # ACTION_SEND vs Accessibility dispatch
        │   └── util/
        │       ├── TextConverter.kt        # O(n) convertText engine, shared host + IME
        │       ├── DefaultPresets.kt       # Math Sans, Bold Serif, Upside Down, Zalgo, Bubble
        │       ├── DefaultShortcuts.kt     # 18 built-in emoji shortcuts
        │       └── BaseDictionary.kt       # Common-words seed for prediction engine
        └── res/
            ├── values/ (colors, strings, themes)
            ├── xml/ (method.xml, accessibility_service_config.xml, backup rules)
            ├── drawable/ (ic_launcher_foreground.xml)
            ├── mipmap-anydpi-v26/ (adaptive launcher icons)
            └── raw/ (drop key_mech.ogg / key_soft.ogg / key_marimba.ogg here)
```

---

## Build & Run

### Prerequisites

- **Android Studio Hedgehog or newer** (or just JDK 17 + Gradle 8.7 from CLI)
- Android SDK with `platform-34` and `build-tools-34`
- An Android device or emulator running API 26 or newer (real device strongly
  recommended — IMEs do not always behave correctly on emulators)

### Build the debug APK

```bash
cd StyleKeyboardApp
./gradlew assembleDebug
# APK appears at: app/build/outputs/apk/debug/app-debug.apk
```

### Install on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and click ▶ Run.

### Run unit tests

```bash
./gradlew testDebugUnitTest
```

---

## Enable & Use the Custom Keyboard

This is the most important step — without it, the keyboard never appears.

### Permission & Setting Order (do these in this exact order)

1. **Install** the APK.
2. **Open StyleKeyboard** once. This triggers first-launch seeding (default
   presets, default emoji shortcuts, base prediction dictionary, app-config row).
3. Go to **Enable Keyboard** in the drawer (or Android **Settings → System →
   Languages & input → On-screen keyboard**).
4. **Toggle ON** "Style Keyboard" in the list. Accept the system warning dialog
   (Android shows a generic warning for any third-party IME).
5. **(Optional) Grant Full Access** on the same screen. Only needed if you plan
   to use an emoji-blend API or imported sound packs. The keyboard does NOT
   transmit what you type.
6. In any text field (e.g. WhatsApp chat), tap the **keyboard-picker icon** in
   the navigation bar and choose **Style Keyboard**.
7. You should now see the QWERTY layout with a purple→cyan toolbar showing the
   active preset name (default: Math Sans).

### Granting the Auto Sender permissions (only if you use that feature)

1. Open **Auto Sender** in the drawer.
2. Pick a target package (e.g. `com.whatsapp`) and choose a send strategy:
   - **Intent.ACTION_SEND (share-intent)** — works for most messengers, no
     extra permission needed.
   - **Accessibility (simulated taps)** — needed for apps that don't expose a
     share-intent. Tap the "Accessibility NOT enabled" warning to jump to
     **Settings → Accessibility → StyleKeyboard Auto Sender** and toggle it ON.
3. Grant **POST_NOTIFICATIONS** on Android 13+ when prompted (needed so the
   foreground-service notification can show).
4. The first time you tap **Start**, the foreground notification appears with
   Pause and **STOP ALL** actions always visible.

### Granting the GIF background permission (only if you use that feature)

1. Open **Appearance → GIF / Video Background → Pick file**.
2. On Android 13+ the system photo picker is used — no runtime permission
   needed.
3. On Android 12 and earlier, grant **READ_EXTERNAL_STORAGE** when prompted.

---

## How the Conversion Engine Works

`com.stylekeyboard.app.util.TextConverter.convertText(input, map)` is the single
shared entry point used by both the Host App preview panel and the live
keyboard input path:

```kotlin
fun convertText(inputString: String, presetMap: Map<String, String>): String {
    if (inputString.isEmpty()) return ""
    if (presetMap.isEmpty()) return inputString
    val len = inputString.length
    val out = StringBuilder(len + (len shr 2))
    for (i in 0 until len) {
        val ch = inputString[i]
        val replacement = presetMap[ch.toString()]
        if (replacement != null) out.append(replacement) else out.append(ch)
    }
    return out.toString()
}
```

- **O(n)** in the input length
- **Allocation-light** — single `StringBuilder`, pre-sized to the input length
  plus a small expansion factor
- **Thread-safe** — pure function, no shared state
- **Per-keystroke path** uses `convertChar(ch, map)` instead so the keyboard
  doesn't reconvert the whole buffer on every key

### Max-3-taps-to-copy UX

From the Home screen:
1. Tap text field → type your message
2. Tap a preset chip in the carousel
3. Tap **Copy**

The converted preview updates live as you type or change presets.

---

## Self-Learning Predictive Text — How It Works

The prediction engine lives entirely on-device. No cloud sync, no reading other
apps' data. Tables (all in the same Room DB):

| Table                | Purpose                                              |
|----------------------|------------------------------------------------------|
| `word_frequency`     | Unigram counts + recency, seeded with a base dict    |
| `bigrams`            | (word₁, word₂) pairs for next-word prediction        |
| `trigrams`           | (word₁, word₂, word₃) for common 3-word phrases     |
| `user_dictionary`    | Always-surface words the user explicitly added       |

### Update logic (per word boundary)

On space / enter / punctuation:

1. The current word's `frequency` is incremented (or inserted with `frequency=1`).
2. The bigram `(previousWord, currentWord)` is incremented (or inserted).
3. If a `previousPreviousWord` exists, the trigram row is likewise updated.
4. All updates run on a background coroutine so the input thread never blocks.

### Suggestion ranking

When the user is mid-word, the engine queries `word_frequency` for prefix
matches ordered by `frequency DESC, lastUsed DESC`. When a word was just
completed, it also queries `bigrams` and `trigrams` for the most likely next
word given the last one or two words typed. Both signal sources are merged
into 3 suggestion slots above the keys, weighted:

```
prefix_score = word.frequency * 10 + recency_bonus(lastUsed)
bigram_score = bigram.frequency * 15 + recency_bonus(lastUsed)
trigram_score = trigram.frequency * 25 + recency_bonus(lastUsed)
user_dict_score = +5000 (always surfaces)
```

### Decay / pruning

A nightly maintenance pass (run from the Smart Typing screen as "Clear History"
or via `PredictionRepository.runDecay()`):

- Halves the frequency of any unigram not used in 30+ days.
- Deletes unigrams with `frequency <= 1` and `lastUsed < cutoff` (excluding
  user-added entries).

### Clear History

The Smart Typing screen has a **Clear History** button that wipes all learned
frequencies, bigrams, and trigrams while keeping the seeded base dictionary
and user-added entries intact.

---

## Auto Sender — Safety Rails

| Rail                       | Implementation                                                  |
|----------------------------|----------------------------------------------------------------|
| Minimum interval floor     | `MIN_INTERVAL_MS = 3000L` enforced in `AutoSenderService.startRun` |
| Foreground notification    | Always shown while running; Pause + STOP ALL actions always visible |
| Wake lock                  | `PARTIAL_WAKE_LOCK` with 10-min auto-release safety net          |
| Hard kill switch           | STOP ALL button in the notification AND on the Auto Sender screen |
| Service type               | `foregroundServiceType="specialUse"` per Android 14 requirement  |
| Accessibility only on opt-in | Only declared in manifest; user must enable in system settings   |

---

## CI/CD

`.github/workflows/android.yml` runs on every push/PR to `main`:

1. Check out
2. Set up JDK 17 (Temurin)
3. Set up Gradle 8.7 (cached)
4. `chmod +x ./gradlew`
5. `./gradlew assembleDebug --no-daemon --stacktrace`
6. `./gradlew testDebugUnitTest --no-daemon`
7. Upload the debug APK as a workflow artifact (`style-keyboard-debug-apk`)

The artifact is retained for 14 days. Download it from the Actions tab on
GitHub.

---

## Adding Sound Files

The keyboard ships silent. To enable key sounds, drop these short clips
(`< 50KB` each, OGG preferred) into `app/src/main/res/raw/`:

- `key_mech.ogg` — mechanical click
- `key_soft.ogg` — soft pop
- `key_marimba.ogg` — marimba note

If absent, the keyboard silently no-ops (see `KeySoundManager.applyConfig`).
No crash, no build failure.

---

## Emoji Kitchen-style Blending

Google's proprietary blend data can't be bundled. The app supports two
extensibility points:

1. **Open-source emoji-blend API** — point `SoundConfig.customUri` (or a
   dedicated blend-API URL field on the Emoji Lab screen) at a public blend
   endpoint. The keyboard fetches blended stickers on demand (requires Full
   Access).
2. **Locally-stored sticker pack** — the user drops a JSON manifest of
   `(emojiA, emojiB) -> stickerUri` mappings into the Emoji Lab screen, and
   the suggestion strip shows them when the user selects two emoji in
   sequence.

For v1 the JSON codec and storage primitives are in place
(`JsonCodec.encodeShortcutList` / `decodeShortcutList`); the UI for managing
blend packs is a straightforward extension of the existing Shortcut editor.

---

## Testing the Build Locally

```bash
# 1. Build the APK
./gradlew assembleDebug

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Run instrumented tests (if you add any under androidTest/)
./gradlew connectedAndroidTest
```

If `./gradlew` is not present (the wrapper JAR isn't bundled in this repo
because it's a binary), run:

```bash
gradle wrapper --gradle-version 8.7
```

once with a system Gradle install to generate the wrapper, then use `./gradlew`
for everything else.

---

## Known Limitations / Next Steps

- **Preset switcher popup** currently routes to the host app's Presets screen
  for simplicity. A production version would show a horizontal popup inside
  the IME window.
- **Symbol layout** (`?123` key) is wired as an action but the symbol layout
  itself is not yet implemented — left as an extension point in
  `KeyboardLayout.kt`.
- **Emoji Kitchen blend API** UI is stubbed; the JSON codec is ready.
- **Accessibility send heuristic** finds `EditText` and a node whose
  `contentDescription` contains "send". Apps that hide these (e.g. some
  Instagram DM views) may need per-app strategies.

---

## License

This is a reference build provided as-is. Replace the launcher icon, sound
pack, and any default preset/shortcut content with your own licensed assets
before shipping to production.
