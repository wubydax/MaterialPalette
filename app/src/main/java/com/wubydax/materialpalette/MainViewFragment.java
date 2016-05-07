package com.wubydax.materialpalette;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.wubydax.materialpalette.utils.Constants;
import com.wubydax.materialpalette.utils.Utils;


@SuppressWarnings("deprecation")
public class MainViewFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = "MainViewFragment";
    private static final SharedPreferences mSharedPreferences = Constants.mSharedPreferences;
    private View mView;

    public MainViewFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.swapColors:
                int primary = mSharedPreferences.getInt(Constants.PRIMARY_COLOR_KEY, getResources().getColor(R.color.colorPrimary));
                int accent = mSharedPreferences.getInt(Constants.ACCENT_COLOR_KEY, getResources().getColor(R.color.colorAccent));
                mSharedPreferences.edit().putInt(Constants.ACCENT_COLOR_KEY, primary).apply();
                mSharedPreferences.edit().putInt(Constants.PRIMARY_COLOR_KEY, accent).apply();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_main_view, container, false);
        Utils.invalidateViews(Constants.PRIMARY_COLOR_KEY, mSharedPreferences.getInt(Constants.PRIMARY_COLOR_KEY, getResources().getColor(R.color.colorPrimary)), mView, true);
        Utils.invalidateViews(Constants.ACCENT_COLOR_KEY, mSharedPreferences.getInt(Constants.ACCENT_COLOR_KEY, getResources().getColor(R.color.colorAccent)), mView, true);
        return mView;
    }



    @Override
    public void onResume() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case Constants.IMAGE_ACTIVITY_REQUEST_RESULT_CODE:
               invalidateViews(data);
                break;
            case Constants.CURSOR_ACTIVITY_REQUEST_CODE:
                invalidateViews(data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void invalidateViews(Intent data) {
        Bundle dataBundle = data.getExtras();
        Utils.invalidateViews(Constants.PRIMARY_COLOR_KEY, dataBundle.getInt(Constants.PRIMARY_COLOR_KEY), mView, true);
        Utils.invalidateViews(Constants.ACCENT_COLOR_KEY, dataBundle.getInt(Constants.ACCENT_COLOR_KEY), mView, true);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.ACCENT_COLOR_KEY) || key.equals(Constants.PRIMARY_COLOR_KEY)) {
            int color = sharedPreferences.getInt(key, Color.RED);
            Utils.invalidateViews(key, color, mView, true);
        }

    }

    public Bitmap getScreenShot(){
        return Utils.createScreenshot(mView.findViewById(R.id.screenshotContainer));
    }

}
