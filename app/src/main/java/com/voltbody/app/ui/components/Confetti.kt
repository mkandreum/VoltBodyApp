package com.voltbody.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private val ConfettiColors = listOf(
    Color(0xFF4ADE80), // Green
    Color(0xFFFBBF24), // Amber
    Color(0xFF38BDF8), // Sky Blue
    Color(0xFFF472B6), // Pink
    Color(0xFFA78BFA)  // Purple
)

private class Particle(
    val color: Color,
    var x: Float,
    var y: Float,
    var speedY: Float,
    var speedX: Float,
    var angle: Float,
    var rotationSpeed: Float,
    val size: Float
)

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        List(80) {
            Particle(
                color = ConfettiColors.random(),
                x = Random.nextFloat(), // relative to width
                y = Random.nextFloat() * -1f, // start above screen
                speedY = Random.nextFloat() * 1.5f + 1f,
                speedX = Random.nextFloat() * 0.4f - 0.2f,
                angle = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 10f - 5f,
                size = Random.nextFloat() * 15f + 10f
            )
        }
    }

    var tick by remember { mutableFloatStateOf(0f) }
    
    // Animation loop
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                tick = (frameTime - startTime) / 1000000f // Delta time in ms
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (tick == 0f) return@Canvas
        
        val w = size.width
        val h = size.height
        
        particles.forEach { p ->
            p.y += p.speedY * 0.015f
            p.x += p.speedX * 0.015f
            p.angle += p.rotationSpeed
            
            // Loop vertically
            if (p.y > 1.2f) {
                p.y = -0.1f
                p.x = Random.nextFloat()
            }
            
            val absoluteX = p.x * w
            val absoluteY = p.y * h
            
            rotate(degrees = p.angle, pivot = Offset(absoluteX, absoluteY)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(absoluteX - p.size/2, absoluteY - p.size/2),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
