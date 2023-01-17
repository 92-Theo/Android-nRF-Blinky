package no.nordicsemi.android.blinky.ble.utils

class StringUtil {
    companion object {
        private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

        fun toByteArray(value: String): ByteArray {
            if (value.isNullOrEmpty())
                return ByteArray(0)

            // value.replace('-', '')
            val v1 = value.filterNot { it == '-' }

            if (v1.isNullOrEmpty() || (v1.length % 2 != 0))
                return ByteArray(0)

            return v1.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

        fun toHexString(data: ByteArray): String {
            if (data.isEmpty())
                return ""

            val out = CharArray(data.size * 3 - 1)
            for (j in data.indices) {
                val v = data[j].toInt() and 0xFF
                out[j * 3] = HEX_ARRAY[v ushr 4]
                out[j * 3 + 1] = HEX_ARRAY[v and 0x0F]
                if (j != data.size - 1) out[j * 3 + 2] = '-'
            }

            return String(out)
        }
    }
}