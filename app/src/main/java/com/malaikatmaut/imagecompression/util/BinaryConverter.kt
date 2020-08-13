package com.malaikatmaut.imagecompression.util

object BinaryConverter {

    @ExperimentalUnsignedTypes
    fun hexToBin(bytes: ByteArray): String {
        val strBuilder = StringBuilder()
        for (b in bytes) {
            val bin = b.toUByte().toString(2)
            strBuilder.append(String.format("%08d", Integer.parseInt(bin)))
        }
        return strBuilder.toString()
    }

    fun binToHex(binary: String): ByteArray {
        val bytes = ByteArray(binary.length / 8)
        for (i in binary.indices step 8) {
            val str = binary.substring(i, i + 8)
            val num = Integer.parseInt(str, 2)
            bytes[i / 8] = num.toByte()
        }
        return bytes
    }
}