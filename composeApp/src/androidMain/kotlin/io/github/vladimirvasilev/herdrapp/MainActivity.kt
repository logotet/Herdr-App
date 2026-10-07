package io.github.vladimirvasilev.herdrapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import io.github.vladimirvasilev.herdrapp.ui.HerdrApp
import io.github.vladimirvasilev.herdrapp.ui.LocalQrScannerService
import io.github.vladimirvasilev.herdrapp.ui.QrScannerService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HerdrApplication
        setContent {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                QrScanResultDispatcher.consume(result.resultCode == Activity.RESULT_OK, result.data?.getStringExtra(QrScannerActivity.EXTRA_RESULT))
            }
            val scanner = remember {
                object : QrScannerService {
                    override fun scan(onResult: (String?) -> Unit) {
                        QrScanResultDispatcher.callback = onResult
                        launcher.launch(Intent(this@MainActivity, QrScannerActivity::class.java))
                    }
                }
            }
            CompositionLocalProvider(LocalQrScannerService provides scanner) {
                HerdrApp(app.container)
            }
        }
    }
}

private object QrScanResultDispatcher {
    var callback: ((String?) -> Unit)? = null
    fun consume(ok: Boolean, value: String?) { callback?.invoke(if (ok) value else null); callback = null }
}
