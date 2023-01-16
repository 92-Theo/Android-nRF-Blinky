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
    }
}