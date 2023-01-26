package no.nordicsemi.android.blinky.ble.utils

class IntUtil {
    companion object {
        fun toInt(bytes: ByteArray): Int {
            var result = 0
            var shift = 0
            for (byte in bytes) {
                result = result or (byte.toInt() shl shift)
                shift += 8
            }
            return result
        }

        fun toByteArray(data: Int): ByteArray {
            var result = ByteArray(4)
            result[0] = (data shr 0).toByte()
            result[1] = (data shr 8).toByte()
            result[2] = (data shr 16).toByte()
            result[3] = (data shr 24).toByte()

            return result
        }
    }
}