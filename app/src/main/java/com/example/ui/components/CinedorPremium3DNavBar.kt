package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CinedorGradientColors
import com.example.ui.theme.CinedorMagenta
import com.example.ui.theme.CinedorPink
import com.example.ui.theme.CinedorViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextSecondary

enum class CinedorNavDestination(val label: String, val tag: String) {
    HOME("Home", "nav_home"),
    EXPLORE("Explore", "nav_explore"),
    REELS("Shorts", "nav_reels"),
    MY_LIST("My List", "nav_library"),
    ME("Me", "nav_me")
}

// 3D Lighting & Shading Palettes (Consistent Top-Left Specular Key Light)
private val Active3DHighlight = Color(0xFFF5D0FE)
private val Active3DViolet = Color(0xFF9F6BFF)
private val Active3DMagenta = Color(0xFFE048F5)
private val Active3DPink = Color(0xFFFF4D94)
private val Active3DDeepShadow = Color(0xFF3E0F5C)

private val Inactive3DHighlight = Color(0xFFC2C9D6)
private val Inactive3DMid = Color(0xFF7B8396)
private val Inactive3DDark = Color(0xFF3B4152)
private val Inactive3DDeepShadow = Color(0xFF141720)

private val DockTopColor = Color(0xFF181B24)
private val DockBottomColor = Color(0xFF0C0E13)

/**
 * Premium 3D Glossy MovieBox-Layout Bottom Navigation Bar for Cinedor.
 * - Opaque beveled obsidian dock extending cleanly behind system gesture bar
 * - Sculpted multi-layered 3D icons with extrusion shadows, metallic/glass gradients,
 *   specular highlights, ambient floor glow, and tactile spring lift on selection
 * - Signature raised 3D domed-glass center "Shorts" orb
 */
