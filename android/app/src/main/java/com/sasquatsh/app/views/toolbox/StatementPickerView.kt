package com.sasquatsh.app.views.toolbox

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sasquatsh.app.BuildConfig
import com.sasquatsh.app.views.shared.D20SpinnerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

private val fallbackStatements = listOf(
    "Whoever last held the door for someone goes first",
    "Whoever last tried a new food goes first",
    "Whoever last took a walk outside goes first",
    "Whoever last read a book goes first",
    "Whoever last cooked a full meal goes first",
    "Whoever last made plans and kept them goes first",
    "Whoever last took a risk goes first",
    "Whoever last helped a stranger goes first",
    "Whoever last learned something new goes first",
    "Whoever last called a friend goes first"
)

private val StatementGreen = Color(0xFF2D5A3D)

private sealed class StatementsState {
    data object Loading : StatementsState()
    data class Loaded(val statements: List<String>) : StatementsState()
    data class Error(val message: String) : StatementsState()
}

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

private suspend fun fetchRandomStatement(): String? = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_FUNCTIONS_URL}/first-player/random")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val body = response.body?.string() ?: return@withContext null
        val json = JSONObject(body)
        val statement = json.optJSONObject("statement")
        statement?.optString("statement")
    } catch (_: Exception) {
        null
    }
}

private suspend fun fetchThreeStatements(): List<String> = withContext(Dispatchers.IO) {
    try {
        val results = (1..3).map {
            async { fetchRandomStatement() }
        }.awaitAll()

        val fetched = results.filterNotNull().distinct()
        if (fetched.size >= 3) {
            fetched.take(3)
        } else {
            // Fill in with fallback statements if we didn't get enough
            val needed = 3 - fetched.size
            val available = fallbackStatements.filter { it !in fetched }.shuffled()
            fetched + available.take(needed)
        }
    } catch (_: Exception) {
        fallbackStatements.shuffled().take(3)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementPickerView(onBack: () -> Unit = {}) {
    var state by remember { mutableStateOf<StatementsState>(StatementsState.Loading) }
    var fetchTrigger by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Text-to-speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    // Fetch statements
    LaunchedEffect(fetchTrigger) {
        state = StatementsState.Loading
        val statements = fetchThreeStatements()
        state = if (statements.size == 3) {
            StatementsState.Loaded(statements)
        } else {
            StatementsState.Error("Could not load statements. Please try again.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Whoever Last Picker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            val currentState = state
            if (currentState is StatementsState.Loaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { fetchTrigger++ },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatementGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Pick Again",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            if (ttsReady) {
                                val engine = tts ?: return@FilledIconButton
                                engine.language = Locale.US
                                val allText = currentState.statements
                                    .mapIndexed { index, s -> "Number ${index + 1}. $s" }
                                    .joinToString(". ")
                                engine.speak(allText, TextToSpeech.QUEUE_FLUSH, null, "statements")
                            }
                        },
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = StatementGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read aloud",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val currentState = state) {
                is StatementsState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        D20SpinnerView(size = 48.dp, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Who goes first?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Loading 3 random prompts...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is StatementsState.Loaded -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Who goes first?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        currentState.statements.forEachIndexed { index, statement ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                StatementCard(
                                    number = index + 1,
                                    statement = statement
                                )
                            }
                        }
                    }
                }

                is StatementsState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            currentState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { fetchTrigger++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatementGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Try Again", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementCard(number: Int, statement: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Green numbered circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(StatementGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$number",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = statement,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
