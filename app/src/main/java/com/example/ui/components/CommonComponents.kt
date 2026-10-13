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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FocusBlueDark
import com.example.ui.theme.FocusBluePrimary
import com.example.ui.theme.FocusStatusAmber
import com.example.ui.theme.FocusStatusGreen
import com.example.ui.theme.FocusStatusRed

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, dotColor, label) = when (status.uppercase()) {
        "ACTIVE" -> Quad(
            Color(0xFFDCFCE7),
            Color(0xFF166534),
            FocusStatusGreen,
            "FOCUS ACTIVE"
        )
        "BREAK" -> Quad(
            Color(0xFFFEF3C7),
            Color(0xFF92400E),
            FocusStatusAmber,
            "BREAK ALLOWED"
        )
        else -> Quad(
            Color(0xFFF1F5F9),
            Color(0xFF475569),
            Color(0xFF94A3B8),
            "INACTIVE"
        )
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun DeviceRestrictionBadge(
    isEnforced: Boolean,
    isReady: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when {
        isEnforced -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF166534),
            "Enforced & Protected"
        )
        isReady -> Triple(
            Color(0xFFDBEAFE),
            Color(0xFF1E40AF),
            "Permissions Ready"
        )
        else -> Triple(
            Color(0xFFFEE2E2),
            Color(0xFF991B1B),
            "Setup Incomplete"
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
