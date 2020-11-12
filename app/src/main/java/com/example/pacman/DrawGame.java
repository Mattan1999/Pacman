package com.example.pacman;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class DrawGame extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private Thread thread;
    private SurfaceHolder holder;
    private boolean canDraw = true;
    private final int TILE_SIZE;
    private int[][] tileMap;
    private int rows, cols;
    private int posX;
    private int posY;
    private int screenWidth;
    private long frameTicker;
    private int totalFrame = 4;
    private int viewDirection = 2;
    private int ghostDirection;
    private Bitmap[] pacManUp, pacManRight, pacManDown, pacManLeft;
    private Bitmap walls, floor;
    private Bitmap ghostBitmap;
    private Paint paint;
    private int currentPacManFrame = 0;
    private int xPosPacman;
    private int yPosPacman;
    private int xPosGhost;
    private int yPosGhost;
    private int xDistance;
    private int yDistance;



    public DrawGame(Context context) {
        super(context);
        holder = getHolder();
        thread = new Thread(this);
        thread.start();
        frameTicker = 1000 / totalFrame;

        createTileMap();


        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((GameActivity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        screenWidth = displayMetrics.widthPixels;
        Log.d("TEST", String.valueOf(screenWidth));

        TILE_SIZE = screenWidth / 17;
        loadBitmapImages();
        Log.d("TEST", String.valueOf(TILE_SIZE));
        ghostDirection = 4;
        yPosGhost = 4 * TILE_SIZE;
        xPosGhost = 8 * TILE_SIZE;
        xPosPacman = 9 * TILE_SIZE;
        yPosPacman = 13 * TILE_SIZE;
    }

    @Override
    public void run() {
        Log.d("TEST", "Inuti RUN!!!");
        while (canDraw) {
            if (!holder.getSurface().isValid()) {
                continue;
            }
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                drawMap(canvas);
                moveGhost(canvas);
                drawPacMan(canvas);
                drawPellets(canvas);
                updateFrame(System.currentTimeMillis());
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    public void moveGhost(Canvas canvas) {
        short ch;

        xDistance = xPosPacman - xPosGhost;
        yDistance = yPosPacman - yPosGhost;

        if ((xPosGhost % TILE_SIZE == 0) && (yPosGhost % TILE_SIZE == 0)) {
            ch = (short) tileMap[yPosGhost / TILE_SIZE][xPosGhost / TILE_SIZE];

            if (xPosGhost >= TILE_SIZE * 17) {
                xPosGhost = 0;
            }
            if (xPosGhost < 0) {
                xPosGhost = TILE_SIZE * 17;
            }


            if (xDistance >= 0 && yDistance >= 0) { // Move right and down
                if ((ch & 4) == 0 && (ch & 8) == 0) {
                    if (Math.abs(xDistance) > Math.abs(yDistance)) {
                        ghostDirection = 1;
                    } else {
                        ghostDirection = 2;
                    }
                }
                else if ((ch & 4) == 0) {
                    ghostDirection = 1;
                }
                else if ((ch & 8) == 0) {
                    ghostDirection = 2;
                }
                else
                    ghostDirection = 3;
            }
            if (xDistance >= 0 && yDistance <= 0) { // Move right and up
                if ((ch & 4) == 0 && (ch & 2) == 0 ) {
                    if (Math.abs(xDistance) > Math.abs(yDistance)) {
                        ghostDirection = 1;
                    } else {
                        ghostDirection = 0;
                    }
                }
                else if ((ch & 4) == 0) {
                    ghostDirection = 1;
                }
                else if ((ch & 2) == 0) {
                    ghostDirection = 0;
                }
                else ghostDirection = 2;
            }
            if (xDistance <= 0 && yDistance >= 0) { // Move left and down
                if ((ch & 1) == 0 && (ch & 8) == 0) {
                    if (Math.abs(xDistance) > Math.abs(yDistance)) {
                        ghostDirection = 3;
                    } else {
                        ghostDirection = 2;
                    }
                }
                else if ((ch & 1) == 0) {
                    ghostDirection = 3;
                }
                else if ((ch & 8) == 0) {
                    ghostDirection = 2;
                }
                else ghostDirection = 1;
            }
            if (xDistance <= 0 && yDistance <= 0) { // Move left and up
                if ((ch & 1) == 0 && (ch & 2) == 0) {
                    if (Math.abs(xDistance) > Math.abs(yDistance)) {
                        ghostDirection = 3;
                    } else {
                        ghostDirection = 0;
                    }
                }
                else if ((ch & 1) == 0) {
                    ghostDirection = 3;
                }
                else if ((ch & 2) == 0) {
                    ghostDirection = 0;
                }
                else ghostDirection = 2;
            }
            // Handles wall collisions
            if ( (ghostDirection == 3 && (ch & 1) != 0) ||
                    (ghostDirection == 1 && (ch & 4) != 0) ||
                    (ghostDirection == 0 && (ch & 2) != 0) ||
                    (ghostDirection == 2 && (ch & 8) != 0) ) {
                ghostDirection = 4;
            }
        }

        if (ghostDirection == 0) {
            yPosGhost += -TILE_SIZE / 20;
        } else if (ghostDirection == 1) {
            xPosGhost += TILE_SIZE / 20;
        } else if (ghostDirection == 2) {
            yPosGhost += TILE_SIZE / 20;
        } else if (ghostDirection == 3) {
            xPosGhost += -TILE_SIZE / 20;
        }

        canvas.drawBitmap(ghostBitmap, xPosGhost, yPosGhost, paint);
    }

    private void updateFrame(long gameTime) {

        if (gameTime > frameTicker + (totalFrame * 30)) {
            frameTicker = gameTime;
            Log.d("TEST", "FPS: " + frameTicker);

            currentPacManFrame++;
            if (currentPacManFrame >= totalFrame) {
                currentPacManFrame = 0;
            }

        }
    }

    public void drawMap(Canvas canvas) {
        super.draw(canvas);

        paint = new Paint();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {

                posY = TILE_SIZE * i;
                posX = TILE_SIZE * j;


                switch (tileMap[i][j]) {
                    case 0:
                        paint.setColor(Color.BLACK);
                        canvas.drawRect(posX, posY, posX + TILE_SIZE, posY + TILE_SIZE, paint);
                        break;
                    case 1:
                        Rect rect = new Rect(posX, posY, posX + TILE_SIZE, posY + TILE_SIZE);
                        canvas.drawBitmap(walls, null, rect,null);
                        break;
                    case 2:
                        Rect rect2 = new Rect(posX, posY, posX + TILE_SIZE, posY + TILE_SIZE);
                        canvas.drawBitmap(floor, null, rect2,null);
                        paint.setColor(Color.parseColor("#A3A3A3"));
                        paint.setStrokeWidth(8);
                        canvas.drawCircle(posX + TILE_SIZE / 2, posY + TILE_SIZE / 2, TILE_SIZE / 10, paint);
                        break;
                    case 3:
                        Rect rect3 = new Rect(posX, posY, posX + TILE_SIZE, posY + TILE_SIZE);
                        canvas.drawBitmap(floor, null, rect3,null);
                        paint.setColor(Color.GRAY);
                        paint.setStrokeWidth(8);
                        canvas.drawLine(posX, posY + 5, posX + TILE_SIZE, posY + 5, paint);
                }
            }
        }
    }

    public void drawPacMan(Canvas canvas){
        switch (viewDirection){
            case 0:
                canvas.drawBitmap(pacManUp[currentPacManFrame],xPosPacman - TILE_SIZE, yPosPacman - TILE_SIZE, paint);
                break;
            case 1:
                canvas.drawBitmap(pacManLeft[currentPacManFrame],xPosPacman - TILE_SIZE, yPosPacman - TILE_SIZE, paint);
                break;
            case 2:
                canvas.drawBitmap(pacManRight[currentPacManFrame],xPosPacman - TILE_SIZE, yPosPacman - TILE_SIZE, paint);
                break;
            default:
                canvas.drawBitmap(pacManDown[currentPacManFrame],xPosPacman - TILE_SIZE, yPosPacman - TILE_SIZE, paint);
        }
    }

    public void drawPellets(Canvas canvas) {

        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 17; j++) {
                posX = j * TILE_SIZE;
                posY= i * TILE_SIZE;


            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("TEST", "Surface Created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("TEST", "Surface Changed");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("TEST", "Surface Destroyed");
    }

    private void loadBitmapImages(){
        int spriteSize = TILE_SIZE;

        pacManRight = new Bitmap[totalFrame];
        pacManRight[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(),R.drawable.pacman_right1), spriteSize, spriteSize, false);
        pacManRight[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_right2), spriteSize, spriteSize, false);
        pacManRight[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_right3), spriteSize, spriteSize, false);
        pacManRight[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_right), spriteSize, spriteSize, false);
        // Add bitmap images of pacman facing down
        pacManDown = new Bitmap[totalFrame];
        pacManDown[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down1), spriteSize, spriteSize, false);
        pacManDown[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down2), spriteSize, spriteSize, false);
        pacManDown[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down3), spriteSize, spriteSize, false);
        pacManDown[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_down), spriteSize, spriteSize, false);
        // Add bitmap images of pacman facing left
        pacManLeft = new Bitmap[totalFrame];
        pacManLeft[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left1), spriteSize, spriteSize, false);
        pacManLeft[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left2), spriteSize, spriteSize, false);
        pacManLeft[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left3), spriteSize, spriteSize, false);
        pacManLeft[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_left), spriteSize, spriteSize, false);
        // Add bitmap images of pacman facing up
        pacManUp = new Bitmap[totalFrame];
        pacManUp[0] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up1), spriteSize, spriteSize, false);
        pacManUp[1] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up2), spriteSize, spriteSize, false);
        pacManUp[2] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up3), spriteSize, spriteSize, false);
        pacManUp[3] = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.pacman_up), spriteSize, spriteSize, false);

        walls = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.wall), spriteSize, spriteSize, false);
        floor = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.floor), spriteSize, spriteSize, false);

        ghostBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.ghost), spriteSize, spriteSize, false);
    }


    public void createTileMap() {
        tileMap = new int[][] {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1},
                {1, 2, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 2, 1},
                {1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1},
                {1, 2, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1, 1, 2, 1, 2, 1},
                {1, 2, 2, 2, 2, 2, 2, 1, 2, 1, 2, 2, 2, 2, 2, 2, 1},
                {1, 1, 1, 1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 1, 1, 1},
                {1, 1, 1, 1, 2, 2, 2, 1, 3, 1, 2, 2, 2, 1, 1, 1, 1},
                {2, 2, 2, 2, 2, 1, 1, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2},
                {1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1},
                {1, 1, 1, 1, 2, 1, 2, 2, 2, 2, 2, 1, 2, 1, 1, 1, 1},
                {1, 2, 2, 2, 2, 2, 2, 1, 1, 1, 2, 2, 2, 2, 2, 2, 1},
                {1, 2, 1, 2, 1, 1, 2, 1, 1, 1, 2, 1, 1, 2, 1, 2, 1},
                {1, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1},
                {1, 2, 2, 1, 1, 2, 1, 1, 2, 1, 1, 2, 1, 1, 2, 2, 1},
                {1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
        rows = tileMap.length;
        cols = tileMap[1].length;
    }


}
