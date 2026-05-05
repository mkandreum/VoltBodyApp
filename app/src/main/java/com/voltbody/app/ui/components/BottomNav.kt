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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.domain.model.AppTab
import com.voltbody.app.ui.theme.*

private data class NavItem(
    val tab: AppTab,
    val icon: ImageVector,
    val label: String
)

private val NavSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

@Composable
fun VoltBodyBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val haptic = LocalHapticFeedback.current

    val leftItems = listOf(
        NavItem(AppTab.WORKOUT, Icons.Outlined.FitnessCenter, "Rutina"),
        NavItem(AppTab.DIET, Icons.Outlined.RestaurantMenu, "Dieta"),
    )
    val rightItems = listOf(
        NavItem(AppTab.CALENDAR, Icons.Outlined.CalendarMonth, "Calendario"),
        NavItem(AppTab.PROFILE, Icons.Outlined.Person, "Perfil"),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        contentAlignment = Alignment.Center
    ) {
        // Main Pill
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = vb.accent.copy(alpha = 0.2f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(0.04f),
                            Color.Transparent
                        )
                    )
                )
                .background(Color(0xF208080C)) // Deep Navy matching web
                .border(1.dp, Color.White.copy(0.1f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            leftItems.forEach { item ->
                NavButton(
                    item = item,
                    isActive = currentTab == item.tab,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(item.tab)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            CenterVoltButton(
                isActive = currentTab == AppTab.HOME || currentTab == AppTab.AI_COACH,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(AppTab.HOME)
                },
                modifier = Modifier.weight(1.8f)
            )

            rightItems.forEach { item ->
                NavButton(
                    item = item,
                    isActive = currentTab == item.tab,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val interactionSource = remember { MutableInteractionSource() }
    
    val iconScale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = NavSpring,
        label = "icon_scale"
    )

    Column(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            // Liquid Indicator behind icon
            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = scaleIn(animationSpec = NavSpring) + fadeIn(),
                exit = scaleOut(animationSpec = NavSpring) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(vb.accent.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )
            }
            
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isActive) vb.accent else Color(0xFFAEB5C1),
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale)
            )
        }
        
        Spacer(Modifier.height(2.dp))

        // Active Dot (Matching web's nav-dot-indicator)
        Box(modifier = Modifier.height(6.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(vb.accent)
                        .shadow(8.dp, CircleShape, spotColor = vb.accent)
                )
            }
        }
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
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = NavSpring,
        label = "center_scale"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .scale(scale)
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
            modifier = Modifier
                .clip(CircleShape)
                .then(if (isActive) Modifier.background(vb.accent.copy(0.1f)) else Modifier)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = "Inicio",
                tint = if (isActive) vb.accent else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp).graphicsLayer {
                    if (isActive) {
                        shadowElevation = 8f
                    }
                }
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = "VOLT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize = 12.sp
                ),
                color = if (isActive) vb.accent else Color(0xFF9CA3AF)
            )
        }
        
        // Liquid glow for the center button too
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(vb.accent.copy(0.05f), Color.Transparent)
                        )
                    )
            )
        }
    }
}
