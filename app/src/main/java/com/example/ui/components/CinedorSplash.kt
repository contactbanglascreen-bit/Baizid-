package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated entry screen: the red ribbon "C" logo springs in with a glow,
 * the CINEDOR wordmark spreads into place, then the app fades in.
 */
@Composable
fun CinedorSplash(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordAlpha = remember { Animatable(0f) }
    val spacing = remember { Animatable(22f) }
    val barProgress = remember { Animatable(0f) }

    val glow = rememberInfiniteTransition(label = "glow")
    val glowPulse by glow.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse"
    )

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(450)) }
        launch {
            logoScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch { barProgress.animateTo(1f, tween(1700, easing = LinearEasing)) }
        delay(350)
        launch { wordAlpha.animateTo(1f, tween(550)) }
        spacing.animateTo(8f, tween(800, easing = FastOutSlowInEasing))
        delay(650)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080607)),
        contentAlignment = Alignment.Center
    ) {
        // Soft red glow behind the logo
        Box(
            modifier = Modifier
                .size(340.dp)
                .graphicsLayer { alpha = 0.55f * glowPulse * logoAlpha.value }
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFE50914).copy(alpha = 0.55f), Color(0xFF7A0710).copy(alpha = 0.18f), Color.Transparent)
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.cinedor_logo),
                contentDescription = "Cinedor",
                modifier = Modifier
                    .size(150.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append("CINE") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Black, color = Color(0xFFFF2A35))) { append("DOR") }
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    letterSpacing = spacing.value.sp
                ),
                color = Color.White,
                modifier = Modifier.alpha(wordAlpha.value)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Movies  ·  Series  ·  Anime  ·  Live",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp),
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.alpha(wordAlpha.value)
            )
        }

        // Thin loading bar at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
                .width(140.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barProgress.value)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF2A35), Color(0xFFA80B14))))
            )
        }
    }
}
