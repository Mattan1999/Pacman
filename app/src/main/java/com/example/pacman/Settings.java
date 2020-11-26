package com.example.pacman;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class Settings extends AppCompatActivity {

    private RadioGroup radioGroup;
    private RadioButton songOne, songTwo;
    private SwitchCompat switchCompat;
    private SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new SharedPref(this);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_settings);
        switchCompat = findViewById(R.id.musicSwitch);
        radioGroup = findViewById(R.id.radioGroup);
        songOne = findViewById(R.id.song1);
        songTwo = findViewById(R.id.song2);

        loadMusicSettings();

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    songOne.setEnabled(true);
                    songTwo.setEnabled(true);
                    sharedPref.setMusicState(true);
                    MainActivity.getMediaPlayer().start();
                } else {
                    songOne.setEnabled(false);
                    songTwo.setEnabled(false);
                    sharedPref.setMusicState(false);
                    MainActivity.getMediaPlayer().pause();
                }
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.song1:
                        sharedPref.songToPlay(1);
                        MainActivity.getMediaPlayer().pause();
                        MainActivity.setMedia(MediaPlayer.create(getApplicationContext(), R.raw.pacman_song));
                        MainActivity.getMediaPlayer().start();
                        break;
                    case R.id.song2:
                        sharedPref.songToPlay(2);
                        MainActivity.getMediaPlayer().pause();
                        MainActivity.setMedia(MediaPlayer.create(getApplicationContext(), R.raw.tetris_song));
                        MainActivity.getMediaPlayer().start();
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMusicSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.getMediaPlayer().pause();
        overridePendingTransition(0, 0);
    }

    public void loadMusicSettings() {
        if (sharedPref.loadMusicState()) {
            switchCompat.setChecked(true);
            songOne.setEnabled(true);
            songTwo.setEnabled(true);
            if (sharedPref.loadSong() == 1) songOne.setChecked(true);
            else if (sharedPref.loadSong() == 2) songTwo.setChecked(true);
            MainActivity.getMediaPlayer().start();
        } else if (!sharedPref.loadMusicState()) {
            switchCompat.setChecked(false);
            songOne.setEnabled(false);
            songTwo.setEnabled(false);
            if (sharedPref.loadSong() == 1) songOne.setChecked(true);
            else if (sharedPref.loadSong() == 2) songTwo.setChecked(true);
            MainActivity.getMediaPlayer().pause();
        }
    }

}