package no.nordicsemi.android.blinky.ble.utils

class ByteUtil {
    companion object {
        fun toInvertedByteArray(bytes: ByteArray): ByteArray {
            var result = ByteArray(bytes.size)

            for(i in result.indices){
                val j = result.size - i - 1
                result[i] = bytes[j]
            }

            return result
        }
    }
}