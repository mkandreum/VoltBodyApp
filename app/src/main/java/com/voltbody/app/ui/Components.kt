package com.voltbody.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.theme.Volt

/** Tarjeta oscura redondeada del estilo del mockup. */
@Composable
fun VoltCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.clip(shape).background(Volt.surface).border(1.dp, Volt.border, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(padding),
        content = content,
    )
}

/** Banner degradado (como "Stay Active" del mockup). */
@Composable
fun GradientBanner(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF2DD4BF), Volt.accent)))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(.8f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Volt.accent)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, color = Color.Black.copy(.7f), fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = Volt.accent, fontSize = 12.sp, modifier = Modifier.clickable { onAction?.invoke() })
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier, color: Color = Volt.accent) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Volt.textDim, fontSize = 11.sp)
    }
}

/** Boton principal neon (pill, texto negro). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick, enabled = enabled && !loading, modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(27.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Volt.accent, contentColor = Color.Black, disabledContainerColor = Volt.surface2, disabledContentColor = Volt.textDim),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
        else Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun voltFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Volt.accent, unfocusedBorderColor = Volt.border, focusedLabelColor = Volt.accent,
    unfocusedLabelColor = Volt.textDim, cursorColor = Volt.accent, focusedTextColor = Volt.text, unfocusedTextColor = Volt.text,
    focusedContainerColor = Volt.surface, unfocusedContainerColor = Volt.surface,
)

@Composable
fun VoltTextField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text, password: Boolean = false,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine, modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), colors = voltFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
    )
}

/** Chip seleccionable. */
@Composable
fun VoltChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(if (selected) Volt.accent else Volt.surface2).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(text, color = if (selected) Color.Black else Volt.text, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(text, color = Volt.textDim, fontSize = 14.sp) }
}
