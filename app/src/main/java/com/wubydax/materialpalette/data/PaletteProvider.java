package com.wubydax.materialpalette.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.wubydax.materialpalette.MainViewFragment;

/**
 * Created by Anna Berkovitch on 31/03/2016.
 * Palettes content provider
 */
public class PaletteProvider extends ContentProvider {
    private SQLiteDatabase mSQLiteDatabase;
    private ContentResolver mContentResolver;
    private static final UriMatcher mUriMatcher;
    private static final int PALETTES = 46;
    private static final int PALETTE_NAME = 58;
    private static final int PALETTE_ID = 29;

    private static final int EXCLUDE_PALETTES = 4;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(PaletteContract.CONTENT_AUTHORITY, PaletteContract.TABLE, PALETTES);
        mUriMatcher.addURI(PaletteContract.CONTENT_AUTHORITY, PaletteContract.TABLE + "/#", PALETTE_ID);
        mUriMatcher.addURI(PaletteContract.CONTENT_AUTHORITY, PaletteContract.TABLE + "/exclude", EXCLUDE_PALETTES);
        mUriMatcher.addURI(PaletteContract.CONTENT_AUTHORITY, PaletteContract.TABLE + "/*", PALETTE_NAME);
    }

    @Override
    public boolean onCreate() {
        PaletteDatabaseHelper paletteDatabaseHelper = new PaletteDatabaseHelper(getContext());
        mSQLiteDatabase = paletteDatabaseHelper.getWritableDatabase();
        assert getContext() != null;
        mContentResolver = getContext().getContentResolver();
        return mSQLiteDatabase != null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(PaletteContract.TABLE);
        switch (mUriMatcher.match(uri)) {
            case PALETTES:
                if(selectionArgs != null) {
                    queryBuilder.appendWhere(PaletteContract.NAME_COLUMN + " LIKE ?");
                }
                break;
            case EXCLUDE_PALETTES:
                Log.d("provider", "query exclude pattern recognized");
                if(selectionArgs != null) {
                    for(int i=0; i<selectionArgs.length; i++) {

                            queryBuilder.appendWhere(i==0 ? PaletteContract.NAME_COLUMN + " NOT LIKE ?" : " AND " + PaletteContract.NAME_COLUMN + " NOT LIKE ?");

                    }


                }
                break;
            case PALETTE_NAME:
                if(selectionArgs == null) {
                    queryBuilder.appendWhere(PaletteContract.NAME_COLUMN + " LIKE " + "\'" + uri.getLastPathSegment() + "\'");
                } else {
                    queryBuilder.appendWhere(PaletteContract.NAME_COLUMN + " LIKE ?");
                }
                break;
            case PALETTE_ID:
                queryBuilder.appendWhere(PaletteContract.PALETTE_ID_COLUMN + "=" + uri.getLastPathSegment());
                break;
        }

        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = PaletteContract.NAME_COLUMN;
        }

        Cursor c = queryBuilder.query(mSQLiteDatabase, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(mContentResolver, uri);

        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case PALETTES:
                return PaletteContract.CONTENT_TYPE;
            case PALETTE_NAME:
                return PaletteContract.CONTENT_ITEM_TYPE;
            case PALETTE_ID:
                return PaletteContract.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long rowId = mSQLiteDatabase.insert(PaletteContract.TABLE, "", values);
        if (rowId > 0) {
            Uri insertedItemUri = ContentUris.withAppendedId(PaletteContract.CONTENT_URI, rowId);
            mContentResolver.notifyChange(insertedItemUri, null);
            return insertedItemUri;
        }

        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        Log.d(MainViewFragment.LOG_TAG, "delete is called");
        int deletedItemsCount;
        Log.d(MainViewFragment.LOG_TAG, "delete " + mUriMatcher.match(uri));
        switch (mUriMatcher.match(uri)) {
            case PALETTES:
                deletedItemsCount = mSQLiteDatabase.delete(PaletteContract.TABLE, selection, selectionArgs);
                break;

            case PALETTE_NAME:
                deletedItemsCount = mSQLiteDatabase.delete(PaletteContract.TABLE, PaletteContract.NAME_COLUMN +
                                " LIKE " +"\'" +  uri.getLastPathSegment() + "\'" +
                                (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            case PALETTE_ID:
                Log.d(MainViewFragment.LOG_TAG, "delete is called with id uri matcher");
                deletedItemsCount = mSQLiteDatabase.delete(PaletteContract.TABLE, PaletteContract.PALETTE_ID_COLUMN +
                                "=" + uri.getLastPathSegment() +
                                (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            default:
                Log.d(MainViewFragment.LOG_TAG, "delete unknown uri");
                throw new IllegalArgumentException("Unknown URI " + uri);

        }
        mContentResolver.notifyChange(uri, null);
        return deletedItemsCount;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int updatedItemsCount;
        switch (mUriMatcher.match(uri)) {
            case PALETTES:
                updatedItemsCount = mSQLiteDatabase.update(PaletteContract.TABLE, values, selection, selectionArgs);
                break;

            case PALETTE_NAME:
                updatedItemsCount = mSQLiteDatabase.update(PaletteContract.TABLE, values, PaletteContract.NAME_COLUMN +
                                " LIKE " +"\'" +  uri.getLastPathSegment() + "\'" +
                                (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            case PALETTE_ID:
                updatedItemsCount = mSQLiteDatabase.update(PaletteContract.TABLE, values, PaletteContract.PALETTE_ID_COLUMN +
                                "=" + uri.getLastPathSegment() +
                                (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);

        }
        mContentResolver.notifyChange(uri, null);
        return updatedItemsCount;
    }
}
