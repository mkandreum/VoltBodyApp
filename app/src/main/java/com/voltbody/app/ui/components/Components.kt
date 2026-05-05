package com.voltbody.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic



// ── LevelUpDialog — premium achievement popup ────────────────────────────────

@Composable
fun LevelUpDialog(
    level: Int,
    onDismiss: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    Dialog(onDismissRequest = onDismiss) {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                    ConfettiOverlay(Modifier.fillMaxSize())
                    // FIX: Use NeonBadge + Icon instead of bare emoji for level-up visual.
                    // Icon is accessible, scalable and brand-consistent.
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = vb.accent,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Text(
                    "\u00a1NUEVO NIVEL!",
                    style = UppercaseLabel.copy(fontSize = 14.sp, letterSpacing = 2.sp),
                    color = vb.accent
                )
                Text(
                    "Has alcanzado el Nivel $level",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Sigue entrenando duro para desbloquear m\u00e1s recompensas y funciones IA.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = vb.textMuted,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = vb.accent,
                        contentColor = ColorBlack
                    )
                ) {
                    Text("\u00a1VAMOS!", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}



// ── ExerciseGifPlayer — plays technique GIFs ─────────────────────────────────

@Composable
fun ExerciseGifPlayer(
    gifUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(vb.surfaceElevated)
            .border(1.dp, vb.border.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (gifUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(gifUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "T\u00e9cnica del ejercicio",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = vb.accent.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// ── AppCard — glass morphism card ─────────────────────────────────────────────

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "card_scale"
    )

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = 16.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = vb.accent.copy(alpha = 0.15f),
            spotColor = vb.accent.copy(alpha = 0.05f)
        )
        .clip(RoundedCornerShape(20.dp))
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(vb.surfaceElevated.copy(alpha = 0.9f), vb.surface.copy(alpha = 0.8f))
            )
        )
        .border(
            width = 1.dp,
            color = vb.border.copy(alpha = 0.5f),
            shape = RoundedCornerShape(20.dp)
        )

    if (onClick != null) {
        Surface(
            onClick = {
                haptic.perform(HapticType.TICK)
                onClick()
            },
            modifier = cardModifier,
            color = Color.Transparent,
            shape = RoundedCornerShape(20.dp),
            interactionSource = interactionSource
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Column(modifier = cardModifier.padding(16.dp), content = content)
    }
}

// ── StatPill — small badge with value + label ─────────────────────────────────

@Composable
fun StatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = LocalVoltBodyColors.current.accent
) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(vb.surface)
            .border(1.dp, vb.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = value,
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> -height } + fadeOut()
                )
            },
            label = "number_roll"
        ) { targetValue ->
            Text(
                text = targetValue,
                style = MonoMetric.copy(
                    fontSize = 18.sp,
                    shadow = Shadow(color = accentColor.copy(alpha = 0.4f), blurRadius = 12f)
                ),
                color = accentColor,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = label.uppercase(),
            style = UppercaseLabel,
            color = vb.textMuted
        )
    }
}

// ── SectionHeader ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val vb = LocalVoltBodyColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = UppercaseLabel,
            color = vb.textMuted
        )
        trailing?.invoke()
    }
}

// ── CircularProgress ring ─────────────────────────────────────────────────────

@Composable
fun CircularProgressRing(
    value: Float,              // 0f – 1f
    modifier: Modifier = Modifier.size(64.dp),
    strokeWidth: Dp = 4.dp,
    trackColor: Color = ColorBorder,
    fillColor: Color = LocalVoltBodyColors.current.accent,
    label: String? = null
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "ring"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension / 2) - strokeWidth.toPx()
            val sweepAngle = animatedValue * 360f
            drawCircle(
                color = trackColor,
                radius = radius,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            rotate(-90f) {
                drawArc(
                    color = fillColor,
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        if (label != null) {
            Text(text = label, style = UppercaseLabel.copy(fontSize = 9.sp), color = ColorWhite)
        }
    }
}

// ── ShimmerBox — loading skeleton ─────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val vb = LocalVoltBodyColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer_progress"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .drawWithCache {
                val gradient = Brush.linearGradient(
                    colors = listOf(
                        vb.surface,
                        vb.surfaceElevated,
                        vb.surface
                    ),
                    start = Offset(size.width * (shimmerProgress - 0.3f), 0f),
                    end = Offset(size.width * (shimmerProgress + 0.3f), size.height)
                )
                onDrawBehind { drawRect(gradient) }
            }
    )
}

