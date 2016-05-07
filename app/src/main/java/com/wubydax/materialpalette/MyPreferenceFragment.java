package com.wubydax.materialpalette;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.wubydax.materialpalette.utils.Constants;
import com.wubydax.materialpalette.utils.DriveIntentService;
import com.wubydax.materialpalette.utils.Utils;

/**
 * A simple {@link Fragment} subclass.
 */
public class MyPreferenceFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{


    private static final String LOG_TAG = "PrefFragment";
    private ProgressDialog mProgressDialog;
    private GoogleApiClient mGoogleApiClient;
    private boolean isFirstConnect;

    public MyPreferenceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        findPreference("rate_us").setOnPreferenceClickListener(this);
        findPreference("mail_us").setOnPreferenceClickListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Constants.DRIVE_KEY)) {
            ((SwitchPreference) findPreference(key)).setChecked(Utils.isDriveEnabled());
            if(Utils.isDriveEnabled()) {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    return;
                }

                mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addApi(Drive.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addScope(Drive.SCOPE_FILE)
                        .build();
                connectClient();
            }
        }

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof PreferenceScreen) {
            switch (preference.getKey()) {
                case "rate_us":
                    Uri uri = Uri.parse(getActivity().getString(R.string.market_intent_schema) + getActivity().getPackageName());
                    Intent playIntent = new Intent(Intent.ACTION_VIEW, uri);
                    if (playIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(playIntent);
                    } else {
                        Toast.makeText(getActivity(), R.string.no_app_installed, Toast.LENGTH_LONG).show();
                    }

                    break;
                case "mail_us":
                    Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
                    mailIntent.setData(Uri.parse(getActivity().getString(R.string.mail_intent_uri_schema)));
                    mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getActivity().getString(R.string.dev_mail)});
                    mailIntent.putExtra(Intent.EXTRA_SUBJECT, getActivity().getString(R.string.mail_subject));
                    if (mailIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(mailIntent);
                    } else {
                        Toast.makeText(getActivity(), R.string.no_app_installed, Toast.LENGTH_LONG).show();

                    }
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("PrefFragment", "onActivityResult in pref fragment is called");
        if(resultCode != Activity.RESULT_OK) {
            Log.d(LOG_TAG, "onActivityResult result is not ok");
            return;
        }

        switch (requestCode) {
            case Constants.RESOLVE_CONNECTION_REQUEST_CODE:
                Log.d(LOG_TAG, "onActivityResult api connection result triggered");
                connectClient();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @SuppressWarnings("WrongConstant")
    private void connectClient() {
        getActivity().setRequestedOrientation(Utils.getCurrentOrientation());
        mProgressDialog = ProgressDialog.show(getActivity(), getString(R.string.connecting_to_client_progress_dialog_title), getString(R.string.please_wait_progress_dialog_message), true, false);
        mGoogleApiClient.connect();
    }

    private void disconnectClient() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
            dismissProgressDialog();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        dismissProgressDialog();
        if(!Utils.isDriveEnabled()) {
            getPreferenceManager().getSharedPreferences().edit().putBoolean(Constants.DRIVE_KEY, true).apply();
        }
        if(isFirstConnect) {
            //necessary because of drive api bug which returns empty query result after creating new google account
            disconnectClient();
            isFirstConnect = false;
            connectClient();
        } else {
            Intent startIntentService = new Intent(getActivity(), DriveIntentService.class);
            Bundle extras = new Bundle();
            extras.putString(Constants.PALETTE_TO_UPLOAD_NAME_KEY, null);
            extras.putBoolean(Constants.SERVICE_BUNDLE_KEY_IS_VERIFYING, true);
            startIntentService.putExtras(extras);
            startIntentService.setAction(DriveIntentService.ACTION_DRIVE);
            getActivity().startService(startIntentService);
            disconnectClient();
            restoreSensorOrientation();
        }


    }

    private void restoreSensorOrientation() {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }


    @Override
    public void onConnectionSuspended(int i) {
        dismissProgressDialog();
        restoreSensorOrientation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        dismissProgressDialog();
        getPreferenceManager().getSharedPreferences().edit().putBoolean(Constants.DRIVE_KEY, false).apply();

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(getActivity(), Constants.RESOLVE_CONNECTION_REQUEST_CODE);
                isFirstConnect = true;
            } catch (IntentSender.SendIntentException e) {
                restoreSensorOrientation();
                e.printStackTrace();
            }
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), connectionResult.getErrorCode(), 0).show();
            restoreSensorOrientation();
        }


    }
}
