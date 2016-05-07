package com.wubydax.materialpalette.utils;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.wubydax.materialpalette.MainViewFragment;
import com.wubydax.materialpalette.MyApplication;
import com.wubydax.materialpalette.R;
import com.wubydax.materialpalette.data.PaletteContract;
import com.wubydax.materialpalette.views.CircleView;
import com.wubydax.materialpalette.widget.PaletteWidgetProvider;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Created by Anna Berkovitch on 26/03/2016.
 * Provides static access to commonly used method
 */
public class Utils {
    private static Context mContext = MyApplication.getContext();

    public static int getCurrentOrientation() {
        WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            case Surface.ROTATION_90:
                return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            case Surface.ROTATION_180:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            case Surface.ROTATION_270:
                return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            default:
                return mContext.getResources().getConfiguration().orientation;
        }
    }

    public static int getDarkColor(int primaryColor) {
        float[] hsv = new float[3];
        Color.colorToHSV(primaryColor, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    public static String getHexColor(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static int getTitleColor(int color) {
        return isBrightColor(color) ? Color.BLACK : Color.WHITE;
    }

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color)
            return true;

        boolean rtnValue = false;

        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};

        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1]
                * rgb[1] * .691 + rgb[2] * rgb[2] * .068);

        // color is light
        if (brightness >= 167) {
            rtnValue = true;
        }

        return rtnValue;
    }

    public static void invalidateViews(String key, int color, View rootView, boolean isFragment) {
        if (key.equals(Constants.ACCENT_COLOR_KEY)) {
            ((TextView) rootView.findViewById(R.id.accentColorText)).setTextColor(color);
            if (isFragment) {
                ((TextView) rootView.findViewById(R.id.accentValueText)).setText(String.format(Locale.getDefault(), mContext.getString(R.string.accent_color_value_text), Utils.getHexColor(color)));
                ((CircleView) rootView.findViewById(R.id.accentColorPreview)).setFillColor(color);
            }
            int[][] states = new int[][]{new int[]{-android.R.attr.state_pressed}};
            int[] colors = new int[]{color};
            ColorStateList csl = new ColorStateList(states, colors);
            ((FloatingActionButton) rootView.findViewById(R.id.fab)).setBackgroundTintList(csl);
        } else if (key.equals(Constants.PRIMARY_COLOR_KEY)) {
            int colorPrimaryDark = Utils.getDarkColor(color);
            rootView.findViewById(R.id.statusBar).setBackgroundColor(colorPrimaryDark);
            TextView appTitlePreview = (TextView) rootView.findViewById(R.id.appTitleText);
            appTitlePreview.setBackgroundColor(color);
            appTitlePreview.setTextColor(Utils.getTitleColor(color));
            if (isFragment) {
                ((TextView) rootView.findViewById(R.id.primaryValueText)).setText(String.format(Locale.getDefault(), mContext.getString(R.string.primary_color_value_text), Utils.getHexColor(color)));
                ((TextView) rootView.findViewById(R.id.primaryDarkValueText)).setText(String.format(Locale.getDefault(), mContext.getString(R.string.primary_dark_color_value_text), Utils.getHexColor(colorPrimaryDark)));
                ((CircleView) rootView.findViewById(R.id.primaryColorPreview)).setFillColor(color);
            }


        }
    }

    public static byte[] getBlob(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    public static Bitmap getBitmap(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    public static Bitmap createScreenshot(View view) {
        Bitmap bitmap = null;
        if (view != null) {
            view.setDrawingCacheEnabled(true);

            bitmap = view.getDrawingCache().copy(Bitmap.Config.RGB_565, false);
            view.setDrawingCacheEnabled(false);
        }
        return bitmap;
    }

    public static boolean isInDb(String paletteName) {
        Uri uri = PaletteContract.getContentUriWithTitle(paletteName);
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        boolean exists = cursor != null && !cursor.isClosed() && cursor.getCount() > 0;
        if(cursor != null) {
            cursor.close();
        }
        return exists;
    }
    public static void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
        ComponentName cn = new ComponentName(mContext, PaletteWidgetProvider.class);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(cn), R.id.widgetGrid);
    }

    public static String buildXmlFile(int primaryColor, int primaryDarkColor, int accentColor) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<resources>\n");
        sb.append("\t<color name=\"colorPrimary\">" + getHexColor(primaryColor) + "</color>\n");
        sb.append("\t<color name=\"colorPrimaryDark\">" + getHexColor(primaryDarkColor) + "</color>\n");
        sb.append("\t<color name=\"colorAccent\">" + getHexColor(accentColor) + "</color>\n");
        sb.append("</resources>");


        return sb.toString();
    }

    public static boolean isDriveEnabled() {
        return Constants.mSharedPreferences.getBoolean(Constants.DRIVE_KEY, false);
    }
}
