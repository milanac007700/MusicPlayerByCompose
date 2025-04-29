package com.milanac007.demo.musicplayerbycompose.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.milanac007.demo.musicplayerbycompose.IMusicService
import com.milanac007.demo.musicplayerbycompose.IMusicServiceStateCallback
import com.milanac007.demo.musicplayerbycompose.MainActivity
import com.milanac007.demo.musicplayerbycompose.R
import com.milanac007.demo.musicplayerbycompose.models.Music
import java.util.Collections
import java.util.concurrent.CopyOnWriteArraySet

/*
在 Android 中，通知本身是通过 NotificationManager 管理的。当你需要更新通知时，实际上并不需要重新构建整个通知，而是可以通过 NotificationManager.notify() 来更新已有的通知。这种方法比重新创建通知并调用 startForeground() 更高效。
每次你调用 NotificationCompat.Builder 并 build() 后，确实会创建一个新的通知对象。然后，你调用 startForeground() 将新的通知发送到通知栏。如果只是更新了通知的某个字段，这种方式是没有必要的，因为你并没有改变通知的其他部分。
使用 NotificationManager.notify(NOTIFICATION_ID, notification) 方法，可以在不改变通知 ID 的情况下，仅更新通知中的某些字段。系统会根据通知 ID 查找现有的通知，并更新它。

CopyOnWriteArraySet
是一个线程安全的集合类，它的内部实现是基于 CopyOnWriteArrayList。它适用于那些读多写少的场景，因为它通过每次修改时都创建集合的一个副本来保证线程安全。
主要特点：
线程安全：通过复制整个底层数组来确保线程安全，每次修改时（如添加或删除元素）都会复制整个集合，因此不会发生并发修改异常。
适用于读多写少的场景：由于每次写操作都会复制集合的一个副本，因此在写操作频繁的场景下会有性能开销。在读多写少的情况下，CopyOnWriteArraySet 的性能表现较好。
无锁操作：在读取操作时不需要加锁，因此读操作非常高效。
 */
