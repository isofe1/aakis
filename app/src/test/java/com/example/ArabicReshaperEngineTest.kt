package com.example

import com.example.engine.ArabicReshaperEngine
import org.junit.Assert.*
import org.junit.Test

class ArabicReshaperEngineTest {

    @Test
    fun testArabicReshapingBasic() {
        val input = "مرحبا"
        val reshaped = ArabicReshaperEngine.reshape(input)
        assertNotNull(reshaped)
        assertTrue(reshaped.isNotEmpty())
        assertNotEquals(input, reshaped) // Shaped string must contain presentation forms
    }

    @Test
    fun testRevertMode() {
        val input = "تصميم جرافيك"
        val reshaped = ArabicReshaperEngine.reshape(input)
        val reverted = ArabicReshaperEngine.revert(reshaped)
        assertEquals(input, reverted)
    }

    @Test
    fun testLamAlefLigature() {
        val input = "سلام"
        val reshaped = ArabicReshaperEngine.reshape(input)
        assertTrue(reshaped.contains('\uFEFC') || reshaped.contains('\uFEFB'))
    }

    @Test
    fun testMultiLineReshaping() {
        val multiLine = "سطر أول\nسطر ثاني"
        val reshaped = ArabicReshaperEngine.reshape(multiLine)
        assertTrue(reshaped.contains("\n"))
        val lines = reshaped.split("\n")
        assertEquals(2, lines.size)
    }
}
