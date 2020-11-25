package com.example.pacman;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.util.ArrayList;


public class DrawGame extends SurfaceView implements SurfaceHolder.Callback {
    private SharedPref sharedPref;
    private Context context;
    private GameThread thread = null;
    private int screenWidth;
    private final int TILE_SIZE;
    private int[][] tileMap;
    private int rows, cols;
    private int totalFrame = 4;
    private Bitmap[] pacManUp, pacManRight, pacManDown, pacManLeft;
    private Bitmap wallBitmap, ghostBitmap;
    private Paint paint;
    private int currentPacManFrame = 0;
    private int currentScore = 0;
    private Points points;
    private Tile blank, floor, door, ghostFloor, pellet;
    private Wall wall;
    private Ghost ghost;
    private Pacman pacman;
    private boolean moveUp = false, moveLeft = false, moveRight = true, moveDown = false, isColliding = false;
    private int viewDirection = 2;
    private ArrayList<Tile> walls, pellets, ghostDoor, ghostFloors;
    public static int LONG_PRESS_TIME=750;
    private final Handler handler = new Handler();
    private boolean gameJustStarted = false;
    public static boolean gameIsPaused = false;
    private boolean eat = false;
    private String won, lost;
    private boolean wonGame;
    private boolean insideCage = true;
    private int ghostPreviousDirection = 0;


    public DrawGame(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.sharedPref = new SharedPref(context);
        getHolder().addCallback(this);

        createTileMap();

        WindowManager wm = (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);

        screenWidth = screenSize.x;

        TILE_SIZE = screenWidth / cols;

        initializeGame();
    }

