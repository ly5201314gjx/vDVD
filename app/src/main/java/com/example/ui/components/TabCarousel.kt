package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TabModel

@Composable
fun TabCarousel(
    tabs: List<TabModel>,
    selectedTabId: String,
    onTabSelected: (String) -> Unit,
    onTabClosed: (String) -> Unit,
    onNewTabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x22FAF8F5),
                        Color(0xE6FAF8F5),
                        Color(0xFFFAF8F5)
                    )
                )
            )
            .padding(top = 18.dp, bottom = 10.dp)
    ) {
        // Shelf Header Layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ZEN GLASS WORKSPACES / 境域标签工作仓",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
            }

            // Quick Floating Launch Circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable { onNewTabClick() }
                    .testTag("add_tab_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New workspace",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Glass Carousel Container List
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("tabs_shelf"),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                val isSelected = tab.id == selectedTabId

                // Soft bouncing scale factors
                val scaleFactor by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 0.95f,
                    animationSpec = spring(stiffness = 250f),
                    label = "workspaceCardScale"
                )

                // Drag states for gestures (Swipe up dismiss)
                var dragOffsetY by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(132.dp)
                        .offset(y = dragOffsetY.dp)
                        .scale(scaleFactor)
                        .shadow(
                            elevation = if (isSelected) 14.dp else 2.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false,
                            ambientColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            spotColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isSelected) {
                                    listOf(Color.White, Color.White.copy(alpha = 0.85f))
                                } else {
                                    listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.35f))
                                }
                            )
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            brush = Brush.linearGradient(
                                colors = if (isSelected) {
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    )
                                } else {
                                    listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .pointerInput(tab.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (dragOffsetY < -50f) {
                                        onTabClosed(tab.id)
                                    } else {
                                        dragOffsetY = 0f
                                    }
                                },
                                onDragCancel = { dragOffsetY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.y < 0 || dragOffsetY < 0) {
                                        dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtMost(0f)
                                    }
                                }
                            )
                        }
                        .clickable { onTabSelected(tab.id) }
                        .testTag("tab_card_${index}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Workspace bar header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSelected) "CURRENT" else "SESSION",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Dismiss button
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)
                                    .clip(CircleShape)
                                    .clickable { onTabClosed(tab.id) }
                                    .testTag("close_tab_button_${tab.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Drop workspace",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        // Tab Titles
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = if (tab.isHome) "Aura Desk Portal" else tab.url,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Drag hint footer indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Idx #0${index + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )

                            Text(
                                text = "Swipe Up to close ↑",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}
