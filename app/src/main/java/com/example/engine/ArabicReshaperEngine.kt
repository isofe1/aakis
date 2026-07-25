package com.example.engine

/**
 * High-performance, 100% offline Arabic Reshaper Engine for Android.
 * Handles contextual letter reshaping (Isolated, Initial, Medial, Final forms),
 * Lam-Alef ligatures, Tatweel/Kashida, Arabic-Indic digits, diacritics (Harakat),
 * and LTR-reordering for non-RTL applications (Photoshop, Premiere, CapCut, Minecraft).
 */
object ArabicReshaperEngine {

    private fun c(code: Int): Char = code.toChar()

    private data class ArabicChar(
        val base: Char,
        val isolated: Char,
        val finalForm: Char,
        val initialForm: Char,
        val medialForm: Char,
        val canConnectAfter: Boolean = true
    )

    // Map of standard Arabic characters to Presentation Forms-B
    private val charMap: Map<Char, ArabicChar> = mapOf(
        c(0x0621) to ArabicChar(c(0x0621), c(0xFE80), c(0xFE80), c(0xFE80), c(0xFE80), canConnectAfter = false), // Hamza
        c(0x0622) to ArabicChar(c(0x0622), c(0xFE81), c(0xFE82), c(0xFE81), c(0xFE82), canConnectAfter = false), // Alef Madda
        c(0x0623) to ArabicChar(c(0x0623), c(0xFE83), c(0xFE84), c(0xFE83), c(0xFE84), canConnectAfter = false), // Alef Hamza Above
        c(0x0624) to ArabicChar(c(0x0624), c(0xFE85), c(0xFE86), c(0xFE85), c(0xFE86), canConnectAfter = false), // Waw Hamza Above
        c(0x0625) to ArabicChar(c(0x0625), c(0xFE87), c(0xFE88), c(0xFE87), c(0xFE88), canConnectAfter = false), // Alef Hamza Below
        c(0x0626) to ArabicChar(c(0x0626), c(0xFE89), c(0xFE8A), c(0xFE8B), c(0xFE8C), canConnectAfter = true),  // Yeh Hamza Above
        c(0x0627) to ArabicChar(c(0x0627), c(0xFE8D), c(0xFE8E), c(0xFE8D), c(0xFE8E), canConnectAfter = false), // Alef
        c(0x0628) to ArabicChar(c(0x0628), c(0xFE8F), c(0xFE90), c(0xFE91), c(0xFE92), canConnectAfter = true),  // Beh
        c(0x0629) to ArabicChar(c(0x0629), c(0xFE93), c(0xFE94), c(0xFE93), c(0xFE94), canConnectAfter = false), // Teh Marbuta
        c(0x062A) to ArabicChar(c(0x062A), c(0xFE95), c(0xFE96), c(0xFE97), c(0xFE98), canConnectAfter = true),  // Teh
        c(0x062B) to ArabicChar(c(0x062B), c(0xFE99), c(0xFE9A), c(0xFE9B), c(0xFE9C), canConnectAfter = true),  // Theh
        c(0x062C) to ArabicChar(c(0x062C), c(0xFE9D), c(0xFE9E), c(0xFE9F), c(0xFEA0), canConnectAfter = true),  // Jeem
        c(0x062D) to ArabicChar(c(0x062D), c(0xFEA1), c(0xFEA2), c(0xFEA3), c(0xFEA4), canConnectAfter = true),  // Hah
        c(0x062E) to ArabicChar(c(0x062E), c(0xFEA5), c(0xFEA6), c(0xFEA7), c(0xFEA8), canConnectAfter = true),  // Khah
        c(0x062F) to ArabicChar(c(0x062F), c(0xFEA9), c(0xFEAA), c(0xFEA9), c(0xFEAA), canConnectAfter = false), // Dal
        c(0x0630) to ArabicChar(c(0x0630), c(0xFEAB), c(0xFEAC), c(0xFEAB), c(0xFEAC), canConnectAfter = false), // Thal
        c(0x0631) to ArabicChar(c(0x0631), c(0xFEAD), c(0xFEAE), c(0xFEAD), c(0xFEAE), canConnectAfter = false), // Reh
        c(0x0632) to ArabicChar(c(0x0632), c(0xFEAF), c(0xFEB0), c(0xFEAF), c(0xFEB0), canConnectAfter = false), // Zain
        c(0x0633) to ArabicChar(c(0x0633), c(0xFEB1), c(0xFEB2), c(0xFEB3), c(0xFEB4), canConnectAfter = true),  // Seen
        c(0x0634) to ArabicChar(c(0x0634), c(0xFEB5), c(0xFEB6), c(0xFEB7), c(0xFEB8), canConnectAfter = true),  // Sheen
        c(0x0635) to ArabicChar(c(0x0635), c(0xFEB9), c(0xFEBA), c(0xFEBB), c(0xFEBC), canConnectAfter = true),  // Sad
        c(0x0636) to ArabicChar(c(0x0636), c(0xFEBD), c(0xFEBE), c(0xFEBF), c(0xFEC0), canConnectAfter = true),  // Dad
        c(0x0637) to ArabicChar(c(0x0637), c(0xFEC1), c(0xFEC2), c(0xFEC3), c(0xFEC4), canConnectAfter = true),  // Tah
        c(0x0638) to ArabicChar(c(0x0638), c(0xFEC5), c(0xFEC6), c(0xFEC7), c(0xFEC8), canConnectAfter = true),  // Zah
        c(0x0639) to ArabicChar(c(0x0639), c(0xFEC9), c(0xFECA), c(0xFECB), c(0xFECC), canConnectAfter = true),  // Ain
        c(0x063A) to ArabicChar(c(0x063A), c(0xFECD), c(0xFECE), c(0xFECF), c(0xFED0), canConnectAfter = true),  // Ghain
        c(0x0641) to ArabicChar(c(0x0641), c(0xFED1), c(0xFED2), c(0xFED3), c(0xFED4), canConnectAfter = true),  // Feh
        c(0x0642) to ArabicChar(c(0x0642), c(0xFED5), c(0xFED6), c(0xFED7), c(0xFED8), canConnectAfter = true),  // Qaf
        c(0x0643) to ArabicChar(c(0x0643), c(0xFED9), c(0xFEDA), c(0xFEDB), c(0xFEDC), canConnectAfter = true),  // Kaf
        c(0x0644) to ArabicChar(c(0x0644), c(0xFEDD), c(0xFEDE), c(0xFEDF), c(0xFEE0), canConnectAfter = true),  // Lam
        c(0x0645) to ArabicChar(c(0x0645), c(0xFEE1), c(0xFEE2), c(0xFEE3), c(0xFEE4), canConnectAfter = true),  // Meem
        c(0x0646) to ArabicChar(c(0x0646), c(0xFEE5), c(0xFEE6), c(0xFEE7), c(0xFEE8), canConnectAfter = true),  // Noon
        c(0x0647) to ArabicChar(c(0x0647), c(0xFEE9), c(0xFEEA), c(0xFEEB), c(0xFEEC), canConnectAfter = true),  // Heh
        c(0x0648) to ArabicChar(c(0x0648), c(0xFEED), c(0xFEEE), c(0xFEED), c(0xFEEE), canConnectAfter = false), // Waw
        c(0x0649) to ArabicChar(c(0x0649), c(0xFEEF), c(0xFEF0), c(0xFEEF), c(0xFEF0), canConnectAfter = false), // Alef Maksura
        c(0x064A) to ArabicChar(c(0x064A), c(0xFEF1), c(0xFEF2), c(0xFEF3), c(0xFEF4), canConnectAfter = true)   // Yeh
    )

