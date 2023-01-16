package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class MacCallback: ProfileReadResponse() {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() == 6) {
            val mac = data.value
            if (mac != null)
                onMacChanged(device, mac)
            else
                onInvalidDataReceived(device, data)
        } else {
            onInvalidDataReceived(device, data)
        }
    }

    abstract fun onMacChanged(device: BluetoothDevice, data: ByteArray)
}