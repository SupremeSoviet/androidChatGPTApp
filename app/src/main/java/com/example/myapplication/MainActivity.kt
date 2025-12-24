package com.example.myapplication

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                            val title = if (currentRoute == AppScreen.Chat.name) {
                                currentChat.title
                            } else {
                                screenTitleForRoute(currentRoute)
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
