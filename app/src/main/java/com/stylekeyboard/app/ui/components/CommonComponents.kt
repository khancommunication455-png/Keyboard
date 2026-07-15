package com.stylekeyboard.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stylekeyboard.app.ui.theme.AccentCyan
import com.stylekeyboard.app.ui.theme.AccentPurple
import com.stylekeyboard.app.ui.theme.Charcoal
import com.stylekeyboard.app.ui.theme.Elevated
import com.stylekeyboard.app.ui.theme.GradientEnd
import com.stylekeyboard.app.ui.theme.GradientStart
import com.stylekeyboard.app.ui.theme.SuccessGreen
import com.stylekeyboard.app.ui.theme.TextPrimary
import com.stylekeyboard.app.ui.theme.TextSecondary

/**
 * Reusable panel with the signature purple→cyan gradient outline and a bold
 * rounded border. Used by the converted-text preview panel and other highlight
 * areas across the app.
 */
@Composable
fun GradientOutlinePanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = Elevated,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(colors = listOf(GradientStart, GradientEnd)),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Elevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            this.content()
        }
    }
}

@Composable
fun StatusDot(active: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (active) SuccessGreen else AccentCyan, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun MonoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .background(Charcoal, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, Elevated), RoundedCornerShape(8.dp))
            .padding(12.dp),
        textStyle = TextStyle(
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        ),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
            inner()
        }
    )
}

@Composable
fun GlowingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlineColor: Color = AccentPurple
) {
    val bg = if (enabled) Brush.linearGradient(listOf(GradientStart, GradientEnd)) else Brush.linearGradient(listOf(Elevated, Elevated))
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            Modifier
                .background(bg, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
