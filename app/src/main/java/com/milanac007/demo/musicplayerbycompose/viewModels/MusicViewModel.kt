package com.milanac007.demo.musicplayerbycompose.viewModels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.milanac007.demo.musicplayerbycompose.IMusicService
import com.milanac007.demo.musicplayerbycompose.IMusicServiceStateCallback
import com.milanac007.demo.musicplayerbycompose.dataStores.MusicDataStore
import com.milanac007.demo.musicplayerbycompose.dataStores.MusicDataStore.Companion.KEY_CURRENT_SONGID
import com.milanac007.demo.musicplayerbycompose.dataStores.MusicDataStore.Companion.KEY_DATA_TYPE
import com.milanac007.demo.musicplayerbycompose.dataStores.MusicDataStore.Companion.KEY_REPEAT_MODE
import com.milanac007.demo.musicplayerbycompose.datas.MusicRepository
import com.milanac007.demo.musicplayerbycompose.models.Music
import com.milanac007.demo.musicplayerbycompose.services.MusicService
import com.milanac007.demo.musicplayerbycompose.utils.PausableDelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.util.Locale

class MusicViewModel(
    application: Application,
    val musicRepository: MusicRepository,
    dataStore: DataStore<Preferences>,
    private val savedStateHandle: SavedStateHandle? = null,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MusicUIState())
    val uiState: StateFlow<MusicUIState> = _uiState.asStateFlow()

    private val _songListFlow = MutableStateFlow<List<Music>>(emptyList())
    val songListFlow: StateFlow<List<Music>> = _songListFlow.asStateFlow()

    private val _currentSong = MutableLiveData<Music>(null)
    val currentSong: LiveData<Music> = _currentSong

    val theDataStore: MusicDataStore = MusicDataStore(dataStore)
    private var musicService: IMusicService? = null
    private var isBound = false

    private var lrcJob: Job? = null
    val pausableDelay = PausableDelay()

    val callback = object : IMusicServiceStateCallback.Stub() {
        override fun onPlayFinish() {
            this@MusicViewModel.onPlayFinish()
        }

        override fun onPause() {
            sendCommand(PlayerCommand.PlayPause)
        }

        override fun onResume() {
            sendCommand(PlayerCommand.PlayPause)
        }

        override fun onPlayPrevious() {
            sendCommand(PlayerCommand.Previous)
        }

        override fun onPlayNext() {
            sendCommand(PlayerCommand.Next)
        }

        override fun onSeekTo(position: Int) {
            currentSong.value?.also {
                val progress = position.toFloat().div(it.duration)
                onSliderPositionChanged(progress)
                onStopTrackingTouch()
            }
        }

        override fun onUpdateProgress(currentPosition: Int, progress: Int) {
            val playTime = currentPosition.div(1000)
            val tvCurrentProgress = formatTime(playTime)
            _uiState.value = _uiState.value.copy(
                sliderPosition = progress.toFloat().div(100),
                tvCurrentProgress = tvCurrentProgress,
            )
        }

        override fun onToggleRepeatMode() {
            sendCommand(PlayerCommand.ToggleRepeat)
        }

        override fun onToggleFavorite() {
            sendCommand(PlayerCommand.ToggleFavorite)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            println("@@@ onServiceConnected: $name")
            isBound = true
            musicService = IMusicService.Stub.asInterface(binder).apply {
                this.registerCallback(callback)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            println("@@@ onServiceDisconnected: $name")
            musicService?.apply {
                unregisterCallback(callback)
            }
            musicService = null
            isBound = false
        }
    }

    fun bindService(context: Context) {
        if (isBound) return

        context.bindService(
            Intent(context, MusicService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    override fun onCleared() {
        getApplication<Application>().also {
            unbindService(it)
        }
        super.onCleared()
    }

    fun init() {
        viewModelScope.launch(Dispatchers.IO) {
            val songList = musicRepository.getLocalMusicList()
            val reppeatMode = RepeatMode.valueOf(theDataStore.getValue(KEY_REPEAT_MODE, "Order"))
            val currentSongId:Long = theDataStore.getValue(KEY_CURRENT_SONGID, 0)

            var dataType = theDataStore.getValue(KEY_DATA_TYPE, "local")
            var list: List<Music>
            if (dataType == "favorite") {
                list = songList.filter {
                    it.isFavorite == true
                }
                if (list.isEmpty()) { //favorite列表为空时，dataType取"local"
                    dataType = "local"
                    theDataStore.putValue(KEY_DATA_TYPE, dataType)
                    list = songList
                }
            } else {
                list = songList
            }

            var error: String? = null
            var currentSong: Music? = null
            if (list.isEmpty()) {
//                _playerCommands.emit(PlayerCommand.PlayStateError("播放列表为空"))
                error = "播放列表为空"
            } else {
                currentSong = if (currentSongId.toInt() == 0) list.get(0) else
                    list.filter { it.id == currentSongId }.first()

                _songListFlow.value = songList

                val songLrc = updateCurrentLrc(currentSong.lrcPath)
                _currentSong.postValue(currentSong)
                theDataStore.putValue(KEY_CURRENT_SONGID, currentSong.id)

                _uiState.value = MusicUIState(
                    songLrc = songLrc,
                    repeatMode = reppeatMode,
                    dataType = dataType,
                    tvDuration = formatTime(currentSong.duration.div(1000).toInt()),
                    isPlaying = false,
                    error = error
                )
            }
        }

        // 监听事件
        viewModelScope.launch(Dispatchers.IO) {
            playerCommand.collectLatest {
                when (it) {
                    is PlayerCommand.PlayPause -> togglePlayPause()
                    is PlayerCommand.PlaySpecific -> playSong(it.music)
                    is PlayerCommand.PlayStateError -> showError(it.error)
                    is PlayerCommand.ToggleRepeat -> updateRepeatMode()
                    is PlayerCommand.ToggleFavorite -> updateSongFavoriteState()
                    is PlayerCommand.ToggleDataType -> updateDataType(it.dataType)
                    else -> Unit
                }
            }
        }
    }

    fun togglePlayPause() {
        currentSong.value?.also {
            if (isFirstPlay) { //首次点击时播放当前歌曲，之后只在暂停/播放间切换
                playSong(it)
                return@also
            }

            if (uiState.value.isPlaying == true) {
                updatePlayState(false)
            } else {
                updatePlayState(true)
            }
        }
    }

    private var isFirstPlay: Boolean = true
    fun playSong(song: Music, seekToTime: Int = 0, startKey: Long = 0, offset: Long = 0) {
        if (isFirstPlay) {
            isFirstPlay = false
        }

        // 更新播放进度
        pausableDelay.resume()

        // 更新当前播放song
        viewModelScope.launch {
            song.apply {
                _currentSong.value = this
                val songLrc = updateCurrentLrc(song.lrcPath)
                _uiState.value = _uiState.value.copy(
                    songLrc = songLrc,
                    songLrcText = if (songLrc.isNullOrEmpty()) null else uiState.value.songLrcText,
                    sliderPosition = if (seekToTime > 0) uiState.value.sliderPosition else 0f,
                    tvCurrentProgress = if (seekToTime > 0) uiState.value.tvCurrentProgress else formatTime(0),
                    tvDuration = formatTime(this.duration.div(1000).toInt()),
                    isPlaying = true,
                )

                theDataStore.putValue(KEY_CURRENT_SONGID, song.id)
            }
        }

        //mediaPlay播放
        musicService?.playSong(song, seekToTime)

        //更新歌词
        updateLrcText(startKey = startKey, offset = offset)
    }

    fun showError(error: String) {
        _uiState.value = _uiState.value.copy(
            error = error
        )
    }

    fun updatePlayState(playState: Boolean) {
        _uiState.value= _uiState.value.copy(
            isPlaying = playState
        )

        if (playState) {
            musicService?.resume()
            pausableDelay.resume()
        } else {
            musicService?.pause()
            pausableDelay.pause()
        }

    }
    
    fun updateRepeatMode() {
        viewModelScope.launch {
            var newMode = when (uiState.value.repeatMode) {
                RepeatMode.Order -> RepeatMode.Single
                RepeatMode.Single -> RepeatMode.Random
                else -> RepeatMode.Order
            }

            _uiState.value = _uiState.value.copy(
                repeatMode = newMode
            )
            musicService?.updateRepeatMode(newMode.ordinal) //更新musicService端，进而更新通知栏


            theDataStore.putValue(KEY_REPEAT_MODE, newMode.name)
        }
    }

    fun updateSongFavoriteState() {
        viewModelScope.launch {
            _currentSong.value?.apply {
                val oldValue = this.isFavorite

                // 创建新对象，更新LiveData -> State
                val currentSong = this.copy(
                    isFavorite = !oldValue
                )
                updateCurrentSong(currentSong) //用新对象更新
                musicService?.updateFavorite(currentSong.isFavorite) //更新musicService端，进而更新通知栏

//              更新列表中原来的_currentSong.value项的isFavorite, 用于后续的Next、Previous、playFinish 等操作
                songListFlow.value.first { id == it.id }.isFavorite = !oldValue

                musicRepository.delFavorite(this.id) //删除旧的对象

                if (currentSong.isFavorite) {
                    musicRepository.insertFavorite(this) //添加新对象
                }
            }
        }
    }

    fun updateDataType(newDataType: String) {
        viewModelScope.launch {
            if (newDataType == "favorite" || newDataType == "local") {
                _uiState.value = _uiState.value.copy(
                    dataType = newDataType
                )
            }
            theDataStore.putValue(KEY_DATA_TYPE, newDataType)
        }
    }

    fun updateCurrentSong(music: Music) {
        viewModelScope.launch {
            music.apply {
                _currentSong.value = music
                theDataStore.putValue(KEY_CURRENT_SONGID, music.id)
            }
        }
    }

    fun updateLrcText(startKey: Long = 0, offset: Long = 0) {
        println("@@@ updateLrcText: lrcJob?.cancel()")
        lrcJob?.cancel()
        lrcJob = viewModelScope.launch {
            uiState.value.songLrc?.let { it1 ->
                try {
                    outputLrc(this, pausableDelay, it1, startKey, offset)
                        .collectLatest {
                            println("@@@ lrc: $it")
                            updateLrcText(it)
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("outputLrc, e: $e")
                }
            }
        }
    }

    fun updateLrcText(lrcText: Pair<Long, String>?) {
        lrcText?.also {
            _uiState.value = _uiState.value.copy(
                songLrcText = lrcText
            )
            musicService?.updateLrc(lrcText.second)
        }
    }

    var time:Long = 0
    fun outputLrc(scope: CoroutineScope, pausableDelay: PausableDelay, lrcMap: Map<Long, String>, startKey: Long = 0, offset: Long = 0): Flow<Pair<Long, String>> = flow {
        time = startKey + offset
        lrcMap.keys.filter {
            it > startKey
        }.forEach {
            pausableDelay.delay(scope, it - time)
            time = it
            lrcMap.get(it)?.apply {
                emit(Pair<Long, String>(it, this))
            }
        }
    }
    
    fun updateCurrentLrc(lrcPath: String?): Map<Long, String>? {
        return lrcPath?.let {
            val lrcFile = File(it)
            if (lrcFile.isFile && lrcFile.exists()) {
                val fis = FileInputStream(lrcFile)
                val lrcText = String(fis.readBytes())
                fis.close()
                val lrc = parseLrc(lrcText)
                println("parseLrc: ${lrc}")
                lrc
            } else {
                null
            }
        }
    }

    /*
    歌词解析
    [00:00.00] 作曲 : 周杰伦
    [00:01.00] 作词 : 方文山
    [00:20.00] 稻香
    [00:30.00] 对这个世界如果你有太多的抱怨
     */
    fun parseLrc(lycText: String): Map<Long, String> {
        val pattern = Regex("\\[(\\d+):(\\d+).(\\d+)\\](.*)")
        val lycMap = mutableMapOf<Long, String>()
        lycText.split("\n").forEach { line ->
            pattern.find(line)?.let { 
                val (min, second, millonSec, text) = it.destructured
                val time = min.toLong() * 60 * 1000 + second.toLong() * 1000 + millonSec.toLong()
                lycMap[time] = text
            }
        }
        return lycMap
    }

    fun onPlayFinish() {
        viewModelScope.launch {
            currentSong.value.apply {

                val songList = songListFlow.value.filter {
                    if (uiState.value.dataType == "favorite") {
                        it.isFavorite == true
                    } else {
                        true
                    }
                }

                if (songList.isEmpty()) {
//                    _playerCommands.emit(PlayerCommand.PlayStateError("当前播放列表为空"))
                    _uiState.value = _uiState.value.copy(
                        error = "当前播放列表为空"
                    )
                    return@apply
                }

                val index = songList.indexOf(this)
                val size = songList.size
                var nextIndex = -1
                when (uiState.value.repeatMode) {
                    RepeatMode.Random -> {
                        nextIndex = Math.random().times(size).toInt()
                        if (index == nextIndex) {//排除自己. 这里简单的加1
                            nextIndex = index.inc().mod(size)
                        }
                    }
                    RepeatMode.Single -> {
                        nextIndex = index
                    }
                    else -> {
                        nextIndex = index.inc().mod(size)
                    }
                }
                println("nextIndex: $nextIndex")
                val song = songList[nextIndex]
                val command = PlayerCommand.PlaySpecific(song)
                _playerCommands.emit(command)
            }
        }
    }


    fun sendCommand(command: PlayerCommand) {
        viewModelScope.launch {
            when (command) {
                is PlayerCommand.Next -> {
                    currentSong.value.apply {
                        val songList = songListFlow.value.filter {
                            if (uiState.value.dataType == "favorite") {
                                it.isFavorite == true
                            } else {
                                true
                            }
                        }

                        if (songList.isEmpty()) {
//                            _playerCommands.emit(PlayerCommand.PlayStateError("当前播放列表为空"))
                            _uiState.value = _uiState.value.copy(
                                error = "当前播放列表为空"
                            )
                            return@apply
                        }

                        val index = songList.indexOf(this)
                        val size = songList.size
                        var nextIndex = -1
                        when (uiState.value.repeatMode) {
                            RepeatMode.Random -> {
                                nextIndex = Math.random().times(size).toInt()
                                if (index == nextIndex) {//排除自己
                                    nextIndex = index.inc().mod(size)
                                }
                            }
                            else -> {
                                nextIndex = index.inc().mod(size)
                            }
                        }
                        println("nextIndex: $nextIndex")
                        val song = songList[nextIndex]
                        val command = PlayerCommand.PlaySpecific(song)
                        _playerCommands.emit(command)
                    }
                }
                is PlayerCommand.Previous -> {
                    currentSong.value.apply {
                        val songList = songListFlow.value.filter {
                            if (uiState.value.dataType == "favorite") {
                                it.isFavorite == true
                            } else {
                                true
                            }
                        }

                        if (songList.isEmpty()) {
//                            _playerCommands.emit(PlayerCommand.PlayStateError("当前播放列表为空"))
                            _uiState.value = _uiState.value.copy(
                                error = "当前播放列表为空"
                            )
                            return@apply
                        }

                        val index = songList.indexOf(this)
                        val size = songList.size
                        var previousIndex = -1
                        when (uiState.value.repeatMode) {
                            RepeatMode.Random -> {
                                previousIndex = Math.random().times(size).toInt()
                                if (index == previousIndex) {//排除自己
                                    previousIndex = index.inc().mod(size)
                                }
                            }
                            else -> {
                                val dec = index.dec()
                                previousIndex = if (dec == -1) size - 1 else dec
                            }
                        }
                        println("previousIndex: $previousIndex")
                        val song = songList[previousIndex]
                        val command = PlayerCommand.PlaySpecific(song)
                        _playerCommands.emit(command)
                    }
                }
                else -> {
                    _playerCommands.emit(command)
                }
            }
        }
    }

    var key:Long = 0
    var playTime = 0
    var findKey:Long = 0
    var offset:Long = 0
    var startTime: Long = 0
    fun onSliderPositionChanged(progress: Float) {
        if (currentSong.value != null) {
            val duration = currentSong.value?.duration ?: 0
            playTime = progress.times(duration).toInt()
            val tvCurrentProgress = formatTime(playTime.div(1000))

            key = progress.times(duration).toLong()
            viewModelScope.launch {
                if (uiState.value.songLrc.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        sliderPosition = progress,
                        tvCurrentProgress = tvCurrentProgress,
                    )
                } else {
                    uiState.value.songLrc?.also {
                        // 查找距离播放时长为key的最近出现的时长findKey和对应的歌词
                        it.keys.sorted().apply {
                            var index = lastIndex
                            while (get(index) > key && index > 0) {
                                index--
                            }
                            findKey = get(index)
                            val lrcText = it.get(findKey)
                            offset = playTime - findKey // 在播放时长 = findKey时显示最后出现的歌词，滑动结束时是playTime，需要减去两者的偏移

                            _uiState.value = _uiState.value.copy(
                                sliderPosition = progress,
                                tvCurrentProgress = tvCurrentProgress,
                                songLrcText = Pair<Long, String>(findKey, lrcText!!)
                            )

                            startTime = System.currentTimeMillis()
//                            println("@@@ [${Thread.currentThread().name}]onProgressChanged, key = ${key}, findKey= ${findKey}, offset = ${offset}")
                        }
                    }
                }
            }
        }
    }

    fun onStopTrackingTouch() {
        currentSong.value?.also {
            if (isFirstPlay) { //首次滑动，先播放当前歌曲
                playSong(it, seekToTime = playTime, startKey = findKey, offset = offset)
            } else {
                if (!uiState.value.isPlaying) {
                    updatePlayState(true)
                }

                musicService?.seekTo(playTime)

                val offsetTime = System.currentTimeMillis() - startTime
                println("@@@ onStopTrackingTouch, key = $key, findKey= $findKey, offset = $offset, offsetTime = $offsetTime")
                //offsetTime 比较小，可忽略
                updateLrcText(findKey, offset)
            }

            key = 0
            playTime = 0
            findKey = 0
            offset = 0
            startTime = 0
        }
    }

    fun formatTime(time: Int): String {
        val minute = time.div(60)
        val second = time.mod(60)
        return String.format(Locale.getDefault(), "%02d:%02d", minute, second)
    }
}


// 使用SharedFlow发送控制命令
val _playerCommands = MutableSharedFlow<PlayerCommand>()
val playerCommand = _playerCommands.asSharedFlow()
sealed class PlayerCommand {
    object PlayPause: PlayerCommand()
    object Next: PlayerCommand()
    object Previous: PlayerCommand()
    object ToggleRepeat: PlayerCommand()
    object ToggleFavorite: PlayerCommand()
    data class ToggleDataType(val dataType: String): PlayerCommand()
    data class PlaySpecific(val music: Music): PlayerCommand()
    data class PlayStateError(val error: String): PlayerCommand()
}

enum class RepeatMode {
    Order,
    Single,
    Random,
}

data class MusicUIState(
    val sliderPosition: Float = 0f,
    val tvCurrentProgress: String? = null,
    val tvDuration: String? = null,
    val songLrc: Map<Long, String>? = null,
    val songLrcText: Pair<Long, String>? = null,
    val isPlaying: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.Order,
    val dataType: String = "local",
    val error: String? = null
)
