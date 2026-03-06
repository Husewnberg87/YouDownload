package com.example.playlistaudioapp

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Context
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    private var selectedFolderUri by mutableStateOf<Uri?>(null)
    private var selectedFolderLabel by mutableStateOf("No folder selected yet")
    private var logTextState by mutableStateOf("No logs yet")
    private var statusTextState by mutableStateOf("Idle")
    private var isProcessing by mutableStateOf(false)
    private var playlistUrlState by mutableStateOf("")
    private var folderNameState by mutableStateOf("")

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                selectedFolderUri = uri
                selectedFolderLabel = extractFolderLabel(uri)
                saveFolderUri(uri)

                statusTextState = "Ready"
                addLog("Folder selected successfully")
            } else {
                statusTextState = "Idle"
                addLog("Folder selection canceled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefs = getSharedPreferences("playlist_audio_app_prefs", Context.MODE_PRIVATE)

        playlistUrlState = loadSavedPlaylistUrl()
        folderNameState = loadSavedFolderName()
        restoreSavedFolder()

        setContent {
            PlaylistAudioAppScreen(
                playlistUrl = playlistUrlState,
                folderName = folderNameState,
                selectedFolderLabel = selectedFolderLabel,
                statusText = statusTextState,
                logText = logTextState,
                isProcessing = isProcessing,
                onPlaylistUrlChange = {
                    playlistUrlState = it
                    savePlaylistUrl(it)
                },
                onFolderNameChange = {
                    folderNameState = it
                    saveFolderName(it)
                },
                onChooseFolderClick = {
                    if (!isProcessing) {
                        folderPickerLauncher.launch(null)
                    }
                },
                onStartDownloadClick = { currentPlaylistUrl, currentFolderName ->
                    if (isProcessing) return@PlaylistAudioAppScreen

                    val validationMessage = validateInputs(
                        playlistUrl = currentPlaylistUrl,
                        folderName = currentFolderName,
                        folderUri = selectedFolderUri
                    )

                    if (validationMessage != null) {
                        statusTextState = "Error"
                        addLog(validationMessage)
                        return@PlaylistAudioAppScreen
                    }

                    isProcessing = true
                    statusTextState = "Working"
                    addLog("Creating test file...")

                    lifecycleScope.launch {
                        val result = createTestFile(
                            folderUri = selectedFolderUri!!,
                            playlistUrl = currentPlaylistUrl.trim(),
                            folderName = currentFolderName.trim()
                        )

                        isProcessing = false

                        if (result.startsWith("Test file created successfully")) {
                            statusTextState = "Success"
                        } else {
                            statusTextState = "Error"
                        }

                        addLog(result)
                    }
                }
            )
        }
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newEntry = "[$timestamp] $message"

        logTextState = if (logTextState == "No logs yet") {
            newEntry
        } else {
            "$newEntry\n\n$logTextState"
        }
    }

    private fun saveFolderUri(uri: Uri) {
        prefs.edit().putString("selected_folder_uri", uri.toString()).apply()
    }

    private fun savePlaylistUrl(url: String) {
        prefs.edit().putString("playlist_url", url).apply()
    }

    private fun saveFolderName(name: String) {
        prefs.edit().putString("folder_name", name).apply()
    }

    private fun loadSavedPlaylistUrl(): String {
        return prefs.getString("playlist_url", "") ?: ""
    }

    private fun loadSavedFolderName(): String {
        return prefs.getString("folder_name", "") ?: ""
    }

    private fun restoreSavedFolder() {
        val savedUriString = prefs.getString("selected_folder_uri", null)

        if (!savedUriString.isNullOrBlank()) {
            val uri = Uri.parse(savedUriString)
            selectedFolderUri = uri
            selectedFolderLabel = extractFolderLabel(uri)
            statusTextState = "Ready"
            addLog("Previous folder restored")
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
                Subfolder: $safeFolderName
                File: test.txt
            """.trimIndent()
        } catch (e: Exception) {
            "Error while creating test file: ${e.message}"
        }
    }
}

@Composable
fun PlaylistAudioAppScreen(
    playlistUrl: String,
    folderName: String,
    selectedFolderLabel: String,
    statusText: String,
    logText: String,
    isProcessing: Boolean,
    onPlaylistUrlChange: (String) -> Unit,
    onFolderNameChange: (String) -> Unit,
    onChooseFolderClick: () -> Unit,
    onStartDownloadClick: (String, String) -> Unit
) {
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
                onValueChange = onPlaylistUrlChange,
                label = { Text("Enter playlist URL") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            )

            OutlinedTextField(
                value = folderName,
                onValueChange = onFolderNameChange,
                label = { Text("Enter folder name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            )

            Button(
                onClick = onChooseFolderClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text("Choose Folder")
            }

            Button(
                onClick = {
                    onStartDownloadClick(playlistUrl, folderName)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Text(
                    if (isProcessing) {
                        "Working..."
                    } else {
                        "Start Download"
                    }
                )
            }

            Text(
                text = "Selected folder: $selectedFolderLabel",
                fontSize = 16.sp
            )

            Text(
                text = "Status: $statusText",
                fontSize = 16.sp
            )

            Text(
                text = "Log history:",
                fontSize = 16.sp
            )

            Text(
                text = logText,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}