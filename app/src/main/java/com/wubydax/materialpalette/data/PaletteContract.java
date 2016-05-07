package com.wubydax.materialpalette.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import java.io.File;

/**
 * Created by Anna Berkovitch on 31/03/2016.
 * Palette database contract
 */
public class PaletteContract implements BaseColumns {

    public static final String TABLE = "palettes";
    public static final String DATABASE_NAME = "Palettes";
    public static final String PALETTE_ID_COLUMN = "_id";
    public static final String NAME_COLUMN = "name";
    public static final String PRIMARY_COLOR_COLUMN = "primary_color";
    public static final String ACCENT_COLOR_COLUMN = "accent_color";
    public static final String PREVIEW_IMAGE_COLUMN = "preview_image";

    public static final int ID_POSITION = 0;
    public static final int NAME_POSITION = 1;
    public static final int PRIMARY_POSITION = 2;
    public static final int ACCENT_POSITION = 3;
    public static final int PREVIEW_POSITION = 4;


    public static final String CONTENT_AUTHORITY = "com.wubydax.materialpalette.Palettes";
    public static final Uri BASE_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + File.pathSeparator + CONTENT_AUTHORITY + File.pathSeparator + TABLE;
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + File.pathSeparator + CONTENT_AUTHORITY + File.pathSeparator + TABLE;
    public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(TABLE).build();

    public static Uri getContentUriWithTitle(String paletteName) {
        return CONTENT_URI.buildUpon().appendPath(paletteName).build();
    }
}