    private val diacritics = setOf(
        c(0x064B), c(0x064C), c(0x064D), c(0x064E), c(0x064F), c(0x0650), c(0x0651), c(0x0652)
    )

    /**
     * Reshapes and reorders standard Arabic text so it renders correctly in non-RTL software.
     */
    fun reshape(input: String, convertNumbers: Boolean = false): String {
        if (input.isBlank()) return input

        // Split multi-line input to keep paragraph breaks
        val lines = input.split("\n")
        return lines.joinToString("\n") { line ->
            reshapeSingleLine(line, convertNumbers)
        }
    }

    private fun reshapeSingleLine(line: String, convertNumbers: Boolean): String {
        if (line.isEmpty()) return ""

        // Phase 1: Contextual Shaping
        val shapedList = mutableListOf<Char>()
        val len = line.length
        var i = 0

        while (i < len) {
            val ch = line[i]

            // Check for Lam-Alef Ligatures
            if (ch == c(0x0644) && i + 1 < len) {
                val nextCh = line[i + 1]
                val prevConnected = isPrevConnected(line, i)
                val ligature = getLamAlefLigature(nextCh, prevConnected)
                if (ligature != null) {
                    shapedList.add(ligature)
                    i += 2
                    continue
                }
            }

            if (charMap.containsKey(ch)) {
                val data = charMap[ch]!!
                val prevConn = isPrevConnected(line, i)
                val nextConn = isNextConnected(line, i)

                val shapedChar = when {
                    !prevConn && !nextConn -> data.isolated
                    prevConn && !nextConn -> data.finalForm
                    !prevConn && nextConn -> data.initialForm
                    else -> data.medialForm
                }
                shapedList.add(shapedChar)
            } else if (diacritics.contains(ch)) {
                shapedList.add(ch)
            } else if (convertNumbers && isWesternDigit(ch)) {
                shapedList.add(toArabicIndicDigit(ch))
            } else {
                shapedList.add(ch)
            }
            i++
        }

        // Phase 2: Visual Reordering for LTR display environments
        return reorderForLtr(shapedList)
    }