@Composable
fun CinedorPremium3DNavBar(
    currentDestination: CinedorNavDestination,
    shortsThumbUrl: String?,
    onSelectDestination: (CinedorNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Solid Beveled Obsidian 3D Dock (covers navigation bar inset cleanly without poster bleed)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    ambientColor = CinedorViolet.copy(alpha = 0.25f),
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(DockTopColor, DockBottomColor)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            CinedorViolet.copy(alpha = 0.34f),
                            CinedorMagenta.copy(alpha = 0.55f),
                            CinedorPink.copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                )
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Premium3DNavSlot(
                    destination = CinedorNavDestination.HOME,
                    selected = currentDestination == CinedorNavDestination.HOME,
                    badgeText = null,
                    showGemDot = false,
                    onClick = { onSelectDestination(CinedorNavDestination.HOME) },
                    modifier = Modifier.weight(1f)
                )
                Premium3DNavSlot(
                    destination = CinedorNavDestination.EXPLORE,
                    selected = currentDestination == CinedorNavDestination.EXPLORE,
                    badgeText = "HOT",
                    showGemDot = false,
                    onClick = { onSelectDestination(CinedorNavDestination.EXPLORE) },
                    modifier = Modifier.weight(1f)
                )
                // Center stage reserved for the raised 3D Shorts Orb
                Spacer(modifier = Modifier.weight(1.18f))
                Premium3DNavSlot(
                    destination = CinedorNavDestination.MY_LIST,
                    selected = currentDestination == CinedorNavDestination.MY_LIST,
                    badgeText = null,
                    showGemDot = false,
                    onClick = { onSelectDestination(CinedorNavDestination.MY_LIST) },
                    modifier = Modifier.weight(1f)
                )
                Premium3DNavSlot(
                    destination = CinedorNavDestination.ME,
                    selected = currentDestination == CinedorNavDestination.ME,
                    badgeText = null,
                    showGemDot = true,
                    onClick = { onSelectDestination(CinedorNavDestination.ME) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Raised Center 3D Domed-Glass "Shorts" Orb
        Shorts3DOrb(
            selected = currentDestination == CinedorNavDestination.REELS,
            thumbUrl = shortsThumbUrl,
            onClick = { onSelectDestination(CinedorNavDestination.REELS) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        )
    }
}

@Composable
private fun Premium3DNavSlot(
    destination: CinedorNavDestination,
    selected: Boolean,
    badgeText: String?,
    showGemDot: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav3d_scale_${destination.name}"
    )
    val liftOffset by animateDpAsState(
        targetValue = if (selected) (-2.5).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav3d_lift_${destination.name}"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        label = "nav3d_label_${destination.name}"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .height(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = CinedorMagenta.copy(alpha = 0.25f)),
                onClick = onClick
            )
            .testTag(destination.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = liftOffset)
                .size(width = 48.dp, height = 34.dp)
        ) {
            // Soft 3D Ambient Neon Glow behind selected icon
            if (selected) {
                Canvas(modifier = Modifier.size(44.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CinedorMagenta.copy(alpha = 0.40f),
                                CinedorViolet.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.56f
                        )
                    )
                }
            }

            // Sculpted 3D Vector Canvas Icon
            Canvas(
                modifier = Modifier
                    .size(30.dp)
                    .scale(iconScale)
            ) {
                when (destination) {
                    CinedorNavDestination.HOME -> drawHome3DIcon(selected)
                    CinedorNavDestination.EXPLORE -> drawExplore3DIcon(selected)
                    CinedorNavDestination.MY_LIST -> drawMyList3DIcon(selected)
                    CinedorNavDestination.ME -> drawMe3DIcon(selected)
                    CinedorNavDestination.REELS -> Unit
                }
            }

            // 3D Glossy Floating "HOT" Pill Badge
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp)
                        .shadow(6.dp, RoundedCornerShape(6.dp), ambientColor = CinedorPink, spotColor = CinedorPink)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Active3DViolet, Active3DMagenta, Active3DPink)
                            )
                        )
                        .border(
                            width = 0.7.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.72f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 4.5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 7.5.sp,
                            lineHeight = 9.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White
                    )
                }
            } else if (showGemDot && !selected) {
                // 3D Glowing Ruby-Pink Sphere Gem
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 1.dp)
                        .size(9.dp)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CinedorPink.copy(alpha = 0.55f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.65f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFC1DC), CinedorPink, Color(0xFF9D174D)),
                            center = Offset(size.width * 0.36f, size.height * 0.32f),
                            radius = size.minDimension * 0.45f
                        ),
                        radius = size.minDimension * 0.38f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f),
                        radius = size.minDimension * 0.11f,
                        center = Offset(size.width * 0.36f, size.height * 0.32f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = if (selected) 0.15.sp else 0.sp
            ),
            color = labelColor,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Luminous 3D Floor Pill Indicator under Active Tab
        Box(
            modifier = Modifier
                .width(if (selected) 18.dp else 0.dp)
                .height(2.5.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(CinedorGradientColors))
        )
    }
}

