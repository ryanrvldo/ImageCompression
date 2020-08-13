package com.malaikatmaut.imagecompression.util

object LevensteinCode {

    const val METHOD_CODE = 222

    fun getCodes(size: Int): List<String> {
        if (size == 0) return emptyList()

        val codes = mutableListOf<String>()
        for (i in 0 until size) {
            codes.add(encoding(i))
        }
        return codes
    }

    private fun encoding(number: Int): String {
        if (number == 0) {
            return "0"
        }
        var count = 1
        var appendableCode = Integer.toBinaryString(number).substringAfter("1")
        var codeSoFar = appendableCode
        var mValue = appendableCode.length
        while (mValue != 0) {
            count += 1
            appendableCode = Integer.toBinaryString(mValue).substringAfter("1")
            codeSoFar = "${appendableCode}$codeSoFar"
            mValue = appendableCode.length
        }
        codeSoFar = "${("1").repeat(count)}0" + codeSoFar
        return codeSoFar
    }

    fun decoding(code: String): Int {
        var num = 1
        val left = code.substringBefore("0")
        val right = code.substringAfter("0")
        var n = left.length - 1

        if (n < 0) return 0

        var idx = 0
        while (n > 0) {
            val x = right.substring(idx, num + idx)
            num.run {
                num = Integer.parseInt("1${x}", 2)
                idx += x.length
                n--
            }
        }
        return num
    }
}

