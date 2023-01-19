package no.nordicsemi.android.blinky.spec

import java.util.UUID

class BlinkySpec {

    companion object {
        val BLINKY_SERVICE_UUID: UUID = UUID.fromString("00001523-1212-efde-1523-785feabcd123")
        val BLINKY_BUTTON_CHARACTERISTIC_UUID: UUID = UUID.fromString("00001524-1212-efde-1523-785feabcd123")
        val BLINKY_LED_CHARACTERISTIC_UUID: UUID = UUID.fromString("00001525-1212-efde-1523-785feabcd123")

        // val KEYPLUS_ADVERTISING_UUID: UUID = UUID.fromString("803fb101-81e3-40d4-a4b7-e5984fe812f4")
        val KEYPLUS_SERVICE_UUID: UUID = UUID.fromString("803fb101-81e3-40d4-a4b7-e5984fe812f4")
        val KEYPLUS_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("803fb103-81e3-40d4-a4b7-e5984fe812f4") // read
        val KEYPLUS_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("803fb102-81e3-40d4-a4b7-e5984fe812f4") // write
        val KEYPLUS_MAC_CHARACTERISTIC_UUID: UUID = UUID.fromString("803fb104-81e3-40d4-a4b7-e5984fe812f4") // mac


        val UWB_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val UWB_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // read
        val UWB_RX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // write


        val KEYPLUS_PRIVATE_KEY: ByteArray = "keypleisgood1234".toByteArray(Charsets.UTF_8)
        val KEYPLUS_NONCE: ByteArray = "keyple".toByteArray(Charsets.UTF_8)
    }
}