@Composable
private fun Shorts3DOrb(
    selected: Boolean,
    thumbUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orbScale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shorts3d_scale"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(orbScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer 3D Halo Glow Aura
        Canvas(modifier = Modifier.size(72.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CinedorMagenta.copy(alpha = if (selected) 0.58f else 0.34f),
                        CinedorViolet.copy(alpha = if (selected) 0.28f else 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.52f
                )
            )
        }

        // Physical 3D Beveled Ring + Domed Glass Poster Sphere
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = if (selected) 16.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = CinedorMagenta,
                    spotColor = CinedorPink
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Active3DHighlight, Active3DViolet, Active3DMagenta, Active3DPink, Active3DDeepShadow),
                        start = Offset(0f, 0f),
                        end = Offset(180f, 180f)
                    )
                )
                .padding(if (selected) 2.8.dp else 2.2.dp)
                .clip(CircleShape)
                .background(ObsidianCard)
                .clickable(onClick = onClick)
                .testTag(CinedorNavDestination.REELS.tag),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = "Shorts",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF3B1A5A), Color(0xFF13151C))
                            )
                        )
                )
            }

            // Cinema Depth Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.14f),
                                Color.Black.copy(alpha = 0.32f),
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
            )

            // 3D Convex Glass Lens Specular Reflection + Sculpted 3D Play Gem
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Top dome glass specular sheen
                drawOval(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        startY = h * 0.03f,
                        endY = h * 0.42f
                    ),
                    topLeft = Offset(w * 0.14f, h * 0.04f),
                    size = Size(w * 0.72f, h * 0.36f)
                )

                // Inner rim bevel light
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent,
                            CinedorPink.copy(alpha = 0.35f)
                        )
                    ),
                    radius = (w / 2f) - 1.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Center 3D Sculpted Frosted Play Button Disc
                val gemCenter = Offset(w * 0.5f, h * 0.43f)
                val gemR = w * 0.23f

                // Drop shadow under play gem
                drawCircle(
                    color = Color.Black.copy(alpha = 0.55f),
                    radius = gemR,
                    center = gemCenter + Offset(0f, 2.dp.toPx())
                )
                // Glossy 3D sphere button
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (selected) {
                            listOf(Active3DHighlight, Active3DMagenta, Active3DPink, Active3DDeepShadow)
                        } else {
                            listOf(Color.White.copy(alpha = 0.45f), Color(0xFF4C226B).copy(alpha = 0.85f), Color(0xFF1A1128).copy(alpha = 0.92f))
                        },
                        center = gemCenter + Offset(-gemR * 0.3f, -gemR * 0.32f),
                        radius = gemR * 1.45f
                    ),
                    radius = gemR,
                    center = gemCenter
                )
                // Crisp top-left glass rim on gem
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.15f))
                    ),
                    radius = gemR,
                    center = gemCenter,
                    style = Stroke(width = 1.1.dp.toPx())
                )

                // Sculpted 3D Play Triangle with depth shadow
                val triSize = gemR * 0.62f
                val triPath = Path().apply {
                    moveTo(gemCenter.x - triSize * 0.38f, gemCenter.y - triSize * 0.66f)
                    lineTo(gemCenter.x + triSize * 0.78f, gemCenter.y)
                    lineTo(gemCenter.x - triSize * 0.38f, gemCenter.y + triSize * 0.66f)
                    close()
                }
                val shadowTri = Path().apply {
                    moveTo(gemCenter.x - triSize * 0.38f, gemCenter.y - triSize * 0.66f + 1.2.dp.toPx())
                    lineTo(gemCenter.x + triSize * 0.78f, gemCenter.y + 1.2.dp.toPx())
                    lineTo(gemCenter.x - triSize * 0.38f, gemCenter.y + triSize * 0.66f + 1.2.dp.toPx())
                    close()
                }
                drawPath(shadowTri, color = Color.Black.copy(alpha = 0.4f))
                drawPath(
                    path = triPath,
                    brush = Brush.linearGradient(
                        listOf(Color.White, Color(0xFFF5D0FE))
                    )
                )
            }

            Text(
                text = "Shorts",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.2.sp
                ),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
            )
        }
    }
}

/**
 * 1) 3D Sculpted Cinema Home Icon
 * Layered depth extrusion + glossy beveled gable roof + frosted glowing cinema doorway + specular sheen.
 */
