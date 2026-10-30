package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserSettings
import com.example.model.SearchEngine
import com.example.model.ThemeMode
import com.example.ui.theme.VarelaRoundFontFamily
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(
    settings: BrowserSettings,
    onSetSearchEngine: (SearchEngine) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onToggleAdShield: () -> Unit,
    onToggleSpeedDials: () -> Unit,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPages = 4
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_ambient_glow")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("welcome_screen")
    ) {
        // Subtle ambient Material 3 Expressive background backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = (w * 0.65f) * pulseProgress
                ),
                center = Offset(w * 0.85f, h * 0.12f),
                radius = (w * 0.65f) * pulseProgress
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.82f),
                    radius = (w * 0.6f) * (2f - pulseProgress)
                ),
                center = Offset(w * 0.15f, h * 0.82f),
                radius = (w * 0.6f) * (2f - pulseProgress)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
        ) {
            // Top Header Bar with Brand Pill, Step Badge & Skip Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = "Aura Browser",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aura Browser",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = VarelaRoundFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / $totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = VarelaRoundFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("welcome_step_counter")
                        )
                    }

                    if (pagerState.currentPage < totalPages - 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = onCompleteOnboarding,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("welcome_skip_button")
                        ) {
                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = VarelaRoundFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Onboarding Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("welcome_pager")
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> WelcomeHeroPage()
                    1 -> WelcomeAutomationPage()
                    2 -> WelcomeDevToolsAndPrivacyPage(
                        adShieldEnabled = settings.adShieldEnabled,
                        onToggleAdShield = onToggleAdShield
                    )
                    3 -> WelcomePersonalizePage(
                        settings = settings,
                        onSetSearchEngine = onSetSearchEngine,
                        onSetThemeMode = onSetThemeMode,
                        onToggleSpeedDials = onToggleSpeedDials
                    )
                }
            }

            // Bottom Navigation & Page Indicators Bar
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    // Animated Expressive Page Indicator Dots
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("welcome_page_indicators")
                    ) {
                        repeat(totalPages) { index ->
                            val isSelected = pagerState.currentPage == index
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 32.dp else 10.dp,
                                animationSpec = tween(durationMillis = 280),
                                label = "dot_width_$index"
                            )
                            val dotColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                animationSpec = tween(durationMillis = 280),
                                label = "dot_color_$index"
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(10.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                    .testTag("welcome_indicator_$index")
                            )
                        }
                    }

                    // Action Buttons Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("welcome_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous step",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Back",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = VarelaRoundFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val isLastPage = pagerState.currentPage == totalPages - 1

                        Button(
                            onClick = {
                                if (isLastPage) {
                                    onCompleteOnboarding()
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag(
                                    if (isLastPage) "welcome_get_started_button" else "welcome_next_button"
                                )
                        ) {
                            AnimatedContent(
                                targetState = isLastPage,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "cta_button_label"
                            ) { finalStep ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (finalStep) {
                                        Icon(
                                            imageVector = Icons.Default.RocketLaunch,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Start Browsing",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = VarelaRoundFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Continue",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = VarelaRoundFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeroPage() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("welcome_page_0")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Expressive Orbital Emblem Card
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.size(170.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.24f), Color.Transparent),
                            center = center,
                            radius = size.minDimension / 1.8f
                        ),
                        center = center,
                        radius = size.minDimension / 1.8f
                    )
                    drawCircle(
                        color = primary.copy(alpha = 0.3f),
                        radius = size.minDimension * 0.42f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawArc(
                        color = secondary,
                        startAngle = -35f,
                        sweepAngle = 120f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = tertiary,
                        startAngle = 145f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Aura Globe",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NEXT-GEN MOBILE BROWSER",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = VarelaRoundFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Welcome to Aura Browser",
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = VarelaRoundFontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Experience a fast, distraction-free web crafted with Material 3 Expressive design, smart horizontal Recents, and autonomous web tools.",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = VarelaRoundFontFamily,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.History,
            accentColor = MaterialTheme.colorScheme.primary,
            title = "Smart Horizontal Recents",
            description = "Clean home screen that automatically surfaces your favorite websites once you visit them multiple times."
        )

        Spacer(modifier = Modifier.height(10.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.Speed,
            accentColor = MaterialTheme.colorScheme.tertiary,
            title = "Instant Search & Discovery",
            description = "Unified search bar with instant domain resolution, clipboard link detection, and multi-engine switching."
        )

        Spacer(modifier = Modifier.height(10.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.Palette,
            accentColor = MaterialTheme.colorScheme.secondary,
            title = "Varela Round Expressive UI",
            description = "Crisp, friendly Varela Round typography paired with fluid animations and adaptive dark/light themes."
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WelcomeAutomationPage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("welcome_page_1")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Automation Engine",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Autonomous Web Automation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = VarelaRoundFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Scan, grab, and automate complex web forms, bottom sheets, and multi-step workflows with one tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = VarelaRoundFontFamily,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.TouchApp,
            accentColor = MaterialTheme.colorScheme.primary,
            title = "Deep DOM & Shadow Scanner",
            description = "Grabs inputs, custom dropdowns, country-code pickers, and multi-stage birthday overlays across Shadow DOM and iframes."
        )

        Spacer(modifier = Modifier.height(10.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.TableChart,
            accentColor = MaterialTheme.colorScheme.secondary,
            title = "1-by-1 DataSheet Execution",
            description = "Bind datasets for emails, phones, names, and dates with Top-to-Bottom or Bottom-to-Top iteration and exhaustion rules."
        )

        Spacer(modifier = Modifier.height(10.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.Bolt,
            accentColor = MaterialTheme.colorScheme.tertiary,
            title = "Smart Page-Load Synchronization",
            description = "Automatically waits for network readiness, active spinners, and DOM mounting before executing every action."
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WelcomeDevToolsAndPrivacyPage(
    adShieldEnabled: Boolean,
    onToggleAdShield: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("welcome_page_2")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy & DevTools",
                            tint = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pro DevTools & Privacy Shield",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = VarelaRoundFontFamily,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Inspect live DOM trees, view HTML source, sniff real network requests, and block intrusive trackers.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = VarelaRoundFontFamily,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Ad & Tracker Shield Toggle Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = 1.dp,
                color = if (adShieldEnabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable { onToggleAdShield() }
                .testTag("welcome_ad_shield_toggle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ad & Tracker Shield",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = VarelaRoundFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (adShieldEnabled) {
                            "Active — Blocking third-party ad networks & telemetry"
                        } else {
                            "Disabled — Tap to enable real-time ad & tracker blocking"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = VarelaRoundFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = adShieldEnabled,
                    onCheckedChange = { onToggleAdShield() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.Code,
            accentColor = MaterialTheme.colorScheme.primary,
            title = "Live HTML Source & DOM Inspector",
            description = "Inspect any element on the page, edit live text or CSS styles, and run JavaScript in the built-in console."
        )

        Spacer(modifier = Modifier.height(10.dp))

        WelcomeFeatureHighlightCard(
            icon = Icons.Default.Radar,
            accentColor = MaterialTheme.colorScheme.secondary,
            title = "Real-Time Network Sniffer & Proxy",
            description = "Capture live XHR, Fetch, and resource traffic with full headers and status codes, plus 1-tap Proxy Node routing."
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WelcomePersonalizePage(
    settings: BrowserSettings,
    onSetSearchEngine: (SearchEngine) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onToggleSpeedDials: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("welcome_page_3")
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Personalize Your Browser",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = VarelaRoundFontFamily,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose your preferred search engine and visual theme before you start browsing.",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = VarelaRoundFontFamily,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Search Engine Selection Section
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Default Search Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = VarelaRoundFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val engines = SearchEngine.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    engines.chunked(2).forEach { rowEngines ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowEngines.forEach { engine ->
                                val isSelected = settings.searchEngine == engine
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onSetSearchEngine(engine) }
                                        .testTag("welcome_engine_${engine.name.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Text(
                                            text = engine.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontFamily = VarelaRoundFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Theme Mode Selection Section
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Appearance & Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = VarelaRoundFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val themeOptions = listOf(
                        Triple(ThemeMode.SYSTEM, "Auto", Icons.Default.SettingsBrightness),
                        Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
                        Triple(ThemeMode.DARK, "Dark", Icons.Default.DarkMode)
                    )
                    themeOptions.forEach { (mode, label, icon) ->
                        val isSelected = settings.themeMode == mode
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSetThemeMode(mode) }
                                .testTag("welcome_theme_${mode.name.lowercase()}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = VarelaRoundFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Smart Recents Toggle Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable { onToggleSpeedDials() }
                .testTag("welcome_recents_toggle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Frequent Recents Row",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = VarelaRoundFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Automatically display frequently visited websites on Home",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = VarelaRoundFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showSpeedDials,
                    onCheckedChange = { onToggleSpeedDials() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun WelcomeFeatureHighlightCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = VarelaRoundFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = VarelaRoundFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
