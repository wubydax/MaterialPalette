package com.wubydax.materialpalette;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.wubydax.materialpalette.utils.Constants;
import com.wubydax.materialpalette.utils.PaletteColorsAdapter;
import com.wubydax.materialpalette.utils.Utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ImagePaletteActivity extends AppCompatActivity implements PaletteColorsAdapter.OnColorSelectedListener {
    private Uri mUri;
    private int mPrimaryColor, mAccentColor;
    private boolean isCapture;
    private Uri mPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_palette);
        setTitle(null);
        Toolbar toolbar = (Toolbar) findViewById(R.id.imagePaletteToolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                mUri = bundle.getParcelable(Constants.URI_BUNDLE_ID_KEY);
                mAccentColor = getResources().getColor(R.color.colorAccent);
                mPrimaryColor = getResources().getColor(R.color.colorPrimary);
                isCapture = bundle.getBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY);
            }
        } else {
            mUri = savedInstanceState.getParcelable(Constants.URI_BUNDLE_ID_KEY);
            mPrimaryColor = savedInstanceState.getInt(Constants.PRIMARY_COLOR_KEY);
            mAccentColor = savedInstanceState.getInt(Constants.ACCENT_COLOR_KEY);
            isCapture = savedInstanceState.getBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY);
        }
        invalidateViews(Constants.PRIMARY_COLOR_KEY, mPrimaryColor);
        invalidateViews(Constants.ACCENT_COLOR_KEY, mAccentColor);
        setUpViews();


    }

    private void invalidateViews(String key, int color) {
        Utils.invalidateViews(key, color, getWindow().getDecorView().getRootView(), false);
    }

    private void setUpViews() {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mUri));
            ImageView imagePreview = (ImageView) findViewById(R.id.loadedImagePreview);
            if(imagePreview != null){
                imagePreview.setImageBitmap(bitmap);
            }
            Palette palette = Palette.from(bitmap).generate();
            List<Integer> colorsList = new ArrayList<>();
            for (Palette.Swatch swatch : palette.getSwatches()) {
                colorsList.add(swatch.getRgb());
            }
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.swatchRecyclerView);
            StaggeredGridLayoutManager manager = new StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL);
            manager.setAutoMeasureEnabled(true);
            if (recyclerView != null) {
                recyclerView.setLayoutManager(manager);
                recyclerView.setNestedScrollingEnabled(false);
                recyclerView.setHasFixedSize(false);
                PaletteColorsAdapter adapter = new PaletteColorsAdapter(colorsList);
                adapter.setOnColorSelectedListener(this);
                recyclerView.setAdapter(adapter);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_from_image_activity_menu, menu);
        menu.findItem(R.id.cropImage).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.loadColors:
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putInt(Constants.PRIMARY_COLOR_KEY, mPrimaryColor);
                bundle.putInt(Constants.ACCENT_COLOR_KEY, mAccentColor);
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
                break;
            case R.id.addImage:
                if(!isCapture) {
                    Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    getContentIntent.setType("image/*");
                    startActivityForResult(getContentIntent, Constants.IMAGE_REQUEST_CODE);
                } else {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.Images.Media.TITLE, "my_image" + DateFormat.format("yyyy-MM-dd_kk.mm.ss", System.currentTimeMillis()).toString() + ".jpeg");
                    mPhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues);
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
                    startActivityForResult(captureIntent, Constants.IMAGE_REQUEST_CODE);
                }
                break;
            case R.id.swapColors:

                int tempColor = mPrimaryColor;
                mPrimaryColor = mAccentColor;
                mAccentColor = tempColor;
                invalidateViews(Constants.PRIMARY_COLOR_KEY, mPrimaryColor);
                invalidateViews(Constants.ACCENT_COLOR_KEY, mAccentColor);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case Constants.IMAGE_REQUEST_CODE:
                mUri = isCapture ? mPhotoUri :data.getData();
                setUpViews();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onColorSelected(int color, boolean isPrimary) {
        if(isPrimary) {
            mPrimaryColor = color;
        } else {
            mAccentColor = color;
        }
        String key = isPrimary ? Constants.PRIMARY_COLOR_KEY : Constants.ACCENT_COLOR_KEY;
        invalidateViews(key, color);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Constants.URI_BUNDLE_ID_KEY, mUri);
        outState.putInt(Constants.PRIMARY_COLOR_KEY, mPrimaryColor);
        outState.putInt(Constants.ACCENT_COLOR_KEY, mAccentColor);
        outState.putBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY, isCapture);
    }
}
