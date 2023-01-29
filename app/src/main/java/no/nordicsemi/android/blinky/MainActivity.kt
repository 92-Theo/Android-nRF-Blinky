package no.nordicsemi.android.blinky

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import no.nordicsemi.android.blinky.control.BlinkyDestination
import no.nordicsemi.android.blinky.scanner.ScannerDestination
import no.nordicsemi.android.common.navigation.NavigationView
import no.nordicsemi.android.common.theme.NordicActivity
import no.nordicsemi.android.common.theme.NordicTheme
import java.util.*

@AndroidEntryPoint
class MainActivity: NordicActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NordicTheme {
                NavigationView(ScannerDestination + BlinkyDestination)
            }
        }

        checkPermission ()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.i("MainActivity", "onRequestPermissionsResult:code=${requestCode},permissions=${permissions}")
        when (requestCode) {
            1000 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun checkPermission() {
        var permission = mutableMapOf<String, String>()
        permission["uwbRanging"] = Manifest.permission.UWB_RANGING
        permission["bluetoothScan"] = Manifest.permission.BLUETOOTH_SCAN
        permission["bluetoothConnect"] = Manifest.permission.BLUETOOTH_CONNECT

        var denied = permission.count { ContextCompat.checkSelfPermission(this, it.value)  == PackageManager.PERMISSION_DENIED }

        if (denied > 0) {
            requestPermissions(permission.values.toTypedArray(), 1000)
        }
    }
}