package no.nordicsemi.android.blinky.control.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.theme.NordicTheme

@Composable
internal fun KeyPlusControlView(
    mac: String,
    version: String,
    nonce: String,
    loginState: String,
    onLoginPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BaseItemView(
            text = "MAC",
            value = mac,
        )
        BaseItemView(
            text = "버전",
            value = version,
        )
        BaseItemView(
            text = "nonce2",
            value = nonce,
        )
        BaseItemView(
            text = "로그인 상태",
            value = loginState,
        )
        BaseItemView(
            text = "로그인 시도",
            onClick = onLoginPressed,
        )
    }
}

@Preview
@Composable
private fun KeyPlusControlViewPreview() {
    NordicTheme {
        KeyPlusControlView(
            mac = "10101010",
            version = "version",
            loginState = "loginState",
            nonce = "nonce",
            modifier = Modifier.padding(16.dp),
            onLoginPressed = {},
        )
    }
}