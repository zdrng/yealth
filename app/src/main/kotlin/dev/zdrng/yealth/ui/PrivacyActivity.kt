package dev.zdrng.yealth.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.zdrng.yealth.R
import dev.zdrng.yealth.ui.components.*
import dev.zdrng.yealth.ui.theme.YealthTheme

/** Both system rationale intents show the same offline policy; never request permissions here. */
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { YealthTheme {
            Scaffold { padding ->
                androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                    ScreenList("privacy-policy") {
                        item { Heading(stringResource(R.string.privacy)) }
                        item { Text(stringResource(R.string.privacy_live)) }
                        item { Button({ finish() }) { Text(stringResource(R.string.close)) } }
                    }
                }
            }
        } }
    }
}
