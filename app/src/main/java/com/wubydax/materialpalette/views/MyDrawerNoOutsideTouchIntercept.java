package com.wubydax.materialpalette.views;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Anna Berkovitch on 02/04/2016.
 * for tablet view converting drawer into dual pane
 */
public class MyDrawerNoOutsideTouchIntercept extends DrawerLayout {
    public MyDrawerNoOutsideTouchIntercept(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        View myDrawer = getChildAt(1);
        return !(getDrawerLockMode(myDrawer) == LOCK_MODE_LOCKED_OPEN && ev.getX() > myDrawer.getWidth()) && super.onInterceptTouchEvent(ev);
    }

}
