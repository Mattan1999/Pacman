package com.example.pacman;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;


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

    @Override
    protected void onPause() {
        drawGame.stopGame();
        DrawGame.gameIsPaused = true;
        super.onPause();
        MainActivity.getMediaPlayer().pause();
    }

    @Override
    protected void onResume() {
        DrawGame.gameIsPaused = false;
        drawGame.startGame();
        super.onResume();
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

    public void loadMusic() {
        if (sharedPref.loadMusicState()) {
            MainActivity.getMediaPlayer().start();
        } else if (!sharedPref.loadMusicState()) {
            MainActivity.getMediaPlayer().pause();
        }
    }
}
