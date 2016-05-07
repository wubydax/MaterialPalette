package com.wubydax.materialpalette;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.wubydax.materialpalette.utils.Constants;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.RESOLVE_CONNECTION_REQUEST_CODE:
                MyPreferenceFragment fragment = (MyPreferenceFragment) getFragmentManager().findFragmentById(R.id.preferenceFragment);
                fragment.onActivityResult(requestCode, resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }
}
