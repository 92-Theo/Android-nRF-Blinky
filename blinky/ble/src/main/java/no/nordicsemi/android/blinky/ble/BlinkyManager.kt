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
import no.nordicsemi.android.ble.WriteRequest
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.callback.DataSentCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.asValidResponseFlow
import no.nordicsemi.android.ble.ktx.getCharacteristic
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend
import no.nordicsemi.android.ble.utils.ParserUtils
import no.nordicsemi.android.blinky.ble.data.*
import no.nordicsemi.android.blinky.ble.utils.AesCcmUtil
import no.nordicsemi.android.blinky.ble.utils.StringUtil
import no.nordicsemi.android.blinky.spec.Blinky
import no.nordicsemi.android.blinky.spec.BlinkySpec
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
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

    private val _loggedInState = MutableStateFlow("unknown")
    override val loggedInState = _loggedInState.asStateFlow()

    private val _version = MutableStateFlow("unknown")
    override val version = _version.asStateFlow()

    private val _loggedInNonce = MutableStateFlow("unknown")
    override val loggedInNonce = _loggedInNonce.asStateFlow()

    private var timerTask: Timer? = null

    override val state = stateAsFlow()
        .map {
            when (it) {
                is ConnectionState.Connecting,
                is ConnectionState.Initializing -> Blinky.State.LOADING
                is ConnectionState.Ready -> Blinky.State.READY
                is ConnectionState.Disconnecting,
                is ConnectionState.Disconnected -> {
                    _loggedInState.tryEmit("unknown")
                    _loggedInNonce.tryEmit("")
                    _version.tryEmit("unknown")
                    _mac.tryEmit("unknown")
                    timerTask?.cancel()
                    Blinky.State.NOT_AVAILABLE
                }
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
        timerTask?.cancel()

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

    override suspend fun login() {
        // Write the value to the characteristic.
        write(
            MsgId.login(
                password = "000000".toByteArray(),
                force = false,
                mode = DeviceMode.MANUAL
            ),
            nonce2 = StringUtil.toByteArray(_loggedInNonce.value)
        ).suspend()

        // Update the state flow with the new value.
        // _ledState.value = state
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
                Log.d("BlinkyManager", "txCallback.onTxChanged:${dataString}")
                received(data)
            }
        }
    }

    private val tx2Callback by lazy {
        object : Tx2Callback() {
            override fun onTxChanged(device: BluetoothDevice, data: ByteArray) {
                val dataString = ParserUtils.parse(data)
                Log.d("BlinkyManager", "tx2Callback.onTxChanged:${dataString}")
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
                setNotificationCallback(txCharacteristic)
                    .with(tx2Callback)

                setNotificationCallback(macCharacteristic)
                    .with(macCallback)

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

                timerTask?.cancel()
                timerTask = Timer(true)
                timerTask?.schedule(
                    object :TimerTask() {
                        override fun run() {
                            write(
                                data = MsgId.login(
                                    password = "000000".toByteArray(),
                                    force = false,
                                    mode = DeviceMode.MANUAL
                                ),
                                nonce2 = null
                            ).enqueue()
                        }
                    },
                    1000,
                    1000
                )


                write(
                    data = MsgId.login(
                        password = "000000".toByteArray(),
                        force = false,
                        mode = DeviceMode.MANUAL
                    ),
                    nonce2 = null
                )
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
              nonce2: ByteArray?) : WriteRequest{
        val dataWithDummy = if (data.size < 16) {
            data.plus(Random.nextBytes(16 - data.size))
        }else{
            data
        }

        Log.d("BlinkyManager", "write:dataWithDummy=${ParserUtils.parse(dataWithDummy)}")

        var nonce = if (nonce2 == null || nonce2.size != 6){
            BlinkySpec.KEYPLUS_NONCE.plus(Random.nextBytes(6))
        }else{
            Log.d("BlinkyManager", "write:nonce2=${ParserUtils.parse(nonce2)}")
            BlinkySpec.KEYPLUS_NONCE.plus(nonce2)
        }

        val encrypted = AesCcmUtil.encrypt(dataWithDummy, BlinkySpec.KEYPLUS_PRIVATE_KEY, nonce)

        Log.d("BlinkyManager", "write:encrypted=${ParserUtils.parse(encrypted)}")

        val writeData = byteArrayOf(MsgType.BLE.type)
            .plus(encrypted.copyOfRange(dataWithDummy.size, dataWithDummy.size + 10))
            .plus(nonce.copyOfRange(6, 12))
            .plus(encrypted.copyOfRange(0, dataWithDummy.size))

        Log.d("BlinkyManager", "write:writeData=${ParserUtils.parse(writeData)}")
        return writeCharacteristic(
            rxCharacteristic,
            writeData,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
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
        when(val type = MsgType.of(data[0])){
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
        Log.d("BlinkyManager", "parseBle:data=${ParserUtils.parse(data)},debug")
        // decrypt
        val plaintext: ByteArray
        try{
            val src = data.copyOfRange(16, data.size).plus(data.copyOfRange(0, 10))
            val key = BlinkySpec.KEYPLUS_PRIVATE_KEY
            val nonce = BlinkySpec.KEYPLUS_NONCE.plus(data.copyOfRange(10, 16))
            Log.d("BlinkyManager", "parseBle:src=${ParserUtils.parse(src)},key=${ParserUtils.parse(key)},nonce=${ParserUtils.parse(nonce)},debug")
            plaintext = AesCcmUtil.decrypt(
                src,
                key,
                nonce
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

        Log.d("BlinkyManager", "parseBle:plaintext=${ParserUtils.parse(plaintext)}")

        when(val id = MsgId.of(plaintext[0])){
            MsgId.SIGNIN_V2 -> {
                when (val code = ResCode.of(plaintext[1])){
                    ResCode.SUCCESS -> {
                        _loggedInState.tryEmit("success")

                        timerTask?.cancel()
                        write(
                            data = MsgId.getVersion(),
                            nonce2 = null
                        ).enqueue()
//                        write(
//                            data = MsgId.getSettings(),
//                            nonce2 = null
//                        )
                    }
                    ResCode.INVALID_NONCE -> {
                        val n2 = StringUtil.toHexString(plaintext.copyOfRange(2, 8))
                        _loggedInState.tryEmit("invalid nonce=${n2}")
                        _loggedInNonce.tryEmit(n2)
                        Log.d("BlinkyManager", "parseBle:id=${id},nonce2=${n2}")
                        write(
                            data = MsgId.login("000000".toByteArray(), false, DeviceMode.MANUAL),
                            nonce2 = StringUtil.toByteArray(n2)
                        ).enqueue()
                    }
                    ResCode.UNKNOWN -> {
                        Log.d("BlinkyManager", "parseBle:id=${id},code=${plaintext[1]},unknown")
                        _loggedInState.tryEmit("unknown code")
                    }
                    else -> {
                        _loggedInState.tryEmit("fail,code=${code}")
                        Log.d("BlinkyManager", "parseBle:id=${id},code=${code},invalid")
                    }
                }
            }
            MsgId.VER_GET -> {
                val code = ResCode.of(plaintext[1])
                if (code ==  ResCode.SUCCESS){
                    _version.tryEmit("fw=${plaintext[2]}.${plaintext[3]}.${plaintext[4]}${System.lineSeparator()}nhw=${plaintext[5]}.${plaintext[6]}.${plaintext[7]}")
                }else{
                    Log.d("BlinkyManager", "parseBle:id=${id},code=${code},invalid")
                }
            }
            MsgId.NOTI_NONCE -> {
                val n2 = StringUtil.toHexString(plaintext.copyOfRange(1, 7))
                _loggedInState.tryEmit("invalid nonce=${n2}")
                _loggedInNonce.tryEmit(n2)

                Log.d("BlinkyManager", "parseBle:id=${id},nonce2=${n2}")
                write(
                    data = MsgId.login("000000".toByteArray(), false, DeviceMode.MANUAL),
                    nonce2 = StringUtil.toByteArray(n2)
                ).enqueue()
            }
            MsgId.UNKNOWN -> {
                Log.d("BlinkyManager", "parseBle:id=${plaintext[0]},unknown")
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