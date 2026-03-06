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
    private var selectedFolderLabel by mutableStateOf("No folder selected yet")
    private var logTextState by mutableStateOf("Logs will appear here...")

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                selectedFolderUri = uri
                selectedFolderLabel = extractFolderLabel(uri)
                logTextState = "Folder selected successfully"
            } else {
                logTextState = "Folder selection canceled"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PlaylistAudioAppScreen(
                selectedFolderLabel = selectedFolderLabel,
                logText = logTextState,
                onChooseFolderClick = {
                    folderPickerLauncher.launch(null)
                },
                onStartDownloadClick = { playlistUrl, folderName ->
                    val validationMessage = validateInputs(
                        playlistUrl = playlistUrl,
                        folderName = folderName,
                        folderUri = selectedFolderUri
                    )

                    if (validationMessage != null) {
                        logTextState = validationMessage
                        return@PlaylistAudioAppScreen
                    }

                    val result = createTestFile(
                        folderUri = selectedFolderUri!!,
                        playlistUrl = playlistUrl.trim(),
                        folderName = folderName.trim()
                    )

                    logTextState = result
                }
            )
        }
    }

    private fun extractFolderLabel(uri: Uri): String {
        val raw = uri.toString()
        return when {
            raw.contains("primary%3AMusic") -> "Music"
            raw.contains("primary%3ADownload") -> "Download"
            raw.contains("primary%3ADocuments") -> "Documents"
            raw.contains("primary%3APictures") -> "Pictures"
            raw.contains("primary%3AMovies") -> "Movies"
            else -> "Custom folder selected"
        }
    }

    private fun validateInputs(
        playlistUrl: String,
        folderName: String,
        folderUri: Uri?
    ): String? {
        if (playlistUrl.trim().isEmpty()) {
            return "Error: Please enter a playlist URL"
        }

        if (!playlistUrl.startsWith("http://") && !playlistUrl.startsWith("https://")) {
            return "Error: Playlist URL must start with http:// or https://"
        }

        if (folderName.trim().isEmpty()) {
            return "Error: Please enter a folder name"
        }

        if (folderUri == null) {
            return "Error: Please choose a folder first"
        }

        return null
    }

    private fun createTestFile(folderUri: Uri, playlistUrl: String, folderName: String): String {
        return try {
            val pickedFolder = DocumentFile.fromTreeUri(this, folderUri)
                ?: return "Could not access selected folder"

            val safeFolderName = folderName
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .trim()

            if (safeFolderName.isEmpty()) {
                return "Error: Folder name contains only invalid characters"
            }

            val targetFolder = pickedFolder.findFile(safeFolderName)
                ?: pickedFolder.createDirectory(safeFolderName)

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
                
                Subfolder:
                $safeFolderName
                
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
    selectedFolderLabel: String,
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
                text = "Selected folder: $selectedFolderLabel",
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