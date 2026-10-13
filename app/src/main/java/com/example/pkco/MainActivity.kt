package com.example.pkco

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pkco.ui.notes.NotesApp
import com.example.pkco.ui.theme.PkcoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PkcoTheme {
                NotesApp()
            }
        }
    }
}
