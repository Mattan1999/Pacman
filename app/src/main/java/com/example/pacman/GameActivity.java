package com.example.pacman;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class GameActivity extends Activity {
    private DrawGame drawGame;
    private SharedPref sharedPref;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        sharedPref = new SharedPref(this);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.game_view);
        drawGame = (DrawGame) findViewById(R.id.drawGame);
        loadMusic();
    }

    public void dPadLeft(View view){
        drawGame.dPadLeft();
    }

    public void dPadUp(View view){
        drawGame.dPadUp();
    }

    public void dPadRight(View view){
        drawGame.dPadRight();
    }

    public void dPadDown(View view){
        drawGame.dPadDown();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void loadMusic() {
        if (sharedPref.loadMusicState()) {
            MainActivity.getMediaPlayer().start();
        } else if (!sharedPref.loadMusicState()) {
            MainActivity.getMediaPlayer().pause();
        }
    }
}
