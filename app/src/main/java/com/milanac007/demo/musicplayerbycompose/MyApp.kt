package com.milanac007.demo.musicplayerbycompose

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.milanac007.demo.musicplayerbycompose.datas.MusicDatabase
import com.milanac007.demo.musicplayerbycompose.datas.MusicRepository

class MyApp : Application() {
    //通过懒加载初始化数据库和Repository
    val dataStore: DataStore<Preferences> by preferencesDataStore(name = "theDataStore") //保证单例
    val database by lazy { MusicDatabase.getInstance(this) }
    val repository by lazy { MusicRepository(database) }
}