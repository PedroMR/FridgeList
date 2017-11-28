package com.pedromr.apps.fridgelist;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by pedro on 11/22/17.
 */

public class DoodleCanvas extends View {

    private static final String LOG_TAG = "DOODLE";
    private Paint mPaint;
    private Path mPath;

    private ArrayList<String> mLines;
    private ArrayList<PointF> mCurrentLine;

    public DoodleCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLines = new ArrayList<String>();
        mPaint = new Paint();
        mCurrentLine = new ArrayList<PointF>();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(10);
        mPath = new Path();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
        super.onDraw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                mCurrentLine = new ArrayList<PointF>();
                mCurrentLine.add(new PointF(event.getX(), event.getY()));
                mPath.moveTo(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(event.getX(), event.getY());
                mCurrentLine.add(new PointF(event.getX(), event.getY()));
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                String line = "draw";
                for (PointF point: mCurrentLine) {
                    line += "|" + point.x + "," +point.y;
                }
                mLines.add(line);
                Log.d(LOG_TAG, "Up adding line "+line);
                Log.d(LOG_TAG, "new array: "+mLines.toString());
                //TODO signal this was modified to request save
                break;
        }

        return true;
    }

    public void clear() {
        mPath.reset();
        mLines.clear();
    }

    public String saveAsJson() {
        return new Gson().toJson(mLines);
    }

    public void loadJSON(String jsonData) {
        mLines = new Gson().fromJson(jsonData, mLines.getClass());
        repathLines();
    }

    private void repathLines() {
        mPath.reset();
        Log.d(LOG_TAG, "mLines "+ mLines.toString());
        Log.d(LOG_TAG, "mLines 0"+mLines.get(0).toString());
        for (String line: mLines)
        {
            String[] tokens = line.split("\\|");

            for (int i = 0; i < tokens.length; i++) {
                if (i == 0) continue; // "draw"
                String[] pair = tokens[i].split(",");
                Log.d(LOG_TAG, "lin 0 pair "+pair);
                boolean first = i == 1;
                float x = Float.parseFloat(pair[0]);
                float y = Float.parseFloat(pair[1]);
                if (first) mPath.moveTo(x, y);
                else mPath.lineTo(x, y);
            }
        }
    }
}