private fun DrawScope.drawHome3DIcon(selected: Boolean) {
    val w = size.width
    val h = size.height
    val depth = w * 0.055f

    val bodyGradient = if (selected) {
        Brush.linearGradient(
            colors = listOf(Active3DViolet, Active3DMagenta, Active3DPink),
            start = Offset(w * 0.15f, h * 0.15f),
            end = Offset(w * 0.88f, h * 0.92f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Inactive3DHighlight, Inactive3DMid, Inactive3DDark),
            start = Offset(w * 0.15f, h * 0.15f),
            end = Offset(w * 0.88f, h * 0.92f)
        )
    }

    val roofGradient = if (selected) {
        Brush.linearGradient(
            colors = listOf(Active3DHighlight, Active3DViolet, Active3DMagenta, Active3DPink),
            start = Offset(w * 0.1f, h * 0.08f),
            end = Offset(w * 0.92f, h * 0.56f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFDCE2EE), Inactive3DHighlight, Inactive3DMid, Inactive3DDark),
            start = Offset(w * 0.1f, h * 0.08f),
            end = Offset(w * 0.92f, h * 0.56f)
        )
    }

    val shadowColor = if (selected) Active3DDeepShadow else Inactive3DDeepShadow

    fun houseBodyPath(dx: Float, dy: Float) = Path().apply {
        val left = w * 0.20f + dx
        val right = w * 0.80f + dx
        val top = h * 0.42f + dy
        val bottom = h * 0.87f + dy
        val r = w * 0.11f
        moveTo(left, top)
        lineTo(w * 0.50f + dx, h * 0.20f + dy)
        lineTo(right, top)
        lineTo(right, bottom - r)
        quadraticTo(right, bottom, right - r, bottom)
        lineTo(left + r, bottom)
        quadraticTo(left, bottom, left, bottom - r)
        close()
    }

    // 1. 3D Extruded Depth Base
    drawPath(houseBodyPath(depth, depth * 1.15f), color = shadowColor)

    // 2. Main Sculpted House Body
    drawPath(houseBodyPath(0f, 0f), brush = bodyGradient)

    // Subtle dark inner eave shadow cast by overhanging 3D roof
    val eaveShadow = Path().apply {
        moveTo(w * 0.20f, h * 0.46f)
        lineTo(w * 0.50f, h * 0.24f)
        lineTo(w * 0.80f, h * 0.46f)
        lineTo(w * 0.80f, h * 0.53f)
        lineTo(w * 0.50f, h * 0.31f)
        lineTo(w * 0.20f, h * 0.53f)
        close()
    }
    drawPath(eaveShadow, color = Color.Black.copy(alpha = 0.26f))

    // 3. Sculpted Overhanging 3D Gable Roof
    fun roofPath(dx: Float, dy: Float) = Path().apply {
        moveTo(w * 0.10f + dx, h * 0.47f + dy)
        lineTo(w * 0.46f + dx, h * 0.14f + dy)
        quadraticTo(w * 0.50f + dx, h * 0.10f + dy, w * 0.54f + dx, h * 0.14f + dy)
        lineTo(w * 0.90f + dx, h * 0.47f + dy)
    }
    drawPath(
        path = roofPath(depth * 0.6f, depth * 0.9f),
        color = shadowColor.copy(alpha = 0.85f),
        style = Stroke(width = w * 0.145f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = roofPath(0f, 0f),
        brush = roofGradient,
        style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 4. Crisp White Top-Left Specular Roof Reflection
    val roofGloss = Path().apply {
        moveTo(w * 0.14f, h * 0.43f)
        lineTo(w * 0.48f, h * 0.13f)
    }
    drawPath(
        path = roofGloss,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = if (selected) 0.78f else 0.52f), Color.Transparent)
        ),
        style = Stroke(width = w * 0.042f, cap = StrokeCap.Round)
    )

    // 5. Recessed Frosted-Glass Cinema Doorway Pill
    val doorW = w * 0.22f
    val doorH = h * 0.29f
    val doorLeft = (w - doorW) / 2f
    val doorTop = h * 0.58f
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.30f),
        topLeft = Offset(doorLeft + w * 0.015f, doorTop + h * 0.02f),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(doorW * 0.48f, doorW * 0.48f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = if (selected) {
                listOf(Color.White.copy(alpha = 0.96f), Color(0xFFF5D0FE).copy(alpha = 0.82f))
            } else {
                listOf(Color(0xFF1F2430), Color(0xFF13161E))
            }
        ),
        topLeft = Offset(doorLeft, doorTop),
        size = Size(doorW, doorH),
        cornerRadius = CornerRadius(doorW * 0.48f, doorW * 0.48f)
    )
    // Rim highlight on house bottom-left bevel
    drawLine(
        color = Color.White.copy(alpha = if (selected) 0.38f else 0.22f),
        start = Offset(w * 0.23f, h * 0.53f),
        end = Offset(w * 0.23f, h * 0.78f),
        strokeWidth = w * 0.032f,
        cap = StrokeCap.Round
    )
}

/**
 * 2) 3D Glossy Compass Orb Icon (Explore)
 * Beveled 3D outer casing + concave recessed dark dial + split two-tone 3D diamond needle + glass crystal sheen.
 */
