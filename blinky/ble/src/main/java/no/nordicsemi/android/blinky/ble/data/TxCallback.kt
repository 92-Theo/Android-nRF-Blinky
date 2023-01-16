package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class TxCallback: ProfileReadResponse() {

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        if (data.size() > 2) {
            val tx = data.value
            if (tx != null)
                onTxChanged(device, tx)
            else
                onInvalidDataReceived(device, data)
        } else {
            onInvalidDataReceived(device, data)
        }
    }

    abstract fun onTxChanged(device: BluetoothDevice, data: ByteArray)
}