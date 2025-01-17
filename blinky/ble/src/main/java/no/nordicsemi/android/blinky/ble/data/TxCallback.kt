package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.ValueChangedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class TxCallback: ProfileReadResponse(), DataSentCallback {

    override fun onDataReceived(device: BluetoothDevice, data: Data) = received(device, data)

    override fun onDataSent(device: BluetoothDevice, data: Data) = received(device, data)

    private fun received(device: BluetoothDevice, data: Data){
        Log.d("TxCallback", "received:data=${data.size()}")
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