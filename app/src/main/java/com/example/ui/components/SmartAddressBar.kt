package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SearchEngine

@Composable
fun SmartAddressBar(
    currentUrl: String,
    progress: Int,
    scrollProgress: Float,
    engine: SearchEngine,
    tabCount: Int,
    isBookmarked: Boolean,
    onNavigateTo: (String) -> Unit,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadClick: () -> Unit,
    onHomeClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onTabToggleClick: () -> Unit,
    onEngineBadgeClick: () -> Unit,
    canGoBack: Boolean = true,
    canGoForward: Boolean = true,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var textState by remember(currentUrl) { mutableStateOf(if (currentUrl == "about:blank") "" else currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showActionDrawer by remember { mutableStateOf(false) }

    val progressAnim by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = spring(stiffness = 180f),
        label = "webProgressAnim"
    )

    val scrollAnim by animateFloatAsState(
        targetValue = scrollProgress,
        animationSpec = spring(stiffness = 150f),
        label = "scrollProgressAnim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = Color(0x111C1E1B),
                spotColor = Color(0x331C1E1B)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.82f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(12.dp)
            .testTag("smart_address_bar")
    ) {
        // High Class Horizon Detail: Smart reading track embedded inside
        if (progress >= 100 && currentUrl.isNotEmpty() && currentUrl != "about:blank") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(scrollAnim)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
        }

        // Web Loading progressing meter
        if (progress > 0 && progress < 100) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(3.dp)
                    .background(Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.tertiary,
                                    Color(0xFFE2C074)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main address and inputs region
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Engine Dynamic Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(engine.hexColor).copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, Color(engine.hexColor).copy(alpha = 0.25f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onEngineBadgeClick() }
                    .testTag("engine_badge"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = engine.logoChar,
                    color = Color(engine.hexColor),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Neo Web Address Input Capsule
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(21.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isEditing) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(21.dp)
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (!isEditing && textState.isEmpty()) {
                    Text(
                        text = "AURA / 搜索或输入网址",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        isEditing = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart)
                        .testTag("url_text_field"),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            isEditing = false
                            onNavigateTo(textState)
                        }
                    )
                )

                // Quick Close clear
                if (isEditing && textState.isNotEmpty()) {
                    IconButton(
                        onClick = { textState = "" },
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterEnd)
                            .testTag("clear_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Bookmark Sparkle
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("bookmark_toggle")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark site",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // workspaces count indicator
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                    .clip(CircleShape)
                    .clickable { onTabToggleClick() }
                    .testTag("tab_switcher_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool Control navigation docks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                enabled = canGoBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Previous page",
                    tint = if (canGoBack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onForwardClick,
                enabled = canGoForward,
                modifier = Modifier.testTag("forward_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Forward page",
                    tint = if (canGoForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Floating Home Orb
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                    .clickable { onHomeClick() }
                    .testTag("home_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home dashboard",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onReloadClick,
                modifier = Modifier.testTag("reload_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Menu triggers
            IconButton(
                onClick = { showActionDrawer = !showActionDrawer },
                modifier = Modifier.testTag("action_drawer_toggle")
            ) {
                Icon(
                    imageVector = if (showActionDrawer) Icons.Default.KeyboardArrowDown else Icons.Default.Menu,
                    contentDescription = "Options menu",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded Glassmorphism Utility Tray
        AnimatedVisibility(
            visible = showActionDrawer,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Copy action button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(currentUrl))
                                showActionDrawer = false
                            }
                            .testTag("copy_url_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "复制链接",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Share action button (Simulated dynamic trigger)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .clickable { showActionDrawer = false }
                            .testTag("share_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "分享网址",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
