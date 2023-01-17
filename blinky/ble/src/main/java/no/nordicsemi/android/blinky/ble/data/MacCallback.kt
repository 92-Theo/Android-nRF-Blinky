package no.nordicsemi.android.blinky.ble.data

import android.bluetooth.BluetoothDevice
import android.util.Log
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.callback.profile.ProfileReadResponse
import no.nordicsemi.android.ble.data.Data

abstract class MacCallback: ProfileReadResponse(), DataSentCallback{

    override fun onDataReceived(device: BluetoothDevice, data: Data) = received(device, data)

    override fun onDataSent(device: BluetoothDevice, data: Data) = received(device, data)

    private fun received(device: BluetoothDevice, data: Data){
        Log.d("MacCallback", "received:data=${data.size()}")
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