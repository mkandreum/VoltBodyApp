package com.voltbody.app.ui.screens.aicoach

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.ui.components.AccentDivider
import com.voltbody.app.ui.components.LiquidGlassScaffold
import com.voltbody.app.ui.theme.*
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

@Composable
fun AiCoachScreen(
    viewModel: AiCoachViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LiquidGlassScaffold(
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(vb.bg)
            )
        }
    ) { hazeState ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Header ──────────────────────────────────────────────────────
            AiCoachHeader(
                onClear = { viewModel.clearChat() }
            )

            AccentDivider()

            // ── Message list ────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 }
                    ) {
                        ChatBubble(message = message)
                    }
                }

                // Bottom padding so last message clears the input bar
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // ── Quick actions ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.messages.size <= 2 && !uiState.isLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                QuickActionsRow(
                    onAction = { text ->
                        viewModel.sendMessage(text)
                        scope.launch {
                            listState.animateScrollToItem(
                                maxOf(0, uiState.messages.lastIndex)
                            )
                        }
                    }
                )
            }

            // ── Input bar ───────────────────────────────────────────────────
            ChatInputBar(
                text = uiState.inputText,
                isLoading = uiState.isLoading,
                focusRequester = focusRequester,
                onTextChange = viewModel::onInputChange,
                onSend = {
                    viewModel.sendMessage()
                    scope.launch {
                        listState.animateScrollToItem(
                            maxOf(0, uiState.messages.lastIndex)
                        )
                    }
                }
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun AiCoachHeader(onClear: () -> Unit) {
    val vb = LocalVoltBodyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(vb.accent, vb.accentDim)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 20.sp)
            }
            Column {
                Text(
                    "Coach IA",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = ColorWhite
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ColorSuccess)
                    )
                    Text(
                        "Online · Gemini Pro",
                        style = MaterialTheme.typography.labelSmall,
                        color = vb.textMuted
                    )
                }
            }
        }
        IconButton(onClick = onClear) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Limpiar chat",
                tint = vb.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage) {
    val vb = LocalVoltBodyColors.current
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Assistant avatar
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(vb.accent, vb.accentDim)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (message.isThinking) {
            ThinkingIndicator(accentColor = vb.accent)
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .background(
                        if (isUser)
                            Brush.linearGradient(
                                colors = listOf(vb.accent, vb.accentDim)
                            )
                        else
                            Brush.linearGradient(
                                colors = listOf(vb.surfaceElevated, vb.surface)
                            )
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUser) vb.accent.copy(alpha = 0.4f) else vb.border,
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 18.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 18.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) ColorBlack else ColorWhite,
                    lineHeight = 22.sp
                )
            }
        }

        if (isUser) Spacer(modifier = Modifier.width(8.dp))
    }
}

// ── Thinking / typing indicator ───────────────────────────────────────────────

@Composable
private fun ThinkingIndicator(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dots = (0..2).map { i ->
        animateFloatAsState(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 900
                    0f at 0
                    1f at 300
                    0f at 600
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(i * 150)
            ),
            label = "dot_$i"
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .background(ColorSurfaceElevated)
            .border(1.dp, ColorBorder, RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dots.forEach { dotAlpha ->
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(0.6f + dotAlpha.value * 0.4f)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.4f + dotAlpha.value * 0.6f))
                )
            }
        }
    }
}

// ── Quick actions ─────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(onAction: (String) -> Unit) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Preguntas rápidas",
            style = MaterialTheme.typography.labelSmall,
            color = vb.textMuted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QUICK_ACTIONS.forEach { action ->
                SuggestionChip(
                    onClick = { onAction(action) },
                    label = {
                        Text(
                            action,
                            style = MaterialTheme.typography.labelSmall,
                            color = vb.accent
                        )
                    },
                    shape = CircleShape,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = vb.accent.copy(alpha = 0.1f)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = vb.accent.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
    text: String,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val vb = LocalVoltBodyColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(vb.surface)
            .border(1.dp, vb.border, RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    "Pregunta algo a tu coach...",
                    color = vb.textMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = vb.accent.copy(alpha = 0.6f),
                unfocusedBorderColor = vb.border,
                focusedContainerColor = vb.surfaceElevated,
                unfocusedContainerColor = vb.surfaceElevated,
                cursorColor = vb.accent,
                focusedTextColor = ColorWhite,
                unfocusedTextColor = ColorWhite
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // Send button
        val canSend = text.isNotBlank() && !isLoading
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (canSend)
                        Brush.radialGradient(listOf(vb.accent, vb.accentDim))
                    else
                        Brush.radialGradient(listOf(vb.surface, vb.surface))
                )
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = vb.accent,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (canSend) ColorBlack else vb.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
