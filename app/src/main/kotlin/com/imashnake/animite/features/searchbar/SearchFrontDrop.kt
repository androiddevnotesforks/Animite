package com.imashnake.animite.features.searchbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imashnake.animite.R
import com.imashnake.animite.api.anilist.sanitize.search.Search
import com.imashnake.animite.api.anilist.type.MediaType
import com.imashnake.animite.core.Constants
import com.imashnake.animite.core.extensions.copy
import com.imashnake.animite.core.extensions.plus
import com.imashnake.animite.core.ui.LocalPaddings
import com.imashnake.animite.core.ui.MediaCard
import com.imashnake.animite.core.R as coreR
import com.imashnake.animite.navigation.R as navigationR

/**
 * Search bar along with a Front Drop list.
 *
 * @param hasExtraPadding if the search bar should have extra bottom padding to accommodate the
 * [com.imashnake.animite.navigation.NavigationBar].
 * @param onItemClick called when media with an ID and [MediaType] is clicked.
 * @param modifier the [Modifier] to be applied to this Front Drop.
 * @param viewModel [SearchViewModel] instance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFrontDrop(
    hasExtraPadding: Boolean,
    onItemClick: (Int, MediaType, String?) -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
    viewModel: SearchViewModel = viewModel()
) {
    val insetPaddingValues = contentWindowInsets.asPaddingValues()

    val searchMediaType = MediaType.ANIME
    viewModel.setMediaType(searchMediaType)
    val searchList by viewModel.searchList.collectAsState()

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val searchBarBottomPadding by animateDpAsState(
        targetValue = if (hasExtraPadding) {
            dimensionResource(navigationR.dimen.navigation_bar_height)
        } else 0.dp,
        label = "translate_search_bar"
    )
    val frontDropColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background.copy(
            alpha = if (isExpanded) 0.95f else 0f
        ),
        animationSpec = tween(Constants.CROSSFADE_DURATION),
        label = "show_front_drop"
    )

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawRect(frontDropColor) }
    )

    searchList.data?.let {
        AnimatedVisibility(
            visible = it.isNotEmpty(),
            enter = fadeIn(tween(750)),
            exit = fadeOut(tween(750)),
        ) {
            SearchList(
                searchList = it,
                modifier = Modifier.imeNestedScroll(),
                contentPadding = insetPaddingValues,
                searchBarBottomPadding = searchBarBottomPadding,
                onItemClick = { id, title ->
                    isExpanded = false
                    viewModel.setQuery(null)
                    onItemClick(id, searchMediaType, title)
                }
            )
        }
    }

    SearchFab(
        isExpanded = isExpanded,
        setExpanded = { isExpanded = it },
        onSearched = viewModel::setQuery,
        modifier = modifier
            .padding(insetPaddingValues.copy(bottom = 0.dp))
            .padding(bottom = searchBarBottomPadding)
            .navigationBarsPadding()
            .consumeWindowInsets(PaddingValues(bottom = searchBarBottomPadding))
            .imePadding()
            .height(dimensionResource(R.dimen.search_bar_height))
    )
}

@Composable
fun SearchList(
    searchList: List<Search>,
    searchBarBottomPadding: Dp,
    onItemClick: (Int, String?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier
            .consumeWindowInsets(
                PaddingValues(
                    bottom = searchBarBottomPadding + contentPadding.calculateBottomPadding()
                )
            )
            .imePadding(),
        contentPadding = PaddingValues(
            LocalPaddings.current.large
        ) + PaddingValues(
            bottom = LocalPaddings.current.large +
                    dimensionResource(R.dimen.search_bar_height) +
                    searchBarBottomPadding
        ) + contentPadding,
        verticalArrangement = Arrangement.spacedBy(LocalPaddings.current.small)
    ) {
        items(searchList.size, key = { searchList[it].id }) { index ->
            SearchItem(
                item = searchList[index],
                onClick = onItemClick,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun SearchItem(
    item: Search,
    onClick: (Int, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(coreR.dimen.media_card_corner_radius)))
            .clickable { onClick(item.id, item.title) }
    ) {
        MediaCard(
            image = item.coverImage,
            label = null,
            onClick = { onClick(item.id, item.title) },
        )

        Column(Modifier.padding(horizontal = LocalPaddings.current.small)) {
            Text(
                text = item.title.orEmpty(),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2
            )
            if (item.seasonYear != null) {
                Text(
                    text = item.seasonYear.toString(),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(Modifier.size(LocalPaddings.current.medium))

            Text(
                text = item.studios.joinToString(),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    item.format.string.takeIf { it.isNotEmpty() },
                    item.episodes?.let { "$it episodes" }
                ).joinToString(" ꞏ "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
