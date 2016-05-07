package com.wubydax.materialpalette.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.wubydax.materialpalette.MainActivity;
import com.wubydax.materialpalette.R;
import com.wubydax.materialpalette.data.PaletteContract;
import com.wubydax.materialpalette.utils.Constants;
import com.wubydax.materialpalette.utils.Utils;

/**
 * Created by Anna Berkovitch on 02/04/2016.
 * Remote views factory for populating the palettes widget
 */
public class PaletteWidgetViewFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;

    public PaletteWidgetViewFactory(Context context) {
        mContext = context;
    }
    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {
        if(mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(PaletteContract.CONTENT_URI, null, null, null, PaletteContract.NAME_COLUMN);

    }

    @Override
    public void onDestroy() {
        if(mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }

    @Override
    public int getCount() {
        return mCursor != null && !mCursor.isClosed() ? mCursor.getCount() : 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Bitmap bitmap = null;
        int primaryColor = 0, accentColor = 0;
        if(mCursor.moveToPosition(position)) {
            bitmap = Utils.getBitmap(mCursor.getBlob(PaletteContract.PREVIEW_POSITION));
            primaryColor = mCursor.getInt(PaletteContract.PRIMARY_POSITION);
            accentColor = mCursor.getInt(PaletteContract.ACCENT_POSITION);
        }
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.widget_grid_item_layout);
        remoteViews.setImageViewBitmap(R.id.previewImageWidget, bitmap);
        Intent fillInIntent = new Intent();
        Bundle extra = new Bundle();
        extra.putInt(Constants.PRIMARY_COLOR_KEY, primaryColor);
        extra.putInt(Constants.ACCENT_COLOR_KEY, accentColor);
        fillInIntent.putExtras(extra);
        remoteViews.setOnClickFillInIntent(R.id.previewImageWidget, fillInIntent);
        return remoteViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