    public void initializeGame() {
        loadBitmapImages();

        won = getResources().getString(R.string.tv_won);
        lost = getResources().getString(R.string.tv_lost);
        wonGame = false;

        paint = new Paint();
        points = new Points();
        blank = new Tile(TILE_SIZE);
        floor = new Tile(TILE_SIZE);
        ghost = new Ghost(TILE_SIZE);
        pacman = new Pacman(TILE_SIZE);
        walls = new ArrayList<>();
        pellets = new ArrayList<>();
        ghostDoor = new ArrayList<>();
        ghostFloors = new ArrayList<>();

        walls.clear();
        pellets.clear();
        ghostDoor.clear();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++){
                if (tileMap[i][j] == 4) {
                    ghost.setTilePosition(TILE_SIZE * j, TILE_SIZE * i);
                }
            }
        }

        pacman.setTilePosition(8 * TILE_SIZE, 12 * TILE_SIZE);

        points.setHighScore(sharedPref.loadHighScore());
        points.setScore(0);

        gameJustStarted = true;
    }

    public void startGame() {
        if (thread == null) {
            thread = new GameThread(this);
            thread.startThread();
        }
    }

    public void stopGame() {
        if (thread != null) {
            thread.stopThread();
            boolean retry = true;
            while (retry) {
                try {
                    thread.join();
                    retry = false;
                } catch (InterruptedException e) {e.printStackTrace();}
                thread = null;
            }
        }
    }

    public void dPadLeft(){
        if (viewDirection == 1) return;
        moveLeft = true;
        moveDown = false;
        moveRight = false;
        moveUp = false;
        isColliding = false;
    }

    public void dPadUp(){
        if (viewDirection == 0) return;
        moveLeft = false;
        moveDown = false;
        moveRight = false;
        moveUp = true;
        isColliding = false;
    }

    public void dPadRight(){
        if (viewDirection == 2) return;
        moveLeft = false;
        moveDown = false;
        moveRight = true;
        moveUp = false;
        isColliding = false;
    }

    public void dPadDown(){
        if (viewDirection == 3) return;
        moveLeft = false;
        moveDown = true;
        moveRight = false;
        moveUp = false;
        isColliding = false;
    }

    public void update() {
        currentPacManFrame++;
        if (currentPacManFrame >= 4) {
            currentPacManFrame = 0;
        }
        checkPelletCollision();
        checkWallCollision(pacman);
        checkWallCollision(ghost);
        if (insideCage) {
            getGhostOut();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawMap(canvas);
        drawPacMan(canvas);
        drawGhost(canvas);
        updateScores(canvas);
    }

    public void updateScores(Canvas canvas){
        paint.setTextSize((float) (TILE_SIZE / 1.1));

        if(points.getScore() > points.getHighScore()) {
            points.setHighScore(points.getScore());
            sharedPref.setHighscore(points.getHighScore());
        }

        String formattedHighScore = String.format("%05d", points.getHighScore());
        String hScore = getResources().getString(R.string.high_score) + " " + formattedHighScore;
        canvas.drawText(hScore, 0, (float) (TILE_SIZE * 1.8), paint);

        String formattedScore = String.format("%05d", points.getScore());
        String score = getResources().getString(R.string.score) + " " + formattedScore;
        canvas.drawText(score, (float) (TILE_SIZE * 11.6), (float) (TILE_SIZE * 1.8), paint);
    }

    public void drawMap(Canvas canvas) {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                switch (tileMap[y][x]) {
                    case 0:
                        blank.setTilePosition(TILE_SIZE * x, TILE_SIZE * y);
                        paint.setColor(Color.BLACK);
                        canvas.drawRect(blank.getX(), blank.getY(), blank.getX() + blank.getTILE_SIZE(), blank.getY() + blank.getTILE_SIZE(), paint);
                        break;
                    case 1:
                        placeWalls(x, y, canvas);
                        break;
                    case 2:
                        placePellets(x, y, canvas);
                        break;
                    case 3:
                        createGhostDoor(x, y, canvas);
                        break;
                    case 4:
                        if (gameJustStarted) {
                            ghostFloor = new Tile(TILE_SIZE);
                            ghostFloor.setTilePosition(x * TILE_SIZE, y * TILE_SIZE);
                            ghostFloors.add(ghostFloor);
                        } else {
                            for (Tile tile: ghostFloors) {
                                tile.setTilePosition(x * tile.getTILE_SIZE(), y * tile.getTILE_SIZE());
                            }
                        }
                }
            }
        }
        gameJustStarted = false;
    }

    public void placeWalls(int x, int y, Canvas canvas) {
        if (gameJustStarted) {
            wall = new Wall(TILE_SIZE);
            wall.setTilePosition(TILE_SIZE * x, TILE_SIZE * y);
            walls.add(wall);
        } else {
            for (Tile tile: walls) {
                wall.setTilePosition(x * tile.getTILE_SIZE(), y * tile.getTILE_SIZE());
            }
        }
        canvas.drawBitmap(wallBitmap, null, wall.getBounds(),null);
    }

    public void placePellets(int x, int y, Canvas canvas) {
        if (gameJustStarted) {
            pellet = new Tile(TILE_SIZE);
            pellet.setTilePosition(x * TILE_SIZE, y * TILE_SIZE);
            pellets.add(pellet);
        } else {
            for (Tile tile : pellets) {
                pellet.setTilePosition(x * tile.getTILE_SIZE(), y * tile.getTILE_SIZE());
            }
        }

        paint.setColor(Color.parseColor("#A3A3A3"));
        paint.setStrokeWidth(8);
        canvas.drawCircle(pellet.getX() + pellet.getTILE_SIZE() / 2, pellet.getY() + pellet.getTILE_SIZE() / 2, pellet.getTILE_SIZE() / 10, paint);

    }

    private void createGhostDoor(int x, int y, Canvas canvas) {
        if (gameJustStarted) {
            door = new Tile(TILE_SIZE);
            door.setTilePosition(TILE_SIZE * x, TILE_SIZE * y);
            ghostDoor.add(door);
        } else {
            for (Tile tile: ghostDoor) {
                door.setTilePosition(x * tile.getTILE_SIZE(), y * tile.getTILE_SIZE());
            }
        }
        paint.setColor(Color.BLACK);
        canvas.drawRect(door.getX(), door.getY(), door.getX() + door.getTILE_SIZE(), door.getY() + door.getTILE_SIZE(), paint);
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(8);
        canvas.drawLine(door.getX(), door.getY() + 5, door.getX() + door.getTILE_SIZE(), door.getY() + 5, paint);
    }

    public boolean pathUp(Tile t) {
        for (Tile tile : walls) {
            if (t.getY() - t.getTILE_SIZE() == tile.getY()) {
                if (t.getX() < tile.getX() + tile.getTILE_SIZE() && t.getX() + t.getTILE_SIZE() > tile.getX()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean pathDown(Tile t) {
        for (Tile tile : walls) {
            if (t.getY() + t.getTILE_SIZE() == tile.getY()) {
                if (t.getX() < tile.getX() + tile.getTILE_SIZE() && t.getX() + t.getTILE_SIZE() > tile.getX()) {
                    return false;
                }
            }
        }
        for (Tile tile : ghostDoor) {
            if (pacman.getY() + pacman.getTILE_SIZE() == tile.getY()) {
                if (pacman.getX() < tile.getX() + tile.getTILE_SIZE() && pacman.getX() + pacman.getTILE_SIZE() > tile.getX()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean pathLeft(Tile t) {
        for (Tile tile : walls) {
            if (t.getX() - t.getTILE_SIZE() == tile.getX()) {
                if (t.getY() < tile.getY() + tile.getTILE_SIZE() && t.getY() + t.getTILE_SIZE() > tile.getY()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean pathRight(Tile t) {
        for (Tile tile : walls) {
            if (t.getX() + t.getTILE_SIZE() == tile.getX()) {
                if (t.getY() < tile.getY() + tile.getTILE_SIZE() && t.getY() + t.getTILE_SIZE() > tile.getY()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void checkWallCollision(Tile t) {
        //Wall collision
        for (Tile tile : walls) {
            Rect rect = t.getBounds();
            Rect wall = tile.getBounds();
            if (Rect.intersects(wall, rect)) {
                isColliding = true;
            }
            if (isColliding) {
                switch (t.equals(pacman)?viewDirection: ghostPreviousDirection) {
                    case 0:
                        t.setTilePosition(t.getX(), wall.bottom);
                        break;
                    case 1:
                        t.setTilePosition(wall.left + t.getTILE_SIZE(), t.getY());
                        break;
                    case 2:
                        t.setTilePosition(wall.left - t.getTILE_SIZE(), t.getY());
                        break;
                    case 3:
                        t.setTilePosition(t.getX(), wall.top - t.getTILE_SIZE());
                        break;
                }
                isColliding = false;
            }
        }

        for (Tile tile: ghostDoor) {
            Rect player = pacman.getBounds();
            Rect wall = tile.getBounds();
            if (Rect.intersects(wall, player)) {
                isColliding = true;
            }
            if (isColliding && viewDirection == 3) {
                pacman.setTilePosition(pacman.getX(), wall.top - pacman.getTILE_SIZE());
            }
            isColliding = false;
        }
    }

    public void checkPelletCollision() {
        float x = pacman.getX();
        float y = pacman.getY();
        int indexX = (int) x / pacman.getTILE_SIZE();
        int indexY = (int) y / pacman.getTILE_SIZE();
        int levelPos = tileMap[indexY][indexX];

        eat = false;
        Tile pelletTile = null;

        for (Tile tile : pellets) {
            Rect player = pacman.getBounds();
            Rect pellet = tile.getBounds();
            if (player.contains(pellet) && levelPos == 2) {
                eat = true;
                pelletTile = tile;
                tileMap[indexY][indexX] = 5;
            }
        }
        if (eat) {
            points.isEaten();
            pellets.remove(pelletTile);
        }
        if (pellets.size() <= 0 && !gameJustStarted) {
            wonGame();
        }
    }

    public void getGhostOut() {
        float x = ghost.getX();
        float y = ghost.getY();
        int indexX = (int) x / ghost.getTILE_SIZE();
        int indexY = (int) y / ghost.getTILE_SIZE();
        int levelPos = tileMap[indexY][indexX];

        if (levelPos == 4 || levelPos == 3 || ghost.getBounds().bottom > door.getBounds().top) {
            insideCage = true;
            ghostPreviousDirection = 0;
            //close the gate
            Log.d("TEST", "Ghost hasn't left spawn!");
        } else {
            insideCage = false;
            Log.d("TEST", "Ghost left spawn!");
        }
        if (pathUp(ghost)) {
            ghost.moveUp(4);
        } else {
            ghost.moveLeft(4);
        }
    }

    public void drawPacMan(Canvas canvas) {
        Rect player = pacman.getBounds();
        Rect enemy = ghost.getBounds();
        if (Rect.intersects(player, enemy)) {
            lostGame();
        }

        if (!isColliding) {
            if (moveUp && pathUp(pacman)) {
                viewDirection = 0;
            } else if (moveRight && pathRight(pacman)) {
                viewDirection = 2;
                if (pacman.getX() + pacman.getTILE_SIZE() / 2 > screenWidth) {
                    pacman.setTilePosition(-pacman.getTILE_SIZE(), pacman.getY());
                }
            } else if (moveLeft && pathLeft(pacman)) {
                viewDirection = 1;
                if (pacman.getX() < -pacman.getTILE_SIZE() / 2) {
                    pacman.setTilePosition(screenWidth / cols * cols, pacman.getY());
                }
            } else if (moveDown && pathDown(pacman)) {
                viewDirection = 3;
            }
        }

        switch (viewDirection){
            case 0:
                pacman.moveUp(4);
                canvas.drawBitmap(pacManUp[currentPacManFrame], (float) (pacman.getX() + 7.5), (float) (pacman.getY() + 11.5), paint);
                break;
            case 1:
                pacman.moveLeft(4);
                canvas.drawBitmap(pacManLeft[currentPacManFrame], (float) (pacman.getX() + 7.5), (float) (pacman.getY() + 7.5), paint);
                break;
            case 2:
                pacman.moveRight(4);
                canvas.drawBitmap(pacManRight[currentPacManFrame], (float) (pacman.getX() + 7.5), (float) (pacman.getY() + 7.5), paint);
                break;
            default:
                pacman.moveDown(4);
                canvas.drawBitmap(pacManDown[currentPacManFrame], (float) (pacman.getX() + 7.5), (float) (pacman.getY() + 2.5), paint);
        }
    }

    public int calculateDirection() {
        if (Math.abs(pacman.getBounds().centerX() - ghost.getBounds().centerX()) < Math.abs(pacman.getBounds().centerY() - ghost.getBounds().centerY())) {
            if (ghost.getBounds().centerY() > pacman.getBounds().centerY()) {
                if (pathUp(ghost)) {
                    return 0;
                }
            } else if (pathDown(ghost)) {
                return 3;
            }
        } else {
            if (ghost.getBounds().centerX() > pacman.getBounds().centerX()) {
                if (pathLeft(ghost))
                    return 1;
            } else if (pathRight(ghost)) {
                return 2;
            }
        }
        return 4;
    }

    public void drawGhost(Canvas canvas) {
        if (!insideCage) {
            for (Tile tile: ghostDoor) {
                Rect enemy = ghost.getBounds();
                Rect wall = tile.getBounds();
                if (Rect.intersects(wall, enemy)) {
                    isColliding = true;
                }
                if (isColliding && ghostPreviousDirection == 3) {
                    ghost.setTilePosition(ghost.getX(), wall.top - ghost.getTILE_SIZE());
                }
                isColliding = false;
            }
            switch (calculateDirection()) {
                case 0:
                    ghost.moveUp(3);
                    ghostPreviousDirection = 0;
                    break;
                case 1:
                    ghost.moveLeft(3);
                    ghostPreviousDirection = 1;
                    break;
                case 2:
                    ghost.moveRight(3);
                    ghostPreviousDirection = 2;
                    break;
                case 3:
                    ghost.moveDown(3);
                    ghostPreviousDirection = 3;
                    break;
                case 4:
                    switch (ghostPreviousDirection) {
                        case 0:
                            ghost.moveUp(3);
                            break;
                        case 1:
                            ghost.moveLeft(3);
                            break;
                        case 2:
                            ghost.moveRight(3);
                            break;
                        case 3:
                            ghost.moveDown(3);
                            break;
                    }
                    break;
            }
        }
        canvas.drawBitmap(ghostBitmap, null, ghost.getBounds(), null);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startGame();
        if (gameIsPaused) {
            stopGame();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopGame();
    }

    Runnable longPressed = new Runnable() {
        public void run() {
            Intent pauseIntent = new Intent(getContext(), PauseActivity.class);
            getContext().startActivity(pauseIntent);
        }
    };

    // Method to get touch events
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case (MotionEvent.ACTION_DOWN):
                handler.postDelayed(longPressed, LONG_PRESS_TIME);
                break;
            case (MotionEvent.ACTION_UP):
                handler.removeCallbacks(longPressed);
                break;
        }
        return true;
    }

    public void wonGame(){
        wonGame = true;
        Intent wonLost = new Intent(getContext(), WinLostActivity.class);
        wonLost.putExtra("wonLost", won);
        wonLost.putExtra("wonGame", wonGame);
        getContext().startActivity(wonLost);
    }

    public void lostGame(){
        wonGame = false;
        Intent wonLost = new Intent(getContext(), WinLostActivity.class);
        wonLost.putExtra("wonLost", lost);
        wonLost.putExtra("wonGame", wonGame);
        getContext().startActivity(wonLost);
    }

    private void loadBitmapImages(){
        int spriteSize = TILE_SIZE - 15;

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

        ghostBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                context.getResources(), R.drawable.ghost), TILE_SIZE, TILE_SIZE, false);

        wallBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                getResources(), R.drawable.wall), TILE_SIZE, TILE_SIZE, false);
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
                {2, 2, 2, 2, 2, 1, 1, 4, 4, 4, 1, 1, 2, 2, 2, 2, 2},
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
