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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AiMessageBackground
import com.example.myapplication.ui.theme.InputBackground
import com.example.myapplication.ui.theme.TextOnYellow
import com.example.myapplication.ui.theme.TextPrimary
import com.example.myapplication.ui.theme.TextSecondary
import com.example.myapplication.ui.theme.UserMessageBackground
import com.example.myapplication.ui.theme.YandexYellow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class AppLanguage {
    English,
    Russian
}

data class AppStrings(
    val menu: String,
    val newChat: String,
    val chatScreenTitle: String,
    val library: String,
    val settings: String,
    val noSavedChats: String,
    val messagePlaceholder: String,
    val generating: String,
    val theme: String,
    val model: String,
    val language: String,
    val light: String,
    val dark: String,
    val deleteChatTitle: String,
    val deleteChatText: String,
    val delete: String,
    val cancel: String,
    val messagesCount: String
)

val EnglishStrings = AppStrings(
    menu = "Menu",
    newChat = "New Chat",
    chatScreenTitle = "Chat",
    library = "Library",
    settings = "Settings",
    noSavedChats = "No saved chats",
    messagePlaceholder = "Message",
    generating = "Generating...",
    theme = "Theme",
    model = "Model",
    language = "Language",
    light = "Light",
    dark = "Dark",
    deleteChatTitle = "Delete Chat?",
    deleteChatText = "Are you sure you want to delete this chat? This action cannot be undone.",
    delete = "Delete",
    cancel = "Cancel",
    messagesCount = "messages"
)

val RussianStrings = AppStrings(
    menu = "Меню",
    newChat = "Новый чат",
    chatScreenTitle = "Чат",
    library = "Библиотека",
    settings = "Настройки",
    noSavedChats = "Нет сохраненных чатов",
    messagePlaceholder = "Сообщение",
    generating = "Ожидание ответа...",
    theme = "Тема",
    model = "Модель",
    language = "Язык",
    light = "Светлая",
    dark = "Темная",
    deleteChatTitle = "Удалить чат?",
    deleteChatText = "Вы уверены, что хотите удалить этот чат? Это действие нельзя отменить.",
    delete = "Удалить",
    cancel = "Отмена",
    messagesCount = "сообщений"
)

// ---------------------------

enum class AppScreen {
    Chat,
    Library,
    Settings
}

enum class AppThemeMode {
    Light,
    Dark
}

