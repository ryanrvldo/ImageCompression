package com.malaikatmaut.imagecompression.util

object EliasOmegaCode {

    const val METHOD_CODE = 111

    fun getCodes(size: Int): List<String> {
        if (size == 0) return emptyList()

        val codes = mutableListOf<String>()
        for (i in 1..size) {
            codes.add(encoding(i))
        }
        return codes
    }

    private tailrec fun encoding(num: Int, code: String = "0"): String = when (num) {
        1 -> code
        else -> {
            val appendBits = Integer.toBinaryString(num)
            encoding(appendBits.length - 1, appendBits + code)
        }
    }

    fun decoding(code: String): Int {
        var num = 1
        var idx = 0
        while (code[idx] != '0') {
            val numBin = code.substring(idx, idx + num + 1)
            num = Integer.parseInt(numBin, 2)
            idx += numBin.length
        }
        return num
    }

}
