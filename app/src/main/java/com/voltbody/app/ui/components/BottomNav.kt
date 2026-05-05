package com.voltbody.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.domain.model.AppTab
import com.voltbody.app.ui.theme.*

private data class NavItem(
    val tab: AppTab,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val label: String
)

private val NavSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 300f)
private val NavOffsetSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

@Composable
fun VoltBodyBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val haptic = LocalHapticFeedback.current

    val leftItems = listOf(
        NavItem(AppTab.WORKOUT, Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter, "Rutina"),
        NavItem(AppTab.DIET, Icons.Outlined.RestaurantMenu, Icons.Filled.RestaurantMenu, "Dieta"),
    )
    val rightItems = listOf(
        NavItem(AppTab.CALENDAR, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth, "Calendario"),
        NavItem(AppTab.PROFILE, Icons.Outlined.Person, Icons.Filled.Person, "Perfil"),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // ── Liquid Glass NavBar Pill ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 32.dp,
                    shape = CircleShape,
                    ambientColor = vb.accent.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            vb.surfaceElevated.copy(alpha = LiquidAlphaLevel0 + 0.05f),
                            vb.surface.copy(alpha = LiquidAlphaLevel0)
                        )
                    )
                )
                // Specular top highlight (iOS 26 glass effect)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            endY = size.height * 0.4f
                        )
                    )
                }
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            leftItems.forEach { item ->
                NavButton(
                    item = item,
                    isActive = currentTab == item.tab,
                    accentColor = vb.accent,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(item.tab)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            CenterVoltButton(
                isActive = currentTab == AppTab.HOME || currentTab == AppTab.AI_COACH,
                isAiMode = currentTab == AppTab.AI_COACH,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val next = if (currentTab == AppTab.AI_COACH) AppTab.HOME else AppTab.AI_COACH
                    onTabSelected(next)
                },
                modifier = Modifier.weight(1.4f)
            )

            rightItems.forEach { item ->
                NavButton(
                    item = item,
                    isActive = currentTab == item.tab,
                    accentColor = vb.accent,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(item.tab)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavButton(
    item: NavItem,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.90f
            isActive -> 1.1f
            else -> 1f
        },
        animationSpec = NavSpring,
        label = "icon_scale"
    )
    
    val bgAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.15f else 0f,
        animationSpec = NavSpring,
        label = "bg_alpha"
    )

    Column(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .pulseOnPress(isPressed, cornerRadius = 18.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Liquid Radial Glow (behind active icon)
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )
            }
            
            Icon(
                imageVector = if (isActive) item.iconSelected else item.icon,
                contentDescription = item.label,
                tint = if (isActive) accentColor else ColorTextMuted,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        
        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(250)) + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut(tween(200)) + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = accentColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Liquid Dot Indicator
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 12.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
fun CenterVoltButton(
    isActive: Boolean,
    isAiMode: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = NavSpring,
        label = "center_scale"
    )
    
    val bgAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.15f else 0f,
        animationSpec = NavSpring,
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .height(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isActive) vb.accent.copy(alpha = 0.12f) else Color.Transparent
            )
            .drawWithContent {
                drawContent()
                if (isActive) {
                    // Accent border glow for center button
                    drawCircle(
                        color = vb.accent.copy(alpha = 0.2f),
                        radius = size.minDimension / 2,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Spinning accent ring when active (Center button special)
                if (isActive) {
                    val infiniteTransition = rememberInfiniteTransition(label = "volt_spin")
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
                        label = "spin"
                    )
                    
                    Canvas(modifier = Modifier.size(32.dp)) {
                        drawArc(
                            color = vb.accent,
                            startAngle = angle,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }

                AnimatedContent(
                    targetState = isAiMode,
                    transitionSpec = {
                        fadeIn(tween(250)) + scaleIn(initialScale = 0.7f) togetherWith
                            fadeOut(tween(200)) + scaleOut(targetScale = 0.7f)
                    },
                    label = "center_icon"
                ) { aiMode ->
                    Icon(
                        imageVector = if (aiMode) Icons.Filled.SmartToy else Icons.Filled.Bolt,
                        contentDescription = if (aiMode) "Coach IA" else "Inicio",
                        tint = if (isActive) vb.accent else vb.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = if (isAiMode) "COACH" else "VOLTBODY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontSize = 11.sp
                ),
                color = if (isActive) vb.accent else vb.textMuted
            )
        }
    }
}
