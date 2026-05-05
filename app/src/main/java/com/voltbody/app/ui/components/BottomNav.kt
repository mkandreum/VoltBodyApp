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
import androidx.compose.ui.text.font.FontWeight
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
            .padding(horizontal = 8.dp)
            .padding(bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x0AFFFFFF),
                            Color(0x00FFFFFF)
                        )
                    )
                )
                .background(Color(0xF208080C))
                .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = NavSpring,
        label = "icon_scale"
    )

    Column(
        modifier = modifier
            .height(52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(vb.accent.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )
            }
            
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isActive) vb.accent else Color(0xFFAEB5C1),
                modifier = Modifier
                    .size(18.dp)
                    .scale(scale)
            )
        }
        
        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = vb.accent,
                modifier = Modifier.padding(top = 1.dp)
            )
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
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = NavSpring,
        label = "center_scale"
    )

    Box(
        modifier = modifier
            .height(52.dp)
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
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = "Inicio",
                tint = if (isActive) Color(0xFFFBBF24) else Color(0xFF6B7280),
                modifier = Modifier.size(22.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = "VOLTBODY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontSize = 13.sp
                ),
                color = if (isActive) vb.accent else Color(0xFF9CA3AF)
            )
        }
    }
}
