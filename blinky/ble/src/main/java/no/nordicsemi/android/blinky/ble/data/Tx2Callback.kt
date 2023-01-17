package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.ValueChangedCallback
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class Tx2Callback: DataReceivedCallback {

    override fun onDataReceived(device: BluetoothDevice, data: Data) = received(device, data)

    private fun received(device: BluetoothDevice, data: Data){
        Log.d("Tx2Callback", "received:data=${data.size()}")
        if (data.size() > 2) {
            val tx = data.value
            if (tx != null)
                onTxChanged(device, tx)
        }
    }

    abstract fun onTxChanged(device: BluetoothDevice, data: ByteArray)
}