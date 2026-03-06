package com.example.playlistaudioapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

    private var selectedFolderUri by mutableStateOf<Uri?>(null)
    private var logTextState by mutableStateOf("Logs will appear here...")

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                selectedFolderUri = uri
                logTextState = "Folder selected:\n$uri"
            } else {
                logTextState = "Folder selection canceled"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PlaylistAudioAppScreen(
                selectedFolderUri = selectedFolderUri,
                logText = logTextState,
                onChooseFolderClick = {
                    folderPickerLauncher.launch(null)
                },
                onStartDownloadClick = { playlistUrl, folderName ->
                    logTextState = """
                        Start Download button clicked
                        
                        Playlist URL: $playlistUrl
                        Folder Name: $folderName
                        Selected Folder: ${selectedFolderUri ?: "No folder selected"}
                    """.trimIndent()
                }
            )
        }
    }
}

@Composable
fun PlaylistAudioAppScreen(
    selectedFolderUri: Uri?,
    logText: String,
    onChooseFolderClick: () -> Unit,
    onStartDownloadClick: (String, String) -> Unit
) {
    var playlistUrl by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }

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
                onClick = onChooseFolderClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose Folder")
            }

            Button(
                onClick = {
                    onStartDownloadClick(playlistUrl, folderName)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Download")
            }

            Text(
                text = if (selectedFolderUri != null) {
                    "Selected folder is ready"
                } else {
                    "No folder selected yet"
                },
                fontSize = 16.sp
            )

            Text(
                text = logText,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}