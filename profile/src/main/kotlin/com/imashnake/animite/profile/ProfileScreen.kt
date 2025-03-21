package com.imashnake.animite.profile

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.boswelja.markdown.material3.MarkdownDocument
import com.boswelja.markdown.material3.m3TextStyles
import com.imashnake.animite.api.anilist.sanitize.profile.User
import com.imashnake.animite.core.data.Resource
import com.imashnake.animite.core.extensions.animiteBlockQuoteStyle
import com.imashnake.animite.core.extensions.animiteCodeBlockStyle
import com.imashnake.animite.core.extensions.copy
import com.imashnake.animite.core.extensions.crossfadeModel
import com.imashnake.animite.core.extensions.horizontalOnly
import com.imashnake.animite.core.extensions.maxHeight
import com.imashnake.animite.core.extensions.plus
import com.imashnake.animite.core.ui.FallbackMessage
import com.imashnake.animite.core.ui.LocalPaddings
import com.imashnake.animite.core.ui.NestedScrollableContent
import com.imashnake.animite.core.ui.ProgressIndicatorScreen
import com.imashnake.animite.core.ui.layouts.BannerLayout
import com.imashnake.animite.media.MediaPage
import com.imashnake.animite.profile.tabs.AboutTab
import com.imashnake.animite.profile.tabs.FavouritesTab
import com.imashnake.animite.profile.tabs.MediaTab
import com.imashnake.animite.profile.tabs.ProfileTab
import kotlinx.coroutines.launch
import com.imashnake.animite.core.R as coreR
import com.imashnake.animite.navigation.R as navigationR

@Suppress("LongMethod")
@Composable
fun ProfileScreen(
    onNavigateToMediaItem: (MediaPage) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val insetPaddingValues = contentWindowInsets.asPaddingValues()
    val navigationComponentPaddingValues = when(LocalConfiguration.current.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> PaddingValues(
            bottom = dimensionResource(navigationR.dimen.navigation_bar_height)
        )
        else -> PaddingValues(
            start = dimensionResource(navigationR.dimen.navigation_rail_width)
        )
    }
    val allPaddingValues = insetPaddingValues + navigationComponentPaddingValues

    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)
    val viewer by viewModel.viewer.collectAsState()
    val viewerAnimeLists by viewModel.viewerAnimeLists.collectAsState()
    val viewerMangaLists by viewModel.viewerMangaLists.collectAsState()

    val data = listOf(viewer, viewerAnimeLists, viewerMangaLists)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoggedIn -> when {
                data.all { it is Resource.Success } -> viewer.data?.run {
                    BannerLayout(
                        banner = {
                            Box {
                                AsyncImage(
                                    model = crossfadeModel(banner),
                                    contentDescription = "banner",
                                    modifier = it,
                                    contentScale = ContentScale.Crop
                                )
                                AsyncImage(
                                    model = crossfadeModel(avatar),
                                    contentDescription = "avatar",
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = LocalPaddings.current.medium)
                                        .padding(allPaddingValues.horizontalOnly)
                                        .size(100.dp),
                                )
                            }
                        },
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(LocalPaddings.current.medium)) {
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = LocalPaddings.current.large)
                                        .padding(allPaddingValues.horizontalOnly)
                                ) {
                                    Text(
                                        text = name,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        style = MaterialTheme.typography.titleLarge,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    UserDescription(
                                        description = description,
                                        modifier = Modifier.maxHeight(dimensionResource(R.dimen.user_about_height))
                                    )
                                }
                                UserTabs(
                                    user = this@run,
                                    animeCollection = viewerAnimeLists.data,
                                    mangaCollection = viewerMangaLists.data,
                                    onNavigateToMediaItem = onNavigateToMediaItem,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    contentPadding = navigationComponentPaddingValues + insetPaddingValues,
                                )
                            }
                        },
                        contentPadding = PaddingValues(top = LocalPaddings.current.large / 2)
                    )
                }
                else -> ProgressIndicatorScreen(Modifier.padding(allPaddingValues))
            }
            else -> Login(Modifier.padding(allPaddingValues))
        }
    }
}

@Composable
private fun UserDescription(description: String?, modifier: Modifier = Modifier) {
    description?.let {
        Box(modifier) {
            NestedScrollableContent { contentModifier ->
                MarkdownDocument(
                    markdown = it,
                    // TODO: Fix typography and make this an `animiteTextStyle()`.
                    textStyles = m3TextStyles().copy(
                        textStyle = m3TextStyles().textStyle.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
                        )
                    ),
                    blockQuoteStyle = animiteBlockQuoteStyle(),
                    codeBlockStyle = animiteCodeBlockStyle(),
                    modifier = contentModifier,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTabs(
    user: User,
    animeCollection: User.MediaCollection?,
    mangaCollection: User.MediaCollection?,
    onNavigateToMediaItem: (MediaPage) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { ProfileTab.entries.size })
    val titles = ProfileTab.entries
    val onBackground = MaterialTheme.colorScheme.onBackground
    val horizontalContentPadding = contentPadding.horizontalOnly

    Column(modifier) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            divider = {},
            modifier = Modifier.padding(horizontalContentPadding)
        ) {
            titles.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = stringResource(tab.titleRes),
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onBackground.copy(
                                alpha = if (pagerState.currentPage == index) 1f else 0.5f
                            ),
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .padding(
                            horizontal = LocalPaddings.current.ultraTiny,
                            vertical = LocalPaddings.current.small
                        )
                        .clip(CircleShape)
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            onBackground.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
        ) { page ->

            val tabContentPadding = PaddingValues(
                all = LocalPaddings.current.large
            ) + contentPadding.copy(top = 0.dp)

            Box(Modifier.fillMaxSize()) {
                when (ProfileTab.entries[page]) {
                    ProfileTab.ABOUT -> AboutTab(
                        user = user,
                        contentPadding = tabContentPadding,
                    )
                    ProfileTab.ANIME -> MediaTab(
                        mediaCollection = animeCollection,
                        onNavigateToMediaItem = onNavigateToMediaItem,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        contentPadding = tabContentPadding,
                    )
                    ProfileTab.MANGA -> MediaTab(
                        mediaCollection = mangaCollection,
                        onNavigateToMediaItem = onNavigateToMediaItem,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        contentPadding = tabContentPadding,
                    )
                    ProfileTab.FAVOURITES -> FavouritesTab(
                        favouriteLists = user.favourites,
                        onNavigateToMediaItem = onNavigateToMediaItem,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        contentPadding = tabContentPadding,
                    )
                    else -> FallbackMessage(
                        message = stringResource(coreR.string.coming_soon),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(tabContentPadding)
                    )
                }
            }
        }
    }
}
