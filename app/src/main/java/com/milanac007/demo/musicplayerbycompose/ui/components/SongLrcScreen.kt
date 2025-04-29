package com.milanac007.demo.musicplayerbycompose.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.milanac007.demo.musicplayerbycompose.R
import com.milanac007.demo.musicplayerbycompose.ui.theme.font_green
import com.milanac007.demo.musicplayerbycompose.ui.theme.font_select
import com.milanac007.demo.musicplayerbycompose.viewModels.PlayerCommand
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SongLrcScreen(
    isPlaying: Boolean = false,
    songTitle: String? = null,
    songArtist: String? = null,
    isFavorite: Boolean = false,
    lrcText: Pair<Long, String>? = null,
    songLrc: Map<Long, String>? = null,
    onCommand: (PlayerCommand) -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items = songLrc?.entries?.toList() ?: emptyList()
    val listState = rememberLazyListState()
    var isUserScrolling by remember { mutableStateOf(false) }
    var flingItemIndex by remember { mutableIntStateOf(-1) }


    //自动滚动到当前播放行
    LaunchedEffect(lrcText) {
        val currentIndex = songLrc?.keys?.indexOfFirst { it == lrcText?.first } ?: -1
        if (currentIndex != -1) {
            listState.animateScrollToItem(index = currentIndex, scrollOffset = -300) //小于0代表和滚动方向相反
        }
    }

    // 监听滑动手势
    LaunchedEffect(listState.interactionSource) {
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start ->  {
                    isUserScrolling = true
                }
                is DragInteraction.Stop,
                    is DragInteraction.Cancel -> {
                        isUserScrolling = false
                        println("@@@@ 手势停止, isUserScrolling: $isUserScrolling")
                }
            }
        }
    }

    // 监听isUserScrolling和 firstVisibleItemIndex 的变化，只在用户主动滚动时更新 flingItemIndex
    LaunchedEffect(listState.firstVisibleItemIndex) {
//        if (isUserScrolling) { //TODO 手指滑动 ～ 手指离开屏幕到滑动完全停止
//            // 如果可见的项数大于5，选中第4项(第5项是填充的item)
//            flingItemIndex = if (listState.layoutInfo.visibleItemsInfo.size > 5) {
//                listState.firstVisibleItemIndex + 3
//            } else {
//                -1
//            }
//        }

        if (listState.layoutInfo.visibleItemsInfo.size > 5) {
            flingItemIndex = listState.firstVisibleItemIndex + 3
        } else {
            -1
        }
    }

    // 监听 isUserScrolling 的变化，停止时delay超时后，清除 flingItemIndex，恢复UI。
    LaunchedEffect(isUserScrolling) {
        if (!isUserScrolling) {
            delay(2000)
            flingItemIndex = -1
        }
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) {
        Box {
            Column {
                Header(
                    songTitle = songTitle,
                    songArtist = songArtist,
                    isFavorite = isFavorite,
                    onToggleFavorite =  { onCommand(PlayerCommand.ToggleFavorite) },
                )

                if (items.isEmpty()) {
                    NoDataView()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(items = items, key = { it.key }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onSeekTo(it.key) }
                            ) {
                                Text(
                                    text = it.value,
                                    modifier = Modifier.weight(1f).padding(vertical = 8.dp,),
                                    style = if (lrcText?.first == it.key && lrcText?.second == it.value)
                                        MaterialTheme.typography.headlineSmall.copy(color = font_green)
                                    else if (flingItemIndex != -1 && flingItemIndex < items.size && it.key == items.get(flingItemIndex).key)
                                        MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onPrimary,)
                                    else
                                        MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary.copy(0.7f),)
                                )
                                if (flingItemIndex != -1 && flingItemIndex < items.size && it.key == items.get(flingItemIndex).key) {
                                    Text(
                                        text = formatTime(time = it.key.div(1000).toInt()),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                                        modifier = Modifier.background(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = font_select
                                        )
                                    )
                                }
                            }
                        }
                        item { //底部填充一个item,使最后几项item可以上滑
                            Spacer(modifier = Modifier.heightIn(min = LocalConfiguration.current.screenHeightDp.dp * 0.6f))
                        }
                    }
                }
            }

            PlayPauseBtn(
                onBtnClick = { onCommand(PlayerCommand.PlayPause) },
                isPlaying = isPlaying,
            )
        }

    }
}

@Composable
fun NoDataView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_nodata),
            contentDescription = null,
            modifier = Modifier.width(100.dp)
        )
        Text(stringResource(R.string.tip_nolrc),
            style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
        )
    }
}

@Composable
fun Header(
    songTitle: String? = null,
    songArtist: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: ()->Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        val (songText, artistText, favoriteBtn) = createRefs()
        Text(
            text = songTitle ?: "未知歌曲",
            style = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
            modifier = Modifier.constrainAs(songText) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        )
        Text(
            text = songArtist ?: "未知歌手",
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
            modifier = Modifier.constrainAs(artistText) {
                top.linkTo(songText.bottom, margin = 15.dp)
                start.linkTo(parent.start)
            }
        )
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .size(30.dp)
                .constrainAs(favoriteBtn) {
                    end.linkTo(parent.end)
                    centerVerticallyTo(parent)
                }
        ) {
            Icon(
                imageVector = if (isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (isFavorite == true) Color.Red else Color.White, // LocalContentColor.current,
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
            )
        }
    }
}

@Composable
fun BoxScope.PlayPauseBtn(
    isPlaying: Boolean,
    onBtnClick: ()->Unit,
) {
    IconButton(
        onClick = onBtnClick,
        modifier = Modifier
            .size(50.dp)
            .align(alignment = Alignment.BottomEnd)
            .offset(x = -30.dp, y = -50.dp)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.PauseCircleOutline else Icons.Default.PlayCircleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(50.dp)
        )
    }
}

fun formatTime(time: Int): String {
    val minute = time.div(60)
    val second = time.mod(60)
    return String.format(Locale.getDefault(), "%02d:%02d", minute, second)
}