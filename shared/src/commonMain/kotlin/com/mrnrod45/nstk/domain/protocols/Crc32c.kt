package com.mrnrod45.nstk.domain.protocols

object Crc32c {
    private val TABLE = IntArray(256) { i ->
        var crc = i
        repeat(8) {
            crc = if (crc and 1 != 0) {
                (crc ushr 1) xor 0x82F63B78.toInt()
            } else {
                crc ushr 1
            }
        }
        crc
    }

    fun calculate(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (i in offset until offset + length) {
            val byte = data[i].toInt() and 0xFF
            val index = (crc xor byte) and 0xFF
            crc = (crc ushr 8) xor TABLE[index]
        }
        return crc xor 0xFFFFFFFF.toInt()
    }
}
