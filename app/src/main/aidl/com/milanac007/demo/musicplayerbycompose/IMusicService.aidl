// IMusicService.aidl
package com.milanac007.demo.musicplayerbycompose;
import com.milanac007.demo.musicplayerbycompose.IMusicServiceStateCallback;
import com.milanac007.demo.musicplayerbycompose.models.Music;
interface IMusicService {
    void registerCallback(IMusicServiceStateCallback cb);
    void unregisterCallback(IMusicServiceStateCallback cb);
    void playSong(in Music song, int offset);
    void updateLrc(String lrc); // 更新歌词
    void pause();
    void resume();
    void seekTo(int position);
    void updateRepeatMode(int repeatMode);
    void updateFavorite(boolean isFavorite);
}