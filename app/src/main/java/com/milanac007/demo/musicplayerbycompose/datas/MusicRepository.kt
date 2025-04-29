package com.milanac007.demo.musicplayerbycompose.datas

import com.milanac007.demo.musicplayerbycompose.models.Music

class MusicRepository(private val musicDatabase: MusicDatabase) {
    suspend fun getLocalMusicList() = musicDatabase.getLocalMusicList()

    suspend fun insertFavorite(song: Music) = musicDatabase.insertFavorite(song)

    suspend fun delFavorite(songId: Long) = musicDatabase.delFavorite(songId)
}