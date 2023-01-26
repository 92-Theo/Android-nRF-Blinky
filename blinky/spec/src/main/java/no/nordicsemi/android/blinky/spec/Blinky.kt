package no.nordicsemi.android.blinky.spec

import kotlinx.coroutines.flow.StateFlow

interface Blinky {

    enum class State {
        LOADING,
        READY,
        NOT_AVAILABLE
    }

    enum class DeviceType{
        BLINKY,
        KEYPLUS,
        NI,
    }

    //    enum class State{
//        NONE,           // 기본 상태
//        CONNECTING,     // 연결 중
//        GATT_GETTING,   // GATT 가져오는 중
//        LOGGIN_IN,      // 로그인 중
//        LOGGED_IN,      // 로그인 완료
//        DISCONNECTED,   // 미연결
//    }

    /**
     * Connects to the device.
     */
    suspend fun connect()

    /**
     * Disconnects from the device.
     */
    fun release()

    /**
     * The current state of the blinky.
     */
    val state: StateFlow<State>

    /**
     * The current state of the LED.
     */
    val ledState: StateFlow<Boolean>

    /**
     * The current state of the button.
     */
    val buttonState: StateFlow<Boolean>

    /**
     * Controls the LED state.
     *
     * @param state the new state of the LED.
     */
    suspend fun turnLed(state: Boolean)

    val mac: StateFlow<String>
    val deviceType: StateFlow<DeviceType>
    val rssi: StateFlow<Int>
    val loggedInState: StateFlow<String>
    val version: StateFlow<String>
    val loggedInNonce: StateFlow<String>
    val dist: StateFlow<String>

    suspend fun login()
    suspend fun initNi()
    suspend fun configureAndStartNi()
    suspend fun stopNi()
}