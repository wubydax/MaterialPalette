package com.wubydax.materialpalette;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.wubydax.materialpalette.data.PaletteContract;
import com.wubydax.materialpalette.utils.Constants;
import com.wubydax.materialpalette.utils.DriveIntentService;
import com.wubydax.materialpalette.utils.MyDialogFragment;
import com.wubydax.materialpalette.utils.Utils;
import com.wubydax.materialpalette.views.CircleView;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ColorPickerDialog.OnColorChangedListener,
        MyDialogFragment.OnDialogResultCallbackListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {


    public static final String WIDGET_ACTION = "com.wubydax.load.palette.WIDGET";
    private static final String LOG_TAG = "Main Activity";
    private static final SharedPreferences mSharedPreferences = Constants.mSharedPreferences;
    private Uri mPhotoUri, sendActionUri;
    private GoogleApiClient mGoogleApiClient;
    private Toast mToast;
    private boolean isTwoPane;
    private MenuItem mLoadFromDrive, mBackupToDrive, mCameraImage;
    private boolean isCameraRequest = false, isPaletteForBackupRequest, isLoadFromDrive = false, isFirstConnect;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);
        Menu navigationMenu = navigationView.getMenu();
        mLoadFromDrive = navigationMenu.findItem(R.id.loadFromDrive);
        mBackupToDrive = navigationMenu.findItem(R.id.backupToDrive);
        mCameraImage = navigationMenu.findItem(R.id.captureImage);
        setDriveMenuVisibility(mSharedPreferences.getBoolean(Constants.DRIVE_KEY, false));
        RelativeLayout mainContentLayout = (RelativeLayout) findViewById(R.id.contentMain);
        if (mainContentLayout != null) {
            if (((ViewGroup.MarginLayoutParams) mainContentLayout.getLayoutParams()).getMarginStart() > 100) {
                assert drawer != null;
                drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, navigationView);
                drawer.setScrimColor(Color.TRANSPARENT);
                drawer.setDrawerElevation(0);
                isTwoPane = true;
            }
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        assert drawer != null;
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (mSharedPreferences.getBoolean(Constants.FIRST_LAUNCH_KEY, true)) {
            displayDialog(Constants.ENABLE_DRIVE_DIALOG_REQUEST);
            onColorChanged(getResources().getColor(R.color.colorPrimary), Constants.PRIMARY_COLOR_KEY);
            onColorChanged(getResources().getColor(R.color.colorAccent), Constants.ACCENT_COLOR_KEY);

            mSharedPreferences.edit().putBoolean(Constants.FIRST_LAUNCH_KEY, false).apply();
        } else if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
            sendActionUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if (mSharedPreferences.getBoolean(Constants.NO_WARNING_SELECT_ACTIVITY_KEY, false)) {
                Class activityToOpen = mSharedPreferences.getString(Constants.IMAGE_ACTIVITY_KEY, "1").equals("1") ? ImageDirectSelectActivity.class : ImagePaletteActivity.class;
                Bundle extras = new Bundle();
                extras.putParcelable(Constants.URI_BUNDLE_ID_KEY, sendActionUri);
                extras.putBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY, false);
                Intent openActivity = new Intent(this, activityToOpen);
                openActivity.putExtras(extras);
                startActivityForResult(openActivity, Constants.IMAGE_ACTIVITY_REQUEST_RESULT_CODE);
            } else {
                displayDialog(Constants.INTENT_SEND_ACTION_DIALOG_REQUEST);
            }
        } else if (getIntent().getAction().equals(WIDGET_ACTION)) {
            onColorChanged(getIntent().getExtras().getInt(Constants.PRIMARY_COLOR_KEY), Constants.PRIMARY_COLOR_KEY);
            onColorChanged(getIntent().getExtras().getInt(Constants.ACCENT_COLOR_KEY), Constants.ACCENT_COLOR_KEY);
            MainViewFragment mainViewFragment = (MainViewFragment) getFragmentManager().findFragmentById(R.id.mainViewFragment);
            mainViewFragment.invalidateViews(getIntent());
        }

    }


    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void connectClient() {
        fixOrientationForProcess();
        mProgressDialog = ProgressDialog.show(this, getString(R.string.connecting_to_client_progress_dialog_title), getString(R.string.please_wait_progress_dialog_message), true, false);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = getApiClient();
        }
        mGoogleApiClient.connect();
    }

    private void disconnectClient() {
        dismissProgressDialog();
        restoreSensorOrientation();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    protected void onResume() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setDriveMenuVisibility(mSharedPreferences.getBoolean(Constants.DRIVE_KEY, false));
        super.onResume();
    }

    @Override
    protected void onPause() {
        dismissProgressDialog();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        disconnectClient();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START) && !isTwoPane) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    private void setDriveMenuVisibility(boolean isDriveEnabled) {
        if (mBackupToDrive != null && mLoadFromDrive != null) {
            mBackupToDrive.setVisible(isDriveEnabled);
            mLoadFromDrive.setVisible(isDriveEnabled);
        }
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && !isTwoPane) {
            drawer.closeDrawer(GravityCompat.START);
        }
        int id = item.getItemId();
        switch (id) {
            case R.id.resetPalette:
                onColorChanged(getResources().getColor(R.color.colorPrimary), Constants.PRIMARY_COLOR_KEY);
                onColorChanged(getResources().getColor(R.color.colorAccent), Constants.ACCENT_COLOR_KEY);
                break;
            case R.id.loadFromImage:
                if (mSharedPreferences.getBoolean(Constants.NO_WARNING_SELECT_ACTIVITY_KEY, false)) {
                    handlePaletteFromImage();
                } else {
                    displayDialog(Constants.LOAD_FROM_IMAGE_SELECT_DIALOG_REQUEST);
                }
                break;
            case R.id.loadFromDrive:
                isLoadFromDrive = true;
                if (mGoogleApiClient == null) {
                    mGoogleApiClient = getApiClient();
                }
                if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
                    connectClient();
                } else {
                    loadFileFromDrive();
                }
                break;
            case R.id.savedPalettes:
                Intent openActivityIntent = new Intent(MainActivity.this, CursorLoaderActivity.class);
                startActivityForResult(openActivityIntent, Constants.CURSOR_ACTIVITY_REQUEST_CODE);
                break;
            case R.id.backupToDrive:
                displayDialog(Constants.BACKUP_TO_DRIVE_DIALOG_REQUEST);
                break;
            case R.id.captureImage:
                isCameraRequest = true;
                if (mSharedPreferences.getBoolean(Constants.NO_WARNING_SELECT_ACTIVITY_KEY, false)) {
                    handlePaletteFromImage();
                } else {
                    displayDialog(Constants.LOAD_FROM_IMAGE_SELECT_DIALOG_REQUEST);
                }
                break;
            case R.id.action_send:
                try {
                    File xmlFile = new File(getExternalFilesDir(null) + File.separator + "colors.xml");
                    if (xmlFile.exists() && !xmlFile.delete()) {
                        Log.d(LOG_TAG, "onNavigationItemSelected problem deleting xml file");
                        return false;
                    }
                    int primaryColor = mSharedPreferences.getInt(Constants.PRIMARY_COLOR_KEY, getResources().getColor(R.color.colorPrimary));
                    int primaryDarkColor = Utils.getDarkColor(primaryColor);
                    int accentColor = mSharedPreferences.getInt(Constants.ACCENT_COLOR_KEY, getResources().getColor(R.color.colorAccent));
                    String fileContent = Utils.buildXmlFile(primaryColor, primaryDarkColor, accentColor);
                    FileOutputStream fileOutputStream = new FileOutputStream(xmlFile);
                    fileOutputStream.write(fileContent.getBytes());
                    fileOutputStream.close();
                    shareFile("application/xml", xmlFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.action_share:
                File pngFile = new File(getExternalFilesDir(null) + File.separator + "preview.png");
                if (pngFile.exists() && !pngFile.delete()) {
                    Log.d(LOG_TAG, "onNavigationItemSelected problem deleting preview file");
                    return false;
                }
                MainViewFragment mainViewFragment = (MainViewFragment) getFragmentManager().findFragmentById(R.id.mainViewFragment);
                Bitmap preview = mainViewFragment.getScreenShot();
                try {
                    FileOutputStream previewOutputStream = new FileOutputStream(pngFile);
                    ByteArrayOutputStream previewByteArrayOutputStream = new ByteArrayOutputStream();
                    preview.compress(Bitmap.CompressFormat.PNG, 100, previewOutputStream);
                    previewOutputStream.write(previewByteArrayOutputStream.toByteArray());
                    previewByteArrayOutputStream.close();
                    previewOutputStream.close();
                    shareFile("image/*", pngFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case R.id.action_settings:
                Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(openSettingsIntent);
                break;
            case R.id.action_send_invites:
                Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invite_title))
                        .setMessage(getString(R.string.invite_message))
                        .build();
                startActivityForResult(intent, 47);
                break;

        }

        return true;
    }

    private void shareFile(String mimeType, File file) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.setType(mimeType);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.intent_chooser_share_file)));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            if (isCameraRequest) {
                isCameraRequest = false;
            }
            if (requestCode == Constants.DRIVE_REQUEST_FILE_CODE) {
                restoreSensorOrientation();
            }
            return;
        }

        switch (requestCode) {
            case Constants.IMAGE_REQUEST_CODE:

                Class activityToOpen = (mSharedPreferences.getString(Constants.IMAGE_ACTIVITY_KEY, "1")).equals("1") ? ImageDirectSelectActivity.class : ImagePaletteActivity.class;
                Bundle bundle = new Bundle();
                Uri imageUri = data != null && data.getData() != null ? data.getData() : mPhotoUri;
                bundle.putParcelable(Constants.URI_BUNDLE_ID_KEY, imageUri);
                bundle.putBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY, isCameraRequest);
                Intent openActivityIntent = new Intent(MainActivity.this, activityToOpen);
                openActivityIntent.putExtras(bundle);
                startActivityForResult(openActivityIntent, Constants.IMAGE_ACTIVITY_REQUEST_RESULT_CODE);
                isCameraRequest = false;
                break;
            case Constants.IMAGE_ACTIVITY_REQUEST_RESULT_CODE:
                updateFragmentViews(requestCode, resultCode, data);
                break;
            case Constants.CURSOR_ACTIVITY_REQUEST_CODE:
                if (isPaletteForBackupRequest) {
                    String paletteToUpload = data.getExtras().getString(Constants.PALETTE_TO_UPLOAD_NAME_KEY);
                    requestAsyncLoader(paletteToUpload, false);
                    isPaletteForBackupRequest = false;
                    return;
                }
                updateFragmentViews(requestCode, resultCode, data);
                break;
            case Constants.RESOLVE_CONNECTION_REQUEST_CODE:
                isFirstConnect = true;
                connectClient();
                break;
            case Constants.PALETTE_TO_UPLOAD_REQUEST_CODE:
                break;
            case Constants.DRIVE_REQUEST_FILE_CODE:
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    isLoadFromDrive = false;

                    DriveId driveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    DriveFile driveFile = driveId.asDriveFile();
                    final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.fetching_palette_progress_dialog_title), getString(R.string.please_wait_progress_dialog_message), true, true);
                    driveFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                            .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                                @Override
                                public void onResult(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
                                    progressDialog.dismiss();
                                    InputStream inputStream = driveContentsResult.getDriveContents().getInputStream();
                                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                                    String primaryColor = "#000000";
                                    String accentColor = "#000000";
                                    try {
                                        String line;
                                        while ((line = bufferedReader.readLine()) != null) {

                                            if (line.contains("colorPrimary") && !line.contains("Dark")) {
                                                primaryColor = getColorSubstring(line);
                                            } else if (line.contains("colorAccent")) {
                                                accentColor = getColorSubstring(line);
                                            }

                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        try {
                                            inputStream.close();
                                            bufferedReader.close();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        onColorChanged(Color.parseColor(primaryColor), Constants.PRIMARY_COLOR_KEY);
                                        onColorChanged(Color.parseColor(accentColor), Constants.ACCENT_COLOR_KEY);
                                        driveContentsResult.getDriveContents().discard(mGoogleApiClient);
                                        restoreSensorOrientation();
                                    }
                                }
                            });
                } else {
                    isLoadFromDrive = true;
                    connectClient();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);

        }
    }

    private String getColorSubstring(String line) {
        return line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
    }

    private void updateFragmentViews(int requestCode, int resultCode, Intent data) {
        MainViewFragment mainViewFragment = (MainViewFragment) getFragmentManager().findFragmentById(R.id.mainViewFragment);
        mainViewFragment.onActivityResult(requestCode, resultCode, data);
        onColorChanged(data.getExtras().getInt(Constants.PRIMARY_COLOR_KEY), Constants.PRIMARY_COLOR_KEY);
        onColorChanged(data.getExtras().getInt(Constants.ACCENT_COLOR_KEY), Constants.ACCENT_COLOR_KEY);
    }

    public void onClick(View view) {
        int color = Color.BLACK;

        if (view instanceof CircleView) {
            color = ((CircleView) view).getFillColor();
        }
        int id = view.getId();
        switch (id) {
            case R.id.accentColorPickerContainer:
                showColorPickerDialog(Constants.ACCENT_COLOR_KEY, color);
                break;
            case R.id.primaryColorPickerContainer:
                showColorPickerDialog(Constants.PRIMARY_COLOR_KEY, color);
                break;
            case R.id.saveFab:
                displayDialog(Constants.SAVE_NAME_DIALOG_REQUEST);
                break;

        }


    }

    private void showColorPickerDialog(String key, int color) {
        int currentColor = mSharedPreferences.getInt(key, color);
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, currentColor, key);
        colorPickerDialog.setOnColorChangedListener(this);
        colorPickerDialog.show();

    }

    @Override
    public void onColorChanged(int color, String key) {
        mSharedPreferences.edit().putInt(key, color).apply();
        if (key.equals(Constants.PRIMARY_COLOR_KEY) && Utils.isBrightColor(color) && mSharedPreferences.getBoolean(Constants.NO_WARNING_TOO_BRIGHT_KEY, true)) {
            displayDialog(Constants.COLOR_TOO_BRIGHT_DIALOG_REQUEST);
        }
    }


    private void displayDialog(int requestCode) {
        MyDialogFragment myDialogFragment = MyDialogFragment.newInstance(requestCode);
        getFragmentManager().beginTransaction().add(myDialogFragment, Constants.DIALOG_FRAGMENT_TAG).commitAllowingStateLoss();
    }

    private void getContent() {
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("image/*");
        startActivityForResult(getContentIntent, Constants.IMAGE_REQUEST_CODE);
    }

    @Override
    public void onDialogResult(int requestCode, String paletteName, boolean isCanceled) {
        if (isCanceled) {
            isCameraRequest = false;
            return;
        }
        switch (requestCode) {
            case Constants.LOAD_FROM_IMAGE_SELECT_DIALOG_REQUEST:
                handlePaletteFromImage();
                break;
            case Constants.INTENT_SEND_ACTION_DIALOG_REQUEST:
                Class activityToOpen = mSharedPreferences.getString(Constants.IMAGE_ACTIVITY_KEY, "1").equals("1") ? ImageDirectSelectActivity.class : ImagePaletteActivity.class;
                Bundle extras = new Bundle();
                extras.putParcelable(Constants.URI_BUNDLE_ID_KEY, sendActionUri);
                extras.putBoolean(Constants.BUNDLE_IS_CAPTURE_BOOLEAN_KEY, isCameraRequest);
                Intent openActivity = new Intent(this, activityToOpen);
                openActivity.putExtras(extras);
                startActivityForResult(openActivity, Constants.IMAGE_ACTIVITY_REQUEST_RESULT_CODE);
                break;

            case Constants.SAVE_NAME_DIALOG_REQUEST:
                insertIntoDb(paletteName);
                break;
            case Constants.BACKUP_TO_DRIVE_DIALOG_REQUEST:
                isPaletteForBackupRequest = mSharedPreferences.getInt(Constants.DRIVE_BACKUP_OPTION_PREF_KEY, 0) == 0;
                if (isPaletteForBackupRequest) {
                    Intent openActivityIntent = new Intent(MainActivity.this, CursorLoaderActivity.class);
                    startActivityForResult(openActivityIntent, Constants.CURSOR_ACTIVITY_REQUEST_CODE);
                } else {
                    requestAsyncLoader(null, false);
                }
                break;

        }

    }

    private void handlePaletteFromImage() {
        if (!isCameraRequest) {
            getContent();
        } else {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 0);
                } else {
                    openCameraForResult();
                }
            } else {
                openCameraForResult();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCameraForResult();
        } else {
            mCameraImage.setVisible(false);
            isCameraRequest = false;
        }
    }

    private void openCameraForResult() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "my_image" + DateFormat.format("yyyy-MM-dd_kk.mm.ss", System.currentTimeMillis()).toString() + ".jpeg");
        mPhotoUri = Uri.fromFile(file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
        startActivityForResult(intent, Constants.IMAGE_REQUEST_CODE);
    }

    protected void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
        mToast.show();
    }

    private void insertIntoDb(String paletteName) {

        MainViewFragment mainViewFragment = (MainViewFragment) getFragmentManager().findFragmentById(R.id.mainViewFragment);
        ContentValues contentValues = new ContentValues();
        contentValues.put(PaletteContract.NAME_COLUMN, paletteName);
        contentValues.put(PaletteContract.PRIMARY_COLOR_COLUMN, mSharedPreferences.getInt(Constants.PRIMARY_COLOR_KEY, Color.RED));
        contentValues.put(PaletteContract.ACCENT_COLOR_COLUMN, mSharedPreferences.getInt(Constants.ACCENT_COLOR_KEY, Color.BLACK));
        contentValues.put(PaletteContract.PREVIEW_IMAGE_COLUMN, Utils.getBlob(mainViewFragment.getScreenShot()));
        Uri uri = getContentResolver().insert(PaletteContract.CONTENT_URI, contentValues);
        if (uri != null) {
            showToast(String.format(Locale.getDefault(), getString(R.string.added_to_db_toast), paletteName));
            Utils.updateWidget();
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.DRIVE_KEY)) {
            boolean isDriveEnabled = sharedPreferences.getBoolean(key, false);
            setDriveMenuVisibility(isDriveEnabled);
            if (isDriveEnabled) {
                GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
                int requestResult = googleApiAvailability.isGooglePlayServicesAvailable(this);
                if (requestResult != ConnectionResult.SUCCESS) {
                    googleApiAvailability.getErrorDialog(this, requestResult, Constants.RESOLVE_CONNECTION_REQUEST_CODE).show();
                    sharedPreferences.edit().putBoolean(key, false).apply();
                    return;
                }
                if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    return;
                }
                mGoogleApiClient = getApiClient();
                connectClient();
            }

        }
    }

    private GoogleApiClient getApiClient() {
        return new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }


    private void requestAsyncLoader(String paletteName, boolean isVerifying) {
        Intent startIntentService = new Intent(this, DriveIntentService.class);
        Bundle extras = new Bundle();
        extras.putString(Constants.PALETTE_TO_UPLOAD_NAME_KEY, paletteName);
        extras.putBoolean(Constants.SERVICE_BUNDLE_KEY_IS_VERIFYING, isVerifying);
        startIntentService.putExtras(extras);
        startIntentService.setAction(DriveIntentService.ACTION_DRIVE);
        startService(startIntentService);
        disconnectClient();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        showToast(getString(R.string.client_connected_toast));
        if(!Utils.isDriveEnabled()) {
            mSharedPreferences.edit().putBoolean(Constants.DRIVE_KEY, true).apply();
        }
        dismissProgressDialog();
        if (isFirstConnect) {
            //necessary because of drive api bug which returns empty query result after creating new google account
            disconnectClient();
            isFirstConnect = false;
            connectClient();
        } else {
            if (!isLoadFromDrive) {
                restoreSensorOrientation();
                requestAsyncLoader(null, true);
            } else {
                loadFileFromDrive();
            }
        }

    }

    private void loadFileFromDrive() {
        String mainFolderIdString = mSharedPreferences.getString(Constants.MAIN_FOLDER_ID_KEY, null);
        if (mainFolderIdString != null) {
            DriveId mainFolderId = DriveId.decodeFromString(mainFolderIdString);
            IntentSender intentSender = Drive.DriveApi.newOpenFileActivityBuilder()
                    .setSelectionFilter(Filters.eq(SearchableField.MIME_TYPE, "application/xml"))
                    .setSelectionFilter(Filters.eq(Constants.CUSTOM_PROPERTY_KEY_XML, Constants.XML_METADATA_INFO))
                    .setActivityStartFolder(mainFolderId)
                    .build(mGoogleApiClient);
            try {
                startIntentSenderForResult(intentSender, Constants.DRIVE_REQUEST_FILE_CODE, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        dismissProgressDialog();
        restoreSensorOrientation();
        showToast("connection suspended");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        dismissProgressDialog();

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, Constants.RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                restoreSensorOrientation();
                showToast("Failed to connect to Drive");
            }
        } else {
            restoreSensorOrientation();
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
        }

    }

    @SuppressWarnings("WrongConstant")
    private void fixOrientationForProcess() {
        setRequestedOrientation(Utils.getCurrentOrientation());
    }

    private void restoreSensorOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }


}