// ── HomeShimmerSkeleton ───────────────────────────────────────────────────────

@Composable
fun HomeShimmerSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(176.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ShimmerBox(modifier = Modifier.weight(1f).height(120.dp))
            ShimmerBox(modifier = Modifier.weight(1f).height(120.dp))
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(176.dp))
    }
}

// ── AccentDivider ─────────────────────────────────────────────────────────────

@Composable
fun AccentDivider(modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, vb.accentDim, Color.Transparent)
                )
            )
    )
}

// ── NeonBadge ─────────────────────────────────────────────────────────────────

@Composable
fun NeonBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalVoltBodyColors.current.accent
) {
    Text(
        text = text,
        style = UppercaseLabel,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ── MacroBadge ────────────────────────────────────────────────────────────────

@Composable
fun MacroBadge(label: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "${value}$label",
            style = MaterialTheme.typography.labelSmall,
            color = ColorWhite
        )
    }
}

// ── SettingsRow ───────────────────────────────────────────────────────────────

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isLast: Boolean = false
) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = vb.accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorWhite,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
                contentDescription = null,
                tint = vb.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(vb.border.copy(alpha = 0.1f))
            )
        }
    }
}

// ── SimpleLineChart (Canvas) ──────────────────────────────────────────────────

@Composable
fun SimpleLineChart(
    data: List<Float?>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    lineColor: Color = LocalVoltBodyColors.current.accent,
    fillColor: Color = LocalVoltBodyColors.current.chartFill
) {
    if (data.isEmpty()) return

    val validData = data.mapIndexed { i, v -> i to v }.filter { it.second != null }
    if (validData.isEmpty()) return

    val minVal = validData.minOf { it.second!! }
    val maxVal = validData.maxOf { it.second!! }
    val range = (maxVal - minVal).coerceAtLeast(0.1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padT = 8f
        val padB = 24f
        val chartH = h - padT - padB
        val stepX = w / (data.size - 1).coerceAtLeast(1)

        fun xOf(i: Int) = i * stepX
        fun yOf(v: Float) = padT + chartH * (1f - (v - minVal) / range)

        // Build path from valid points using smooth cubic Bezier
        val path = Path()
        val fillPath = Path()
        var started = false
        validData.forEachIndexed { idx, (i, v) ->
            val x = xOf(i)
            val y = yOf(v!!)
            if (!started) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - padB)
                fillPath.lineTo(x, y)
                started = true
            } else {
                // Smooth cubic Bezier — avoids sharp corners on volume charts
                val prev = validData[idx - 1]
                val px = xOf(prev.first)
                val py = yOf(prev.second!!)
                val cx = (px + x) / 2
                path.cubicTo(cx, py, cx, y, x, y)
                fillPath.cubicTo(cx, py, cx, y, x, y)
            }
        }
        // Close fill path
        val lastX = xOf(validData.last().first)
        fillPath.lineTo(lastX, h - padB)
        fillPath.close()

        // Draw fill
        drawPath(fillPath, fillColor)
        // Draw line
        drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Draw data points
        validData.forEach { (i, v) ->
            drawCircle(lineColor, radius = 3.dp.toPx(), center = Offset(xOf(i), yOf(v!!)))
        }
    }
}

// ── StaggeredEntrance — premium entrance animation ───────────────────────────
// FIX: Added `key` parameter so the animation re-triggers when the content
// identity changes (e.g. new workout data loaded). Without this, re-compositions
// from data changes don't replay the entrance since `appeared` is already true.
// Also documents the required contract: caller MUST pass unique, consecutive
// indices — duplicate indices cause simultaneous entry, breaking the choreography.

@Composable
fun StaggeredEntrance(
    index: Int,
    key: Any? = Unit,
    content: @Composable () -> Unit
) {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        // FIX: reset visibility so animation replays on key change
        visible = false
        kotlinx.coroutines.delay(index * 60L)
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "staggered_alpha_$index"
    )

    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggered_offset_$index"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY.toPx()
            }
    ) {
        content()
    }
}
