package com.android.app.cube;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Handler mHandler = new Handler();

    private int size = 600;
    private final Paint mPaint = new Paint();
    private float mCenterX;
    private float mCenterY;

    private float mCurrentYRot = 0;
    private float mCurrentAcceleration = 0;
    private float mInitialSpeed = 0;
    private long mStartTime = SystemClock.elapsedRealtime();
    private boolean direction = true;

    private float getPosition() {
        long timeElapsed = (SystemClock.elapsedRealtime() - mStartTime);
        float finalVelocity = mInitialSpeed + mCurrentAcceleration * timeElapsed;
        float position = mCurrentYRot;
        if (direction && finalVelocity > 0)
            position = (mInitialSpeed * timeElapsed) + (0.5f * mCurrentAcceleration * (timeElapsed * timeElapsed)) % 360;
        if (! direction && finalVelocity < 0)
            position = (mInitialSpeed * timeElapsed) + (0.5f * mCurrentAcceleration * (timeElapsed * timeElapsed)) % 360;
        Log.e(TAG, "finalVelocity: "  + finalVelocity + " mCurrentAcceleration: " + mCurrentAcceleration + " position: " + position);
        return position;
    }

    private final Runnable mDrawCube = new Runnable() {
        public void run() {
            mCurrentYRot = getPosition();
            tryDrawing(mSurfaceHolder);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeRight(float velocityX) {
                super.onSwipeRight(velocityX);
                direction = true;
                Log.e(TAG, "velocityX: " + velocityX );
                mCurrentAcceleration = - 0.5f;
                mInitialSpeed = velocityX / 20;
                mStartTime = SystemClock.elapsedRealtime();
            }

            @Override
            public void onSwipeLeft(float velocityX) {
                super.onSwipeLeft(velocityX);
                direction = false;
                mCurrentAcceleration = 0.5f;
                mInitialSpeed = velocityX / 20;
                mStartTime = SystemClock.elapsedRealtime();
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (direction)
                    mCurrentAcceleration = - Integer.MAX_VALUE;
                else mCurrentAcceleration = Integer.MAX_VALUE;
                return super.onTouch(v, event);
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
        mStartTime = SystemClock.elapsedRealtime();
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
        mHandler.postDelayed(mDrawCube, 1000 / 25);

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
        float xrot = 0;
        float yrot = mCurrentYRot;
        //float zrot = 1;

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

        // 3D-to-2D projection
        float startX = newx1 / (4 - newz1 / size);
        float startY = newy1 / (4 - newz1 / size);
        float stopX = newx2 / (4 - newz2 / size);
        float stopY = newy2 / (4 - newz2 / size);

        c.drawLine(startX, startY, stopX, stopY, mPaint);
    }

}
