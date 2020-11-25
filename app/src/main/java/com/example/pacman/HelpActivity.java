package com.example.pacman;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class HelpActivity extends AppCompatActivity {

    private SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new SharedPref(this);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_help);
        loadMusic();
    }

    @Override
    protected void onPause(){
        super.onPause();
        overridePendingTransition(0, 0);
    }

    public void loadMusic() {
        if (sharedPref.loadMusicState()) {
            Settings.getMediaPlayer().start();
        } else if (!sharedPref.loadMusicState()) {
            Settings.getMediaPlayer().pause();
        }
    }
}