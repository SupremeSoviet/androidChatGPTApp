package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.ChatAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

suspend fun getAssistantReplyFromProxyApi(
    inputText: String,
    apiKey: String,
    onChunkReceived: suspend (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()

        val messagesArray = JSONArray()
        val userMessage = JSONObject().apply {
            put("role", "user")
            put("content", inputText)
        }
        messagesArray.put(userMessage)

        val json = JSONObject()
        json.put("model", "gpt-5-nano")
        json.put("messages", messagesArray)
        json.put("stream", true)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.proxyapi.ru/openai/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Accept", "text/event-stream")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("StreamError", "Code: ${response.code}")
                    return@withContext
                }

                val source = response.body?.source() ?: return@withContext

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break

                    if (line.startsWith("data:")) {
                        val data = line.substringAfter("data:").trim()
                        if (data == "[DONE]") break
                        try {
                            val jsonObj = JSONObject(data)
                            val choices = jsonObj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val content = delta?.optString("content")
                                if (!content.isNullOrEmpty()) {
                                    onChunkReceived(content)
                                }
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkError", "Connection error", e)
            onChunkReceived("\n[Ошибка сети: ${e.localizedMessage}]")
        }
    }
}
suspend fun generateChatTitle(userMessage: String, apiKey: String): String {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", "Summarize this message into a short title (max 4 words). Do not use quotes.\n\nMessage: $userMessage")
        })

        val json = JSONObject()
        json.put("model", "gpt-5-nano")
        json.put("messages", messagesArray)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.proxyapi.ru/openai/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "New Chat"
                val text = response.body?.string().orEmpty()
                val obj = JSONObject(text)
                val choices = obj.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrEmpty()) {
                        return@withContext content.trim().removeSurrounding("\"")
                    }
                }
                "New Chat"
            }
        } catch (e: Exception) {
            "New Chat"
        }
    }
}

enum class AppScreen {
    Chat,
    Library,
    Settings
}

enum class AppThemeMode {
    Light,
    Dark
}

enum class ChatModel(val displayName: String) {
    GPT5("GPT-5"),
    Sonnet45("Sonnet 4.5"),
    Grok4("Grok 4"),
    GigaChat("GigaChat"),
    YandexGPT("YandexGPT")
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

data class ChatSession(
    val id: Int,
    val messages: List<ChatMessage>,
    var title: String = "New Chat"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onMenuClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
            }
        }
    )
}

