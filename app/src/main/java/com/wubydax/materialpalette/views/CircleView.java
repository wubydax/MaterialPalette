package com.wubydax.materialpalette.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.wubydax.materialpalette.R;

/**
 * Created by Anna Berkovitch on 26/03/2016 as part of Udacity capstone project
 * The purpose is to create a simple circular view, as opposed to setting s background of a view to be a circle.
 * The idea is to provide an easy way to change the circle color based on chosen color
 */
public class CircleView extends View {

    private int mFillColor;
    private Paint mCirclePaint;
    private RectF mViewRect;

    public CircleView(Context context) {
        super(context);
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleView, 0, 0);
        try {
            mFillColor = typedArray.getColor(R.styleable.CircleView_fillColor, Color.RED);
        } finally {
            typedArray.recycle();
            init();
        }
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(mFillColor);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        mViewRect = new RectF(0, 0, params.width, params.height);
    }

    public void setFillColor(int color) {
        mFillColor = color;
        mCirclePaint.setColor(color);
        invalidate();
        requestLayout();
    }

    public int getFillColor() {
        return mFillColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawOval(mViewRect, mCirclePaint);

    }

}
