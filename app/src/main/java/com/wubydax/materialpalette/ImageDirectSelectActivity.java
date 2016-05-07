package com.wubydax.materialpalette;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.wubydax.materialpalette.utils.Constants;
import com.wubydax.materialpalette.utils.Utils;

import java.io.FileNotFoundException;

@SuppressWarnings("deprecation")
public class ImageDirectSelectActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private Uri mUri;
    private String mKey;
    private int mPrimaryColor, mAccentColor;
    private int mActiveColorPosition;
    private Uri mPhotoUri;
    private boolean isCapture, isCrop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_direct_select);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);
        mKey = Constants.PRIMARY_COLOR_KEY;
        if (savedInstanceState == null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                mUri = bundle.getParcelable(Constants.URI_BUNDLE_ID_KEY);
                mPrimaryColor = getResources().getColor(R.color.colorPrimary);
                mAccentColor = getResources().getColor(R.color.colorAccent);
                isCapture = bundle.getBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY);
                mActiveColorPosition = 0;
            }
        } else {
            mUri = savedInstanceState.getParcelable(Constants.URI_BUNDLE_ID_KEY);
            mPrimaryColor = savedInstanceState.getInt(Constants.PRIMARY_COLOR_KEY);
            mAccentColor = savedInstanceState.getInt(Constants.ACCENT_COLOR_KEY);
            mActiveColorPosition = savedInstanceState.getInt(Constants.ACTIVE_COLOR_POSITION_KEY);
            isCapture = savedInstanceState.getBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY);
            isCrop = savedInstanceState.getBoolean(Constants.BUNDLE_IS_CROP_BOOLEAN_KEY);
        }

        invalidateViews(Constants.PRIMARY_COLOR_KEY, mPrimaryColor);
        invalidateViews(Constants.ACCENT_COLOR_KEY, mAccentColor);
        setUpImageView();
        setUpSpinner();

    }

    private void invalidateViews(String key, int color) {
        Utils.invalidateViews(key, color, getWindow().getDecorView().getRootView(), false);
    }

    private void setUpSpinner() {
        AppCompatSpinner spinner = (AppCompatSpinner) findViewById(R.id.colorSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.spinner_items));
        if (spinner != null) {
            spinner.setSelection(mActiveColorPosition);
            spinner.setOnItemSelectedListener(this);
            spinner.setAdapter(adapter);
        }

    }

    public void onClick(View view) {
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("image/*");
        startActivityForResult(getContentIntent, Constants.IMAGE_REQUEST_CODE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_from_image_activity_menu, menu);
        menu.findItem(R.id.cropImage).setIcon(isCrop ? R.drawable.ic_full : R.drawable.ic_crop);
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
                if (!isCapture) {
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
            case R.id.cropImage:
                isCrop = !isCrop;
                item.setIcon(isCrop ? R.drawable.ic_full : R.drawable.ic_crop);
                setUpImageView();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == Constants.IMAGE_REQUEST_CODE) {
            mUri = isCapture ? mPhotoUri : data.getData();
            setUpImageView();

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setUpImageView() {
        try {

            final Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mUri));
            ImageView view = ((ImageView) findViewById(R.id.testImageView));
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(isCrop ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            ImageView.ScaleType scaleType = isCrop ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER;
            if (view != null) {
                view.setLayoutParams(params);
                view.setScaleType(scaleType);
                view.setImageBitmap(bitmap);
                BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getDrawable();
                final Bitmap drawableBitmap = bitmapDrawable.getBitmap();
                final Matrix inverse = new Matrix();

                view.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        ((ImageView) v).getImageMatrix().invert(inverse);
                        float[] touchPoint = new float[]{event.getX(), event.getY()};
                        inverse.mapPoints(touchPoint);
                        int xPosition = (int) touchPoint[0];
                        int yPosition = (int) touchPoint[1];
                        if (xPosition < drawableBitmap.getWidth() && xPosition >= 0 && yPosition >= 0 && yPosition < drawableBitmap.getHeight()) {
                            int pixel = drawableBitmap.getPixel(xPosition, yPosition);
                            invalidateViews(mKey, pixel);
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                if (mKey.equals(Constants.PRIMARY_COLOR_KEY)) {
                                    mPrimaryColor = pixel;
                                } else if (mKey.equals(Constants.ACCENT_COLOR_KEY)) {
                                    mAccentColor = pixel;
                                }
                            }
                        }

                        return true;

                    }
                });
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(Constants.URI_BUNDLE_ID_KEY, mUri);
        outState.putInt(Constants.PRIMARY_COLOR_KEY, mPrimaryColor);
        outState.putInt(Constants.ACCENT_COLOR_KEY, mAccentColor);
        outState.putInt(Constants.ACTIVE_COLOR_POSITION_KEY, mActiveColorPosition);
        outState.putBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY, isCapture);
        outState.putBoolean(Constants.BUNDLE_IS_CROP_BOOLEAN_KEY, isCrop);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String[] spinnerValues = {Constants.PRIMARY_COLOR_KEY, Constants.ACCENT_COLOR_KEY};
        mKey = spinnerValues[position];

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
