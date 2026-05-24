package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SearchEngine

@Composable
fun EngineDial(
    selectedEngine: SearchEngine,
    onEngineSelected: (SearchEngine) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Rotation animation for the Lens circle center
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High luxury lens header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BlurOn,
                contentDescription = null,
                tint = Color(selectedEngine.hexColor),
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotationAngle)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "NEBULA APERTURE / 星云透镜引擎舱",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
        }

        // Circular Frosted Glass Selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.75f),
                            Color.White.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.82f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchEngine.entries.forEach { engine ->
                    val isSelected = engine == selectedEngine
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 0.9f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f),
                        label = "engineScale"
                    )

                    // Frosted Glass Lens Item Design
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .scale(animatedScale)
                            .shadow(
                                elevation = if (isSelected) 8.dp else 0.dp,
                                shape = RoundedCornerShape(20.dp),
                                clip = false,
                                ambientColor = Color(engine.hexColor).copy(alpha = 0.15f),
                                spotColor = Color(engine.hexColor).copy(alpha = 0.3f)
                            )
                            .background(
                                brush = if (isSelected) {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(engine.hexColor),
                                            Color(engine.hexColor).copy(alpha = 0.85f)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = if (isSelected) {
                                        listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)
                                    } else {
                                        listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f))
                                    }
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEngineSelected(engine)
                            }
                            .testTag("engine_dial_item_${engine.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Circular Lens Icon Center
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (isSelected) Color.White.copy(alpha = 0.22f) else Color(engine.hexColor).copy(alpha = 0.08f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = engine.logoChar,
                                    color = if (isSelected) Color.White else Color(engine.hexColor),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = engine.displayName,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