@Composable
fun AppDrawer(
    currentRoute: String,
    onDestinationSelected: (String) -> Unit
) {
    val screens = listOf(AppScreen.Chat, AppScreen.Library, AppScreen.Settings)
    ModalDrawerSheet {
        Text(
            text = "Menu",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            style = MaterialTheme.typography.titleMedium
        )
        screens.forEach { screen ->
            NavigationDrawerItem(
                label = { Text(text = screenTitleForRoute(screen.name)) },
                selected = currentRoute == screen.name,
                onClick = { onDestinationSelected(screen.name) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun ChatScreen(
    chatSession: ChatSession,
    selectedModel: ChatModel,
    onSendMessage: (String) -> Unit,
    onStartNewChat: () -> Unit
) {
    var messageText by rememberSaveable(chatSession.id) { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Chat ${chatSession.id}", fontWeight = FontWeight.SemiBold)
        Text(text = "Model: ${selectedModel.displayName}")
        Button(onClick = onStartNewChat) {
            Text(text = "Start New Chat")
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(
                items = chatSession.messages,
                key = { it.id }
            ) { message ->
                val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                val containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = alignment
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier.padding(12.dp),
                            textAlign = if (message.isUser) TextAlign.End else TextAlign.Start
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Message") }
            )
            Button(
                onClick = {
                    onSendMessage(messageText)
                    messageText = ""
                },
                enabled = messageText.isNotBlank()
            ) {
                Text(text = "Send")
            }
        }
    }
}

@Composable
fun LibraryScreen(
    chats: List<ChatSession>,
    activeChatId: Int,
    onChatSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Library", fontWeight = FontWeight.Bold)
        if (chats.isEmpty()) {
            Text(text = "No saved chats")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chats) { chat ->
                    val containerColor =
                        if (chat.id == activeChatId) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    Card(
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatSelected(chat.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = chat.title, fontWeight = FontWeight.SemiBold)
                            Text(text = "${chat.messages.size} messages")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    selectedModel: ChatModel,
    onModelChange: (ChatModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", fontWeight = FontWeight.Bold)
        Text(text = "Theme")
        ThemeSelectionRow(
            currentTheme = themeMode,
            onThemeModeChange = onThemeModeChange
        )
        Text(text = "Model")
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ChatModel.values()) { model ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModelChange(model) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (model == selectedModel) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = model == selectedModel,
                            onClick = { onModelChange(model) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = model.displayName)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionRow(
    currentTheme: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemeChoiceButton(
            text = "Light",
            selected = currentTheme == AppThemeMode.Light,
            onClick = { onThemeModeChange(AppThemeMode.Light) }
        )
        ThemeChoiceButton(
            text = "Dark",
            selected = currentTheme == AppThemeMode.Dark,
            onClick = { onThemeModeChange(AppThemeMode.Dark) }
        )
    }
}

@Composable
fun ThemeChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick) {
            Text(text = text)
        }
    } else {
        TextButton(onClick = onClick) {
            Text(text = text)
        }
    }
}

fun screenTitleForRoute(route: String): String {
    return when (route) {
        AppScreen.Chat.name -> "New Chat"
        AppScreen.Library.name -> "Library"
        AppScreen.Settings.name -> "Settings"
        else -> "New Chat"
    }
}

private fun saveChatsToPrefs(context: Context, chats: List<ChatSession>) {
    val sharedPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    val editor = sharedPrefs.edit()
    val gson = Gson()
    val json = gson.toJson(chats)
    editor.putString("chats_data", json)
    editor.apply()
}

private fun getChatsFromPrefs(context: Context): List<ChatSession> {
    val sharedPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    val json = sharedPrefs.getString("chats_data", null)
    return if (json != null) {
        val gson = Gson()
        val type = object : TypeToken<List<ChatSession>>() {}.type
        gson.fromJson(json, type)
    } else {
        emptyList()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loadedChats = getChatsFromPrefs(this)
        val initialChat = if (loadedChats.isNotEmpty()) loadedChats.first() else ChatSession(id = 1, messages = emptyList())
        val initialNextId = if (loadedChats.isNotEmpty()) (loadedChats.maxOfOrNull { it.id } ?: 0) + 1 else 2

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(AppThemeMode.Light) }
            var selectedModel by rememberSaveable { mutableStateOf(ChatModel.GPT5) }
            var chats by remember { mutableStateOf(loadedChats) }
            var currentChat by remember { mutableStateOf(initialChat) }
            var nextChatId by remember { mutableStateOf(initialNextId) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: AppScreen.Chat.name

            fun saveChat(updatedChat: ChatSession) {
                val index = chats.indexOfFirst { it.id == updatedChat.id }
                chats = if (index == -1) {
                    chats + updatedChat
                } else {
                    chats.toMutableList().also { it[index] = updatedChat }
                }
                saveChatsToPrefs(this@MainActivity, chats)
            }

            fun handleSendMessage(text: String) {
                if (text.isBlank()) return
                val cleanedText = text.trim()

                val userMsgId = System.currentTimeMillis()
                val userMessage = ChatMessage(userMsgId, cleanedText, true)

                val assistantMsgId = userMsgId + 1
                val assistantMessage = ChatMessage(assistantMsgId, "", false)

                val newMessages = currentChat.messages + userMessage + assistantMessage
                var updatedChat = currentChat.copy(messages = newMessages)
                currentChat = updatedChat
                saveChat(updatedChat)

                scope.launch {
                    var accumulatedText = ""
                    getAssistantReplyFromProxyApi(cleanedText, ApiConfig.proxyApiKey) { chunk ->
                        withContext(Dispatchers.Main) {
                            accumulatedText += chunk
                            val currentMessages = currentChat.messages.toMutableList()
                            val msgIndex = currentMessages.indexOfFirst { it.id == assistantMsgId }
                            if (msgIndex != -1) {
                                currentMessages[msgIndex] = currentMessages[msgIndex].copy(text = accumulatedText)
                                currentChat = currentChat.copy(messages = currentMessages)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        saveChat(currentChat)
                        if (currentChat.messages.size <= 2) {
                            launch(Dispatchers.IO) {
                                val newTitle = generateChatTitle(cleanedText, ApiConfig.proxyApiKey)
                                withContext(Dispatchers.Main) {
                                    val titledChat = currentChat.copy(title = newTitle)
                                    currentChat = titledChat
                                    saveChat(titledChat)
                                }
                            }
                        }
                    }
                }
            }

            ChatAppTheme(themeMode = themeMode) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            currentRoute = currentRoute,
                            onDestinationSelected = { route ->
                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            AppTopBar(
                                title = screenTitleForRoute(currentRoute),
                                onMenuClick = {
                                    scope.launch { drawerState.open() }
                                }
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = AppScreen.Chat.name,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(AppScreen.Chat.name) {
                                ChatScreen(
                                    chatSession = currentChat,
                                    selectedModel = selectedModel,
                                    onSendMessage = { text -> handleSendMessage(text) },
                                    onStartNewChat = {
                                        val newChat = ChatSession(id = nextChatId, messages = emptyList())
                                        chats = chats + newChat
                                        currentChat = newChat
                                        nextChatId += 1
                                    }
                                )
                            }
                            composable(AppScreen.Library.name) {
                                LibraryScreen(
                                    chats = chats,
                                    activeChatId = currentChat.id,
                                    onChatSelected = { chatId ->
                                        val selectedChat = chats.find { it.id == chatId }
                                        if (selectedChat != null) {
                                            currentChat = selectedChat
                                            navController.navigate(AppScreen.Chat.name) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                            composable(AppScreen.Settings.name) {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    onThemeModeChange = { themeMode = it },
                                    selectedModel = selectedModel,
                                    onModelChange = { selectedModel = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}