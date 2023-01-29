package no.nordicsemi.android.blinky.ble.data

import no.nordicsemi.android.ble.data.Data

enum class MsgType(val type: Byte ) {
    UNKNOWN(0xFF.toByte()),
    BLE(0x00),
    UWB(0x01);

    companion object {
        fun of(type: Byte) = values().find { it.type == type }
            ?: UNKNOWN
    }
}


enum class ResCode(val code: Byte ) {
    UNKNOWN(0xFF.toByte()),
    SUCCESS(0x00),
    FAIL (0x01),
    INVALID_PARAM(0x02),
    SHUT_OFF(0x03),
    FAIL_AD_SET(0x04),
    ALREADY_IO_ON(0x10),
    IO_NOT_ON(0x11),
    ALREADY_NFC_REG_START(0x12),
    NFC_REG_NOT_START(0x13),
    INVALID_IO_KEEP_COUNT(0x15),
    IS_SIGNED_IN(0x16),
    IS_BOOTING(0x17),
    INVALID_PASSWORD(0x18),
    INVALID_NONCE(0x19),
    NFC_CANNOT_USE(0x1A);

    companion object {
        fun of(code: Byte) = values().find { it.code == code }
            ?: UNKNOWN
    }
}

enum class MsgId(val id: Byte) {
    UNKNOWN(0xFF.toByte()),

    IO_ON(0x01),
    IO_KEEP(0x02),
    IO_OFF(0x03),
    IO_CUSTOM(0x04),
    NFC_REG_START(0x10),
    NFC_REG_END(0x11),
    NFC_REMOVE(0x12),
    AD_OPEN(0x20),
    AD_OPEN_SET(0x21),
    AD_CLOSE(0x23),
    AD_CLOSE_SET(0x24),
    MODE_SET(0x30),
    TX_POWER_SET(0x32),
    BOOT_TIME_SET(0x34),
    SIGNIN_V2(0x44),
    REGISTER(0x45),
    RESET(0x50),
    DFU(0x51),
    REBOOT(0x67),
    BATT2_GET(0x69),
    SETTINGS3_GET(0x6A),
    AMBIENT_LIGHT_SET(0x6B),
    VER_GET(0x6C),
    NOTI_DOOR_OPENED_BY_NFC(0xD1.toByte()),
    NOTI_DOOR_CLOSED_BY_NFC(0xD2.toByte()),
    NOTI_NFC_REG_RET(0xD3.toByte()),
    NOTI_IO_KEEP_TIMEOUT(0xD4.toByte()),
    NOTI_DOOR_OPENED_BY_AD(0xD6.toByte()),
    NOTI_DOOR_CLOSED_BY_AD(0xD7.toByte()),
    NOTI_HB_BATT2(0xD9.toByte()),
    NOTI_SIGNIN_FROM_ANOTHER(0xDC.toByte()),
    NOTI_RSSI(0xDD.toByte()),
    NOTI_BATT_CHANGED(0xDE.toByte()),
    NOTI_NFC_CHANGED(0xDF.toByte()),
    NOTI_NONCE(0xE1.toByte());

    companion object {
        fun of(id: Byte) = values().find { it.id == id }
            ?: UNKNOWN

        fun login(password: ByteArray, force: Boolean, mode: DeviceMode) = byteArrayOf(SIGNIN_V2.id)
            .plus(if (force) 0x01 else 0x00)
            .plus(mode.mode)
            .plus(password)

        fun getVersion() = byteArrayOf(VER_GET.id)

        fun getSettings() = byteArrayOf(SETTINGS3_GET.id)
    }
}

enum class NiMsgId(val id: Byte) {
    UNKNOWN(0xFF.toByte()),

    ACCESSORY_CONFIGURATION_DATA(0x1),
    ACCESSORY_UWB_DID_START(0x2),
    ACCESSORY_UWB_DID_STOP(0x3),

    INIT(0xA),
    CONFIGURE_AND_START(0xB),
    STOP(0xC),

    ANDROID_CONFIGURATION_DATA(0x11),
    ANDROID_UWB_DID_START(0x12),
    ANDROID_UWB_DID_STOP(0x13),

    INIT_ANDROID(0x1A),
    CONFIGURE_AND_START_ANDROID(0x1B),
    STOP_ANDROID(0x1C);

    companion object {
        fun of(id: Byte) = values().find { it.id == id }
            ?: UNKNOWN
        fun init() = byteArrayOf(INIT.id)

        fun configureAndStart(sessionId: ByteArray, preamble: Byte, channel: Byte, stsIV : ByteArray, address: ByteArray) = byteArrayOf(CONFIGURE_AND_START.id)
            .plus(byteArrayOf(0x01, 0x00, 0x00, 0x00))
            .plus(0x17)
            .plus(byteArrayOf(0x4b, 0x52)) // fixed data
            .plus(sessionId) // session id, 4byte
            .plus(preamble) // preamble
            .plus(channel) // channel
            .plus(byteArrayOf(0x06, 0x00)) //num slots
            .plus(byteArrayOf(0x60, 0x09))// .plus(byteArrayOf(0x10, 0x0e)) //slot duration
            .plus(byteArrayOf(0xF0.toByte(), 0x00))// .plus(byteArrayOf(0xB4.toByte(), 0x00)) //block duration
            .plus(0x03) // fixed data
            .plus(stsIV)
            .plus(address)

        fun stop() = byteArrayOf(STOP.id)

        fun initAndroid() = byteArrayOf(INIT_ANDROID.id)

        fun configureAndStartAndroid(sessionId: ByteArray, preamble: Byte, channel: Byte, stsIV : ByteArray, address: ByteArray) = byteArrayOf(CONFIGURE_AND_START_ANDROID.id)
            .plus(14) // length
            .plus(sessionId) // session id, 4byte
            .plus(preamble) // preamble, 1byte
            .plus(channel) // channel, 1byte
            .plus(stsIV) // , 6byte
            .plus(address) //, 2byte

        fun stopAndroid() = byteArrayOf(STOP_ANDROID.id)
    }
}

enum class DeviceMode(val mode: Byte) {
    UNKNOWN(0xFF.toByte()),

    MANUAL(0x00),
    AUTO(0x01),
    SMARTKEY(0x02),
    SAFETY(0x03),
    AUTO_UWB(0x04),
}