enum class ChatModel(val displayName: String, val modelId: String, val endpoint: String) {
    GPT5("GPT-5 Nano", "gpt-5-nano", "https://api.proxyapi.ru/openai/v1/chat/completions"),
    Xiaomi("Xiaomi Mimo", "xiaomi/mimo-v2-flash:free", "https://api.proxyapi.ru/openrouter/v1/chat/completions"),
    Glm4Air("GLM 4.5 Air", "z-ai/glm-4.5-air:free", "https://api.proxyapi.ru/openrouter/v1/chat/completions"),
    DolphinMistral("Dolphin Mistral", "cognitivecomputations/dolphin-mistral-24b-venice-edition:free", "https://api.proxyapi.ru/openrouter/v1/chat/completions"),
    DeepSeekR1("DeepSeek R1", "deepseek/deepseek-r1-0528:free", "https://api.proxyapi.ru/openrouter/v1/chat/completions")
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

data class ChatSession(
    val id: Int,
    val messages: List<ChatMessage>,
    var title: String = "New Chat",
    var model: ChatModel? = ChatModel.GPT5
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
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun AppDrawer(
    currentRoute: String,
    appStrings: AppStrings,
    onDestinationSelected: (String) -> Unit
) {
    val screens = listOf(AppScreen.Chat, AppScreen.Library, AppScreen.Settings)
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = appStrings.menu,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        screens.forEach { screen ->
            val label = when(screen) {
                AppScreen.Chat -> appStrings.chatScreenTitle
                AppScreen.Library -> appStrings.library
                AppScreen.Settings -> appStrings.settings
            }

            NavigationDrawerItem(
                label = {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                selected = currentRoute == screen.name,
                onClick = { onDestinationSelected(screen.name) },
                modifier = Modifier.padding(horizontal = 12.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = if (MaterialTheme.colorScheme.background == Color(0xFF1A1A1A))
                        Color(0xFF3A3A3A) else UserMessageBackground,
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
    onModelSelected: (ChatModel) -> Unit,
    isGenerating: Boolean,
    appStrings: AppStrings,
    onSendMessage: (String) -> Unit,
    onStartNewChat: () -> Unit
) {
    var messageText by rememberSaveable(chatSession.id) { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(chatSession.messages.size, chatSession.messages.lastOrNull()?.text) {
        if (chatSession.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatSession.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .clickable { showModelMenu = true }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${appStrings.model}: ${selectedModel.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Model",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .widthIn(min = 200.dp)
                    ) {
                        ChatModel.values().forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = model.displayName,
                                        color = if(model == selectedModel) YandexYellow else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if(model == selectedModel) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onModelSelected(model)
                                    showModelMenu = false
                                }
                            )
                        }
                    }
                }

                TextButton(onClick = onStartNewChat) {
                    Text(
                        text = appStrings.newChat,
                        color = YandexYellow,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

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

        Surface(
            color = MaterialTheme.colorScheme.surface,
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
                    enabled = !isGenerating,
                    placeholder = {
                        Text(
                            text = if (isGenerating) appStrings.generating else appStrings.messagePlaceholder,
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
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = YandexYellow
                    ),
                    shape = RoundedCornerShape(24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                IconButton(
                    onClick = {
                        onSendMessage(messageText)
                        messageText = ""
                    },
                    enabled = messageText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (messageText.isNotBlank() && !isGenerating) YandexYellow else Color.Gray.copy(
                                alpha = 0.3f
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageText.isNotBlank() && !isGenerating) TextOnYellow else Color.Gray
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
    appStrings: AppStrings,
    onChatSelected: (Int) -> Unit,
    onDeleteChat: (Int) -> Unit
) {
    var chatToDeleteId by remember { mutableStateOf<Int?>(null) }

    if (chatToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { chatToDeleteId = null },
            title = {
                Text(text = appStrings.deleteChatTitle)
            },
            text = {
                Text(text = appStrings.deleteChatText)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChat(chatToDeleteId!!)
                        chatToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YandexYellow,
                        contentColor = TextOnYellow
                    )
                ) {
                    Text(appStrings.delete)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { chatToDeleteId = null }
                ) {
                    Text(appStrings.cancel, color = TextPrimary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = appStrings.noSavedChats,
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
                            containerColor = if (isActive) {
                                if(MaterialTheme.colorScheme.background == Color(0xFF1A1A1A))
                                    Color(0xFF3A3A3A) else UserMessageBackground
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isActive) 0.dp else 2.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatSelected(chat.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = chat.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${chat.messages.size} ${appStrings.messagesCount}",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(
                                onClick = { chatToDeleteId = chat.id }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Chat",
                                    tint = TextSecondary
                                )
                            }
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
    appLanguage: AppLanguage,
    appStrings: AppStrings,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = appStrings.language,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceButton(
                text = "English",
                selected = appLanguage == AppLanguage.English,
                onClick = { onLanguageChange(AppLanguage.English) },
                modifier = Modifier.weight(1f)
            )
            ChoiceButton(
                text = "Русский",
                selected = appLanguage == AppLanguage.Russian,
                onClick = { onLanguageChange(AppLanguage.Russian) },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = appStrings.theme,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceButton(
                text = appStrings.light,
                selected = themeMode == AppThemeMode.Light,
                onClick = { onThemeModeChange(AppThemeMode.Light) },
                modifier = Modifier.weight(1f)
            )
            ChoiceButton(
                text = appStrings.dark,
                selected = themeMode == AppThemeMode.Dark,
                onClick = { onThemeModeChange(AppThemeMode.Dark) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ChoiceButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = YandexYellow,
                contentColor = TextOnYellow
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        TextButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
        ) {
            Text(
                text = text,
                color = TextSecondary
            )
        }
    }
}

fun screenTitleForRoute(route: String, appStrings: AppStrings): String {
    return when (route) {
        AppScreen.Chat.name -> appStrings.chatScreenTitle
        AppScreen.Library.name -> appStrings.library
        AppScreen.Settings.name -> appStrings.settings
        else -> appStrings.chatScreenTitle
    }
}

fun saveChatsToPrefs(context: Context, chats: List<ChatSession>) {
    val sharedPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    val editor = sharedPrefs.edit()
    val gson = Gson()
    val nonEmptyChats = chats.filter { it.messages.isNotEmpty() }
    val json = gson.toJson(nonEmptyChats)
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