class MusicService : Service() {
    val callbacks = CopyOnWriteArraySet<IMusicServiceStateCallback>()
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var handler: Handler
    private var currentSong: Music? = null

    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper())
        createNotificationChannel() // Android 8.0+ 要求必须创建通知渠道
        initMediaSession()
        mediaPlayer = MediaPlayer().apply {
            setOnErrorListener(object : MediaPlayer.OnErrorListener {
                override fun onError(
                    mp: MediaPlayer?,
                    what: Int,
                    extra: Int
                ): Boolean {
                    println("@@@ mediaPlayer onError, what=$what, extra=$extra")
                    return true
                }
            })

            setOnCompletionListener {
                println("@@@ mediaPlayer playFinish, currentSong: $currentSong")
                callbacks.forEach {
                    it.onPlayFinish()
                }
            }
        }
    }


    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaSession.release()
        mediaPlayer.apply {
            stop()
            release()
        }
        super.onDestroy()
    }

    fun playSong(song: Music, offset: Int = 0) {
        println("@@@ exec play, music: $song")
        currentSong = song
        mediaPlayer.apply {
            reset()
            setDataSource(currentSong?.path)
            prepareAsync()
            setOnPreparedListener {
                if (offset > 0) {
                    seekTo(offset)
                }
                start()
                lrcMetadata = "${currentSong?.title}-${currentSong?.artist}"
                startNotification()
                startProgressUpdates()
            }
        }
    }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            currentSong?.also {
                callbacks.forEach { //通知App更新播放进度
                    it.onUpdateProgress(getCurrentPosition(), getProgress())
                }
                updateMediaMetadata(it) // 更新元数据
            }
            if (isPlaying() && getProgress() < 100) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun startProgressUpdates() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(updateProgressRunnable, 1000)
    }

    fun pause() {
        mediaPlayer.pause()
        handler.removeCallbacksAndMessages(null)

        // 更新mediaSession元数据
        currentSong?.also {
            callbacks.forEach { // 通知App更新播放进度
                it.onUpdateProgress(getCurrentPosition(), getProgress())
            }
            updateMediaMetadata(it)
        }
    }

    fun resume() {
        mediaPlayer.start()
        startProgressUpdates()
    }

    fun onResume() {
        callbacks.forEach {
            it.onResume()
        }
    }

    fun onPause() {
        callbacks.forEach {
            it.onPause()
        }
    }

    fun onPlayPrevious() {
        callbacks.forEach {
            it.onPlayPrevious()
        }
    }
    fun onPlayNext() {
        callbacks.forEach {
            it.onPlayNext()
        }
    }

    fun onSeekTo(position: Int) {
        callbacks.forEach {
            it.onSeekTo(position)
        }
    }

    fun onToggleRepeatMode() {
        callbacks.forEach {
            it.onToggleRepeatMode()
        }
    }

    fun onToggleFavorite() {
        callbacks.forEach {
            it.onToggleFavorite()
        }
    }

    fun isPlaying() = mediaPlayer.isPlaying

    // currentPosition/duration 不应该>1, 实际中duration可能为325876, currentPosition最后两次为 325400, 326400
    fun getCurrentPosition(): Int = minOf(mediaPlayer.currentPosition, mediaPlayer.duration)

    fun getDuration() = mediaPlayer.duration

    fun seekTo(position: Int) = mediaPlayer.seekTo(position)

    fun getProgress(): Int {
        return getCurrentPosition().div(getDuration().toDouble()).times(100).toInt()
    }

    private var repeatMode: Int = 0
    fun updateRepeatMode(mode: Int) {
        repeatMode = mode
    }

    fun updateFavorite(isFavorite: Boolean) {
        currentSong?.isFavorite = isFavorite
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CHANNEL_ID
            val channel = NotificationChannel(
                channelId, // 渠道ID
                "Music Channel",
                NotificationManager.IMPORTANCE_HIGH // 必须为 HIGH 或 DEFAULT 才能显示按钮
            ).apply {
                description = "Music Player Channel"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 锁屏可见
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startNotification() {
        startForeground(NOTIFICATION_ID, createNotification())
    }
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
//            println("@@@ createNotification channel?.importance: ${channel?.importance}") //低于3，通知不显示Action按钮图标
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), //"com.milanac007.demo.musicplayerbycompose.main"
            PendingIntent.FLAG_IMMUTABLE)

