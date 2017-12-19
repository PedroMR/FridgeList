package com.pedromr.apps.fridgelist;

import android.content.Context;
import android.database.Observable;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by pedro on 11/22/17.
 */

public class DoodleCanvas extends View {

    private static final String LOG_TAG = "DOODLE";
    private static final int MY_CAPSULE_VERSION = 1;
    private Paint mCurrentPaint;
    private Path mCurrentPath;
    private LinkedList<Path> mPaths;
    private LinkedList<Paint> mPaints;

    private ArrayList<String> mLines;
    private ArrayList<Point> mCurrentLine;
    private Point lastTouch;
    private int eraserRadius = 50;
    private Paint eraserFeedbackPaint;

    final class Capsule {
        int version;
        ArrayList<String> mLines;
    }

    public Observable<DrawingChanged> OnDrawingChanged() {
        return mDrawingChanged;
    }

    public Mode getCurrentMode() {
        return mCurrentMode;
    }

    public interface DrawingChanged {
        void onDrawingChanged(DoodleCanvas observable);
    }

    private class NotifyDrawingChanged extends Observable<DrawingChanged> {
        void notifyDrawingChanged(DoodleCanvas parentCanvas) {
            for(DrawingChanged observer : mObservers)
                observer.onDrawingChanged(parentCanvas);
        }
    }

    private NotifyDrawingChanged mDrawingChanged;

    enum Mode {
        DRAW,
        ERASE
    }

    private Mode mCurrentMode;

    public DoodleCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLines = new ArrayList<>();
        mCurrentPaint = new Paint();
        mCurrentLine = new ArrayList<>();
        setDrawingMode();
        mCurrentPath = new Path();
        mCurrentMode = Mode.DRAW;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mPaints = new LinkedList<>();
        mPaths = new LinkedList<>();

        mDrawingChanged = new NotifyDrawingChanged();

        eraserFeedbackPaint = new Paint();
        eraserFeedbackPaint.setColor(Color.BLACK);
        eraserFeedbackPaint.setStyle(Paint.Style.STROKE);
        eraserFeedbackPaint.setStrokeWidth(5);
    }

    private void setDrawingMode() {
        mCurrentPaint.setColor(Color.RED);
        mCurrentPaint.setStyle(Paint.Style.STROKE);
        mCurrentPaint.setStrokeJoin(Paint.Join.ROUND);
        mCurrentPaint.setStrokeCap(Paint.Cap.ROUND);
        mCurrentPaint.setStrokeWidth(10);
        mCurrentPaint.setAlpha(255);
        if (mCurrentMode == Mode.DRAW) {
            mCurrentPaint.setXfermode(null);
        } else {
            mCurrentPaint.setStrokeWidth(eraserRadius*2);
            mCurrentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        for (int i=0; i < mPaths.size(); i++) {
            Path path = mPaths.get(i);
            Paint paint = mPaints.get(i);
            canvas.drawPath(path, paint);
        }
        canvas.drawPath(mCurrentPath, mCurrentPaint);

        if (isInEraseMode() && !mCurrentPath.isEmpty()) {
            canvas.drawCircle(lastTouch.x, lastTouch.y, eraserRadius, eraserFeedbackPaint);
        }

        super.onDraw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        lastTouch = new Point((int)event.getX(), (int)event.getY());

        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                mCurrentLine = new ArrayList<>();
                mCurrentLine.add(lastTouch);
                mCurrentPath.moveTo(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                mCurrentPath.lineTo(event.getX(), event.getY());
                mCurrentLine.add(lastTouch);
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                StringBuilder line = new StringBuilder(mCurrentMode == Mode.DRAW ? "draw" : "erase");
                for (Point point: mCurrentLine) {
                    line.append("|").append(point.x).append(",").append(point.y);
                }
                mLines.add(line.toString());
                addCurrentStroke();
                Log.d(LOG_TAG, "Up adding line "+line);
                notifyDrawingModified();
                break;
        }

        return true;
    }

    private void notifyDrawingModified() {
        mDrawingChanged.notifyDrawingChanged(this);
    }

    public void clear() {
        mCurrentPath.reset();
        mLines.clear();
        mCurrentMode = Mode.DRAW;
        repathLines();
        invalidate();
    }

    public String saveAsJson() {
        Gson gson = new Gson();
        Capsule capsule = new Capsule();
        capsule.version = MY_CAPSULE_VERSION;
        capsule.mLines = mLines;

        return gson.toJson(capsule);
    }

    public void loadJSON(String jsonData) {
        Gson gson = new Gson();

        try {
            Capsule capsule = gson.fromJson(jsonData, Capsule.class);
            if (capsule.version > MY_CAPSULE_VERSION) {
                Log.w(LOG_TAG, "Saved data version "+capsule.version+"; expected "+MY_CAPSULE_VERSION);
            }
            mLines = capsule.mLines;
        } catch(Exception e) {
            Log.w(LOG_TAG, e);
            mLines = gson.fromJson(jsonData, mLines.getClass());
        }
        repathLines();
    }

    private void repathLines() {
        Mode previousMode = mCurrentMode;
        mCurrentPath.reset();
        mCurrentPaint.reset();
        mPaths.clear();
        mPaints.clear();
        Log.d(LOG_TAG, "mLines "+ mLines.toString());
        for (String line: mLines)
        {
            mCurrentPath = new Path();
            mCurrentPaint = new Paint();
            String[] tokens = line.split("\\|");

            for (int i = 0; i < tokens.length; i++) {
                if (i == 0) {
                    mCurrentMode = ("erase".equals(tokens[i])) ? Mode.ERASE : Mode.DRAW;
                    setDrawingMode();
                    continue;
                }
                String[] pair = tokens[i].split(",");
                boolean first = i == 1;
                int x = Integer.parseInt(pair[0]);
                int y = Integer.parseInt(pair[1]);
                if (first) mCurrentPath.moveTo(x, y);
                else mCurrentPath.lineTo(x, y);
            }
            addCurrentStroke();
        }

        mCurrentMode = previousMode;
        setDrawingMode();
    }

    private void addCurrentStroke() {
        mPaths.add(mCurrentPath);
        mCurrentPath = new Path();
        mPaints.add(mCurrentPaint);
        mCurrentPaint = new Paint();
        setDrawingMode();
    }

    public void undo() {
        if (!canUndo()) return;

        mLines.remove(mLines.size()-1);
        repathLines();
        invalidate();

        notifyDrawingModified();
    }

    public boolean canUndo() {
        return mLines.size() > 0;
    }

    public void setEraseMode(boolean eraseMode) {
        if (eraseMode) {
            mCurrentMode = Mode.ERASE;
        } else {
            mCurrentMode = Mode.DRAW;
        }

        setDrawingMode();
    }

    public void toggleEraseMode() {
        if (mCurrentMode == Mode.ERASE)
            mCurrentMode = Mode.DRAW;
        else
            mCurrentMode = Mode.ERASE;

        setDrawingMode();
    }

    public boolean isInEraseMode() {
        return mCurrentMode == Mode.ERASE;
    }
}