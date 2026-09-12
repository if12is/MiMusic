import it.vfsfitvnm.kugou.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test
    fun parsesStandardAndWordTimedLines() {
        val lyrics = """
            [ti:ليالي]
            [00:12.00]السطر الأول
            [01:05.5]السطر الثاني
            [01:20.000] <01:20.000>كلمة <01:20.400>تانية
        """.trimIndent()

        val lines = LrcParser.parse(lyrics).filter { it.text.isNotBlank() }

        assertEquals(3, lines.size)
        assertEquals(12_000L, lines[0].timeMs)
        assertEquals("السطر الأول", lines[0].text)
        assertEquals(65_500L, lines[1].timeMs)
        assertEquals(2, lines[2].words.size)
        assertEquals("كلمة", lines[2].words[0].text)
        assertEquals(80_400L, lines[2].words[1].timeMs)
        assertTrue(LrcParser.plainText(lyrics).contains("السطر الأول"))
    }
}
