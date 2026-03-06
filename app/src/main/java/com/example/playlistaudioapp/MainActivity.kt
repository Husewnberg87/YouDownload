package com.example.playlistaudioapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PlaylistAudioAppScreen()
        }
    }
}

@Composable
fun PlaylistAudioAppScreen() {
    var playlistUrl by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf("Logs will appear here...") }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Playlist Audio Downloader",
                fontSize = 24.sp
            )

            OutlinedTextField(
                value = playlistUrl,
                onValueChange = { playlistUrl = it },
                label = { Text("Enter playlist URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Enter folder name") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    logText = "Choose Folder button clicked"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Folder")
            }

            Button(
                onClick = {
                    logText = """
                        Start Download button clicked
                        
                        Playlist URL: $playlistUrl
                        Folder Name: $folderName
                    """.trimIndent()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Download")
            }

            Text(
                text = logText,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}