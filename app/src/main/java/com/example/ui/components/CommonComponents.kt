package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchType
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusWarning

@Composable
fun StatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MatchTypeChip(matchType: MatchType, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (matchType) {
        MatchType.EXACT -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Exact")
        MatchType.CONTAINS -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Contains")
        MatchType.STARTS_WITH -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Starts with")
        MatchType.REGEX -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), "Regex")
        MatchType.FALLBACK_DEFAULT -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), "Fallback")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SenderAvatar(
    name: String,
    modifier: Modifier = Modifier,
    isGroup: Boolean = false,
    platform: String = "WhatsApp"
) {
    val initial = if (isGroup) "👥" else name.trim().take(1).uppercase().ifBlank { "?" }
    val isMessenger = platform.equals("Messenger", ignoreCase = true)
    val avatarBg = when {
        isGroup && isMessenger -> Color(0xFF1D4ED8)
        isGroup -> Color(0xFF2E7D32)
        isMessenger -> Color(0xFF2563EB)
        else -> Color(0xFF008069)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(avatarBg)
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = if (isGroup) 16.sp else 18.sp
        )
    }
}

@Composable
fun PlatformBadge(platform: String, modifier: Modifier = Modifier) {
    val isMessenger = platform.equals("Messenger", ignoreCase = true)
    val bg = if (isMessenger) Color(0xFFDBEAFE) else Color(0xFFDCFCE7)
    val fg = if (isMessenger) Color(0xFF1D4ED8) else Color(0xFF15803D)
    val label = if (isMessenger) "Messenger" else "WhatsApp"

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
