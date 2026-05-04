package com.voltbody.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.perform
import com.voltbody.app.util.rememberHaptic

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
        AnimatedContent(
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

        // Build path from valid points
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
                // Smooth cubic Bezier
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

@Composable
fun StaggeredEntrance(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L) // Faster stagger
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
        label = "staggered_alpha"
    )

    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggered_offset"
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
