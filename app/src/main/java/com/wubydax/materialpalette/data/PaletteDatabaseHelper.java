package com.wubydax.materialpalette.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Anna Berkovitch on 31/03/2016.
 * Database helper class for SQLite
 */
public class PaletteDatabaseHelper extends SQLiteOpenHelper{
    public static final int DATABASE_VERSION = 1;
    public static final String CREATE_TABLE = "CREATE TABLE " + PaletteContract.TABLE + " (" +
            PaletteContract.PALETTE_ID_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            PaletteContract.NAME_COLUMN + " TEXT NOT NULL UNIQUE, " +
            PaletteContract.PRIMARY_COLOR_COLUMN + " INTEGER NOT NULL, " +
            PaletteContract.ACCENT_COLOR_COLUMN + " INTEGER NOT NULL, " +
            PaletteContract.PREVIEW_IMAGE_COLUMN + " BLOB);";

    public PaletteDatabaseHelper(Context context) {
        super(context, PaletteContract.DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + PaletteContract.TABLE);
        onCreate(db);

    }
}
