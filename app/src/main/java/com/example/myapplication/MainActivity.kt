package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.ChatAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val savedLangStr = sharedPrefs.getString("app_language", AppLanguage.English.name)
        val initialLanguage = try {
            AppLanguage.valueOf(savedLangStr ?: AppLanguage.English.name)
        } catch (e: Exception) {
            AppLanguage.English
        }

        val savedThemeStr = sharedPrefs.getString("app_theme", AppThemeMode.Light.name)
        val initialTheme = try {
            AppThemeMode.valueOf(savedThemeStr ?: AppThemeMode.Light.name)
        } catch (e: Exception) {
            AppThemeMode.Light
        }

        val rawChats = getChatsFromPrefs(this)

        val loadedChats = rawChats.map {
            if (it.model == null) it.copy(model = ChatModel.GPT5) else it
        }.filter { it.messages.isNotEmpty() }

        if (rawChats.size != loadedChats.size || rawChats.any { it.model == null }) {
            saveChatsToPrefs(this, loadedChats)
        }

        val initialChat = if (loadedChats.isNotEmpty()) loadedChats.first() else ChatSession(id = 1, messages = emptyList(), model = ChatModel.GPT5)
        val initialNextId = if (loadedChats.isNotEmpty()) (loadedChats.maxOfOrNull { it.id } ?: 0) + 1 else 2

        setContent {
            var themeMode by rememberSaveable { mutableStateOf(initialTheme) }
            var appLanguage by rememberSaveable { mutableStateOf(initialLanguage) }
            var selectedModel by rememberSaveable { mutableStateOf(initialChat.model ?: ChatModel.GPT5) }

            var chats by remember { mutableStateOf(loadedChats) }
            var currentChat by remember { mutableStateOf(initialChat) }
            var nextChatId by remember { mutableStateOf(initialNextId) }

            var isGenerating by remember { mutableStateOf(false) }

            val appStrings = if (appLanguage == AppLanguage.English) EnglishStrings else RussianStrings

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: AppScreen.Chat.name

            fun onLanguageSelected(lang: AppLanguage) {
                appLanguage = lang
                sharedPrefs.edit().putString("app_language", lang.name).apply()
            }

            fun onThemeSelected(theme: AppThemeMode) {
                themeMode = theme
                sharedPrefs.edit().putString("app_theme", theme.name).apply()
            }

            fun saveChat(updatedChat: ChatSession) {
                val index = chats.indexOfFirst { it.id == updatedChat.id }
                chats = if (index == -1) {
                    listOf(updatedChat) + chats
                } else {
                    chats.toMutableList().also { it[index] = updatedChat }
                }
                saveChatsToPrefs(this@MainActivity, chats)
            }

            fun deleteChat(chatId: Int) {
                chats = chats.filter { it.id != chatId }
                saveChatsToPrefs(this@MainActivity, chats)

                if (currentChat.id == chatId) {
                    if (chats.isNotEmpty()) {
                        val firstChat = chats.first()
                        currentChat = firstChat
                        selectedModel = firstChat.model ?: ChatModel.GPT5
                    } else {
                        val newChat = ChatSession(id = nextChatId, messages = emptyList(), model = ChatModel.GPT5)
                        currentChat = newChat
                        selectedModel = ChatModel.GPT5
                        nextChatId += 1
                    }
                }
            }

            fun handleSendMessage(text: String) {
                if (text.isBlank() || isGenerating) return

                isGenerating = true

                val cleanedText = text.trim()
                val userMsgId = System.currentTimeMillis()
                val userMessage = ChatMessage(userMsgId, cleanedText, true)
                val assistantMsgId = userMsgId + 1
                val assistantMessage = ChatMessage(assistantMsgId, "", false)

                val newMessages = currentChat.messages + userMessage + assistantMessage
                val updatedChat = currentChat.copy(messages = newMessages)
                currentChat = updatedChat

                saveChat(updatedChat)

                scope.launch {
                    try {
                        val loadingJob = launch {
                            val loadingTexts = listOf(
                                "${appStrings.generating}",
                                "${appStrings.generating}.",
                                "${appStrings.generating}.."
                            )
                            while (isActive) {
                                for (loadingText in loadingTexts) {
                                    withContext(Dispatchers.Main) {
                                        val currentMessages = currentChat.messages.toMutableList()
                                        val msgIndex = currentMessages.indexOfFirst { it.id == assistantMsgId }
                                        if (msgIndex != -1) {
                                            currentMessages[msgIndex] = currentMessages[msgIndex].copy(text = loadingText)
                                            currentChat = currentChat.copy(messages = currentMessages)
                                        }
                                    }
                                    delay(400)
                                }
                            }
                        }

                        val modelToUse = currentChat.model ?: ChatModel.GPT5
                        val fullResponse = getAssistantReplyFromProxyApi(
                            chatHistory = updatedChat.messages,
                            apiKey = ApiConfig.proxyApiKey,
                            modelId = modelToUse.modelId,
                            apiEndpoint = modelToUse.endpoint
                        )

                        loadingJob.cancel()

                        var displayedText = ""
                        val chars = fullResponse.toCharArray()

                        for (char in chars) {
                            displayedText += char
                            withContext(Dispatchers.Main) {
                                val currentMessages = currentChat.messages.toMutableList()
                                val msgIndex = currentMessages.indexOfFirst { it.id == assistantMsgId }
                                if (msgIndex != -1) {
                                    currentMessages[msgIndex] = currentMessages[msgIndex].copy(text = displayedText)
                                    currentChat = currentChat.copy(messages = currentMessages)
                                }
                            }
                            delay(10)
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
                    } finally {
                        isGenerating = false
                    }
                }
            }

            ChatAppTheme(themeMode = themeMode) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            currentRoute = currentRoute,
                            appStrings = appStrings,
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
                            val title = if (currentRoute == AppScreen.Chat.name) {
                                currentChat.title
                            } else {
                                screenTitleForRoute(currentRoute, appStrings)
                            }
                            AppTopBar(
                                title = title,
                                onMenuClick = {
                                    scope.launch { drawerState.open() }
                                }
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = AppScreen.Library.name,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(AppScreen.Chat.name) {
                                ChatScreen(
                                    chatSession = currentChat,
                                    selectedModel = selectedModel,
                                    onModelSelected = { newModel ->
                                        selectedModel = newModel
                                        currentChat = currentChat.copy(model = newModel)
                                        saveChat(currentChat)
                                    },
                                    isGenerating = isGenerating,
                                    appStrings = appStrings,
                                    onSendMessage = { text -> handleSendMessage(text) },
                                    onStartNewChat = {
                                        val newChat = ChatSession(id = nextChatId, messages = emptyList(), model = selectedModel)
                                        currentChat = newChat
                                        nextChatId += 1
                                    }
                                )
                            }
                            composable(AppScreen.Library.name) {
                                LibraryScreen(
                                    chats = chats,
                                    activeChatId = currentChat.id,
                                    appStrings = appStrings,
                                    onChatSelected = { chatId ->
                                        val selectedChat = chats.find { it.id == chatId }
                                        if (selectedChat != null) {
                                            currentChat = selectedChat
                                            selectedModel = selectedChat.model ?: ChatModel.GPT5
                                            navController.navigate(AppScreen.Chat.name) {
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    onDeleteChat = { chatId ->
                                        deleteChat(chatId)
                                    }
                                )
                            }
                            composable(AppScreen.Settings.name) {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    appLanguage = appLanguage,
                                    appStrings = appStrings,
                                    onThemeModeChange = { onThemeSelected(it) },
                                    onLanguageChange = { onLanguageSelected(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}