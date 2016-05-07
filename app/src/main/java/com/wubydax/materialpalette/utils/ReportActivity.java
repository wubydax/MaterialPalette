package com.wubydax.materialpalette.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;

import com.wubydax.materialpalette.R;

public class ReportActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme))
                .setTitle(R.string.drive_upload_report_dialog_title)
                .setMessage(Constants.mSharedPreferences.getString(Constants.UPLOAD_RESULT_KEY, getString(R.string.no_info_available_upload_report)))
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            finish();
                        }
                        return false;
                    }
                })
                .create()
                .show();
    }
}
