package com.milanac007.demo.musicplayerbycompose.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.milanac007.demo.musicplayerbycompose.ui.theme.Green
import com.milanac007.demo.musicplayerbycompose.ui.theme.GreenLight
import com.milanac007.demo.musicplayerbycompose.ui.theme.PaleDogwood
import com.milanac007.demo.musicplayerbycompose.ui.theme.Seashell
import com.milanac007.demo.musicplayerbycompose.viewModels.MusicViewModel
import kotlinx.coroutines.launch


@Composable
fun MusicMainScreen(
    myViewModel: MusicViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        myViewModel.bindService(context)
        println("@@@ MusicMainScreen LaunchedEffect, myViewModel.bindService(context)")
    }

    DisposableEffect(Unit) {
        onDispose {
            myViewModel.unbindService(context)
            println("@@@ MusicMainScreen DisposableEffect, myViewModel.unbindService(context)")
        }
    }

    val currentSong by myViewModel.currentSong.observeAsState()
    val songList by myViewModel.songListFlow.collectAsStateWithLifecycle()
    val uiState by myViewModel.uiState.collectAsStateWithLifecycle()
    val pagerState: PagerState =  rememberPagerState(pageCount = { 2 } )
    val scope = rememberCoroutineScope()
    val backgroundColor = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.inversePrimary

    /*
     使用Scaffold设置topBar时, 需手动设置Spacer以填充状态栏
     使用Scaffold的innerPadding 设置内容位于AppBar(如果有的话)或状态栏的下面、导航栏的上面

     没有Scaffold时，可在rootView设置如下：
     modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()       //内容在状态栏下面
            .navigationBarsPadding() //底部没有NavigationBar时，设置此属性，让内容在导航栏上方
     */
    Scaffold(
        containerColor = backgroundColor,
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //背景模糊
                val bmp: Bitmap
                if (!currentSong?.coverImagePath.isNullOrEmpty()) {
                    bmp = BitmapFactory.decodeFile(currentSong?.coverImagePath)
                } else { //default
                    bmp = BitmapFactory.decodeStream(LocalContext.current.assets.open("晴天 周杰伦.jpeg"))
                }
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blurBackgroundEffect()
                )
            }

            Column {
                HomeTabBar(
                    currentTabPage = pagerState.currentPage,
                    onTabSelected = {
                        scope.launch {
                            pagerState.animateScrollToPage(it)
                        }
                    }
                )
                HorizontalPager(
                    state = pagerState,
                ) { pagerIndex ->
                    if (pagerIndex == 0) {
                        PlayerControllerScreen(
                            song = currentSong,
                            songList = songList,
                            dataType = uiState.dataType,
                            repeatMode = uiState.repeatMode,
                            isPlaying = uiState.isPlaying,
                            lrcText = uiState.songLrcText,
                            sliderPosition = uiState.sliderPosition,
                            tvCurrentProgress = uiState.tvCurrentProgress,
                            tvDuration = uiState.tvDuration,
                            onSliderPositionChanged = { position ->
                                myViewModel.onSliderPositionChanged(position)
                            },
                            onStopTrackingTouch = {
                                myViewModel.onStopTrackingTouch()
                            },
                            onCommand = { command ->
                                myViewModel.sendCommand(command)
                            },
                        )
                    } else {
                        SongLrcScreen(
                            isPlaying = uiState.isPlaying,
                            songTitle = currentSong?.title,
                            songArtist = currentSong?.artist,
                            isFavorite = currentSong?.isFavorite == true,
                            lrcText = uiState.songLrcText,
                            songLrc = uiState.songLrc,
                            onCommand = { command ->
                                myViewModel.sendCommand(command)
                            },
                            onSeekTo = { pos ->
                                currentSong?.also {
                                    val progress = pos.toFloat().div(it.duration)
                                    myViewModel.onSliderPositionChanged(progress)
                                    myViewModel.onStopTrackingTouch()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTabBar(
    background: Color = Color.Transparent,
    currentTabPage: Int,
    onTabSelected: (Int) -> Unit,
) {
    Column {
        TabRow(
            selectedTabIndex = currentTabPage,
            containerColor = background,
            divider = { HorizontalDivider(color = Color.Transparent) },
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[currentTabPage]) //指示器位置控制. tabIndicatorOffset 是核心扩展函数，用于计算指示器位置
                        .padding(horizontal = 50.dp)
                        .height(2.dp), // 指示器高度
                    color = Color.White, // 指示器颜色
                )
            }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(50.dp)
                    .clickable {
                        onTabSelected(0)
                    }
            ) {
                Text(text = "歌曲",
                    style = if (currentTabPage == 0) MaterialTheme.typography.titleLarge.copy(color = Color.White)
                    else MaterialTheme.typography.titleLarge
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(50.dp)
                    .clickable {
                        onTabSelected(1)
                    }
            ) {
                Text(text = "歌词",
                    style = if (currentTabPage == 1) MaterialTheme.typography.titleLarge.copy(color = Color.White)
                    else MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MusicMainScreen()
}