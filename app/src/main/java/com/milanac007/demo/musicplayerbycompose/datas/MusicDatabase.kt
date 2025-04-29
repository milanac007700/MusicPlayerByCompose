package com.milanac007.demo.musicplayerbycompose.datas

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.milanac007.demo.musicplayerbycompose.models.Music
import java.io.File
import java.io.FileOutputStream

@Database(entities = arrayOf(Music::class), version = 1, exportSchema = false)
abstract class MusicDatabase: RoomDatabase() {
    abstract fun musicDao(): MusicDao

    companion object {
        @Volatile private var instance: MusicDatabase? = null
        private lateinit var context: Context

        fun getInstance(ctx: Context) : MusicDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(ctx).also { instance = it }
            }

        private fun buildDatabase(ctx: Context): MusicDatabase {
            context = ctx
            return Room.databaseBuilder(ctx, MusicDatabase::class.java, "music_db")
//                .fallbackToDestructiveMigration()
                .build()
        }
    }

    suspend fun insertFavorite(song: Music) {
        musicDao().insertFavorite(song)
    }

    suspend fun delFavorite(songId: Long) {
        musicDao().delFavorite(songId)
    }

    suspend fun getLocalMusicList(): List<Music> {
        val contentResolver = context.contentResolver
        val musicList = mutableListOf<Music>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,    //歌曲名称
            MediaStore.Audio.Media.ALBUM,    //专辑名
            MediaStore.Audio.Media.ARTIST,   //歌手名
            MediaStore.Audio.Media.DURATION, //总时长
            MediaStore.Audio.Media.SIZE,     //歌曲文件大小
            MediaStore.Audio.Media.DATA,     //歌曲文件路径
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null,
        )?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                var title = it.getString(1)
                val album = it.getString(2)
                var artist = it.getString(3)
                val duration = it.getLong(4)
                val size = it.getLong(5)
                val path = it.getString(6)
                if (artist.isNullOrEmpty() || artist == "<unknown>") {
                    val array = title.split("-")
                    if (array.size > 1) {
                        title = array[0].trim()
                        artist = array[1].trim()
                    }
                }
                musicList.add(Music(id, title, artist, duration, path,size))
            }
        }

        musicList.forEach {
            fetchCoverImage(it, contentResolver)
        }

        // 查询favorite db并更新缓存的isFavorite字段
        val favoriteIds = musicDao().getAllFavoriteIds()
        musicList.forEach {
            it.isFavorite = favoriteIds.contains(it.id)
        }

        return musicList
    }

    // 从mp3中提取专辑封面照片， 但并非所有 MP3 都有封面。某些音频格式（如 FLAC、WAV）需要其他方式提取封面
    private fun extractAlbumArtFromMp3(music: Music, musicFilePath: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever().apply {
                setDataSource(musicFilePath)
            }
            retriever.embeddedPicture?.let {
                val coverImageFile = File(File(musicFilePath).parentFile, "${music.title}-${music.artist}.jpg")
                val fout = FileOutputStream(coverImageFile)
                fout.write(it)
                music.coverImagePath = coverImageFile.path

                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun fetchCoverImage(music: Music, contentResolver: ContentResolver) {
        // 查询封面图片
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,   //名称
            MediaStore.Images.Media.DATA,           //文件路径
        )

        //模糊查找名字中包含 歌名和歌手的图片
        val selection = "${MediaStore.Images.Media.MIME_TYPE} in ('image/png', 'image/jpeg') and ${MediaStore.Images.Media.DISPLAY_NAME} like ?" +
                "and ${MediaStore.Images.Media.DISPLAY_NAME} like ?"
        val selection2Args = arrayOf(
            "%${music.title}%", //like模糊查找通配符%：匹配任意字符（包括零个字符）
            "%${music.artist}%"
        )
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selection2Args,
            null,
        )?.use {
            if (it.moveToNext()) {
                val _display_name = it.getString(1)
                val coverImagePath = it.getString(2)
                println("cover: _display_name = $_display_name, path = $coverImagePath")
                music.coverImagePath = coverImagePath
            } else { //尝试从mp3文件中获取封面图
                extractAlbumArtFromMp3(music, music.path)
            }

            // 查询歌词, 同目录下查找名字中包含歌曲名和歌手名的后缀为lrc的文件
            val musicFilePath = music.path
            val lrcFiles = File(musicFilePath).parentFile.listFiles()?.filter {
                it.name.contains(music.title) and it.name.contains(music.artist) && it.name.endsWith("lrc")
            }
            if (!lrcFiles.isNullOrEmpty()) {
                music.lrcPath = lrcFiles.first().path
                println("cover music.lrcPath = ${music.lrcPath}")
            }
        }
    }
}