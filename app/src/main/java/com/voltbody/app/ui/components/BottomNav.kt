package com.voltbody.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.*
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

// Sprint 2 — Expressive spring spec (dampingRatio=0.45, stiffness=380)
private val NavSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 380f)
private val NavOffsetSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 420f)

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
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 28.dp,
                    shape = CircleShape,
                    ambientColor = vb.accent.copy(alpha = 0.18f),
                    spotColor = vb.accent.copy(alpha = 0.12f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            vb.surfaceElevated.copy(alpha = 0.97f),
                            vb.surface.copy(alpha = 0.97f)
                        )
                    )
                )
                .border(1.dp, vb.border, CircleShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leftItems.forEach { item ->
                NavButton(
                    item = item,
                    isActive = currentTab == item.tab,
                    accentColor = vb.accent,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(item.tab)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            CenterVoltButton(
                isActive = currentTab == AppTab.HOME || currentTab == AppTab.AI_COACH,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Toggle between HOME and AI_COACH on repeated taps
                    val next = if (currentTab == AppTab.AI_COACH) AppTab.HOME else AppTab.AI_COACH
                    onTabSelected(if (currentTab == AppTab.HOME) AppTab.AI_COACH else AppTab.HOME)
                },
                modifier = Modifier.weight(1.6f)
            )

            rightItems.forEach { item ->
                NavButton(
                    item = item,
                    isActive = currentTab == item.tab,
                    accentColor = vb.accent,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

    // Sprint 2 — expressive spring physics
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.13f else 1f,
        animationSpec = NavSpring,
        label = "icon_scale"
    )
    val iconOffsetY by animateFloatAsState(
        targetValue = if (isActive) -2f else 0f,
        animationSpec = NavOffsetSpring,
        label = "icon_offset"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.12f else 0f,
        animationSpec = NavSpring,
        label = "bg_alpha"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (isActive) item.iconSelected else item.icon,
            contentDescription = item.label,
            tint = if (isActive) accentColor else ColorTextMuted,
            modifier = Modifier
                .size(22.dp)
                .scale(iconScale)
                .offset(y = iconOffsetY.dp)
        )
        // Active dot indicator
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) accentColor
                    else Color.Transparent
                )
        )
    }
}

@Composable
fun CenterVoltButton(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = NavSpring,
        label = "center_scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.30f else 0.15f,
        animationSpec = NavSpring,
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .shadow(
                elevation = if (isActive) 16.dp else 8.dp,
                shape = CircleShape,
                ambientColor = vb.accent.copy(alpha = glowAlpha),
                spotColor = vb.accent.copy(alpha = glowAlpha)
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        vb.accent,
                        vb.accentDim
                    )
                )
            )
            .border(
                1.dp,
                vb.accent.copy(alpha = 0.6f),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Show ⚡ for HOME, brain icon for AI_COACH
        AnimatedContent(
            targetState = isActive,
            transitionSpec = {
                fadeIn(tween(150)) togetherWith fadeOut(tween(150))
            },
            label = "center_icon"
        ) { active ->
            Text(
                text = if (active) "🤖" else "⚡",
                fontSize = 20.sp,
                color = ColorBlack
            )
        }
    }
}
