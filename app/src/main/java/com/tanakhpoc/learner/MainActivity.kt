package com.tanakhpoc.learner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tanakhpoc.learner.navigation.TanakhNavGraph
import com.tanakhpoc.learner.ui.theme.TanakhTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TanakhTheme {
                Surface(Modifier.fillMaxSize()) { TanakhNavGraph() }
            }
        }
    }
}
