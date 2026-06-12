package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.api.GeminiClient
import com.example.data.MessageEntity
import com.example.data.ProjectEntity
import com.example.data.TaskEntity
import com.example.model.Agent
import com.example.model.AgentRoster
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val isRunningSimulation by viewModel.isRunningSimulation.collectAsStateWithLifecycle()
    val currentThinkingAgent by viewModel.currentThinkingAgent.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val tasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val configs by viewModel.allConfigs.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    // Form inputs for board
    var titleInput by remember { mutableStateOf("") }
    var promptInput by remember { mutableStateOf("") }
    
    // selectedTab: 0: Working Board, 1: APIs & Squad, 2: GitHub Sync, 3: Workspace Catalog, 4: AI Squad Roster
    var selectedTab by remember { mutableIntStateOf(0) }

    // Auto scroll chat to bottom when message list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    // App background brush - Cosmic slate dark space
    val darkSpaceGrad = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B0F19), // Midnight Deep Void
            Color(0xFF1E293B), // Slate Void
            Color(0xFF0B0F19)  // Dark Blue-grey Core
        )
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF0B0F19)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkSpaceGrad)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                // 1. Sleek Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🚀",
                                fontSize = 22.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "AI Team Workspace",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Text(
                            text = "Multi-Agent Android Agency Simulator",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Dynamic Gemini API configuration checker
                    val customApiKey = configs.find { it.key == "gemini_api_key" }?.value ?: ""
                    val isKeyLive = remember(customApiKey) { GeminiClient.isApiKeyConfigured(customApiKey.ifBlank { null }) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isKeyLive) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFFF59E0B).copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isKeyLive) Color(0xFF10B981).copy(alpha = 0.4f)
                                else Color(0xFFF59E0B).copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isKeyLive) "GEMINI LIVE" else "SIMULATOR MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isKeyLive) Color(0xFF34D399) else Color(0xFFFBBF24)
                        )
                    }
                }

                // 2. Custom M3 Tab Navigation Bar (Expanded to 5 tabs with small compact styling)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("Board", "APIs & Squad", "GitHub Sync", "Catalog", "Roster")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF334155) else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Tab Contents
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) with fadeOut(animationSpec = spring())
                    },
                    modifier = Modifier.weight(1f)
                ) { targetTab ->
                    when (targetTab) {
                        0 -> ActiveBoardTab(
                            activeProject = activeProject,
                            isRunningSimulation = isRunningSimulation,
                            currentThinkingAgent = currentThinkingAgent,
                            messages = messages,
                            tasks = tasks,
                            titleInput = titleInput,
                            onTitleChange = { titleInput = it },
                            promptInput = promptInput,
                            onPromptChange = { promptInput = it },
                            scrollState = scrollState,
                            onStartSimulate = { title, prompt, offline ->
                                viewModel.startTeamCollaboration(title, prompt, offline)
                            },
                            viewModel = viewModel
                        )
                        1 -> ApiAndSquadTab(viewModel = viewModel)
                        2 -> GithubConsoleTab(viewModel = viewModel)
                        3 -> CatalogTab(
                            projects = projects,
                            activeProject = activeProject,
                            onSelectProject = {
                                viewModel.selectProject(it.id)
                                selectedTab = 0
                            },
                            onDeleteProject = { viewModel.deleteProject(it.id) }
                        )
                        4 -> RosterTab(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// ======================== WORKING BOARD TAB ========================

@Composable
fun ActiveBoardTab(
    activeProject: ProjectEntity?,
    isRunningSimulation: Boolean,
    currentThinkingAgent: String?,
    messages: List<MessageEntity>,
    tasks: List<TaskEntity>,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    promptInput: String,
    onPromptChange: (String) -> Unit,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    onStartSimulate: (String, String, Boolean) -> Unit,
    viewModel: ProjectViewModel
) {
    if (activeProject == null && !isRunningSimulation) {
        EmptyProjectCreator(
            title = titleInput,
            onTitleChange = onTitleChange,
            prompt = promptInput,
            onPromptChange = onPromptChange,
            onStartSimulate = onStartSimulate
        )
    } else {
        var feedbackInput by remember { mutableStateOf("") }
        var selectedAgentDm by remember { mutableStateOf<String?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Task status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeProject?.title ?: "AI Collaboration Session",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = activeProject?.prompt ?: "",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val statusColor = when (activeProject?.status) {
                            "Planning" -> Color(0xFF6366F1)
                            "Running" -> Color(0xFFEC4899)
                            "Completed" -> Color(0xFF10B981)
                            "Failed" -> Color(0xFFEF4444)
                            else -> Color(0xFF64748B)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.2f))
                                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = (activeProject?.status ?: "Unknown").uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    if (tasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            tasks.forEach { task ->
                                val currentAgent = viewModel.getAgentByName(task.assignee)
                                val dotBg = when (task.status) {
                                    "Completed" -> Color(0xFF10B981)
                                    "Running" -> Color(0xFFEC4899)
                                    else -> Color(0xFF475569)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(dotBg.copy(alpha = 0.2f))
                                            .border(1.2.dp, dotBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (task.status == "Completed") {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Done",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else if (task.status == "Running") {
                                            CircularProgressIndicator(
                                                color = Color(0xFFEC4899),
                                                strokeWidth = 1.2.dp,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        } else {
                                            Text(
                                                text = currentAgent.avatarChar,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentAgent.name.split(" ")[0],
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (task.status == "Running") Color.White else Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time Thinking Alert
            AnimatedVisibility(
                visible = currentThinkingAgent != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                currentThinkingAgent?.let { agentName ->
                    val activeAgent = viewModel.getAgentByName(agentName)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = activeAgent.baseColor.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, activeAgent.baseColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeAgent.avatarChar,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${activeAgent.name} Active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAgent.baseColor
                                )
                                Text(
                                    text = if (agentName.contains("API")) agentName else "Synthesizing custom contribution scripts...",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            CircularProgressIndicator(
                                color = activeAgent.baseColor,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(16.dp)
                              )
                        }
                    }
                }
            }

            // Central Terminal Output / Discussions console
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF070A13))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            ) {
                // Scrollable Chat Log
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF6366F1), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Director AI is allocating work clusters...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { msg ->
                                val isUser = msg.senderName == "User"
                                val agentObj = viewModel.getAgentByName(msg.senderName)
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        if (!isUser) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(agentObj.baseColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(agentObj.avatarChar, fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = if (isUser) "Client Blueprint" else msg.senderName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) Color(0xFFEC4899) else agentObj.baseColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isUser) "" else "(${msg.senderRole})",
                                            fontSize = 8.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 10.dp,
                                            topEnd = 10.dp,
                                            bottomStart = if (isUser) 10.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 10.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isUser) Color(0xFF1E1E2F) else Color(0xFF131A2D)
                                        ),
                                        border = BorderStroke(
                                            0.5.dp,
                                            if (isUser) Color(0xFFEC4899).copy(alpha = 0.4f) else Color(0xFF334155)
                                        ),
                                        modifier = Modifier.widthIn(max = 290.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = msg.message,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFFE2E8F0),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Divider and Chat Input Box
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                        .padding(8.dp)
                ) {
                    // Agent Recipient Selection Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speak to:",
                            fontSize = 8.5.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        
                        val recipientOptions = listOf(
                            null to "🧠 Auto",
                            AgentRoster.director.name to "💼 Director",
                            AgentRoster.designer.name to "🎨 Designer",
                            AgentRoster.developer.name to "💻 Developer",
                            AgentRoster.tester.name to "🧪 QA",
                            AgentRoster.marketer.name to "✍️ Copy"
                        )
                        
                        recipientOptions.forEach { (agentName, labelStr) ->
                            val isSelected = selectedAgentDm == agentName
                            val themeColor = agentName?.let { viewModel.getAgentByName(it).baseColor } ?: Color(0xFF6366F1)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) themeColor.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                    .border(
                                        1.dp,
                                        if (isSelected) themeColor else Color(0xFF334155),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedAgentDm = agentName }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = labelStr,
                                    fontSize = 8.5.sp,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Text Field Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = feedbackInput,
                            onValueChange = { feedbackInput = it },
                            placeholder = {
                                val placeholderHint = selectedAgentDm?.let { "Critique ${it.split(" ")[0]} directly..." }
                                    ?: "Provide instructions / review results..."
                                Text(placeholderHint, fontSize = 11.sp, color = Color(0xFF475569))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                        )

                        val isSendingDisabled = feedbackInput.isBlank() || currentThinkingAgent != null
                        IconButton(
                            enabled = !isSendingDisabled,
                            onClick = {
                                if (feedbackInput.isNotBlank() && currentThinkingAgent == null) {
                                    val formattedWithDm = if (selectedAgentDm != null) {
                                        "[Direct comment to @${selectedAgentDm!!.split(" ")[0]}] $feedbackInput"
                                    } else {
                                        feedbackInput
                                    }
                                    viewModel.sendUserFeedback(formattedWithDm)
                                    feedbackInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSendingDisabled) Color(0xFF1E293B) else Color(0xFF6366F1))
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send feedback",
                                tint = if (isSendingDisabled) Color(0xFF475569) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// ======================== APP BUILDER FILL FORM ========================

@Composable
fun EmptyProjectCreator(
    title: String,
    onTitleChange: (String) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onStartSimulate: (String, String, Boolean) -> Unit
) {
    var errorMsg by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚒️", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Create Collaborative Spec",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "Orchestrate standard and customized AI agents to draft responsive architectures in real time.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { onTitleChange(it); errorMsg = "" },
                label = { Text("Android App Name", fontSize = 11.sp) },
                singleLine = true,
                placeholder = { Text("e.g. FitTrack Companion", color = Color(0xFF475569), fontSize = 11.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = prompt,
                onValueChange = { onPromptChange(it); errorMsg = "" },
                label = { Text("Feature Blueprint Idea Details", fontSize = 11.sp) },
                minLines = 3,
                placeholder = {
                    Text(
                        "e.g. A material 3 step calculator that persists runs locally and queries Fitbit activity metrics API...",
                        color = Color(0xFF475569),
                        fontSize = 11.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_prompt_input")
            )

            if (errorMsg.isNotBlank()) {
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start Live workflow Button
            Button(
                onClick = {
                    if (title.isBlank() || prompt.isBlank()) {
                        errorMsg = "Please supply both App Name and Idea Blueprint."
                        return@Button
                    }
                    onStartSimulate(title, prompt, false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("live_simulate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Orchestrate AI Squad Pipeline", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Demo Offline mode button
            OutlinedButton(
                onClick = {
                    if (title.isBlank() || prompt.isBlank()) {
                        errorMsg = "Please supply both App Name and Idea Blueprint."
                        return@OutlinedButton
                    }
                    onStartSimulate(title, prompt, true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("offline_simulate_button"),
                border = BorderStroke(1.dp, Color(0xFFEC4899)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF472B6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simulate Demo Offline Workflow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "Platform API Guide",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Custom models and customized personal keys (including free tier options) can be registered under the 'APIs & Squad' tab.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 9.5.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// ======================== API & SQUAD MANAGEMENT TAB ========================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiAndSquadTab(viewModel: ProjectViewModel) {
    val configs by viewModel.allConfigs.collectAsStateWithLifecycle()
    val customMembers by viewModel.customTeamMembers.collectAsStateWithLifecycle()

    var savedApiKey by remember { mutableStateOf("") }
    var savedModelId by remember { mutableStateOf("gemini-3.5-flash") }
    var debateModeEnabled by remember { mutableStateOf(false) }
    var rTopology by remember { mutableStateOf("Queen-led Hierarchy (Raft)") }
    var rMemoryRouting by remember { mutableStateOf("SONA Self-Learning Memory + HNSW Retrieve") }
    var rAiDefence by remember { mutableStateOf(true) }
    var rTrustRating by remember { mutableStateOf(true) }

    // Init inputs with saved values
    LaunchedEffect(configs) {
        savedApiKey = configs.find { it.key == "gemini_api_key" }?.value ?: ""
        savedModelId = configs.find { it.key == "gemini_model_id" }?.value ?: "gemini-3.5-flash"
        debateModeEnabled = configs.find { it.key == "debate_mode" }?.value == "true"
        rTopology = configs.find { it.key == "ruflo_topology" }?.value ?: "Queen-led Hierarchy (Raft)"
        rMemoryRouting = configs.find { it.key == "ruflo_memory_routing" }?.value ?: "SONA Self-Learning Memory + HNSW Retrieve"
        rAiDefence = (configs.find { it.key == "ruflo_ai_defence" }?.value ?: "true") == "true"
        rTrustRating = (configs.find { it.key == "ruflo_trust_rating" }?.value ?: "true") == "true"
    }

    var showApiKey by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("") }

    // New Team member builder form state
    var builderName by remember { mutableStateOf("") }
    var builderRole by remember { mutableStateOf("") }
    var builderMotto by remember { mutableStateOf("") }
    var builderEmoji by remember { mutableStateOf("🤖") }
    var builderColor by remember { mutableStateOf("#10B981") } // emerald default
    var builderPersonalKey by remember { mutableStateOf("") }
    var showPersonalKey by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Section 1: API Configuration & Free Model Selector
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🔌 API Credentials & Engine",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize API keys or select free/low-tier models to keep developer workspace running within quotas.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Model selector (Free/Groq tiers)
                    val isKeyGroq = com.example.api.GroqClient.isGroqKey(savedApiKey)
                    Text(
                        text = if (isKeyGroq) "Target Groq Model" else "Target Generative Model ID",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val modelsList = if (isKeyGroq) {
                        listOf(
                            com.example.api.GroqClient.DEFAULT_MODEL to "Llama 3.3 70B (High Intelligence & Reasoning) [Groq]",
                            "llama-3.1-8b-instant" to "Llama 3.1 8B (Super Fast Inference) [Groq]",
                            "mixtral-8x7b-32768" to "Mixtral 8x7B (High-context Mixture of Experts) [Groq]",
                            "gemma2-9b-it" to "Gemma 2 9B (Google Open Weights Optimized) [Groq]"
                        )
                    } else {
                        listOf(
                            "gemini-3.5-flash" to "Basic Text, Summarization & Simple Q&A (Default) [Gemini]",
                            "gemini-3.1-pro-preview" to "Complex Reasoning, Coding, Math & Advanced Planning [Gemini]",
                            "llama-3.3-70b-versatile" to "Use Groq Llama 3.3 (Requires a gsk_ Key in field below)"
                        )
                    }
                    
                    // Make sure the selected model ID is valid for the current key type, if not, auto-default
                    if (isKeyGroq && !modelsList.any { it.first == savedModelId }) {
                        savedModelId = com.example.api.GroqClient.DEFAULT_MODEL
                    } else if (!isKeyGroq && savedModelId.startsWith("llama-") && savedModelId != "llama-3.3-70b-versatile") {
                        savedModelId = "gemini-3.5-flash"
                    }
                    
                    modelsList.forEach { (id, desc) ->
                        val isSelectedModel = savedModelId == id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelectedModel) Color(0xFF334155) else Color.Transparent)
                                .clickable { savedModelId = id }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelectedModel,
                                onClick = { savedModelId = id },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6366F1))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = id,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8)
                                )
							}
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // API Key override
                    OutlinedTextField(
                        value = savedApiKey,
                        onValueChange = { savedApiKey = it; saveStatus = "" },
                        label = { 
                            Text(
                                text = if (isKeyGroq) "⚡ Groq API Key Activated (gsk_...)" else "Personal Gemini or Groq API Key (gsk_...) Override",
                                fontSize = 11.sp
                            ) 
                        },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showApiKey) "HIDE" else "SHOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1),
                                modifier = Modifier
                                    .clickable { showApiKey = !showApiKey }
                                    .padding(end = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (isKeyGroq) Color(0xFF34D399) else Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (saveStatus.isNotBlank()) {
                        Text(
                            text = saveStatus,
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.12f))
                            .clickable { debateModeEnabled = !debateModeEnabled }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💬 Enable Agent Debate Mode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Each agent's work gets peer-reviewed with critique comments from fellow experts in the chat console.",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = debateModeEnabled,
                            onCheckedChange = { debateModeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Topology row
                    Text(
                        text = "🕸️ Swarm Topology Coordination:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val topologies = listOf("Queen-led Hierarchy (Raft)", "Fully Federated Gossip Network", "Sequential Direct Pipeline")
                        topologies.forEach { topo ->
                            val isSelected = rTopology == topo
                            val label = when (topo) {
                                "Queen-led Hierarchy (Raft)" -> "👑 Queen-Raft"
                                "Fully Federated Gossip Network" -> "🕸️ Gossip Swarm"
                                else -> "⛓️ Sequential"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF6366F1).copy(alpha = 0.25f) else Color(0xFF1E293B))
                                    .border(1.dp, if (isSelected) Color(0xFF6366F1) else Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .clickable { rTopology = topo }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shared Swarm Memory row
                    Text(
                        text = "🧠 Shared Swarm Memory Type:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val memoryTypes = listOf("SONA Self-Learning Memory + HNSW Retrieve", "Static System System Routing (No Context Memory)")
                        memoryTypes.forEach { mem ->
                            val isSelected = rMemoryRouting == mem
                            val label = if (mem.contains("SONA")) "🧠 SONA + HNSW" else "💤 Direct Routing"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFFEC4899).copy(alpha = 0.25f) else Color(0xFF1E293B))
                                    .border(1.dp, if (isSelected) Color(0xFFEC4899) else Color(0xFF334155), RoundedCornerShape(6.dp))
                                    .clickable { rMemoryRouting = mem }
                                    .padding(vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AIDefence Security Shield row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.12f))
                            .clickable { rAiDefence = !rAiDefence }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🛡️ Enable Ruflo AIDefence",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Cleanses prompt injection attacks and intercepts CVE vulnerabilities inside generated files automatically.",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = rAiDefence,
                            onCheckedChange = { rAiDefence = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Trust behavioral rating row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.12f))
                            .clickable { rTrustRating = !rTrustRating }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📊 Real-Time Trust Recalibration",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Agent contributions undergo behavioral merit checks. Trust scores escalate / downgrade after peer audits.",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = rTrustRating,
                            onCheckedChange = { rTrustRating = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.saveConfigValue("gemini_api_key", savedApiKey)
                            viewModel.saveConfigValue("gemini_model_id", savedModelId)
                            viewModel.saveConfigValue("debate_mode", if (debateModeEnabled) "true" else "false")
                            viewModel.saveConfigValue("ruflo_topology", rTopology)
                            viewModel.saveConfigValue("ruflo_memory_routing", rMemoryRouting)
                            viewModel.saveConfigValue("ruflo_ai_defence", if (rAiDefence) "true" else "false")
                            viewModel.saveConfigValue("ruflo_trust_rating", if (rTrustRating) "true" else "false")
                            saveStatus = "Engine settings applied successfully!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Apply & Save Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Create custom teammate Agent Form
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "➕ Register Custom AI Teammate",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize roles, mottos, base color identifiers, and supply custom API keys for specialized team member clones.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = builderName,
                        onValueChange = { builderName = it },
                        label = { Text("Agent Teammate Name", fontSize = 11.sp) },
                        placeholder = { Text("e.g. CryptoCzar AI", color = Color(0xFF475569)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = builderRole,
                        onValueChange = { builderRole = it },
                        label = { Text("Workspace Role Specs", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Blockchain & Smart Contract Consultant", color = Color(0xFF475569)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = builderMotto,
                        onValueChange = { builderMotto = it },
                        label = { Text("Corporate Motto", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Auditing code blocks, ensuring decentralized logic.", color = Color(0xFF475569)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = builderEmoji,
                            onValueChange = { builderEmoji = it.take(1) },
                            label = { Text("Teammate Emoji", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Color picker hex
                        OutlinedTextField(
                            value = builderColor,
                            onValueChange = { builderColor = it },
                            label = { Text("ID Color Hex", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Personalized API Key for this agent override
                    OutlinedTextField(
                        value = builderPersonalKey,
                        onValueChange = { builderPersonalKey = it },
                        label = { Text("Personalized API Key (Optional Gemini or Groq gsk_...)", fontSize = 11.sp) },
                        visualTransformation = if (showPersonalKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showPersonalKey) "HIDE" else "SHOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1),
                                modifier = Modifier
                                    .clickable { showPersonalKey = !showPersonalKey }
                                    .padding(end = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (builderName.isBlank() || builderRole.isBlank()) return@Button
                            viewModel.addCustomTeamMember(
                                name = builderName,
                                role = builderRole,
                                motto = builderMotto.ifBlank { "Let's innovate together." },
                                avatarChar = builderEmoji.ifBlank { "👽" },
                                baseColorHex = builderColor.ifBlank { "#6366F1" },
                                personalizedApiKey = builderPersonalKey
                            )
                            // Clear form
                            builderName = ""
                            builderRole = ""
                            builderMotto = ""
                            builderEmoji = "🤖"
                            builderPersonalKey = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deploy Customized Agent Teammate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ======================== GITHUB SYNC TAB ========================

@Composable
fun GithubConsoleTab(viewModel: ProjectViewModel) {
    val configs by viewModel.allConfigs.collectAsStateWithLifecycle()
    val repoFiles by viewModel.githubRepoFiles.collectAsStateWithLifecycle()
    val githubStatus by viewModel.githubStatus.collectAsStateWithLifecycle()
    val pulledFileContent by viewModel.pulledFileContent.collectAsStateWithLifecycle()

    var token by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("main") }

    // Initialize fields
    LaunchedEffect(configs) {
        token = configs.find { it.key == "github_token" }?.value ?: ""
        owner = configs.find { it.key == "github_owner" }?.value ?: ""
        repo = configs.find { it.key == "github_repo" }?.value ?: ""
        branch = configs.find { it.key == "github_branch" }?.value ?: "main"
    }

    var showToken by remember { mutableStateOf(false) }

    // Push states
    var pushPathInput by remember { mutableStateOf("models/blueprint.kt") }
    var pushContentInput by remember { mutableStateOf("") }
    var pushCommitMsg by remember { mutableStateOf("Added blueprints via Workspace Studio") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Core Repository Form
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🐱 GitHub Sync & Management",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Establish secure continuous sync to commit files or pull blueprints from active repositories.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("Personal Access Token (PAT)", fontSize = 11.sp) },
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showToken) "HIDE" else "SHOW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6366F1),
                                modifier = Modifier
                                    .clickable { showToken = !showToken }
                                    .padding(end = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = owner,
                            onValueChange = { owner = it },
                            label = { Text("Repo Owner", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = repo,
                            onValueChange = { repo = it },
                            label = { Text("Repo Name", fontSize = 11.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text("Target Branch", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(0.5.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                    ) {
                        Text(
                            text = "Status: $githubStatus",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (githubStatus.contains("Error") || githubStatus.contains("Failure")) Color(0xFFEF4444) 
                                    else if (githubStatus.contains("Connected") || githubStatus.contains("Active")) Color(0xFF10B981)
                                    else Color(0xFFCBD5E1)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()) {
                                    viewModel.saveConfigValue("github_token", token)
                                    viewModel.saveConfigValue("github_owner", owner)
                                    viewModel.saveConfigValue("github_repo", repo)
                                    viewModel.saveConfigValue("github_branch", branch)
                                    viewModel.refreshGithubFiles(token, owner, repo, branch)
                                }
                            },
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pull File List", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()) {
                                    viewModel.saveConfigValue("github_token", token)
                                    viewModel.saveConfigValue("github_owner", owner)
                                    viewModel.saveConfigValue("github_repo", repo)
                                    viewModel.saveConfigValue("github_branch", branch)
                                    viewModel.testGithubConnection(token, owner, repo)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Connection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: File lists from GitHub
        if (repoFiles.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "📁 Remote Files Explorer",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap a code document to download and render content inside our local retro debugger terminal below.",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repoFiles.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.15f))
                                        .clickable {
                                            if (file.type == "file") {
                                                viewModel.pullFile(file.path)
                                            }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (file.type == "dir") "📁 " else "📄 ",
                                        fontSize = 12.sp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Path: ${file.path} (${(file.size / 1024.0).toInt()} KB)",
                                            fontSize = 9.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    if (file.type == "file") {
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = "Load",
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: File terminal inspector (Retro style)
        pulledFileContent?.let { content ->
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF070A13)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Interactive Blueprint Code Inspector",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { pushContentInput = content },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Inject to push text Area",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Text(
                            text = "Edit text or copy values into target templates below.",
                            color = Color(0xFF64748B),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp)
                        ) {
                            Text(
                                text = content,
                                color = Color(0xFF34D399),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Section: Commit and Push Composer
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📤 Commit Content & Push File",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prepare design templates, dynamic models, or agent outputs, then publish them up to the repo branch instantly.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = pushPathInput,
                        onValueChange = { pushPathInput = it },
                        label = { Text("Target Commit Path (relative directory)", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pushContentInput,
                        onValueChange = { pushContentInput = it },
                        label = { Text("Code Material Contents", fontSize = 11.sp) },
                        minLines = 4,
                        placeholder = { Text("Enter plain code text or documentation lines", color = Color(0xFF475569)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pushCommitMsg,
                        onValueChange = { pushCommitMsg = it },
                        label = { Text("Launcher Commit Message", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (pushPathInput.isNotBlank() && pushContentInput.isNotBlank()) {
                                viewModel.pushFile(pushPathInput, pushContentInput, pushCommitMsg)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Commit & Publish to GitHub Branch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// ======================== TAB Catalog ========================

@Composable
fun CatalogTab(
    projects: List<ProjectEntity>,
    activeProject: ProjectEntity?,
    onSelectProject: (ProjectEntity) -> Unit,
    onDeleteProject: (ProjectEntity) -> Unit
) {
    if (projects.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("📚", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Catalog is Empty",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Enlist your first AI agent squad in Working Board to begin logging system specs.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(projects) { project ->
                val isSelected = activeProject?.id == project.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProject(project) }
                        .testTag("catalog_item_${project.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B).copy(alpha = 0.8f)
                        else Color(0xFF1E293B).copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF6366F1) else Color(0xFF334155)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = project.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Badge
                                val statusColor = when (project.status) {
                                    "Completed" -> Color(0xFF10B981)
                                    "Running" -> Color(0xFFEC4899)
                                    else -> Color(0xFF64748B)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = project.status,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = project.prompt,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onDeleteProject(project) },
                            modifier = Modifier.testTag("delete_project_${project.id}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RosterTab(viewModel: ProjectViewModel) {
    val customMembers by viewModel.customTeamMembers.collectAsStateWithLifecycle()
    val configs by viewModel.allConfigs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Subsection: Core Roster
        item {
            Text(
                text = "⚡ Default System Architects",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(AgentRoster.allAgents) { agent ->
            val customKey = configs.find { it.key == "gemini_api_key" }?.value ?: ""
            val assignedKey = if (customKey.isNotBlank()) viewModel.getKeyForAgent(agent.name, customKey) else ""
            val hasAssignedKey = assignedKey.isNotBlank()
            val maskedKey = if (hasAssignedKey) {
                if (assignedKey.length > 10) {
                    assignedKey.take(8) + "..." + assignedKey.takeLast(4)
                } else "Key Connected"
            } else ""

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(agent.baseColor.copy(alpha = 0.15f))
                            .border(1.2.dp, agent.baseColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(agent.avatarChar, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = agent.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = agent.role,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = agent.baseColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = agent.motto,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 14.sp
                        )
                        if (hasAssignedKey) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF34D399).copy(alpha = 0.12f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Allocated API Key: $maskedKey",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subsection: Custom registered builders
        if (customMembers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🛠️ Custom Registered Squad Members",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(customMembers) { member ->
                val baseColorHex = try {
                    Color(android.graphics.Color.parseColor(member.baseColorHex))
                } catch (e: Exception) {
                    Color(0xFF6366F1)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, baseColorHex.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(baseColorHex.copy(alpha = 0.15f))
                                    .border(1.2.dp, baseColorHex, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(member.avatarChar, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = member.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = member.role,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = baseColorHex
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = member.motto,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 14.sp
                                )
                                if (member.personalizedApiKey.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Personal authorization key linked",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteCustomTeamMember(member.id) }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove Teammate",
                                tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