private fun DrawScope.drawExplore3DIcon(selected: Boolean) {
    val w = size.width
    val h = size.height
    val c = Offset(w * 0.49f, h * 0.50f)
    val outerR = w * 0.41f
    val innerR = w * 0.295f
    val depth = w * 0.055f

    val rimGradient = if (selected) {
        Brush.linearGradient(
            colors = listOf(Active3DHighlight, Active3DViolet, Active3DMagenta, Active3DPink, Active3DDeepShadow),
            start = Offset(w * 0.08f, h * 0.08f),
            end = Offset(w * 0.92f, h * 0.94f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFD8DFEC), Inactive3DHighlight, Inactive3DMid, Inactive3DDark),
            start = Offset(w * 0.08f, h * 0.08f),
            end = Offset(w * 0.92f, h * 0.94f)
        )
    }

    // 1. Extruded 3D Casing Depth Shadow
    drawCircle(
        color = if (selected) Active3DDeepShadow else Inactive3DDeepShadow,
        radius = outerR,
        center = c + Offset(depth * 0.85f, depth * 1.15f)
    )

    // 2. Sculpted 3D Metallic Bezel Body
    drawCircle(
        brush = rimGradient,
        radius = outerR,
        center = c
    )

    // 3. Concave Recessed Dark Compass Basin
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (selected) {
                listOf(Color(0xFF28123F), Color(0xFF140B24), Color(0xFF0A0612))
            } else {
                listOf(Color(0xFF242936), Color(0xFF161922), Color(0xFF0E1017))
            },
            center = c + Offset(innerR * 0.22f, innerR * 0.24f),
            radius = innerR * 1.3f
        ),
        radius = innerR,
        center = c
    )

    // 4. Tiny Cardinal Etched Dots
    val tickColor = if (selected) Active3DHighlight.copy(alpha = 0.65f) else Inactive3DHighlight.copy(alpha = 0.45f)
    val tickDist = innerR * 0.78f
    val tickR = w * 0.022f
    drawCircle(tickColor, tickR, c + Offset(0f, -tickDist))
    drawCircle(tickColor, tickR, c + Offset(0f, tickDist))
    drawCircle(tickColor, tickR, c + Offset(-tickDist, 0f))
    drawCircle(tickColor, tickR, c + Offset(tickDist, 0f))

    // 5. Split Two-Tone 3D Diamond Compass Needle
    val tipNE = c + Offset(innerR * 0.62f, -innerR * 0.62f)
    val tipSW = c + Offset(-innerR * 0.62f, innerR * 0.62f)
    val sideNW = c + Offset(-innerR * 0.22f, -innerR * 0.22f)
    val sideSE = c + Offset(innerR * 0.22f, innerR * 0.22f)

    // Needle drop shadow
    val needleShadow = Path().apply {
        moveTo(tipNE.x + 1.dp.toPx(), tipNE.y + 1.4.dp.toPx())
        lineTo(sideSE.x + 1.dp.toPx(), sideSE.y + 1.4.dp.toPx())
        lineTo(tipSW.x + 1.dp.toPx(), tipSW.y + 1.4.dp.toPx())
        lineTo(sideNW.x + 1.dp.toPx(), sideNW.y + 1.4.dp.toPx())
        close()
    }
    drawPath(needleShadow, color = Color.Black.copy(alpha = 0.55f))

    // Bright upper-right North blade
    val northBlade = Path().apply {
        moveTo(tipNE.x, tipNE.y)
        lineTo(sideSE.x, sideSE.y)
        lineTo(sideNW.x, sideNW.y)
        close()
    }
    drawPath(
        northBlade,
        brush = Brush.linearGradient(
            colors = if (selected) listOf(Color.White, Active3DPink) else listOf(Color(0xFFE2E8F0), Inactive3DHighlight),
            start = tipNE,
            end = c
        )
    )

    // Shaded lower-left South blade
    val southBlade = Path().apply {
        moveTo(tipSW.x, tipSW.y)
        lineTo(sideNW.x, sideNW.y)
        lineTo(sideSE.x, sideSE.y)
        close()
    }
    drawPath(
        southBlade,
        brush = Brush.linearGradient(
            colors = if (selected) listOf(Active3DViolet, Active3DDeepShadow) else listOf(Inactive3DMid, Inactive3DDark),
            start = c,
            end = tipSW
        )
    )

    // Raised Glossy Center Pin
    drawCircle(
        color = Color.White,
        radius = w * 0.055f,
        center = c
    )

    // 6. Curved Top-Left Specular Glass Bezel Arc
    drawArc(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = if (selected) 0.82f else 0.55f), Color.Transparent)
        ),
        startAngle = 200f,
        sweepAngle = 82f,
        useCenter = false,
        topLeft = Offset(c.x - outerR + w * 0.03f, c.y - outerR + w * 0.03f),
        size = Size((outerR - w * 0.03f) * 2f, (outerR - w * 0.03f) * 2f),
        style = Stroke(width = w * 0.042f, cap = StrokeCap.Round)
    )
}

