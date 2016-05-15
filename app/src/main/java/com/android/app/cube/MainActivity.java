package com.android.app.cube;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Handler mHandler = new Handler();

    private int size = 600;
    private final Paint mPaint = new Paint();
    private float mCenterX;
    private float mCenterY;

    // rotation across Y
    private float mCurrentYRot = 0;
    private float mCurrentAccelerationY = 0;
    private float mInitialSpeedY = 0;
    private long mStartTimeY = SystemClock.elapsedRealtime();
    private boolean directionY = true;

    // rotation across X
    private float mCurrentXRot = 0;
    private float mCurrentAccelerationX = 0;
    private float mInitialSpeedX = 0;
    private long mStartTimeX = SystemClock.elapsedRealtime();
    private boolean directionX = true;

    private void stop() {
        mInitialSpeedX = 0;
        mInitialSpeedY = 0;
        mCurrentXRot = 0;
        mCurrentYRot = 0;
    }

    private float getPositionY() {
        long timeElapsed = (SystemClock.elapsedRealtime() - mStartTimeY);
        float finalVelocity = mInitialSpeedY + mCurrentAccelerationY * timeElapsed;
        float position = mCurrentYRot;
        if (directionY && finalVelocity > 0)
            position = (mInitialSpeedY * timeElapsed) + (0.5f * mCurrentAccelerationY * (timeElapsed * timeElapsed)) % 360;
        if (! directionY && finalVelocity < 0)
            position = (mInitialSpeedY * timeElapsed) + (0.5f * mCurrentAccelerationY * (timeElapsed * timeElapsed)) % 360;
        Log.e(TAG, "finalVelocityY: "  + finalVelocity + " mCurrentAccelerationY: " + mCurrentAccelerationY + " positionY: " + position);
        return position;
    }

    private float getPositionX() {
        long timeElapsed = (SystemClock.elapsedRealtime() - mStartTimeX);
        float finalVelocity = mInitialSpeedX + mCurrentAccelerationX * timeElapsed;
        float position = mCurrentXRot;
        if (directionX && finalVelocity > 0)
            position = (mInitialSpeedX * timeElapsed) + (0.5f * mCurrentAccelerationX * (timeElapsed * timeElapsed)) % 360;
        if (! directionX && finalVelocity < 0)
            position = (mInitialSpeedX * timeElapsed) + (0.5f * mCurrentAccelerationX * (timeElapsed * timeElapsed)) % 360;
        Log.e(TAG, "finalVelocityX: "  + finalVelocity + " mCurrentAccelerationX: " + mCurrentAccelerationX + " positionX: " + position);
        return position;
    }

    private final Runnable mDrawCube = new Runnable() {
        public void run() {
            mCurrentYRot = getPositionY();
            mCurrentXRot = getPositionX();
            tryDrawing(mSurfaceHolder);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop();
            }
        });
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeRight(float velocityX) {
                super.onSwipeRight(velocityX);
                directionY = true;
                Log.e(TAG, "velocityX: " + velocityX );
                mCurrentAccelerationY = - 0.5f;
                mInitialSpeedY = velocityX / 10;
                mStartTimeY = SystemClock.elapsedRealtime();
            }

            @Override
            public void onSwipeLeft(float velocityX) {
                super.onSwipeLeft(velocityX);
                directionY = false;
                mCurrentAccelerationY = 0.5f;
                mInitialSpeedY = velocityX / 10;
                mStartTimeY = SystemClock.elapsedRealtime();
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e(TAG, "touch down");
                    if (directionY)
                        mCurrentAccelerationY = - Integer.MAX_VALUE;
                    else mCurrentAccelerationY = Integer.MAX_VALUE;

                    if (directionX)
                        mCurrentAccelerationX = - Integer.MAX_VALUE;
                    else mCurrentAccelerationX = Integer.MAX_VALUE;
                }
                return super.onTouch(v, event);
            }

            @Override
            public void onSwipeTop(float velocityY) {
                super.onSwipeTop(velocityY);
                directionX = false;
                Log.e(TAG, "velocityY top -ve: " + velocityY );
                mCurrentAccelerationX = 0.5f;
                mInitialSpeedX = velocityY / 10;
                mStartTimeX = SystemClock.elapsedRealtime();
            }

            @Override
            public void onSwipeBottom(float velocityY) {
                super.onSwipeBottom(velocityY);
                directionX = true;
                Log.e(TAG, "velocityY bottom +ve:" + velocityY );
                mCurrentAccelerationX = - 0.5f;
                mInitialSpeedX = velocityY / 10;
                mStartTimeX = SystemClock.elapsedRealtime();
            }
        });
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        final Paint paint = mPaint;
        paint.setColor(0xffffffff);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(2);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        mStartTimeY = SystemClock.elapsedRealtime();
        mStartTimeX = SystemClock.elapsedRealtime();
        tryDrawing(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        mCenterX = width / 2.0f;
        mCenterY = height / 2.0f;
        tryDrawing(surfaceHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mHandler.removeCallbacks(mDrawCube);
    }

    private void tryDrawing(SurfaceHolder surfaceHolder) {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas();
            if (canvas == null) {
                Log.e(TAG, "canvas is not ready for drawing");
            } else {
                drawOnCanvas(canvas);
            }
        } finally {
            if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas);
        }

        mHandler.removeCallbacks(mDrawCube);
        mHandler.postDelayed(mDrawCube, 1000 / 20);

    }

    private void drawOnCanvas(Canvas c) {
        c.save();
        c.translate(mCenterX, mCenterY);
        c.drawColor(0xff000000);
        drawLine(c, -size, -size, -size, size, -size, -size);
        drawLine(c, size, -size, -size, size, size, -size);
        drawLine(c, size, size, -size, -size, size, -size);
        drawLine(c, -size, size, -size, -size, -size, -size);

        drawLine(c, -size, -size, size, size, -size, size);
        drawLine(c, size, -size, size, size, size, size);
        drawLine(c, size, size, size, -size, size, size);
        drawLine(c, -size, size, size, -size, -size, size);

        drawLine(c, -size, -size, size, -size, -size, -size);
        drawLine(c, size, -size, size, size, -size, -size);
        drawLine(c, size, size, size, size, size, -size);
        drawLine(c, -size, size, size, -size, size, -size);
        c.restore();
    }

    void drawLine(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2) {
        float xrot = mCurrentXRot;
        float yrot = mCurrentYRot;
        float zrot = 0;

        // 3D transformations

        // rotation around X-axis
        float newy1 = (float) (Math.sin(xrot) * z1 + Math.cos(xrot) * y1);
        float newy2 = (float) (Math.sin(xrot) * z2 + Math.cos(xrot) * y2);
        float newz1 = (float) (Math.cos(xrot) * z1 - Math.sin(xrot) * y1);
        float newz2 = (float) (Math.cos(xrot) * z2 - Math.sin(xrot) * y2);


        // rotation around Y-axis
        float newx1 = (float) (Math.sin(yrot) * newz1 + Math.cos(yrot) * x1);
        float newx2 = (float) (Math.sin(yrot) * newz2 + Math.cos(yrot) * x2);
        newz1 = (float) (Math.cos(yrot) * newz1 - Math.sin(yrot) * x1);
        newz2 = (float) (Math.cos(yrot) * newz2 - Math.sin(yrot) * x2);

        // rotation around Z-axis
        newy1 = (float) (Math.sin(zrot) * newx1 + Math.cos(zrot) * newy1);
        newy2 = (float) (Math.sin(zrot) * newx2 + Math.cos(zrot) * newy2);
        newx1 = (float) (Math.cos(zrot) * newx1 - Math.sin(zrot) * newy1);
        newx2 = (float) (Math.cos(zrot) * newx2 - Math.sin(zrot) * newy2);

        // 3D-to-2D projection
        float startX = newx1 / (4 - newz1 / size);
        float startY = newy1 / (4 - newz1 / size);
        float stopX = newx2 / (4 - newz2 / size);
        float stopY = newy2 / (4 - newz2 / size);

        c.drawLine(startX, startY, stopX, stopY, mPaint);
    }



}
