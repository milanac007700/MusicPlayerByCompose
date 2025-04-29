package com.milanac007.demo.musicplayerbycompose.dataStores

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

class MusicDataStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        val KEY_DATA_TYPE = stringPreferencesKey("dataType")
        val KEY_REPEAT_MODE = stringPreferencesKey("repeatMode")
        val KEY_CURRENT_SONGID = longPreferencesKey("currentSongId")

        @Volatile private var instance: MusicDataStore? = null

        fun getInstance(dataStore: DataStore<Preferences>) =
            instance ?: synchronized(this) {
                instance ?: MusicDataStore(dataStore).also {
                    instance = it
                }
            }
    }

    suspend fun<T> putValue(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    suspend fun <T, R> getValue(key: Preferences.Key<T>, defValue: R) : R {
        var value: R? = null
        dataStore.edit { preferences ->
            value = preferences[key] as R
        }
        return value ?: defValue
    }

}