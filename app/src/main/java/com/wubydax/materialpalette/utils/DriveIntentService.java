package com.wubydax.materialpalette.utils;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.wubydax.materialpalette.R;
import com.wubydax.materialpalette.data.PaletteContract;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class DriveIntentService extends IntentService {
    public static final String ACTION_DRIVE = "com.wubydax.materialpalette.ACTION_DRIVE";
    private static final String LOG_TAG = "intent_service";
    private boolean isBatchUpload;
    private boolean isResultOk;
    private GoogleApiClient mGoogleApiClient;
    private Notification.Builder mBuilder;
    private String[] mBackedUpPalettes;
    private String mUploadResult;
    private NotificationManager mNotificationManager;
    private String mPaletteName;
    private StringBuilder mStringBuilderReport;
    private boolean isCanceled = false;
    private Handler mHandler;


    public DriveIntentService() {
        super("material_palette_service_working_thread");


    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && intent.getAction().equals(ACTION_DRIVE)) {
            mHandler = new Handler(getMainLooper());
            mStringBuilderReport = new StringBuilder();
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mBuilder = getNotificationBuilder(true, "");
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .build();
            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                return;
            }
            LocalBroadcastReceiver localBroadcastReceiver = new LocalBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(LocalBroadcastReceiver.ACTION_TASK_CANCEL);
            intentFilter.addAction(LocalBroadcastReceiver.ACTION_TASK_COMPLETE);
            registerReceiver(localBroadcastReceiver, intentFilter);
            Bundle bundle = intent.getExtras();
            mPaletteName = bundle.getString(Constants.PALETTE_TO_UPLOAD_NAME_KEY);
            boolean isVerifying = bundle.getBoolean(Constants.SERVICE_BUNDLE_KEY_IS_VERIFYING);
            if(isVerifying) {
                Drive.DriveApi.requestSync(mGoogleApiClient).await();
            }
            isBatchUpload = mPaletteName == null;

            if (isVerified()) {
                if (!isVerifying) {
                    mHandler.post(new DisplayToast(this, getString(R.string.backup_began_toast), Toast.LENGTH_SHORT));
                    mBackedUpPalettes = getBackedUpPalettes();
                    Cursor cursor = mBackedUpPalettes != null ? getCursor() : null;
                    if (cursor != null && !cursor.isClosed()) {
                        if (cursor.getCount() == 0) {
                            mUploadResult = getString(R.string.nothing_to_upload_up_to_date_toast);
                            publishProgressUpdate(LocalBroadcastReceiver.ACTION_TASK_COMPLETE, false);
                        } else {
                            backupPalettes(cursor);
                        }
                    }


                }

            }
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
            Constants.mSharedPreferences.edit().putString(Constants.UPLOAD_RESULT_KEY, mStringBuilderReport.toString()).commit();
        try {
            unregisterReceiver(localBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(LOG_TAG, "onHandleIntent receiver already unregistered");
        }
        }

    }

    private boolean isVerified() {
        return isMainFolder() || isResultOk && createMainFolder();
    }


    private Notification.Builder getNotificationBuilder(boolean isAction, String contentText) {
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.backup_notification_content_title))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_noti);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(this.getResources().getColor(R.color.colorPrimary));
        }
        if (isAction) {
            PendingIntent pendingIntentAction = PendingIntent.getBroadcast(this, 0, new Intent(LocalBroadcastReceiver.ACTION_TASK_CANCEL), 0);
            builder.addAction(R.drawable.ic_cancel, getString(android.R.string.cancel), pendingIntentAction);
        }
        return builder;
    }

    private void updateNotification(int progress, int max, String contentText, boolean isIndeterminate) {
        if (progress < max) {
            mBuilder.setProgress(max, progress, isIndeterminate);
        } else {
            mBuilder.setProgress(0, 0, isIndeterminate);
        }
        if (progress == 0 && max == 0 && !isIndeterminate) {
            mBuilder = getNotificationBuilder(false, getString(R.string.backup_notification_complete));
            mBuilder.setOngoing(false);
            Intent intent = new Intent(this, ReportActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
            mBuilder.setContentIntent(pi);
            mBuilder.setAutoCancel(true);
            mNotificationManager.cancelAll();
            mNotificationManager.notify(Constants.NOTIFICATION_ID, mBuilder.build());

        } else {
            mBuilder.setContentText(contentText);
            mBuilder.setOngoing(true);
            mNotificationManager.notify(Constants.NOTIFICATION_ID, mBuilder.build());
        }

    }


    @SuppressLint("CommitPrefEdits")
    private boolean isMainFolder() {
        boolean isFolder = false;
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, Constants.MAIN_FOLDER_NAME))
                .addFilter(Filters.eq(Constants.CUSTOM_PROPERTY_KEY_MAIN_FOLDER, Constants.MAIN_FOLDER_METADATA_INFO))
                .addFilter(Filters.eq(SearchableField.TRASHED, false))
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE))
                .build();
        DriveApi.MetadataBufferResult metadataBufferResult = Drive.DriveApi.query(mGoogleApiClient, query).await();
        if (!metadataBufferResult.getStatus().isSuccess()) {
            isResultOk = false;

        } else {
            int count = metadataBufferResult.getMetadataBuffer().getCount();
            isResultOk = true;
            Log.d(LOG_TAG, "isMainFolder count is " + count);
            if (count > 0) {
                for (Metadata metadata : metadataBufferResult.getMetadataBuffer()) {
                    DriveId driveId = metadata.getDriveId();
                    String prefString = Constants.mSharedPreferences.getString(Constants.MAIN_FOLDER_ID_KEY, null);
                    if (prefString != null) {
                        DriveId savedDriveId = DriveId.decodeFromString(prefString);
                        Log.d(LOG_TAG, "onResult current drive id is " + driveId + " and saved drive id is " + savedDriveId);
                        if (!driveId.equals(savedDriveId)) {

                            Constants.mSharedPreferences.edit().putString(Constants.MAIN_FOLDER_ID_KEY, driveId.encodeToString()).commit();
                            Log.d(LOG_TAG, "onResult folder exists, preferences updated");
                        }

                    } else {
                        Constants.mSharedPreferences.edit().putString(Constants.MAIN_FOLDER_ID_KEY, driveId.encodeToString()).commit();
                    }
                }
                metadataBufferResult.getMetadataBuffer().release();
                isFolder = true;
            } else {
                Log.d(LOG_TAG, "isMainFolder returning false");
            }
        }
        return isFolder;
    }

    private boolean createMainFolder() {
        if (!isResultOk) {
            return false;
        } else {
            MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                    .setTitle(Constants.MAIN_FOLDER_NAME)
                    .setCustomProperty(Constants.CUSTOM_PROPERTY_KEY_MAIN_FOLDER, Constants.MAIN_FOLDER_METADATA_INFO)
                    .setStarred(true)
                    .build();
            DriveFolder.DriveFolderResult driveFolderResult = Drive.DriveApi.getRootFolder(mGoogleApiClient)
                    .createFolder(mGoogleApiClient, metadataChangeSet).await();

            if (!driveFolderResult.getStatus().isSuccess()) {
                isResultOk = false;
                return false;
            } else {
                DriveId driveId = driveFolderResult.getDriveFolder().getDriveId();
                Constants.mSharedPreferences.edit().putString(Constants.MAIN_FOLDER_ID_KEY, driveId.encodeToString()).commit();
                isResultOk = true;
                return true;
            }

        }
    }

    private void backupPalettes(Cursor cursor) {

        int primaryColor, primaryDarkColor, accentColor;
        String folderName;
        Bitmap bitmap;
        String xmlContent;
        int count = 0, cursorCount = cursor.getCount();
        DriveFolder.DriveFileResult xmlResult, bitmapResult;
        boolean isBitmapSuccess = false, isXmlSuccess = false;
        DriveFolder mainFolder;
        String idString = Constants.mSharedPreferences.getString(Constants.MAIN_FOLDER_ID_KEY, null);
        if (idString != null) {
            DriveId mainFolderId = DriveId.decodeFromString(Constants.mSharedPreferences.getString(Constants.MAIN_FOLDER_ID_KEY, ""));
            mainFolder = mainFolderId.asDriveFolder();
        } else {
            return;
        }
        try {
            updateNotification(0, 0, getString(R.string.backup_notification_backup_in_progress), true);
            while (cursor.moveToNext()) {
                if (isCanceled) {
                    cursor.close();
                    return;
                }
                primaryColor = cursor.getInt(PaletteContract.PRIMARY_POSITION);
                primaryDarkColor = Utils.getDarkColor(primaryColor);
                accentColor = cursor.getInt(PaletteContract.ACCENT_POSITION);
                folderName = cursor.getString(PaletteContract.NAME_POSITION);
                bitmap = Utils.getBitmap(cursor.getBlob(PaletteContract.PREVIEW_POSITION));
                xmlContent = Utils.buildXmlFile(primaryColor, primaryDarkColor, accentColor);
                Log.d(LOG_TAG, "backupPalettes xml content is " + xmlContent);

                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                        .setTitle(folderName)
                        .setCustomProperty(Constants.CUSTOM_PROPERTY_KEY_MAIN_FOLDER, Constants.MAIN_FOLDER_METADATA_INFO)
                        .build();
                DriveFolder.DriveFolderResult driveFolderResult = mainFolder.createFolder(mGoogleApiClient, metadataChangeSet).await();
                if (driveFolderResult.getStatus().isSuccess()) {

                    updateNotification(count, cursorCount, String.format(Locale.getDefault(), getString(R.string.backup_notification_uploading_palette), folderName), false);
                    DriveFolder subFolder = driveFolderResult.getDriveFolder();
                    DriveApi.DriveContentsResult driveContentResultBitmap = Drive.DriveApi.newDriveContents(mGoogleApiClient).await();
                    if (driveContentResultBitmap.getStatus().isSuccess()) {
                        OutputStream outputStream = driveContentResultBitmap.getDriveContents().getOutputStream();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                        outputStream.write(byteArrayOutputStream.toByteArray());

                        MetadataChangeSet fileMetaDataChangeSet = new MetadataChangeSet.Builder()
                                .setTitle("Preview.png")
                                .setMimeType("image/png")
                                .build();
                        bitmapResult = subFolder.createFile(mGoogleApiClient, fileMetaDataChangeSet, driveContentResultBitmap.getDriveContents()).await();
                        isBitmapSuccess = bitmapResult.getStatus().isSuccess();
                    } else {
                        mStringBuilderReport.append(String.format(Locale.getDefault(), getString(R.string.failed_to_create_preview_image), folderName)).append(driveContentResultBitmap.getStatus()).append("\n");

                    }
                    DriveApi.DriveContentsResult driveContentsResultXML = Drive.DriveApi.newDriveContents(mGoogleApiClient).await();
                    if (driveContentsResultXML.getStatus().isSuccess()) {
                        OutputStream xmlOutputStream = driveContentsResultXML.getDriveContents().getOutputStream();

                        xmlOutputStream.write(xmlContent.getBytes());

                        MetadataChangeSet xmlMetadataChangeSet = new MetadataChangeSet.Builder()
                                .setTitle("colors.xml")
                                .setMimeType("application/xml")
                                .setCustomProperty(Constants.CUSTOM_PROPERTY_KEY_XML, Constants.XML_METADATA_INFO)
                                .build();

                        xmlResult = subFolder.createFile(mGoogleApiClient, xmlMetadataChangeSet, driveContentsResultXML.getDriveContents()).await();

                        isXmlSuccess = xmlResult.getStatus().isSuccess();

                    } else {
                        mStringBuilderReport.append(String.format(Locale.getDefault(), getString(R.string.failed_to_create_xml_file), folderName)).append(driveContentsResultXML.getStatus()).append("\n");

                    }
                    if (isBitmapSuccess && isXmlSuccess) {
                        mStringBuilderReport.append(String.format(Locale.getDefault(), getString(R.string.successfully_uploaded_palette), folderName)).append("\n");
                        count++;
                    }

                } else {
                    mStringBuilderReport.append(String.format(Locale.getDefault(), getString(R.string.failed_to_create_folder), folderName)).append(driveFolderResult.getStatus()).append("\n");
                }


            }
            mUploadResult = (cursorCount == count) ? getString(R.string.all_backed_up_successfully) : getString(R.string.some_problem_with_backup);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (!isCanceled) {
                cursor.close();
                updateNotification(0, 0, mUploadResult, false);
                publishProgressUpdate(LocalBroadcastReceiver.ACTION_TASK_COMPLETE, (cursorCount-count) == 0);
            } else {
                mNotificationManager.cancelAll();
            }
        }


    }

    private void publishProgressUpdate(String action, boolean isSuccess) {
        Intent intent = new Intent(action);
        Bundle extras = new Bundle();
        extras.putBoolean(Constants.RECEIVER_BUNDLE_IS_SUCCESS_BOOLEAN, isSuccess);
        intent.putExtras(extras);
        sendBroadcast(intent);
    }

    private String[] getBackedUpPalettes() {
        String[] foldersArray = null;
        ArrayList<String> listArray = new ArrayList<>();
        Drive.DriveApi.requestSync(mGoogleApiClient).await();
        String driveIdPrefString = Constants.mSharedPreferences.getString(Constants.MAIN_FOLDER_ID_KEY, null);
        if (driveIdPrefString != null) {
            DriveFolder mainFolder = DriveId.decodeFromString(driveIdPrefString).asDriveFolder();
            DriveApi.MetadataBufferResult childrenList = mainFolder.listChildren(mGoogleApiClient).await();
            if (childrenList.getStatus().isSuccess()) {
                for (Metadata metadata : childrenList.getMetadataBuffer()) {
                    if (metadata.getMimeType().equals(DriveFolder.MIME_TYPE) && !metadata.isTrashed()) {
                        String title = metadata.getTitle();
                        if (!listArray.contains(title)) {
                            listArray.add(metadata.getTitle());
                        }
                    }
                }
                childrenList.getMetadataBuffer().release();
                foldersArray = new String[listArray.size()];
                for (int i = 0; i < listArray.size(); i++) {
                    foldersArray[i] = listArray.get(i);
                }
            } else {
                mStringBuilderReport.append(getString(R.string.failed_to_retrieve_saved_from_drive)).append(childrenList.getStatus());
            }
        }
        return foldersArray;
    }

    private Cursor getCursor() {
        Uri uri;
        boolean isBackUpOnDrive = mBackedUpPalettes.length > 0;
        String[] selectionArgs = null;
        if (!isBatchUpload) {
            if (Arrays.asList(mBackedUpPalettes).contains(mPaletteName)) {
                mStringBuilderReport.append(String.format(Locale.getDefault(), getString(R.string.palette_already_on_drive), mPaletteName)).append("\n");
                publishProgressUpdate(LocalBroadcastReceiver.ACTION_TASK_COMPLETE, false);
                return null;
            } else {
                uri = PaletteContract.getContentUriWithTitle(mPaletteName);

            }
        } else {
            uri = isBackUpOnDrive ? Uri.withAppendedPath(PaletteContract.CONTENT_URI, "exclude") : PaletteContract.CONTENT_URI;
            selectionArgs = isBackUpOnDrive ? mBackedUpPalettes : null;

        }
        return getContentResolver().query(uri, null, null, selectionArgs, null);
    }

    private final class LocalBroadcastReceiver extends BroadcastReceiver {
        public static final String ACTION_TASK_CANCEL = "com.wubydax.materialpalette.TASK_CANCEL";
        public static final String ACTION_TASK_COMPLETE = "com.wubydax.materialpalette.TASK_COMPLETE";


        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_TASK_COMPLETE:
                    Log.d(LOG_TAG, "onReceive received completed intent");
                    Bundle bundle = intent.getExtras();
                    boolean isSuccess = bundle.getBoolean(Constants.RECEIVER_BUNDLE_IS_SUCCESS_BOOLEAN);
                    String success = isSuccess ? getString(R.string.upload_successful) : getString(R.string.upload_incomplete);
                    String toast = isSuccess || isBatchUpload ? success + "\n\n" + mUploadResult : success + "\n\n" + mStringBuilderReport.toString();
                    mHandler.post(new DisplayToast(context, toast, Toast.LENGTH_LONG));
                    break;
                case ACTION_TASK_CANCEL:
                    isCanceled = true;
                    mHandler.post(new DisplayToast(context, getString(R.string.cancelling_backup_toast), Toast.LENGTH_SHORT));
                    break;

            }
            context.unregisterReceiver(this);
        }
    }

    public final class DisplayToast implements Runnable {
        private Toast mToast;
        private String mMessage;
        private Context mContext;
        private int mDuration;

        public DisplayToast(Context context, String message, int duration) {
            mMessage = message;
            mContext = context;
            mDuration = duration;
        }

        protected void showToast() {
            if (mToast != null) {
                mToast.cancel();
            }
            mToast = Toast.makeText(mContext, mMessage, mDuration);
            mToast.show();
        }

        @Override
        public void run() {
            Log.d(LOG_TAG, "run is called in runnable");
            showToast();
        }
    }


}
