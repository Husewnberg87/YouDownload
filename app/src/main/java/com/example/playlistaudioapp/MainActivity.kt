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
import androidx.documentfile.provider.DocumentFile

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
                    if (selectedFolderUri == null) {
                        logTextState = "Please choose a folder first"
                        return@PlaylistAudioAppScreen
                    }

                    val result = createTestFile(selectedFolderUri!!, playlistUrl, folderName)
                    logTextState = result
                }
            )
        }
    }

    private fun createTestFile(folderUri: Uri, playlistUrl: String, folderName: String): String {
        return try {
            val pickedFolder = DocumentFile.fromTreeUri(this, folderUri)
                ?: return "Could not access selected folder"

            val appFolderName = if (folderName.isBlank()) "MyPlaylistFolder" else folderName
            val targetFolder = pickedFolder.findFile(appFolderName)
                ?: pickedFolder.createDirectory(appFolderName)

            if (targetFolder == null || !targetFolder.isDirectory) {
                return "Could not create target folder"
            }

            val existingFile = targetFolder.findFile("test.txt")
            val testFile = existingFile ?: targetFolder.createFile("text/plain", "test")

            if (testFile == null) {
                return "Could not create test.txt"
            }

            val content = """
                PlaylistAudioApp test file
                
                Playlist URL:
                $playlistUrl
                
                Folder Name:
                $folderName
            """.trimIndent()

            contentResolver.openOutputStream(testFile.uri, "wt")?.use { outputStream ->
                outputStream.write(content.toByteArray())
            } ?: return "Could not open file output stream"

            """
                Test file created successfully
                
                Folder URI:
                $folderUri
                
                Subfolder:
                $appFolderName
                
                File:
                test.txt
            """.trimIndent()
        } catch (e: Exception) {
            "Error while creating test file:\n${e.message}"
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