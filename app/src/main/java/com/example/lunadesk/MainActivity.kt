package com.example.lunadesk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lunadesk.ui.LunaDeskRoot
import com.example.lunadesk.ui.LunaDeskViewModel
import com.example.lunadesk.ui.LunaDeskViewModelFactory
import com.example.lunadesk.ui.theme.LunaDeskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val app = application as com.example.lunadesk.LunaDeskApp
            val viewModel: LunaDeskViewModel = viewModel(
                factory = LunaDeskViewModelFactory(app.container)
            )

            LunaDeskTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LunaDeskRoot(viewModel = viewModel)
                }
            }
        }
    }
}
