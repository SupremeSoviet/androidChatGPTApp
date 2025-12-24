package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AiMessageBackground
import com.example.myapplication.ui.theme.AppBackground
import com.example.myapplication.ui.theme.InputBackground
import com.example.myapplication.ui.theme.SurfaceWhite
import com.example.myapplication.ui.theme.TextOnYellow
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import com.example.myapplication.ui.theme.UserMessageBackground
import com.example.myapplication.ui.theme.YandexYellow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceWhite,
            titleContentColor = TextPrimary
        )
    )
}

@Composable
fun AppDrawer(
    currentRoute: String,
    onDestinationSelected: (String) -> Unit
) {
    val screens = listOf(AppScreen.Chat, AppScreen.Library, AppScreen.Settings)
    ModalDrawerSheet(
        drawerContainerColor = SurfaceWhite
    ) {
        Text(
            text = "Menu",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        screens.forEach { screen ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = screenTitleForRoute(screen.name),
                        color = TextPrimary
                    )
                },
                selected = currentRoute == screen.name,
                onClick = { onDestinationSelected(screen.name) },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = UserMessageBackground,
                    unselectedContainerColor = Color.Transparent
                )
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
    val listState = rememberLazyListState()

    LaunchedEffect(chatSession.messages.size, chatSession.messages.lastOrNull()?.text) {
        if (chatSession.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatSession.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Header section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = chatSession.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Model: ${selectedModel.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                TextButton(onClick = onStartNewChat) {
                    Text(
                        text = "New Chat",
                        color = YandexYellow,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = chatSession.messages,
                key = { it.id }
            ) { message ->
                val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                val backgroundColor = if (message.isUser) UserMessageBackground else AiMessageBackground

                val shape = if (message.isUser) {
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 4.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 20.dp
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = alignment
                ) {
                    Surface(
                        color = backgroundColor,
                        shape = shape,
                        shadowElevation = if (!message.isUser) 2.dp else 0.dp,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = message.text,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                        )
                    }
                }
            }
        }

        // Input bar
        Surface(
            color = SurfaceWhite,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Message",
                            color = TextSecondary
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = InputBackground,
                        unfocusedContainerColor = InputBackground,
                        disabledContainerColor = InputBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                IconButton(
                    onClick = {
                        onSendMessage(messageText)
                        messageText = ""
                    },
                    enabled = messageText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (messageText.isNotBlank()) YandexYellow else Color.Gray.copy(
                                alpha = 0.3f
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank()) TextOnYellow else Color.Gray
                    )
                }
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
            .background(AppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved chats",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chats) { chat ->
                    val isActive = chat.id == activeChatId
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) UserMessageBackground else SurfaceWhite
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isActive) 0.dp else 2.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatSelected(chat.id) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = chat.title,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${chat.messages.size} messages",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
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
            .background(AppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        ThemeSelectionRow(
            currentTheme = themeMode,
            onThemeModeChange = onThemeModeChange
        )

        Text(
            text = "Model",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

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
                            YandexYellow.copy(alpha = 0.15f)
                        } else {
                            SurfaceWhite
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (model == selectedModel) 0.dp else 2.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = model == selectedModel,
                            onClick = { onModelChange(model) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = YandexYellow,
                                unselectedColor = TextSecondary
                            )
                        )
                        Text(
                            text = model.displayName,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = YandexYellow,
                contentColor = TextOnYellow
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        TextButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                color = TextSecondary
            )
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

fun saveChatsToPrefs(context: Context, chats: List<ChatSession>) {
    val sharedPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    val editor = sharedPrefs.edit()
    val gson = Gson()
    val json = gson.toJson(chats)
    editor.putString("chats_data", json)
    editor.apply()
}

fun getChatsFromPrefs(context: Context): List<ChatSession> {
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