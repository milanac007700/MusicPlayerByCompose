// IMusicServiceStateCallback.aidl
package com.milanac007.demo.musicplayerbycompose;

// Declare any non-default types here with import statements

interface IMusicServiceStateCallback {
    void onPlayFinish();
    void onPause();
    void onResume();
    void onPlayPrevious();
    void onPlayNext();
    void onSeekTo(int position);
    void onUpdateProgress(int currentPosition, int progress); //已播放时长, 已播放百分比 * 100
    void onToggleRepeatMode();
    void onToggleFavorite();
}