package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinetorRed
import com.example.ui.theme.CinetorRedSoft
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class CinedorNavDestination(val label: String, val tag: String) {
    HOME("Home", "nav_home"),
    EXPLORE("Explore", "nav_explore"),
    REELS("Shorts", "nav_reels"),
    MY_LIST("My List", "nav_library"),
    ME("Me", "nav_me")
}

private fun CinedorNavDestination.icon(selected: Boolean): ImageVector = when (this) {
    CinedorNavDestination.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    CinedorNavDestination.EXPLORE -> if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore
    CinedorNavDestination.REELS -> if (selected) Icons.Rounded.PlayCircle else Icons.Outlined.PlayCircle
    CinedorNavDestination.MY_LIST -> if (selected) Icons.Rounded.Bookmarks else Icons.Outlined.Bookmarks
    CinedorNavDestination.ME -> if (selected) Icons.Rounded.Person else Icons.Outlined.Person
}

/**
 * Floating frosted-glass pill bottom navigation.
 * - White translucent capsule with a soft red rim and shadow
 * - Selected tab gets a soft red pill behind a red icon, with a small red marker on top
 *
 * Signature is unchanged; [shortsThumbUrl] is kept for API compatibility.
 */
@Composable
fun CinedorPremium3DNavBar(
    currentDestination: CinedorNavDestination,
    shortsThumbUrl: String?,
    onSelectDestination: (CinedorNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(36.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = pillShape,
                    ambientColor = CinetorRed.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.28f)
                )
                .clip(pillShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFF7F2F2).copy(alpha = 0.94f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color(0xFFE9DEDE),
                            CinetorRed.copy(alpha = 0.28f),
                            Color(0xFFE9DEDE)
                        )
                    ),
                    shape = pillShape
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinedorNavDestination.entries.forEach { dest ->
                NavPillSlot(
                    destination = dest,
                    selected = dest == currentDestination,
                    onClick = { onSelectDestination(dest) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavPillSlot(
    destination: CinedorNavDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_scale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) CinetorRed else Color(0xFF5F6470),
        label = "nav_icon_color"
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) CinetorRedSoft else Color.Transparent,
        label = "nav_indicator_color"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = CinetorRed),
                onClick = onClick
            )
            .testTag(destination.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .width(54.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(indicatorColor)
                    .then(
                        if (selected) {
                            Modifier.border(1.dp, CinetorRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = destination.icon(selected),
                    contentDescription = destination.label,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CinetorRed)
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = if (selected) TextPrimary else TextSecondary,
            maxLines = 1
        )
    }
}
