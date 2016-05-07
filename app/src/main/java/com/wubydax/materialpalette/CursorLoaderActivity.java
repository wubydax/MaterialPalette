package com.wubydax.materialpalette;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.wubydax.materialpalette.data.PaletteContract;
import com.wubydax.materialpalette.utils.Constants;
import com.wubydax.materialpalette.utils.CursorAdapter;
import com.wubydax.materialpalette.utils.Utils;

import java.util.ArrayList;

public class CursorLoaderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, CursorAdapter.OnCursorSelectedListener {

    private static final int LOADER_ID = 46;
    private CursorAdapter mCursorAdapter;
    private MenuItem mDelete;
    private ArrayList<Long> mIdList;
    private Toast mToast;
    private boolean isSelectMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cursor_loader);
        Toolbar toolbar = (Toolbar) findViewById(R.id.cursorLoaderToolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mCursorAdapter = new CursorAdapter(this);
        mCursorAdapter.setOnCursorSelectedListener(this);
        if (savedInstanceState != null) {
            isSelectMode = savedInstanceState.getBoolean(Constants.SELECT_MODE_KEY);
            mCursorAdapter.setSelectedPositionList(savedInstanceState.getIntegerArrayList(Constants.LIST_KEY));
            mCursorAdapter.setSelectMode(isSelectMode);
            onSelectModeEnabled();
        }
        getLoaderManager().initLoader(LOADER_ID, null, this);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.cursorRecyclerView);
        int columnCount = getResources().getInteger(R.integer.cursor_view_column_count);
        StaggeredGridLayoutManager staggeredGridLayoutManager = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        assert recyclerView != null;
        recyclerView.setLayoutManager(staggeredGridLayoutManager);
        recyclerView.setAdapter(mCursorAdapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                PaletteContract.CONTENT_URI,
                null,
                null,
                null,
                PaletteContract.NAME_COLUMN);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cursor_loader_menu, menu);
        mDelete = menu.findItem(R.id.actionDeleteFromDb);
        mDelete.setVisible(isSelectMode);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionDeleteFromDb:
                deleteFromDb();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteFromDb() {
        if (mIdList != null && mIdList.size() > 0) {
            Log.d(MainViewFragment.LOG_TAG, "deleteFromDb " + mIdList.size());
            for (long id : mIdList) {
                Uri uri = ContentUris.withAppendedId(PaletteContract.CONTENT_URI, id);
                Log.d(MainViewFragment.LOG_TAG, "deleteFromDb uri is " + uri);
                int deletedItems = getContentResolver().delete(uri, null, null);
                if(deletedItems > 0) {
                    Utils.updateWidget();
                }
            }
        } else {
            showToast("No items selected");
        }
        if (mCursorAdapter != null) {
            mCursorAdapter.setSelectMode(false);
            mDelete.setVisible(false);
            isSelectMode = false;
        }
    }

    protected void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(CursorLoaderActivity.this, message, Toast.LENGTH_LONG);
        mToast.show();
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mCursorAdapter != null) {
            mCursorAdapter.swapCursor(data);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mCursorAdapter != null) {
            mCursorAdapter.swapCursor(null);
            mCursorAdapter.setOnCursorSelectedListener(null);
        }

    }

    @Override
    public void onCursorSelected(int primaryColor, int accentColor, String paletteName) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.PRIMARY_COLOR_KEY, primaryColor);
        bundle.putInt(Constants.ACCENT_COLOR_KEY, accentColor);
        bundle.putString(Constants.PALETTE_TO_UPLOAD_NAME_KEY, paletteName);
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSelectModeEnabled() {
        if (mDelete != null) {
            mDelete.setVisible(true);
        }
        mIdList = new ArrayList<>();
    }

    @Override
    public void onItemSelectedForDeletion(boolean isSelected, long id) {
        Log.d(MainViewFragment.LOG_TAG, "onItemSelectedForDeletion id is " + id);
        if (isSelected) {
            mIdList.add(id);
        } else {
            mIdList.remove(id);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCursorAdapter != null && mCursorAdapter.isSelectMode()) {
            mCursorAdapter.setSelectMode(false);
            mDelete.setVisible(false);
            mIdList = null;
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mCursorAdapter != null) {
            outState.putIntegerArrayList(Constants.LIST_KEY, mCursorAdapter.getSelectedPositionList());
            outState.putBoolean(Constants.SELECT_MODE_KEY, mCursorAdapter.isSelectMode());
        }
        super.onSaveInstanceState(outState);
    }
}
