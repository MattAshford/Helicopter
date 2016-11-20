package com.mattashford.helicopter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private long smokeStartTime;
    private long missileStartTime;
    private Random rand = new Random();
    private MainThread thread;
    private Background bg;
    private Helicopter helicopter;
    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private boolean newGameCreated = false;
    private boolean firstGame = true;
    // Increase = easier, decrease = harder
    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean dissapear;
    private boolean started;
    private int best;

    public GamePanel(Context context) {
        super(context);
        // Add the callback to the surfaceholder to intercept events
        getHolder().addCallback(this);
        // Make gamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        int counter = 0;
        while (retry && counter < 1000) {
            counter++;
            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        helicopter = new Helicopter(BitmapFactory.decodeResource(getResources(),
                                    R.drawable.helicopter), 65, 25, 3);
        smoke = new ArrayList<Smokepuff>();
        missiles = new ArrayList<Missile>();
        smokeStartTime = System.nanoTime();
        missileStartTime = System.nanoTime();
        thread = new MainThread(getHolder(), this);
        // We can safely start the game loop
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!helicopter.getPlaying() && newGameCreated && reset) {
                helicopter.setPlaying(true);
                helicopter.setUp(true);
            }
            if (helicopter.getPlaying()){
                if (!started) started = true;
                reset = false;
                helicopter.setUp(true);
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            helicopter.setUp(false);
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void update() {
       if (helicopter.getPlaying()) {

            bg.update();
            helicopter.update();

            // Check border collisions
                if (helicopter.getY() > HEIGHT -50 ) {
                    helicopter.setPlaying(false);
                }

                if (helicopter.getY() < 0) {
                    helicopter.setPlaying(false);
                }

            // Add missiles on timer
            long missilesElapsed = (System.nanoTime()-missileStartTime)/1000000;
            if (missilesElapsed > (2000- helicopter.getScore()/4)) {
                // First missile always goes down the middle
                if (missiles.size() == 0) {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),
                                 R.drawable.missile),
                                 WIDTH+10, HEIGHT/2, 45, 15, helicopter.getScore(), 13));
                } else {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(),
                                 R.drawable.missile),
                                 WIDTH+10, (int)(rand.nextDouble()*HEIGHT)
                                 /*-(maxBorderHeight*2))+maxBorderHeight */,
                                 45, 15, helicopter.getScore(), 13));
                }
                missileStartTime = System.nanoTime();
            }
            for (int i = 0; i < missiles.size(); i++) {
                missiles.get(i).update();
                if (collision(missiles.get(i), helicopter)) {
                    missiles.remove(i);
                    helicopter.setPlaying(false);
                    break;
                }
                if (missiles.get(i).getX() < -45) {
                    missiles.remove(i);
                    break;
                }
            }

            // Add smoke puffs on timer
            long elapsed = (System.nanoTime()-smokeStartTime)/1000000;
            if (elapsed > 120) {
                smoke.add(new Smokepuff(helicopter.getX(), helicopter.getY()+10));
                smokeStartTime = System.nanoTime();
            }
            for (int i = 0; i < smoke.size(); i++) {
                smoke.get(i).update();
                if (smoke.get(i).getX() < -10) {
                    smoke.remove(i);
                }
            }
        } else {
            helicopter.resetDY();
            if (!reset) {
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                dissapear = true;
                explosion = new Explosion(BitmapFactory.decodeResource(getResources(),
                                          R.drawable.explosion), helicopter.getX(),
                                            helicopter.getY()-30, 100, 100, 25);
            }
            explosion.update();
            long resetElapsed = (System.nanoTime()-startReset)/1000000;
            if ((resetElapsed > 2500 && !newGameCreated) || firstGame) {
                if (firstGame) explosion.setPlayedOnce(true);
                newGame();
                firstGame = false;
            }
        }
    }
    public boolean collision (GameObject a, GameObject b) {
        if (Rect.intersects(a.getRectange(), b.getRectange())) {
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            final float scaleFactorX =  (float)getWidth()/WIDTH;
            final float scaleFactorY =  (float)getHeight()/HEIGHT;
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            if (!dissapear) helicopter.draw(canvas);
            for (Smokepuff sp: smoke) {
                sp.draw(canvas);
            }
            for (Missile m: missiles) {
                m.draw(canvas);
            }

            // Draw explosion
            if (started) {
                explosion.draw(canvas);
            }
            drawText(canvas);
            canvas.restoreToCount(savedState);
        }
    }

    public void newGame() {
        dissapear = false;
  //      topborder.clear();
    //    bottomborder.clear();
        missiles.clear();
        smoke.clear();
        helicopter.setY(HEIGHT/2);
        helicopter.resetDY();

        if (helicopter.getScore() > best) {
            best = helicopter.getScore();
        }
        helicopter.resetScore();
        newGameCreated = true;
    }

    public void drawText(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("DISTANCE: " + helicopter.getScore()*3, 10, HEIGHT-10, paint);
        canvas.drawText("BEST: " + best*3, WIDTH-215, HEIGHT - 10, paint);
        if (!helicopter.getPlaying() && newGameCreated && reset) {
            Paint paint1 = new Paint();
            paint1.setColor(Color.GREEN);
            paint1.setTextSize(30);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("PRESS TO START", WIDTH/2-50, HEIGHT/2, paint1);
            paint1.setTextSize(20);
            canvas.drawText("PRESS AND HOLD TO GO UP", WIDTH/2-50, HEIGHT/2+20, paint1);
            canvas.drawText("RELEASE TO GO DOWN", WIDTH/2-50, HEIGHT/2+40, paint1);
        }
    }
}
