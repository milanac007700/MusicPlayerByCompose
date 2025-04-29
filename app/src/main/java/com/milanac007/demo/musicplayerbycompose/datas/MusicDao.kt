package com.milanac007.demo.musicplayerbycompose.datas

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.milanac007.demo.musicplayerbycompose.models.Music

@Dao
interface MusicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(music: Music)

    @Query("SELECT * FROM favorites WHERE id = :id")
    suspend fun isFavorite(id: Long): Music?

    @Query("SELECT id FROM favorites")
    suspend fun getAllFavoriteIds(): List<Long>

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delFavorite(id: Long)
}