/**
 * 3) 3D Stacked Ribbon Bookmark Icon (My List)
 * Offset frosted back card + extruded 3D front ribbon + specular edge sheen + embossed 3D sparkle star.
 */
private fun DrawScope.drawMyList3DIcon(selected: Boolean) {
    val w = size.width
    val h = size.height
    val depth = w * 0.052f

    // 1. Layered Back 3D Card peeking top-right for true stacked parallax depth
    val backPath = Path().apply {
        val l = w * 0.34f
        val r = w * 0.84f
        val t = h * 0.08f
        val b = h * 0.72f
        val rad = w * 0.11f
        moveTo(l + rad, t)
        lineTo(r - rad, t)
        quadraticTo(r, t, r, t + rad)
        lineTo(r, b)
        lineTo(l, b)
        lineTo(l, t + rad)
        quadraticTo(l, t, l + rad, t)
        close()
    }
    drawPath(
        path = backPath,
        brush = Brush.linearGradient(
            colors = if (selected) {
                listOf(Active3DViolet.copy(alpha = 0.85f), Color(0xFF4C1D95).copy(alpha = 0.85f))
            } else {
                listOf(Color(0xFF646D82).copy(alpha = 0.78f), Color(0xFF282D3B).copy(alpha = 0.90f))
            }
        )
    )
    drawPath(
        path = backPath,
        color = Color.White.copy(alpha = if (selected) 0.36f else 0.20f),
        style = Stroke(width = w * 0.025f)
    )

    // 2. Front Sculpted Bookmark Ribbon Path
    fun frontRibbon(dx: Float, dy: Float) = Path().apply {
        val l = w * 0.17f + dx
        val r = w * 0.71f + dx
        val t = h * 0.20f + dy
        val b = h * 0.89f + dy
        val notchY = h * 0.73f + dy
        val midX = (l + r) / 2f
        val rad = w * 0.11f
        moveTo(l + rad, t)
        lineTo(r - rad, t)
        quadraticTo(r, t, r, t + rad)
        lineTo(r, b - w * 0.03f)
        quadraticTo(r, b + w * 0.02f, r - w * 0.05f, b - w * 0.01f)
        lineTo(midX, notchY)
        lineTo(l + w * 0.05f, b - w * 0.01f)
        quadraticTo(l, b + w * 0.02f, l, b - w * 0.03f)
        lineTo(l, t + rad)
        quadraticTo(l, t, l + rad, t)
        close()
    }

    // Extruded 3D Shadow Base
    drawPath(
        path = frontRibbon(depth, depth * 1.15f),
        color = if (selected) Active3DDeepShadow else Inactive3DDeepShadow
    )

    // Glossy 3D Front Ribbon Surface
    drawPath(
        path = frontRibbon(0f, 0f),
        brush = if (selected) {
            Brush.linearGradient(
                colors = listOf(Active3DHighlight, Active3DViolet, Active3DMagenta, Active3DPink),
                start = Offset(w * 0.15f, h * 0.18f),
                end = Offset(w * 0.75f, h * 0.90f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFFD5DCE8), Inactive3DHighlight, Inactive3DMid, Inactive3DDark),
                start = Offset(w * 0.15f, h * 0.18f),
                end = Offset(w * 0.75f, h * 0.90f)
            )
        }
    )

    // Top-Left Specular Glass Bevel Highlight
    drawLine(
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = if (selected) 0.85f else 0.55f), Color.Transparent)
        ),
        start = Offset(w * 0.24f, h * 0.24f),
        end = Offset(w * 0.62f, h * 0.24f),
        strokeWidth = w * 0.038f,
        cap = StrokeCap.Round
    )

    // Embossed 4-Point Sparkle Star in center of ribbon
    val sc = Offset(w * 0.44f, h * 0.47f)
    val outerS = w * 0.13f
    val innerS = w * 0.042f
    val starPath = Path().apply {
        moveTo(sc.x, sc.y - outerS)
        lineTo(sc.x + innerS, sc.y - innerS)
        lineTo(sc.x + outerS, sc.y)
        lineTo(sc.x + innerS, sc.y + innerS)
        lineTo(sc.x, sc.y + outerS)
        lineTo(sc.x - innerS, sc.y + innerS)
        lineTo(sc.x - outerS, sc.y)
        lineTo(sc.x - innerS, sc.y - innerS)
        close()
    }
    drawPath(
        path = starPath,
        color = if (selected) Color.White.copy(alpha = 0.94f) else Color(0xFF1A1F2B)
    )
}

