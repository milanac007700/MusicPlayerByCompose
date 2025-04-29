package com.milanac007.demo.musicplayerbycompose.ui.components

import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.milanac007.demo.musicplayerbycompose.models.Music
import com.milanac007.demo.musicplayerbycompose.ui.theme.MusicPlayerByComposeTheme
import com.milanac007.demo.musicplayerbycompose.viewModels.PlayerCommand
import com.milanac007.demo.musicplayerbycompose.viewModels.RepeatMode
import com.milanac007.demo.musicplayerbycompose.R
import com.milanac007.demo.musicplayerbycompose.ui.theme.GreenLight

@Preview
@Composable
private fun PlayerMainScreenPreview() {
    MusicPlayerByComposeTheme(
        dynamicColor = false
    ) {
        PlayerControllerScreen(
            song = Music(
                id = 0,
                title = "为你我受冷风吹",
                artist = "林忆莲",
                duration = 222000,
                size = 232000,
                path = ""
            ),
        )
    }
}

// 自定义模糊Modifier扩展
@RequiresApi(Build.VERSION_CODES.S)
fun Modifier.blurBackgroundEffect(): Modifier = composed {
    this.graphicsLayer {
        renderEffect = RenderEffect.createBlurEffect(
            this.size.width / 4,
            this.size.height / 4,
            Shader.TileMode.MIRROR
        ).asComposeRenderEffect()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControllerScreen(
    song: Music? = null,
    songList: List<Music>? = null,
    dataType: String? = null,
    repeatMode: RepeatMode = RepeatMode.Order,
    isPlaying: Boolean = false,
    lrcText: Pair<Long, String>? = null,
    sliderPosition: Float = 0f,
    tvCurrentProgress: String? = null,
    tvDuration: String? = null,
    onSliderPositionChanged: (Float) -> Unit = {
        println("onSliderPositionChanged, $it")
    },
    onStopTrackingTouch:()-> Unit = {},
    onCommand: (PlayerCommand) -> Unit = {
        println("onStopTrackingTouch")
    },
    modifier: Modifier = Modifier
) {
    var showBottomSheet by  remember { mutableStateOf(false) }

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    song?.coverImagePath?.apply {
                        AnimatedContent(
                            targetState = this,
                            transitionSpec = { // 定义动画：旧图片淡出，新图片淡入
                                fadeIn(animationSpec = tween(2000)) togetherWith fadeOut(animationSpec = tween(2000))
                            },
                            modifier = Modifier.align(alignment = Alignment.Center)
                        ) { targetImagePath ->
                            Image(
                                bitmap = BitmapFactory.decodeFile(targetImagePath).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(fraction = 0.75f),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    ConstraintLayout(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                    ) {
                        val (songText, artistText, theLrcText, slider, progressText, durationText, favoriteBtn) = createRefs()
                        Text(
                            text = song?.title ?: "",
                            style = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
                            modifier = Modifier.constrainAs(songText) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                            }
                        )
                        Text(
                            text = song?.artist ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                            modifier = Modifier.constrainAs(artistText) {
                                top.linkTo(songText.bottom, margin = 15.dp)
                                start.linkTo(parent.start)
                            }
                        )
                        Text(
                            text = lrcText?.second ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .constrainAs(theLrcText) {
                                    top.linkTo(artistText.bottom, margin = 15.dp)
                                    start.linkTo(parent.start)
                                }
                        )
                        IconButton(
                            onClick = {
                                onCommand(PlayerCommand.ToggleFavorite)
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .constrainAs(favoriteBtn) {
                                    end.linkTo(parent.end)
                                    bottom.linkTo(songText.bottom)
                                }
                        ) {
                            Icon(
                                imageVector = if (song?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                tint = if (song?.isFavorite == true) Color.Red else Color.White, // LocalContentColor.current,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                            )
                        }

                        Slider(
                            enabled = song != null,
                            value = sliderPosition,
                            onValueChange = onSliderPositionChanged,
                            onValueChangeFinished = onStopTrackingTouch,
                            track = { sliderPosition ->
                                Box( //轨道
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(
                                            shape = RoundedCornerShape(2.dp),
                                            color = MaterialTheme.colorScheme.outline, //Color.Gray
                                        )
                                ) {
                                    Box( //active轨道,即滑块滑过的长度。
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = sliderPosition.value)
                                            .height(2.dp)
                                            .background(
                                                shape = RoundedCornerShape(2.dp),
                                                color = MaterialTheme.colorScheme.onPrimary //Color.White
                                            )
                                    )
                                }
                            },
                            thumb = {
                                Icon(
                                    imageVector = Icons.Default.Circle,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .constrainAs(slider) {
                                    top.linkTo(theLrcText.bottom, margin = 30.dp)
                                }
                        )
                        Text(
                            text = tvCurrentProgress?: "00:00",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                            modifier = Modifier
                                .constrainAs(progressText) {
                                    top.linkTo(slider.bottom)
                                    start.linkTo(parent.start)
                                }
                        )
                        Text(
                            text = tvDuration ?: "00:00",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                            modifier = Modifier
                                .constrainAs(durationText) {
                                    top.linkTo(slider.bottom)
                                    end.linkTo(parent.end)
                                }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().weight(0.5f)
                        ) {
                            IconButton(
                                onClick = { onCommand(PlayerCommand.ToggleRepeat) }
                            ) {
                                Icon(
                                    imageVector = when (repeatMode) {
                                        RepeatMode.Order -> Icons.Default.Repeat
                                        RepeatMode.Single -> Icons.Default.RepeatOne
                                        RepeatMode.Random -> Icons.Default.AllInclusive
                                    },
                                    tint = Color.White,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                )
                            }
                            IconButton(
                                onClick = { onCommand(PlayerCommand.Previous) },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    tint = Color.White,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                )
                            }
                            IconButton(
                                onClick = { onCommand(PlayerCommand.PlayPause) },
                                modifier = Modifier
                                    .size(80.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.PauseCircleOutline else Icons.Default.PlayCircleOutline,
                                    tint = Color.White,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                )
                            }
                            IconButton(
                                onClick = { onCommand(PlayerCommand.Next) },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    tint = Color.White,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    showBottomSheet = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    tint = Color.White,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                )
                            }
                        }
                    }

                }
            }
            SongListBottomSheet(
                showBottomSheet = showBottomSheet,
                onBottomSheetDismiss = {
                    showBottomSheet = false
                },
                dataType = dataType,
                repeatMode = repeatMode,
                song = song,
                songList = songList,
                onCommand = onCommand
            )
        }
    }
}