package com.pedromr.apps.fridgelist;

import android.content.Context;
import android.database.Observable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.ResultReceiver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Observer;

/**
 * Created by pedro on 11/22/17.
 */

public class DoodleCanvas extends View {

    private static final String LOG_TAG = "DOODLE";
    private Paint mCurrentPaint;
    private Path mCurrentPath;
    private LinkedList<Path> mPaths;
    private LinkedList<Paint> mPaints;

    private ArrayList<String> mLines;
    private ArrayList<PointF> mCurrentLine;

    public Observable<DrawingChanged> OnDrawingChanged() {
        return mDrawingChanged;
    }

    public interface DrawingChanged {
        void OnDrawingChanged(DoodleCanvas observable);
    }

    private class NotifyDrawingChanged extends Observable<DrawingChanged> {
        void notifyDrawingChanged(DoodleCanvas parentCanvas) {
            for(DrawingChanged observer : mObservers)
                observer.OnDrawingChanged(parentCanvas);
        }
    }

    private NotifyDrawingChanged mDrawingChanged;

    public void setBitmap(Bitmap bitmap) {
        backgroundImage = bitmap;
    }

    enum Mode {
        DRAW,
        ERASE
    }

    Mode mCurrentMode;

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
            mCurrentPaint.setStrokeWidth(30);
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
        super.onDraw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                mCurrentLine = new ArrayList<>();
                mCurrentLine.add(new PointF(event.getX(), event.getY()));
                mCurrentPath.moveTo(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                mCurrentPath.lineTo(event.getX(), event.getY());
                mCurrentLine.add(new PointF(event.getX(), event.getY()));
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                StringBuilder line = new StringBuilder(mCurrentMode == Mode.DRAW ? "draw" : "erase");
                for (PointF point: mCurrentLine) {
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
        invalidate();
    }

    public String saveAsJson() {
        return new Gson().toJson(mLines);
    }

    public void loadJSON(String jsonData) {
        mLines = new Gson().fromJson(jsonData, mLines.getClass());
        repathLines();
    }

    private void repathLines() {
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
                Log.d(LOG_TAG, "lin 0 pair "+pair.toString());
                boolean first = i == 1;
                float x = Float.parseFloat(pair[0]);
                float y = Float.parseFloat(pair[1]);
                if (first) mCurrentPath.moveTo(x, y);
                else mCurrentPath.lineTo(x, y);
            }
            addCurrentStroke();
        }
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
}