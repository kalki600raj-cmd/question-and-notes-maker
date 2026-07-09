package com.example.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import android.graphics.Canvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.geometry.Size
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.CyberBlue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.StudyNote
import com.example.utils.PdfExportHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ImageAnnotationPin(val xPercent: Float, val yPercent: Float, val text: String)
data class VideoAnnotation(val timeSecs: Int, val text: String)
data class CollabComment(val user: String, val text: String, val timestamp: Long = System.currentTimeMillis())

val defaultTooltips = listOf(
    "mitochondria" to "The powerhouse of the cell fr, making energy like an absolute boss ⚡️",
    "photosynthesis" to "How leaves cook their own food using pure solar energy, major plant slay ☀️🍃",
    "gravity" to "The invisible pull keeping us grounded so we don't float away into deep space 🌌",
    "dna" to "The biological source code that makes you, well, YOU 🧬💅",
    "calculus" to "The math of change that tells us how fast things are going, big brain style 📈",
    "nucleus" to "The brain of the cell, basically the chief executive officer directing all operations 🧠👑",
    "quantum" to "Subatomic particles behaving in wild ways that defy normal physics. Mind blown fr 🤯",
    "velocity" to "How fast you are moving in a specific direction. Zooming fr! 🏎️💨",
    "ecosystem" to "A massive community where plants, bugs, and beasts vibe together in perfect balance 🦊☘️",
    "mutation" to "A glitch in the biological code that creates brand-new traits. Mutant powers fr! 🧬💥"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveNoteBody(
    content: String,
    onTermClicked: (String, String) -> Unit
) {
    val lines = content.split("\n")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            if (line.trim().startsWith("-") || line.trim().startsWith("•")) {
                Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    WordFlowParagraph(
                        text = line.trim().substring(1).trim(),
                        onTermClicked = onTermClicked
                    )
                }
            } else if (line.isNotEmpty()) {
                WordFlowParagraph(
                    text = line,
                    onTermClicked = onTermClicked
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordFlowParagraph(
    text: String,
    onTermClicked: (String, String) -> Unit
) {
    val words = text.split(" ")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        words.forEach { wordWithPunctuation ->
            val cleanWord = wordWithPunctuation.lowercase()
                .replace(",", "")
                .replace(".", "")
                .replace("?", "")
                .replace("!", "")
                .replace(":", "")
                .replace(";", "")
                .replace("\"", "")
                .trim()
            
            val matchingTooltip = defaultTooltips.firstOrNull { it.first == cleanWord }
            if (matchingTooltip != null) {
                Text(
                    text = wordWithPunctuation,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp)
                        .clickable { onTermClicked(matchingTooltip.first, matchingTooltip.second) }
                )
            } else {
                Text(wordWithPunctuation)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: MainAppViewModel,
    onNavigateToQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val isGeneratingNote by viewModel.isGeneratingNote.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedNote by remember { mutableStateOf<StudyNote?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    
    var uploadedImagePath by remember { mutableStateOf<String?>(null) }
    
    val createDialogFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.path?.endsWith(".pdf", ignoreCase = true) == true
            
            if (isPdf) {
                val bitmap = renderPdfFirstPageToBitmap(context, uri)
                if (bitmap != null) {
                    viewModel.saveDoodleImage(bitmap) { savedPath ->
                        uploadedImagePath = savedPath
                        Toast.makeText(context, "Slayed! PDF rendered & ready as diagram 📜✨", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Could not render PDF 🥺", Toast.LENGTH_SHORT).show()
                }
            } else {
                val bitmap = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                } catch (e: Exception) {
                    null
                }

                if (bitmap != null) {
                    viewModel.saveDoodleImage(bitmap) { savedPath ->
                        uploadedImagePath = savedPath
                        Toast.makeText(context, "Diagram uploaded fr! 🖼️🔥", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Could not load image 🥺", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filteredNotes = notes.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
    }

    if (selectedNote != null) {
        NoteDetailView(
            note = selectedNote!!,
            viewModel = viewModel,
            onBack = { selectedNote = null },
            onGenerateQuiz = {
                viewModel.generateQuestionsFromNote(selectedNote!!)
                onNavigateToQuiz()
            }
        )
    } else {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                "LEVEL UP, GENZ!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                "STUDY\nSLABS 📚✍️",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 34.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = ComposeColor.White),
                            border = BorderStroke(3.dp, ComposeColor.Black),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("😎", fontSize = 22.sp)
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = { showAiDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("ai_note_fab")
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, "AI Generation")
                            Spacer(Modifier.width(4.dp))
                            Text("AI STUDY SLAY ✨", fontWeight = FontWeight.Black)
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            showCreateDialog = true
                            uploadedImagePath = null
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("create_note_fab")
                    ) {
                        Icon(Icons.Default.Add, "Add Note")
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your brain slabs... fr") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("search_note_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.onBackground
                    )
                )

                if (isGeneratingNote) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "AI study partner is cookin' your notes... 🔥🍳",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (filteredNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🥺", fontSize = 64.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No notes found. Create one or let AI slay it!",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        items(filteredNotes) { note ->
                            val typeColor = when (note.noteType) {
                                "revision" -> MaterialTheme.colorScheme.secondary
                                "quick_revision" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Card(
                                onClick = { selectedNote = note },
                                border = BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .testTag("note_card_${note.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            note.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            maxLines = 2,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            note.content,
                                            fontSize = 12.sp,
                                            maxLines = 4,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            note.noteType.uppercase().replace("_", " "),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = typeColor,
                                            modifier = Modifier
                                                .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteNote(note) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create Note Dialog (Manual) ---
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newContent by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf("detailed") }
        var bulletInput by remember { mutableStateOf("") }
        val bulletPointsList = remember { mutableStateListOf<String>() }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Study Slab 🧠✍️", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Slab Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("Main study concept body") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Slab Style:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("detailed", "revision", "quick_revision").forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.uppercase().replace("_", " ")) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Add Quick Highlights (Bullet points):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bulletInput,
                            onValueChange = { bulletInput = it },
                            placeholder = { Text("Add rapid bullet point") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = {
                                if (bulletInput.isNotEmpty()) {
                                    bulletPointsList.add(bulletInput)
                                    bulletInput = ""
                                }
                            }
                        ) {
                            Text("Add")
                        }
                    }
                    bulletPointsList.forEachIndexed { i, pt ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• $pt", fontSize = 11.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { bulletPointsList.removeAt(i) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, "Remove", tint = ComposeColor.Red, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Attach Study Diagram or PDF 📜📂:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    
                    if (uploadedImagePath != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Attachment, "Attached file", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Attached diagram / rendered page fr! ✅",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(onClick = { uploadedImagePath = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Remove attachment", tint = ComposeColor.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = { createDialogFilePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.UploadFile, "Upload File")
                            Spacer(Modifier.width(8.dp))
                            Text("Upload Study PDF or Diagram fr 📂", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotEmpty() && newContent.isNotEmpty()) {
                            viewModel.saveNote(
                                title = newTitle,
                                content = newContent,
                                type = selectedType,
                                bullets = bulletPointsList.toList(),
                                imagePath = uploadedImagePath
                            )
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Slay & Save 💅")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Create AI Note Dialog ---
    if (showAiDialog) {
        var topicInput by remember { mutableStateOf("") }
        var selectedStyle by remember { mutableStateOf("detailed") }

        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, "AI", tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Study Generator 🪄", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tell our AI Study Slay bot what you need to study, and we will generate fully detailed notes, bullet points, and mock material for you fr!",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        placeholder = { Text("e.g. Photosynthesis, Newton's Laws, Civil War") },
                        label = { Text("Study Topic / Text chunk") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Notes Vibe Format:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("detailed", "revision", "quick_revision").forEach { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { selectedStyle = style },
                                label = { Text(style.replace("_", " ").uppercase()) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (topicInput.isNotEmpty()) {
                            viewModel.generateAiNotesAndStudyPack(topicInput, selectedStyle)
                            showAiDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Generate Slab ✨")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- Note Detail and Edit View ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailView(
    note: StudyNote,
    viewModel: MainAppViewModel,
    onBack: () -> Unit,
    onGenerateQuiz: () -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf(note.title) }
    var noteContent by remember { mutableStateOf(note.content) }
    
    // Charts config
    var chartType by remember { mutableStateOf(note.chartType) }
    val chartPoints = remember { mutableStateListOf<Pair<String, Float>>() }
    var labelInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }

    // Flowchart config
    val flowchartNodes = remember { mutableStateListOf<String>() }
    var nodeInput by remember { mutableStateOf("") }

    // Drawing Canvas
    var showDoodlePad by remember { mutableStateOf(false) }
    var localImagePath by remember { mutableStateOf(note.imagePath) }

    // Interactive tab selection
    var activeTab by remember { mutableStateOf("Slab Study 📖") }

    // Term Tooltip states
    var selectedTooltipTerm by remember { mutableStateOf<String?>(null) }
    var selectedTooltipDef by remember { mutableStateOf<String?>(null) }

    // Interactive video player simulation states
    var videoPlaying by remember { mutableStateOf(false) }
    var videoTime by remember { mutableStateOf(0) }
    var annotationInput by remember { mutableStateOf("") }

    // Diagram / Image spot pin states
    var pinCaptionInput by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var clickedPinX by remember { mutableStateOf(0f) }
    var clickedPinY by remember { mutableStateOf(0f) }
    var selectedPinCaption by remember { mutableStateOf<String?>(null) }

    // Group collab simulation states
    var collabActive by remember { mutableStateOf(note.groupSessionActive) }
    var collabMessageInput by remember { mutableStateOf("") }

    // Parsing initial data JSONs safely
    val parsedPins = remember(note.imageAnnotationsJson) {
        val list = mutableStateListOf<ImageAnnotationPin>()
        try {
            if (!note.imageAnnotationsJson.isNullOrEmpty()) {
                val arr = JSONArray(note.imageAnnotationsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(ImageAnnotationPin(
                        xPercent = obj.getDouble("x").toFloat(),
                        yPercent = obj.getDouble("y").toFloat(),
                        text = obj.getString("text")
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    val parsedVideoAnnotations = remember(note.videoAnnotationsJson) {
        val list = mutableStateListOf<VideoAnnotation>()
        try {
            if (!note.videoAnnotationsJson.isNullOrEmpty()) {
                val arr = JSONArray(note.videoAnnotationsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(VideoAnnotation(
                        timeSecs = obj.getInt("time"),
                        text = obj.getString("text")
                    ))
                }
            } else {
                // Seed defaults
                list.add(VideoAnnotation(5, "Intro: Slay this topic today fr! 💅"))
                list.add(VideoAnnotation(15, "Concept alert: This portion is on the test! 🧠"))
                list.add(VideoAnnotation(30, "Formula alert: Commit this to memory! ⚡"))
                list.add(VideoAnnotation(45, "Mock exam tip: Absolute core definition here! 🔥"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    val parsedCollabAnnotations = remember(note.collabAnnotationsJson) {
        val list = mutableStateListOf<CollabComment>()
        try {
            if (!note.collabAnnotationsJson.isNullOrEmpty()) {
                val arr = JSONArray(note.collabAnnotationsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(CollabComment(
                        user = obj.getString("user"),
                        text = obj.getString("text")
                    ))
                }
            } else {
                list.add(CollabComment("SlayQueen99", "Mitochondria definition is so real fr! 🔥"))
                list.add(CollabComment("SkibidiTutor", "Remember to highlight the formulas guys!"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    val bulletsList = remember(note.bulletPointsJson) {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(note.bulletPointsJson)
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // Load initial chart and flowchart data from serialized fields
    LaunchedEffect(note) {
        try {
            chartPoints.clear()
            val array = JSONArray(note.chartDataJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                chartPoints.add(obj.getString("label") to obj.getDouble("value").toFloat())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            flowchartNodes.clear()
            val array = JSONArray(note.flowchartNodesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                flowchartNodes.add(obj.getString("text"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Video Playback Loop Simulation
    LaunchedEffect(videoPlaying) {
        if (videoPlaying) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                videoTime = (videoTime + 1) % 61
            }
        }
    }

    // Active Group Collaboration Simulation Comments Generator
    LaunchedEffect(collabActive) {
        if (collabActive == true) {
            val names = listOf("SlayQueen99 👑", "SkibidiTutor 🎓", "Brainiac_Jay 🧠", "RizzMaster99 🔥")
            val commentPool = listOf(
                "OMGG, this topic is actually simple when we look at it this way fr! 💅",
                "Bro, write this down, it is high-key going to be on the mock test, no cap! 🧠",
                "Wait, is photosynthesis only for plants or do some algae slay it too? 🌿✨",
                "Lock in, guys! We are so close to finishing this slab!",
                "Who is ready to smash the mock test after this? 📈",
                "Mitochondria is the absolute boss of cellular respiration 🔋"
            )
            while (true) {
                kotlinx.coroutines.delay(7000) // post comment every 7s
                val randomUser = names.random()
                val randomText = commentPool.random()
                parsedCollabAnnotations.add(CollabComment(randomUser, randomText))
                // Keep max 10
                if (parsedCollabAnnotations.size > 10) {
                    parsedCollabAnnotations.removeAt(0)
                }
            }
        }
    }

    // Camera Launcher to snap photos
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.saveDoodleImage(bitmap) { savedPath ->
                localImagePath = savedPath
                viewModel.saveNote(
                    id = note.id,
                    title = noteTitle,
                    content = noteContent,
                    type = note.noteType,
                    bullets = emptyList(), // keeps original
                    chartType = chartType,
                    chartData = chartPoints.toList(),
                    flowchartNodes = flowchartNodes.toList(),
                    imagePath = savedPath
                )
            }
        }
    }

    // PDF and Image File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val isPdf = mimeType.contains("pdf", ignoreCase = true) || uri.path?.endsWith(".pdf", ignoreCase = true) == true
            
            if (isPdf) {
                val bitmap = renderPdfFirstPageToBitmap(context, uri)
                if (bitmap != null) {
                    viewModel.saveDoodleImage(bitmap) { savedPath ->
                        localImagePath = savedPath
                        viewModel.saveNote(
                            id = note.id,
                            title = noteTitle,
                            content = noteContent,
                            type = note.noteType,
                            bullets = emptyList(),
                            chartType = chartType,
                            chartData = chartPoints.toList(),
                            flowchartNodes = flowchartNodes.toList(),
                            imagePath = savedPath
                        )
                        Toast.makeText(context, "Slayed! PDF first page uploaded fr! 📜✨", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Could not render PDF 🥺", Toast.LENGTH_SHORT).show()
                }
            } else {
                val bitmap = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                } catch (e: Exception) {
                    null
                }

                if (bitmap != null) {
                    viewModel.saveDoodleImage(bitmap) { savedPath ->
                        localImagePath = savedPath
                        viewModel.saveNote(
                            id = note.id,
                            title = noteTitle,
                            content = noteContent,
                            type = note.noteType,
                            bullets = emptyList(),
                            chartType = chartType,
                            chartData = chartPoints.toList(),
                            flowchartNodes = flowchartNodes.toList(),
                            imagePath = savedPath
                        )
                        Toast.makeText(context, "Image uploaded successfully fr fr! 🖼️🔥", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Could not load image 🥺", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Slab: Details", maxLines = 1, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        PdfExportHelper.exportNoteToPdf(context, note) { pdfFile ->
                            PdfExportHelper.sharePdfFile(context, pdfFile)
                        }
                    }) {
                        Icon(Icons.Default.Download, "Download PDF")
                    }
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, "Edit")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Slab Title") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_note_title")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("Slab Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("edit_note_content")
                )
                Spacer(Modifier.height(12.dp))

                // --- Chart Designer ---
                Text("Design Chart 📊", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("none", "bar", "pie", "line").forEach { cType ->
                        FilterChip(
                            selected = chartType == cType,
                            onClick = { chartType = cType },
                            label = { Text(cType.uppercase()) }
                        )
                    }
                }
                if (chartType != "none") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = labelInput,
                            onValueChange = { labelInput = it },
                            placeholder = { Text("Label") },
                            modifier = Modifier.weight(1.5f)
                        )
                        OutlinedTextField(
                            value = valueInput,
                            onValueChange = { valueInput = it },
                            placeholder = { Text("Val %") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val value = valueInput.toFloatOrNull() ?: 0f
                                if (labelInput.isNotEmpty()) {
                                    chartPoints.add(labelInput to value)
                                    labelInput = ""
                                    valueInput = ""
                                }
                            }
                        ) {
                            Text("+")
                        }
                    }
                    // List current points
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        chartPoints.forEachIndexed { i, (l, v) ->
                            AssistChip(
                                onClick = { chartPoints.removeAt(i) },
                                label = { Text("$l: $v%") },
                                trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // --- Flowchart Designer ---
                Text("Design Flowchart map 🗺️", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = nodeInput,
                        onValueChange = { nodeInput = it },
                        placeholder = { Text("Next connection step") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            if (nodeInput.isNotEmpty()) {
                                flowchartNodes.add(nodeInput)
                                nodeInput = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
                flowchartNodes.forEachIndexed { i, nodeText ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. $nodeText", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { flowchartNodes.removeAt(i) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Remove", tint = ComposeColor.Red, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.saveNote(
                            id = note.id,
                            title = noteTitle,
                            content = noteContent,
                            type = note.noteType,
                            bullets = emptyList(),
                            chartType = chartType,
                            chartData = chartPoints.toList(),
                            flowchartNodes = flowchartNodes.toList(),
                            imagePath = localImagePath
                        )
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes 💾")
                }
            } else {
                // Interactive Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(3.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Slab Study 📖", "Explanatory Video 📹", "Group Collab 💅🌴").forEach { tab ->
                        val isSelected = activeTab == tab
                        Button(
                            onClick = { activeTab = tab },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else ComposeColor.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Text(tab, fontSize = 10.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                    }
                }

                if (activeTab == "Slab Study 📖") {
                    // Read Only View
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    note.noteType.uppercase().replace("_", " "),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                                Text(
                                    "Locked In 🔒",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                noteTitle,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(12.dp))
                            // Interactive note body with tooltips!
                            InteractiveNoteBody(noteContent) { term, definition ->
                                selectedTooltipTerm = term
                                selectedTooltipDef = definition
                            }
                        }
                    }

                    // Floating Tooltip card display
                    selectedTooltipTerm?.let { term ->
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            border = BorderStroke(3.dp, MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedTooltipTerm = null
                                selectedTooltipDef = null
                            }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HelpCenter, "Tooltip explanation", tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Slay Study Tooltip: ${term.uppercase()} 🔮📖",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    selectedTooltipDef ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap this card to close fr fr!",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Render Bullet highlights
                    if (bulletsList.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("High-Key Points fr 📌", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        bulletsList.forEach { pointText ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PushPin, "Pin", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(pointText, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Render Flowchart
                    if (flowchartNodes.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Concept Map Flowchart 🗺️🤖", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            flowchartNodes.forEachIndexed { i, text ->
                                Card(
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            "${i + 1}. $text",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                if (i < flowchartNodes.size - 1) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        "flows",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Render Chart
                    if (chartPoints.isNotEmpty() && chartType != "none") {
                        Spacer(Modifier.height(16.dp))
                        Text("Interactive Data Graph 📊", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        
                        // Live rendered custom bar chart on Canvas!
                        Card(
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                val barWidth = 60f
                                val gap = 40f
                                val maxVal = chartPoints.maxOf { it.second }.coerceAtLeast(1f)
                                val canvasHeight = size.height - 40f
                                val canvasWidth = size.width

                                // Draw baseline
                                drawLine(
                                    color = ComposeColor.Gray,
                                    start = Offset(20f, size.height - 20f),
                                    end = Offset(canvasWidth - 20f, size.height - 20f),
                                    strokeWidth = 3f
                                )

                                var xOffset = 40f
                                chartPoints.forEach { (label, value) ->
                                    val barHeight = (value / maxVal) * canvasHeight
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(NeonPurple, CyberBlue)
                                        ),
                                        topLeft = Offset(xOffset, size.height - 20f - barHeight),
                                        size = Size(barWidth, barHeight)
                                    )
                                    // Draw bar value label
                                    drawContext.canvas.nativeCanvas.drawText(
                                        "$value%",
                                        xOffset + 5f,
                                        size.height - 25f - barHeight,
                                        AndroidPaint().apply {
                                            color = android.graphics.Color.DKGRAY
                                            textSize = 24f
                                            isAntiAlias = true
                                            typeface = Typeface.DEFAULT_BOLD
                                        }
                                    )
                                    // Draw bottom label
                                    drawContext.canvas.nativeCanvas.drawText(
                                        label,
                                        xOffset,
                                        size.height - 2f,
                                        AndroidPaint().apply {
                                            color = android.graphics.Color.GRAY
                                            textSize = 24f
                                            isAntiAlias = true
                                        }
                                    )
                                    xOffset += barWidth + gap
                                }
                            }
                        }
                    }

                    // Render Pictures or drawing with coordinate annotations pinning!
                    if (localImagePath != null) {
                        Spacer(Modifier.height(16.dp))
                        Text("Slab Diagram 🎨 (Tap diagram to pin captions fr! 📍)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        clickedPinX = (offset.x / size.width) * 100f
                                        clickedPinY = (offset.y / size.height) * 100f
                                        pinCaptionInput = ""
                                        showPinDialog = true
                                    }
                                }
                        ) {
                            AsyncImage(
                                model = File(localImagePath!!),
                                contentDescription = "Note illustration image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Render each saved pin
                            parsedPins.forEach { pin ->
                                val xOffset = (pin.xPercent / 100f)
                                val yOffset = (pin.yPercent / 100f)

                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val absX = (xOffset * maxWidth.value).dp
                                    val absY = (yOffset * maxHeight.value).dp

                                    Box(
                                        modifier = Modifier
                                            .absoluteOffset(x = absX - 16.dp, y = absY - 16.dp)
                                            .size(32.dp)
                                            .background(ComposeColor.Yellow, RoundedCornerShape(16.dp))
                                            .border(2.dp, ComposeColor.Black, RoundedCornerShape(16.dp))
                                            .clickable {
                                                selectedPinCaption = pin.text
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            "Pin Spot",
                                            tint = ComposeColor.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Selected pin caption overlay card
                        selectedPinCaption?.let { caption ->
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedPinCaption = null }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, "Pin Detail", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Spot Annotation Pin 📍", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(caption, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Dismiss", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                } else if (activeTab == "Explanatory Video 📹") {
                    // Explanatory Video Player Container
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Pulsing neon-green header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (videoPlaying) ComposeColor.Green else ComposeColor.Red,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (videoPlaying) "🔴 LIVE STREAMING STUDY LESSON" else "⏸️ STUDY LESSON PAUSED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (videoPlaying) MaterialTheme.colorScheme.primary else ComposeColor.Gray
                                )
                            }
                            Spacer(Modifier.height(12.dp))

                            // Simulated player screen
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(ComposeColor.Black, RoundedCornerShape(16.dp))
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        if (videoPlaying) Icons.Default.PlayCircleFilled else Icons.Default.PauseCircle,
                                        contentDescription = "Simulated Video Player Screen",
                                        tint = if (videoPlaying) NeonPurple else ComposeColor.White,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Concept: $noteTitle",
                                        color = ComposeColor.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Simulated Lesson Duration: 0:${videoTime.toString().padStart(2, '0')} / 1:00",
                                        color = ComposeColor.Gray,
                                        fontSize = 11.sp
                                    )
                                }

                                // Glowing active annotation alert overlay
                                val activeVideoAnn = parsedVideoAnnotations.firstOrNull {
                                    videoTime in it.timeSecs until (it.timeSecs + 5)
                                }
                                if (activeVideoAnn != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            "TUTOR FR: ${activeVideoAnn.text}",
                                            color = MaterialTheme.colorScheme.onTertiary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Custom progress bar scrub indicator
                            LinearProgressIndicator(
                                progress = { videoTime.toFloat() / 60f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(Modifier.height(12.dp))

                            // Playback controls row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { videoPlaying = !videoPlaying },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        if (videoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause"
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (videoPlaying) "Pause fr" else "Slay Play")
                                }

                                Button(
                                    onClick = { videoTime = 0 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ComposeColor.Transparent,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                ) {
                                    Text("Re-slam 🔄")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Timeline Annotations list
                    Text("Tutor Video Timeline Annotations 📹📍", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    parsedVideoAnnotations.forEach { ann ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            onClick = { videoTime = ann.timeSecs } // Seek directly to time
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "0:${ann.timeSecs.toString().padStart(2, '0')}",
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(ann.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowForwardIos, "Seek", modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Field to append custom video timeline annotations
                    Text("Post custom timeline annotation fr @ current time 📍", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = annotationInput,
                            onValueChange = { annotationInput = it },
                            placeholder = { Text("Add custom note details here...") },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (annotationInput.isNotEmpty()) {
                                    val newAnn = VideoAnnotation(videoTime, annotationInput)
                                    parsedVideoAnnotations.add(newAnn)
                                    parsedVideoAnnotations.sortBy { it.timeSecs }
                                    annotationInput = ""

                                    // Save to database
                                    val jsonArr = JSONArray()
                                    parsedVideoAnnotations.forEach { a ->
                                        val o = JSONObject()
                                        o.put("time", a.timeSecs)
                                        o.put("text", a.text)
                                        jsonArr.put(o)
                                    }
                                    viewModel.updateInteractiveNote(note.id, videoAnnotationsJson = jsonArr.toString())
                                }
                            }
                        ) {
                            Text("Pin")
                        }
                    }
                } else if (activeTab == "Group Collab 💅🌴") {
                    // Group Collab Studio Room
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.secondary),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "STUDY SLAY GROUP ROOM 💅✨",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Button(
                                    onClick = {
                                        collabActive = !collabActive
                                        viewModel.updateInteractiveNote(note.id, groupSessionActive = collabActive)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (collabActive == true) ComposeColor.Red else MaterialTheme.colorScheme.secondary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(if (collabActive == true) "Leave Room" else "Join Room fr", fontSize = 10.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Participant bubbles
                            Text("Slayers Active Now:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ComposeColor.Gray)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val users = listOf(
                                    "YOU 🦁" to true,
                                    "SlayQueen99 💅" to (collabActive == true),
                                    "SkibidiTutor 🎓" to (collabActive == true),
                                    "RizzMaster99 ⚡" to (collabActive == true)
                                )
                                users.forEach { (usr, active) ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (active) MaterialTheme.colorScheme.secondaryContainer else ComposeColor.LightGray.copy(alpha = 0.4f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (active) MaterialTheme.colorScheme.secondary else ComposeColor.Gray,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(usr, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Chat comments list
                            Text("Group Collab Notes Annotation Feed:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (parsedCollabAnnotations.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No annotations in feed. Speak up fr! 📢", fontSize = 11.sp, color = ComposeColor.Gray)
                                    }
                                } else {
                                    parsedCollabAnnotations.forEach { comment ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(comment.user, fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text("Just now fr", fontSize = 8.sp, color = ComposeColor.Gray)
                                                }
                                                Spacer(Modifier.height(2.dp))
                                                Text(comment.text, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Send custom comments to the feed
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = collabMessageInput,
                                    onValueChange = { collabMessageInput = it },
                                    placeholder = { Text("Comment on these study notes fr...") },
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (collabMessageInput.isNotEmpty()) {
                                            val myComment = CollabComment("YOU (Slayer) ✨", collabMessageInput)
                                            parsedCollabAnnotations.add(myComment)
                                            collabMessageInput = ""

                                            // Save to database
                                            val jsonArr = JSONArray()
                                            parsedCollabAnnotations.forEach { c ->
                                                val o = JSONObject()
                                                o.put("user", c.user)
                                                o.put("text", c.text)
                                                jsonArr.put(o)
                                            }
                                            viewModel.updateInteractiveNote(note.id, collabAnnotationsJson = jsonArr.toString())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Post", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                // Visual buttons for picture capture, pdf/image upload, and custom doodle canvas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, "Camera", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Capture 📸", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, "Upload", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Upload PDF/Img 📂", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { showDoodlePad = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Gesture, "Doodle", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Doodle 🎨", fontSize = 10.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onGenerateQuiz,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.AutoAwesome, "AI Generation")
                    Spacer(Modifier.width(6.dp))
                    Text("Generate Quiz with AI 🪄🔥", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Label This Diagram Spot 📍", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pinCaptionInput,
                    onValueChange = { pinCaptionInput = it },
                    placeholder = { Text("e.g. Mitochondria Powerhouse, Solar absorption cell...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinCaptionInput.isNotEmpty()) {
                            val newPin = ImageAnnotationPin(clickedPinX, clickedPinY, pinCaptionInput)
                            parsedPins.add(newPin)
                            
                            // Save to database
                            val jsonArr = JSONArray()
                            parsedPins.forEach { p ->
                                val o = JSONObject()
                                o.put("x", p.xPercent.toDouble())
                                o.put("y", p.yPercent.toDouble())
                                o.put("text", p.text)
                                jsonArr.put(o)
                            }
                            viewModel.updateInteractiveNote(note.id, imageAnnotationsJson = jsonArr.toString())
                        }
                        showPinDialog = false
                    }
                ) {
                    Text("Slay Pin 📌")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Doodle Pad Drawer Canvas (Signature Style) ---
    if (showDoodlePad) {
        var doodleBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val points = remember { mutableStateListOf<Offset>() }

        AlertDialog(
            onDismissRequest = { showDoodlePad = false },
            title = { Text("Draw Sketch 🎨🖌️", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .background(ComposeColor.White, RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    points.add(change.position)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (i in 0 until points.size - 1) {
                                drawLine(
                                    color = NeonPurple,
                                    start = points[i],
                                    end = points[i+1],
                                    strokeWidth = 8f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { points.clear() }) {
                            Text("Clear", color = ComposeColor.Red)
                        }
                        TextButton(onClick = {
                            // Render path directly to bitmap to save physically
                            val bmp = Bitmap.createBitmap(280, 280, Bitmap.Config.ARGB_8888)
                            val cvs = Canvas(bmp)
                            cvs.drawColor(android.graphics.Color.WHITE)
                            val paint = AndroidPaint().apply {
                                color = android.graphics.Color.parseColor("#9F5FFF")
                                strokeWidth = 8f
                                style = AndroidPaint.Style.STROKE
                                isAntiAlias = true
                                strokeCap = AndroidPaint.Cap.ROUND
                            }
                            val path = AndroidPath()
                            if (points.isNotEmpty()) {
                                path.moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    path.lineTo(points[i].x, points[i].y)
                                }
                                cvs.drawPath(path, paint)
                            }
                            doodleBitmap = bmp
                        }) {
                            Text("Lock Drawing 🔒")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        doodleBitmap?.let { bmp ->
                            viewModel.saveDoodleImage(bmp) { savedPath ->
                                localImagePath = savedPath
                                viewModel.saveNote(
                                    id = note.id,
                                    title = noteTitle,
                                    content = noteContent,
                                    type = note.noteType,
                                    bullets = emptyList(),
                                    chartType = chartType,
                                    chartData = chartPoints.toList(),
                                    flowchartNodes = flowchartNodes.toList(),
                                    imagePath = savedPath
                                )
                            }
                        }
                        showDoodlePad = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Save to Slab 💾")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDoodlePad = false }) { Text("Cancel") }
            }
        )
    }
}

fun renderPdfFirstPageToBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        val pdfRenderer = PdfRenderer(fileDescriptor)
        if (pdfRenderer.pageCount > 0) {
            val page = pdfRenderer.openPage(0)
            val scale = 2f
            val width = (page.width * scale).toInt()
            val height = (page.height * scale).toInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pdfRenderer.close()
            fileDescriptor.close()
            bitmap
        } else {
            pdfRenderer.close()
            fileDescriptor.close()
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