//        val remoteViews = RemoteViews(packageName, R.layout.custom_music_notification)
//        remoteViews.setTextViewText(R.id.tv_lrc, lrcMetadata)
//        remoteViews.setTextViewText(R.id.title, "${currentSong?.title}-${currentSong?.artist}")
//        remoteViews.setImageViewBitmap(R.id.image, BitmapFactory.decodeFile(currentSong?.coverImagePath))

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true) //让通知不能被用户划掉
            .setContentTitle(lrcMetadata)
            .setContentText("${currentSong?.title}-${currentSong?.artist}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeFile(currentSong?.coverImagePath))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)       // 兼容 Android 7.1 及以下
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            .addAction(
                when (repeatMode) {
                    1 -> R.drawable.repeat_one_24px
                    2 -> R.drawable.all_inclusive_24px
                    else -> R.drawable.repeat_24px
                },
                "循环模式",
                getActionPendingIntent(ACTION_TOGGLE_REPEAT_MODE)
            )
            .addAction(
                R.drawable.skip_previous_24px,
                "上一首",
                getActionPendingIntent(ACTION_PREVIOUS)
            )
            .addAction(
                if (isPlaying()) R.drawable.pause_circle_24px else R.drawable.play_circle_24px,
                if (isPlaying()) "暂停" else "播放",
                getActionPendingIntent(if (isPlaying()) ACTION_PAUSE else ACTION_PLAY)
            )
            .addAction(
                R.drawable.skip_next_24px,
                "下一首",
                getActionPendingIntent(ACTION_NEXT)
            )
            .addAction(
                if (currentSong?.isFavorite == true) R.drawable.favorite_fill_24px else R.drawable.favorite_24px,
                "收藏",
                getActionPendingIntent(ACTION_TOGGLE_FAVORITE)
            )
            //MediaStyle 可以让通知变成一个多媒体播放风格通知（带播放控制按钮、锁屏显示专辑图等），是播放类 App（如音乐播放器、播客、视频）推荐使用的通知样式。
            //MIUI 有时会限制非 MediaStyle 的通知按钮在锁屏或后台显示。MIUI 可能对某些通知样式做了折叠处理，尤其是非媒体类。
            // TODO MIUI 有bug：默认即展开状态，不能折叠，点击折叠按钮通知就消失
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(1, 2, 3)  // 通知栏折叠时显示3个按钮
            )
            .build()
    }

    private fun getActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { handleAction(it) }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleAction(action: String) {
        when (action) {
            ACTION_PLAY -> onResume()
            ACTION_PAUSE -> onPause()
            ACTION_PREVIOUS -> onPlayPrevious()
            ACTION_NEXT -> onPlayNext()
            ACTION_TOGGLE_REPEAT_MODE -> onToggleRepeatMode()
            ACTION_TOGGLE_FAVORITE -> onToggleFavorite()
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = onResume()
                override fun onPause() = this@MusicService.onPause()
                override fun onSkipToNext() = onPlayNext()
                override fun onSkipToPrevious() = onPlayPrevious()
                override fun onSeekTo(pos: Long) {
                    println("@@@ mediaSession onSeekTo: $pos")
                    onSeekTo(pos.toInt()) //当用户拖动进度条结束时，系统会发送一个 ACTION_SEEK_TO 的 Intent
                }
                override fun onSetRepeatMode(repeatMode: Int) = onToggleRepeatMode()

//                PlaybackStateCompat.CustomAction
                override fun onCustomAction(action: String?, extras: Bundle?) {
                    super.onCustomAction(action, extras)
                }
            })
        }
    }

    // 同步在App内修改的播放状态
    private fun updatePlaybackState() {
        val state = if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else
            PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, getCurrentPosition().toLong(), 1.0f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE)
            .build()
        mediaSession.setPlaybackState(playbackState)

    }

    // 通过 setMetadata() 来更新歌曲的元数据，如歌曲的标题、艺术家、专辑封面、播放进度等。
    private var lrcMetadata: String = ""
    private fun updateMediaMetadata(song: Music, lrcText: String? = null) {
        updatePlaybackState()

        lrcText?.also {
            if (it.isNotEmpty()) {
                lrcMetadata = it
            }
        }
        val albumArtBitmap = BitmapFactory.decodeFile(song.coverImagePath)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, lrcMetadata)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "${song.title}-${song.artist}")
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getCurrentPosition().toLong()) //当前播放位置
            .build()

        mediaSession.setMetadata(metadata)

        updateNotification() //同步更新通知栏
    }


    override fun onBind(intent: Intent): IBinder = myBinder
    val myBinder = object : IMusicService.Stub() {
        override fun registerCallback(cb: IMusicServiceStateCallback?) {
            callbacks.add(cb)
        }

        override fun unregisterCallback(cb: IMusicServiceStateCallback?) {
            callbacks.remove(cb)
        }

        override fun playSong(song: Music?, offset: Int) {
            song?.apply {
                this@MusicService.playSong(song, offset)
            }
        }

        override fun updateLrc(lrc: String?) {
            currentSong?.apply {
                this@MusicService.updateMediaMetadata(this, lrc)
            }
        }

        override fun pause() {
            this@MusicService.pause()
        }

        override fun resume() {
            this@MusicService.resume()
        }

        override fun seekTo(position: Int) = this@MusicService.seekTo(position)

        override fun updateRepeatMode(repeatMode: Int) = this@MusicService.updateRepeatMode(repeatMode)

        override fun updateFavorite(isFavorite: Boolean) {
            this@MusicService.updateFavorite(isFavorite)
        }
    }

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_NEXT = "action_next"
        const val ACTION_SEEKTO = "action_seekTo"
        const val ACTION_TOGGLE_REPEAT_MODE = "action_toggleRepeatMode"
        const val ACTION_TOGGLE_FAVORITE = "action_toggleFavorite"
    }
}