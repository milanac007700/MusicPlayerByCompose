package com.milanac007.demo.musicplayerbycompose.models
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "favorites")
@Parcelize
data class Music(
    @PrimaryKey
    val id: Long,
    val title: String,          //歌名
    val artist: String,          //歌手
    val duration: Long,         //时长，单位ms
    val path: String,           // 歌曲文件路径
    val size: Long = 0,             //歌曲大小，单位byte
    var coverImagePath: String? = null, //歌曲封面图片路径
    var lrcPath: String? = null,        // 歌词文件路径
    var isFavorite: Boolean = false
): Parcelable