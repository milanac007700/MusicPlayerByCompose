package com.milanac007.demo.musicplayerbycompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.milanac007.demo.musicplayerbycompose.ui.components.MusicApp
import com.milanac007.demo.musicplayerbycompose.ui.theme.MusicPlayerByComposeTheme
import com.milanac007.demo.musicplayerbycompose.viewModels.MusicViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = (application as MyApp).repository
        val dataStore = (application as MyApp).dataStore
        val viewModel: MusicViewModel by viewModels {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
                        return MusicViewModel(
                            application = application,
                            musicRepository = repository,
                            dataStore = dataStore
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }

        enableEdgeToEdge() //设置内容布局充满状态栏和导航栏
        setContent {
            MusicPlayerByComposeTheme(
                dynamicColor = false,
            ) {
                MusicApp(viewModel) {
                    viewModel.init()
                }
            }
        }
    }
}


