package com.example.pavel.moneyflow.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.pavel.moneyflow.R;
import com.example.pavel.moneyflow.util.Prefs;

public class AnimatedRoundChart extends SurfaceView implements SurfaceHolder.Callback {

    private enum ColorPalette {GREEN, RED};

    private int animationSpeed = 1;

    private SurfaceHolder holder;
    private DrawThread drawThread;

    private int diameter;

    private RectF sector;
    private Rect textBounds;

    private Paint paintPrimarySector;
    private Paint paintAscendSector;
    private Paint paintInnerCircle;
    private Paint paintText;
    private Paint paintBackground;

    private int colorPrimary;
    private int colorAscend;
    private int backgroundColor = Color.WHITE;

    int drawFromPercent;
    int drawToPercent;
    int currentDrawnPercent;

    boolean isDraw = false;

    public AnimatedRoundChart(Context context, AttributeSet attrs) {
        super(context, attrs);

//        backgroundColor = ((ColorDrawable) getBackground()).getColor();
//        Log.d(Prefs.LOG_TAG, "AnimatedRoundChart: " + backgroundColor);

//        setWillNotDraw(false);
//        setZOrderOnTop(true);
//        setZOrderMediaOverlay(true);
//        setZOrderOnTop(true);

        holder = getHolder();
        holder.addCallback(this);


//        holder.setFormat(PixelFormat.TRANSPARENT);

        setFocusable(true);


        textBounds = new Rect();
        sector = new RectF();

        paintPrimarySector = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintAscendSector = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintInnerCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);

        paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintText.setTypeface(Typeface.DEFAULT_BOLD);
    }

    public void setValues(int value){
        drawToPercent = value;
        if (drawFromPercent < drawToPercent){
            drawThread = new DrawThread(DrawThread.FORWARD);
        } else {
            drawThread = new DrawThread(DrawThread.BACK);
        }
        drawThread.setRunning(true);
        drawThread.start();
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        diameter = (getMeasuredHeight()/2);
        setMeasuredDimension(diameter, diameter);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        Log.d(Prefs.LOG_TAG, "onWindowVisibilityChanged: ");
    }



    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(Prefs.LOG_TAG, "onDraw: ");
        drawChart(canvas, drawToPercent);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        holder.setFormat(PixelFormat.TRANSLUCENT);

//        setWillNotDraw(true);
        drawThread = new DrawThread(DrawThread.FORWARD);
        drawThread.setRunning(true);
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.d(Prefs.LOG_TAG, "surfaceChanged: format - " + format + ", width - " + width + ", height - " + height);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        Log.d(Prefs.LOG_TAG, "onScrollChanged: ");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        if (drawThread == null) return;
        drawThread.setRunning(false);
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            }
            catch (InterruptedException e) {}
        }
    }

    private void initColor(ColorPalette palette){
        switch (palette){
            case GREEN:
                colorPrimary = getResources().getColor(R.color.darkGreen);
                colorAscend = getResources().getColor(R.color.lightGreen);
                break;
            case RED:
                colorPrimary = getResources().getColor(R.color.darkRed);
                colorAscend = getResources().getColor(R.color.lightRed);
                break;
        }

        paintText.setColor(Color.BLACK);
        paintInnerCircle.setColor(Color.WHITE);
        paintPrimarySector.setColor(colorPrimary);
        paintAscendSector.setColor(colorAscend);
    }

    private void drawChart(Canvas canvas, int percent){
        paintBackground.setColor(backgroundColor);
        paintBackground.setStyle(Paint.Style.FILL_AND_STROKE);
        paintBackground.setStrokeWidth(3);
        canvas.drawRect(0, 0, diameter, diameter, paintBackground);
        sector.set(0, 0, diameter, diameter);

        float center = diameter/2;
        String percentText = percent + "%";

        float angle;
        if (percent < 100){
            angle = percent * 3.6f;
            initColor(ColorPalette.GREEN);
        } else if (percent >= 100 && percent < 200){
            angle = percent * 3.6f - 360;
            initColor(ColorPalette.RED);
        } else {
            initColor(ColorPalette.RED);
            paintText.getTextBounds(percentText, 0, percentText.length(), textBounds);
            paintText.setTextSize(diameter/5);

            canvas.drawArc(sector, 0, 360, true, paintPrimarySector);
            canvas.drawCircle(diameter/2, diameter/2, diameter/3, paintInnerCircle);
            canvas.drawText(percentText, center - textBounds.exactCenterX(),
                    center - textBounds.exactCenterY(), paintText);
            return;
        }

        paintText.getTextBounds(percentText, 0, percentText.length(), textBounds);
        paintText.setTextSize(diameter/5);

        canvas.drawArc(sector, 0, 360, true, paintPrimarySector);
        canvas.drawArc(sector, 270, angle, true, paintAscendSector);
        canvas.drawCircle(diameter/2, diameter/2, diameter/3, paintInnerCircle);
        canvas.drawText(percentText, center - textBounds.exactCenterX(),
                center - textBounds.exactCenterY(), paintText);
    }

    private boolean redrawForward(Canvas canvas){
        if ((drawFromPercent < 100 && drawToPercent < 100) ||
                (drawFromPercent >= 100 && drawToPercent >= 100) ||
                (drawFromPercent < 100 && drawToPercent >= 100)){
            if (currentDrawnPercent <= drawToPercent){
                drawChart(canvas, currentDrawnPercent);
                currentDrawnPercent += animationSpeed;
            } else {
                drawChart(canvas, drawToPercent);
                return true;
            }
        } else {
            throw new UnsupportedOperationException("Check drawFromPercent & drawToPercent values!");
        }
        return false;
    }

    private boolean redrawBack(Canvas canvas){
        if ((drawFromPercent < 100 && drawToPercent < 100) ||
                (drawFromPercent >= 100 && drawToPercent >= 100) ||
                (drawFromPercent > 100 && drawToPercent <= 100)){
            if (currentDrawnPercent >= drawToPercent){
                drawChart(canvas, currentDrawnPercent);
                currentDrawnPercent -= animationSpeed;
            } else {
                drawChart(canvas, drawToPercent);
                return true;
            }
        } else {
            throw new UnsupportedOperationException("Check drawFromPercent & drawToPercent values!");
        }
        return false;
    }

    private class DrawThread extends Thread {
        public static final int FORWARD = 0;
        public static final int BACK = 1;

        private int direction;
        private volatile boolean isRunning = false;

        public DrawThread(int direction){
            this.direction = direction;
        }

        public void setRunning(boolean running){
            isRunning = running;
        }

        @Override
        public void run() {
            Log.d(Prefs.LOG_TAG, "run: ");
            while (isRunning){
                Canvas canvas = null;
                try {
                    synchronized (holder) {
                        if (!holder.getSurface().isValid()) continue;
                        canvas = holder.lockCanvas();
                        if (direction == FORWARD && redrawForward(canvas)) {
                            drawFromPercent = drawToPercent;
                            break;
                        } else if (direction == BACK && redrawBack(canvas)) {
                            drawFromPercent = drawToPercent;
                            break;
                        }
                    }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