/**
 * 4) 3D Shaded Glass Avatar Bust Icon (Me)
 * True off-center shaded 3D sphere head + sculpted extruded shoulders dome + specular gloss reflection.
 */
private fun DrawScope.drawMe3DIcon(selected: Boolean) {
    val w = size.width
    val h = size.height
    val depth = w * 0.052f
    val shadowColor = if (selected) Active3DDeepShadow else Inactive3DDeepShadow

    // 1. Sculpted 3D Shoulders Dome Path
    fun shouldersPath(dx: Float, dy: Float) = Path().apply {
        val left = w * 0.14f + dx
        val right = w * 0.86f + dx
        val top = h * 0.55f + dy
        val bottom = h * 0.88f + dy
        val r = w * 0.14f
        moveTo(left, bottom - r * 0.7f)
        quadraticTo(left, top, w * 0.50f + dx, top)
        quadraticTo(right, top, right, bottom - r * 0.7f)
        quadraticTo(right, bottom, right - r, bottom)
        lineTo(left + r, bottom)
        quadraticTo(left, bottom, left, bottom - r * 0.7f)
        close()
    }

    // Shoulders 3D depth extrusion
    drawPath(shouldersPath(depth * 0.9f, depth * 1.1f), color = shadowColor)

    // Shoulders 3D glossy gradient surface
    drawPath(
        path = shouldersPath(0f, 0f),
        brush = if (selected) {
            Brush.linearGradient(
                colors = listOf(Active3DViolet, Active3DMagenta, Active3DPink),
                start = Offset(w * 0.14f, h * 0.54f),
                end = Offset(w * 0.88f, h * 0.92f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(Color(0xFFCCD4E3), Inactive3DMid, Inactive3DDark),
                start = Offset(w * 0.14f, h * 0.54f),
                end = Offset(w * 0.88f, h * 0.92f)
            )
        }
    )

    // Specular curved highlight along left shoulder slope
    val shoulderGloss = Path().apply {
        moveTo(w * 0.21f, h * 0.73f)
        quadraticTo(w * 0.24f, h * 0.60f, w * 0.44f, h * 0.58f)
    }
    drawPath(
        path = shoulderGloss,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = if (selected) 0.72f else 0.45f), Color.Transparent)
        ),
        style = Stroke(width = w * 0.042f, cap = StrokeCap.Round)
    )

    // 2. True 3D Shaded Spherical Head
    val headCenter = Offset(w * 0.50f, h * 0.30f)
    val headR = w * 0.215f

    // Head drop shadow onto collar
    drawCircle(
        color = shadowColor.copy(alpha = 0.85f),
        radius = headR,
        center = headCenter + Offset(depth * 0.75f, depth * 1.15f)
    )

    // 3D Off-center radial lit sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = if (selected) {
                listOf(
                    Color.White,
                    Active3DHighlight,
                    Active3DMagenta,
                    Active3DViolet,
                    Active3DDeepShadow
                )
            } else {
                listOf(
                    Color(0xFFEDF2FA),
                    Inactive3DHighlight,
                    Inactive3DMid,
                    Inactive3DDark,
                    Inactive3DDeepShadow
                )
            },
            center = headCenter + Offset(-headR * 0.34f, -headR * 0.34f),
            radius = headR * 1.55f
        ),
        radius = headR,
        center = headCenter
    )

    // Crisp Specular Gloss Dot on Head Sphere
    drawOval(
        color = Color.White.copy(alpha = if (selected) 0.82f else 0.55f),
        topLeft = Offset(headCenter.x - headR * 0.56f, headCenter.y - headR * 0.58f),
        size = Size(headR * 0.46f, headR * 0.28f)
    )
}
