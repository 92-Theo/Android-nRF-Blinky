package no.nordicsemi.android.blinky.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import androidx.annotation.Nullable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.ktx.asValidResponseFlow
import no.nordicsemi.android.ble.ktx.getCharacteristic
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.utils.ParserUtils
import no.nordicsemi.android.blinky.ble.data.*
import no.nordicsemi.android.blinky.ble.utils.AesCcmUtil
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import timber.log.Timber
import kotlin.random.Random

class BlinkyManager(
    context: Context,
    device: BluetoothDevice
): Blinky by BlinkyManagerImpl(context, device)

private class BlinkyManagerImpl(
    context: Context,
    private val device: BluetoothDevice,
): BleManager(context), Blinky {
    private val scope = CoroutineScope(Dispatchers.IO)

    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var buttonCharacteristic: BluetoothGattCharacteristic? = null

    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var macCharacteristic: BluetoothGattCharacteristic? = null

    private val _ledState = MutableStateFlow(false)
    override val ledState = _ledState.asStateFlow()

    private val _buttonState = MutableStateFlow(false)
    override val buttonState = _buttonState.asStateFlow()

    private val _mac = MutableStateFlow("unknown")
    override val mac = _mac.asStateFlow()

    private val _deviceType = MutableStateFlow(Blinky.DeviceType.BLINKY)
    override val deviceType = _deviceType.asStateFlow()

    private val _rssi = MutableStateFlow(0)
    override val rssi = _rssi.asStateFlow();

    override val state = stateAsFlow()
        .map {
            when (it) {
                is ConnectionState.Connecting,
                is ConnectionState.Initializing -> Blinky.State.LOADING
                is ConnectionState.Ready -> Blinky.State.READY
                is ConnectionState.Disconnecting,
                is ConnectionState.Disconnected -> Blinky.State.NOT_AVAILABLE
            }
        }
        .stateIn(scope, SharingStarted.Lazily, Blinky.State.NOT_AVAILABLE)

    override suspend fun connect() = connect(device)
        .retry(3, 300)
        .useAutoConnect(false)
        .timeout(3000)
        .suspend()

    override fun release() {
        // Cancel all coroutines.
        scope.cancel()

        val wasConnected = isReady
        // If the device wasn't connected, it means that ConnectRequest was still pending.
        // Cancelling queue will initiate disconnecting automatically.
        cancelQueue()

        // If the device was connected, we have to disconnect manually.
        if (wasConnected) {
            disconnect().enqueue()
        }
    }

    override suspend fun turnLed(state: Boolean) {
        // Write the value to the characteristic.
        writeCharacteristic(
            ledCharacteristic,
            LedData.from(state),
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        ).suspend()

        // Update the state flow with the new value.
        _ledState.value = state
    }

    override fun log(priority: Int, message: String) {
        Timber.log(priority, message)
    }

    override fun getMinLogPriority(): Int {
        // By default, the library logs only INFO or
        // higher priority messages. You may change it here.
        return Log.VERBOSE
    }

    private val buttonCallback by lazy {
        object : ButtonCallback() {
            override fun onButtonStateChanged(device: BluetoothDevice, state: Boolean) {
                _buttonState.tryEmit(state)
            }
        }
    }

    private val ledCallback by lazy {
        object : LedCallback() {
            override fun onLedStateChanged(device: BluetoothDevice, state: Boolean) {
                _ledState.tryEmit(state)
            }
        }
    }

    private val macCallback by lazy {
        object : MacCallback() {
            override fun onMacChanged(device: BluetoothDevice, data: ByteArray) {
                if (data.isEmpty()) {
                    _mac.tryEmit("unknown")
                } else {
                    val dataString = ParserUtils.parse(data)
                    Log.d("BlinkyManager", "onMacChanged:${dataString}")
                    _mac.tryEmit(dataString)
                }
            }
        }
    }

    private val txCallback by lazy {
        object : TxCallback() {
            override fun onTxChanged(device: BluetoothDevice, data: ByteArray) {
                val dataString = ParserUtils.parse(data)
                Log.d("BlinkyManager", "onTxChanged:${dataString}")
                received(data)
            }
        }
    }

    override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
        // Get the LBS Service from the gatt object.
        gatt.getService(BlinkySpec.BLINKY_SERVICE_UUID)?.apply {
            // Get the LED characteristic.
            ledCharacteristic = getCharacteristic(
                BlinkySpec.BLINKY_LED_CHARACTERISTIC_UUID,
                // Mind, that below we pass required properties.
                // If your implementation supports only WRITE_NO_RESPONSE,
                // change the property to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE.
                BluetoothGattCharacteristic.PROPERTY_WRITE
            )
            // Get the Button characteristic.
            buttonCharacteristic = getCharacteristic(
                BlinkySpec.BLINKY_BUTTON_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY
            )

            _deviceType.tryEmit(Blinky.DeviceType.BLINKY)

            // Return true if all required characteristics are supported.
            return ledCharacteristic != null && buttonCharacteristic != null
        }

        gatt.getService(BlinkySpec.KEYPLUS_SERVICE_UUID)?.apply {
            macCharacteristic = getCharacteristic(
                BlinkySpec.KEYPLUS_MAC_CHARACTERISTIC_UUID,
                (BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            )
            txCharacteristic = getCharacteristic(
                BlinkySpec.KEYPLUS_TX_CHARACTERISTIC_UUID,
                (BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY)
            )
            rxCharacteristic = getCharacteristic(
                BlinkySpec.KEYPLUS_RX_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            )

            _deviceType.tryEmit(Blinky.DeviceType.KEYPLUS)

            // Return true if all required characteristics are supported.
            return macCharacteristic != null && txCharacteristic != null && rxCharacteristic != null
        }

        return false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun initialize() {
        when(deviceType.value){
            Blinky.DeviceType.BLINKY -> {
                // Enable notifications for the button characteristic.
                val flow: Flow<ButtonState> = setNotificationCallback(buttonCharacteristic)
                    .asValidResponseFlow()

                // Forward the button state to the buttonState flow.
                scope.launch {
                    flow.map { it.state }.collect { _buttonState.tryEmit(it) }
                }

                enableNotifications(buttonCharacteristic)
                    .enqueue()

                // Read the initial value of the button characteristic.
                readCharacteristic(buttonCharacteristic)
                    .with(buttonCallback)
                    .enqueue()

                // Read the initial value of the LED characteristic.
                readCharacteristic(ledCharacteristic)
                    .with(ledCallback)
                    .enqueue()
            }

            Blinky.DeviceType.KEYPLUS -> {
                enableNotifications(txCharacteristic)
                    .enqueue()

                enableNotifications(macCharacteristic)
                    .enqueue()

                readCharacteristic(txCharacteristic)
                    .with(txCallback)
                    .enqueue()

                readCharacteristic(macCharacteristic)
                    .with(macCallback)
                    .enqueue()

                // writeCharacteristic(rxCharacteristic, )
            }
        }
    }

    override fun onServicesInvalidated() {
        ledCharacteristic = null
        buttonCharacteristic = null

        txCharacteristic = null
        rxCharacteristic = null
        macCharacteristic = null
    }


    fun write(data: ByteArray,
              nonce2: ByteArray?){
        val dataWithDummy = if (data.size < 16) {
            data.plus(Random.nextBytes(16 - data.size))
        }else{
            data
        }

        var nonce = if (nonce2 == null){
            BlinkySpec.KEYPLUS_NONCE.plus(Random.nextBytes(6))
        }else{
            BlinkySpec.KEYPLUS_NONCE.plus(nonce2)
        }

        val encrypted = AesCcmUtil.encrypt(dataWithDummy, BlinkySpec.KEYPLUS_PRIVATE_KEY, nonce)


        val writeData = byteArrayOf(MsgType.BLE.type)
            .plus(encrypted.copyOfRange(dataWithDummy.size, dataWithDummy.size + 10))
            .plus(nonce.copyOfRange(6, 12))
            .plus(encrypted.copyOfRange(0, dataWithDummy.size))

        Log.d("BlinkyManager", "write:${ParserUtils.parse(writeData)}")
        writeCharacteristic(
            rxCharacteristic,
            writeData,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            .enqueue()
    }

    fun received(data: ByteArray) {
        if (data.isEmpty()) {
            Log.d("BlinkyManager", "received:data.isEmpty")
            return
        }

        if (data.size < 2){
            Log.d("BlinkyManager", "received:data.size=${data.size},invalid")
            return
        }
        val type = MsgType.of(data[0])
        when(type){
            MsgType.UNKNOWN -> {
                Log.d("BlinkyManager", "received:type=${type},invalid")
            }
            MsgType.BLE -> {
                if (data.size != 33){
                    Log.d("BlinkyManager", "received:data.size=${data.size},invalid ble size")
                    return;
                }
                parseBle(data.copyOfRange(1, data.size))
            }
            MsgType.UWB -> {
                parseUwb(data.copyOfRange(1, data.size))
            }
        }
    }

    private fun parseBle(data: ByteArray){
        // decrypt
        val plaintext: ByteArray
        try{
            plaintext = AesCcmUtil.decrypt(
                data.copyOfRange(16, data.size).plus(data.copyOfRange(0, 10)),
                BlinkySpec.KEYPLUS_PRIVATE_KEY,
                BlinkySpec.KEYPLUS_NONCE.plus(data.copyOfRange(10, 16))
            )
        }catch (e: Exception){
            Log.d("BlinkyManager", "parseBle:e=${e.message},exception")
            return;
        }

        if (plaintext.isEmpty()) {
            Log.d("BlinkyManager", "parseBle:plaintext=${plaintext},invalid")
            return;
        }
        if (plaintext.size != 16) {
            Log.d("BlinkyManager", "parseBle:plaintext=${ParserUtils.parse(plaintext)},invalid size")
            return;
        }

        val id = MsgId.of(plaintext[0])
        when(id){
            MsgId.SIGNIN_V2 -> {
                val resCode = ResCode.of(plaintext[1])
            }
            MsgId.SETTINGS3_GET ->{

            }
            MsgId.VER_GET -> {

            }
            MsgId.NOTI_NONCE -> {

            }
            else ->{
                Log.d("BlinkyManager", "parseBle:id=${id},invalid")
            }
        }
    }

    private fun parseUwb(data: ByteArray){
        Log.d("BlinkyManager", "parseUwb:data=${ParserUtils.parse(data)},invalid")
    }
}