    private fun isPrevConnected(text: String, index: Int): Boolean {
        var p = index - 1
        while (p >= 0 && diacritics.contains(text[p])) {
            p--
        }
        if (p < 0) return false
        val prevCh = text[p]
        if (prevCh == c(0x0640)) return true
        val prevData = charMap[prevCh] ?: return false
        return prevData.canConnectAfter
    }

    private fun isNextConnected(text: String, index: Int): Boolean {
        var n = index + 1
        while (n < text.length && diacritics.contains(text[n])) {
            n++
        }
        if (n >= text.length) return false
        val nextCh = text[n]
        if (nextCh == c(0x0640)) return true
        return charMap.containsKey(nextCh)
    }

    private fun getLamAlefLigature(nextCh: Char, prevConnected: Boolean): Char? {
        return when (nextCh) {
            c(0x0627) -> if (prevConnected) c(0xFEFC) else c(0xFEFB) // Alef
            c(0x0622) -> if (prevConnected) c(0xFEF6) else c(0xFEF5) // Alef Madda
            c(0x0623) -> if (prevConnected) c(0xFEF8) else c(0xFEF7) // Alef Hamza Above
            c(0x0625) -> if (prevConnected) c(0xFEFA) else c(0xFEF9) // Alef Hamza Below
            else -> null
        }
    }

    /**
     * Reorders the shaped glyph sequence so that LTR rendering engines display Arabic right-to-left.
     * Preserves internal direction of numbers and English/Latin text segments.
     */
    private fun reorderForLtr(shapedChars: List<Char>): String {
        if (shapedChars.isEmpty()) return ""

        val tokens = mutableListOf<Token>()
        val currentToken = StringBuilder()
        var currentIsArabic = false

        for (ch in shapedChars) {
            val isAr = isArabicShapedOrBase(ch) || diacritics.contains(ch) || ch == c(0x0640)
            if (currentToken.isEmpty()) {
                currentIsArabic = isAr
                currentToken.append(ch)
            } else if (currentIsArabic == isAr) {
                currentToken.append(ch)
            } else {
                tokens.add(Token(currentToken.toString(), currentIsArabic))
                currentToken.clear()
                currentIsArabic = isAr
                currentToken.append(ch)
            }
        }
        if (currentToken.isNotEmpty()) {
            tokens.add(Token(currentToken.toString(), currentIsArabic))
        }

        val result = StringBuilder()
        for (token in tokens.reversed()) {
            if (token.isArabic) {
                result.append(token.content.reversed())
            } else {
                result.append(token.content)
            }
        }

        return result.toString()
    }

    private data class Token(val content: String, val isArabic: Boolean)

    private fun isArabicShapedOrBase(ch: Char): Boolean {
        if (charMap.containsKey(ch)) return true
        val code = ch.code
        return (code in 0xFE70..0xFEFF) || (code in 0xFB50..0xFDFF)
    }

    private fun isWesternDigit(ch: Char) = ch in '0'..'9'

    private fun toArabicIndicDigit(ch: Char): Char {
        return when (ch) {
            '0' -> '٠'
            '1' -> '١'
            '2' -> '٢'
            '3' -> '٣'
            '4' -> '٤'
            '5' -> '٥'
            '6' -> '٦'
            '7' -> '٧'
            '8' -> '٨'
            '9' -> '٩'
            else -> ch
        }
    }
}
