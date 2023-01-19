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
internal fun NIControlView(
    dist: String,
    uwbState: String,
    onInit: () -> Unit,
    onConfigureAndStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BaseItemView(
            text = "UWB State",
            value = uwbState
        )

        BaseItemView(
            text = "거리",
            value = dist
        )

        BaseItemView(
            text = "init",
            onClick = onInit,
        )

        BaseItemView(
            text = "configure and start",
            onClick = onConfigureAndStart,
        )

        BaseItemView(
            text = "stop",
            onClick = onStop,
        )
    }
}

@Preview
@Composable
private fun NIControlViewPreview() {
    NordicTheme {
        NIControlView(
            dist = "10m",
            uwbState = "not start",
            onInit = {},
            onConfigureAndStart = {},
            onStop ={},
            modifier = Modifier.padding(16.dp),
        )
    }
}