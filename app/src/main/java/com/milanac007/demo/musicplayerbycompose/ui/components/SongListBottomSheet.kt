package com.milanac007.demo.musicplayerbycompose.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.milanac007.demo.musicplayerbycompose.R
import com.milanac007.demo.musicplayerbycompose.models.Music
import com.milanac007.demo.musicplayerbycompose.ui.theme.label_light_green
import com.milanac007.demo.musicplayerbycompose.viewModels.PlayerCommand
import com.milanac007.demo.musicplayerbycompose.viewModels.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/*
snapshotFlow:
背景：
LazyColumn 是按需渲染的，也就是说它不会一次性加载所有的项，而是根据可见区域进行懒加载。这使得它的布局在初次渲染时可能尚未完全完成，尤其在数据量比较大的时候。为了确保滚动操作（比如 scrollToItem）是在布局完成之后进行的，我们需要等待 LazyColumn 渲染好之后再执行滚动。
snapshotFlow 是 Compose 提供的一个 API，它能够监听 Compose UI 状态的变化，并且触发响应的操作。这里的 listState.layoutInfo.totalItemsCount 就是 LazyListState 里一个非常有用的属性，表示列表中的项目总数。

snapshotFlow { listState.layoutInfo.totalItemsCount }
    .filter { it > 0 } // 过滤，确保有项目加载出来
    .first() // 等待直到 `totalItemsCount` 大于0，表明列表已加载完成

这段代码的作用：
snapshotFlow 会监听 listState.layoutInfo.totalItemsCount 的变化。这个值表示当前 LazyColumn 列表的总项目数（即总共多少个项）。
由于 LazyColumn 是按需加载的，这个值会在列表开始渲染时为 0，直到项目被加载完毕，才会更新为真实的项目总数。

.filter { it > 0 }:
这个 filter 操作会筛选掉不符合条件的项。它确保我们只关心当项目数大于 0 时的情况，避免在 LazyColumn 还没有加载任何项目时触发不必要的操作。

.first():
.first() 是一个挂起函数，它会一直等待直到第一个符合条件的值出现。在这个例子中，就是等待 totalItemsCount > 0 的时候。
一旦 totalItemsCount 大于 0，它会返回并继续执行后续操作。

为什么要用这段代码？
这段代码的目的是确保在列表渲染完成并且有项目可见时才执行后续操作，比如滚动到特定项目的逻辑。
如果你在列表还未完全加载时就执行 scrollToItem()，可能会因为没有项目被渲染出来而导致滚动失败或滚动位置不准确。通过这个 snapshotFlow，你可以确保等到列表真正有内容时再执行滚动操作。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListBottomSheet(
    showBottomSheet: Boolean,
    onBottomSheetDismiss: () -> Unit,
    song: Music? = null,
    songList: List<Music>? = null,
    dataType: String? = "local",
    repeatMode: RepeatMode = RepeatMode.Order,
    onCommand: (PlayerCommand) -> Unit = {},
) {
    val sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true, //跳过中间状态. skipPartiallyExpanded - 如果BottomSheet足够高，是否应跳过部分展开状态。如果为 true，则工作表将始终展开至展开状态，并在隐藏工作表时（无论是通过编程方式还是用户交互）切换至隐藏状态。
    )
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = if (dataType == "favorite") 1 else 0 )
    val scope = rememberCoroutineScope()

    if (showBottomSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = onBottomSheetDismiss,
        ) {
            Surface {
                Column {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,  //此属性用于切换 TabRow的Indicator
                        containerColor = Color.Transparent,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(40.dp)
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(page = 0)
                                    }
                                }
                        ) {
                            Text(text = "本地",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(40.dp)
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(page = 1)
                                    }
                                }
                        ) {
                            Text(text = "喜欢",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .padding(horizontal = 15.dp)
                            .height(500.dp),
                    ) { page ->
                        val itemList = when(page) {
                            0 -> songList
                            1 -> songList?.filter { it.isFavorite == true }
                            else -> emptyList()
                        }

                        val listState = rememberLazyListState()
                        LaunchedEffect(itemList) {
                            snapshotFlow { listState.layoutInfo.totalItemsCount }
                                .filter { it > 0 }
                                .first()

                            val index = itemList?.indexOfFirst { item: Music ->
                                when(page) {
                                    0 -> dataType == "local" && song?.id == item.id
                                    1 -> dataType == "favorite" && song?.id == item.id
                                    else -> false
                                }
                            } ?: -1

                            println("@@@ LaunchedEffect index: $index, listState.firstVisibleItemIndex: ${listState.firstVisibleItemIndex}, lastVisibleItemIndex: ${listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index}")
                            if (index != -1 && (index < listState.firstVisibleItemIndex || index > (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0))) {
                                listState.scrollToItem(index)
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (!itemList.isNullOrEmpty()) {
                                DataTypeAndRepeatModeItem(
                                    page = page,
                                    dataType = dataType,
                                    repeatMode = repeatMode,
                                    songList = itemList,
                                    onCommand = onCommand
                                )
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                SongListView(
                                    page = page,
                                    dataType = dataType,
                                    song = song,
                                    songList = itemList,
                                    onItemClick = {
                                        onCommand(PlayerCommand.PlaySpecific(it))
                                        if (page == 0 && dataType != "local") {
                                            onCommand(PlayerCommand.ToggleDataType("local"))
                                        } else if (page == 1 && dataType != "favorite") {
                                            onCommand(PlayerCommand.ToggleDataType("favorite"))
                                        }
                                    }
                                )
                            }
                            CloseView(
                                sheetState = sheetState,
                                onBottomSheetDismiss = onBottomSheetDismiss,
                                scope = scope,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.SongListView(
    dataType: String?,
    page: Int,
    song: Music?,
    songList: List<Music>?,
    onItemClick: (song: Music) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (songList.isNullOrEmpty()) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = modifier.fillParentMaxSize()
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_nodata),
                    contentDescription = null,
                    modifier = Modifier.width(100.dp)
                )
                Text(stringResource(R.string.tip_nodata))
            }
        }
    } else {
        itemsIndexed(songList) { index: Int, item: Music ->
            val condition = (page == 0 && dataType == "local" || page == 1 && dataType == "favorite")
                    && song?.id == item.id

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .clickable {
                        onItemClick(item)
                    }
            ) {
                Text(item.title,
                    style = if (condition) MaterialTheme.typography.bodyLarge.copy(color = label_light_green)
                    else MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("- ${item.artist}",
                    style = if (condition) MaterialTheme.typography.bodyLarge.copy(color = label_light_green)
                    else MaterialTheme.typography.bodyLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (condition) {
                    Image(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color = label_light_green),
                    )
                }
            }
            if (index < songList.size - 1) {
                HorizontalDivider(modifier = Modifier.height(0.5.dp))
            }
        }
    }
}

@Composable
fun DataTypeAndRepeatModeItem(
    page: Int = 0,
    dataType: String? = "local",
    repeatMode: RepeatMode = RepeatMode.Order,
    songList: List<Music>? = null,
    onCommand: (PlayerCommand) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.height(45.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (page == 0 && dataType == "local" || page == 1 && dataType == "favorite")
                "正在播放此列表" else "播放此列表",
            style = if (page == 0 && dataType == "local" || page == 1 && dataType == "favorite")
                MaterialTheme.typography.bodyLarge.copy(color = label_light_green) else MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .clickable {
                    onCommand(
                        if (page == 1) PlayerCommand.ToggleDataType("favorite")
                        else PlayerCommand.ToggleDataType("local")
                    )
                    songList?.also {
                        onCommand( PlayerCommand.PlaySpecific(it.get(0)))
                    }
                }
        )
        Spacer(modifier = Modifier.width(15.dp))
        IconButton(onClick = {
            onCommand(PlayerCommand.ToggleRepeat)
        }) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.Order -> Icons.Default.Repeat
                    RepeatMode.Single -> Icons.Default.RepeatOne
                    RepeatMode.Random -> Icons.Default.AllInclusive
                },
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
            )
        }
    }
    HorizontalDivider(modifier = Modifier.height(0.5.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseView(
    sheetState: SheetState,
    onBottomSheetDismiss: () -> Unit,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable {
                scope
                    .launch {
                        sheetState.hide()
                    }
                    .invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onBottomSheetDismiss()
                        }
                    }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HorizontalDivider(modifier = Modifier.height(0.5.dp))
        Text(
            text = stringResource(R.string.close),
        )
        HorizontalDivider(modifier = Modifier.height(0.5.dp))
    }
}

