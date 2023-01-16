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
    // onStateChanged: (Boolean) -> Unit,
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
    }
}

@Preview
@Composable
private fun KeyPlusControlViewPreview() {
    NordicTheme {
        KeyPlusControlView(
            mac = "10101010",
            modifier = Modifier.padding(16.dp),
        )
    }
}