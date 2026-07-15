package com.stylekeyboard.app.util

/**
 * Small common-words base dictionary used to seed [WordFrequencyEntity] on
 * first launch. A few hundred frequent English words with a starting weight,
 * so the keyboard has reasonable suggestions out of the box before the user
 * has typed anything.
 */
object BaseDictionary {

    fun words(): List<String> = """
        a an the and or but if when while of in on at to for from with by as is are was were be been
        being have has had do does did will would shall should can could may might must i you he she
        it we they me him her us them my your his its our their this that these those here there now
        then today tomorrow yesterday morning afternoon evening night day week month year time hour
        minute second hello hi hey bye goodbye please thanks thank you welcome sorry ok okay yes no
        maybe sure great good bad nice cool fun funny happy sad angry love like want need get give
        take make do go come see hear know think say tell ask answer reply message text call email
        work job home school office family friend mother father son daughter brother sister baby kid
        child man woman boy girl people person guy water food coffee tea drink eat sleep walk run
        drive fly play game music movie book news phone computer car house door window cat dog bird
        fish tree flower sun moon star rain snow fire ice cold hot warm cool fast slow new old big
        small long short high low good better best more most less least one two three four five six
        seven eight nine ten hundred thousand million first last next previous monday tuesday
        wednesday thursday friday saturday sunday january february march april may june july august
        september october november december morning evening night week weekend year month day
        how what when where why who which whose whom is am are was were be been being have has had
        do does did will would shall should can could may might must about above across after again
        against all almost alone along already also always among any are around because been before
        being below between both but came come could does does done down during each enough even
        every from front got had has have here him himself his how into its just keep last left less
        like little made make many might more most much never none nothing now off often only other
        our out over own put said same see should some such take than that their them then there
        these they this those through too under until very was way well were what where which while
        who why will with would yes you your
    """.trimIndent().split(Regex("\\s+")).filter { it.isNotBlank() }
}
