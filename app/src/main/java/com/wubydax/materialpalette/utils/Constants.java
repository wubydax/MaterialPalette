package com.wubydax.materialpalette.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.wubydax.materialpalette.MyApplication;
import com.wubydax.materialpalette.R;

/**
 * Created by Anna Berkovitch on 29/03/2016.
 * Constant fields for general use
 */
public class Constants {

    public static final Context mContext = MyApplication.getContext();
    public static final SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);


    /*String keys*/
    public static final String ACCENT_COLOR_KEY = mContext.getString(R.string.accent_color_key);
    public static final String ACTIVE_COLOR_POSITION_KEY = mContext.getString(R.string.active_color_key);
    public static final String BUNDLE_IS_CAPTURE_BOOLEAN_KEY = mContext.getString(R.string.bundle_is_capture_key);
    public static final String BUNDLE_IS_CROP_BOOLEAN_KEY = mContext.getString(R.string.is_crop_bundle_key);
    public static final String CUSTOM_PROPERTY_COLORS_XML = mContext.getString(R.string.drive_xml_custom_property);
    public static final String CUSTOM_PROPERTY_MAIN_FOLDER = mContext.getString(R.string.main_folder_custom_property);
    public static final String DIALOG_FRAGMENT_TAG = mContext.getString(R.string.dialog_fragment_tag);
    public static final String DIALOG_REQUEST_CODE_KEY = mContext.getString(R.string.request_dialog_key);
    public static final String DRIVE_BACKUP_OPTION_PREF_KEY = mContext.getString(R.string.drive_backup_option_pref_key);
    public static final String DRIVE_KEY = mContext.getString(R.string.is_drive_support_enabled_key);
    public static final String FIRST_LAUNCH_KEY = mContext.getString(R.string.first_launch_boolean_key);
    public static final String IMAGE_ACTIVITY_KEY = mContext.getString(R.string.image_activity_key);
    public static final String LIST_KEY = mContext.getString(R.string.cursor_adapter_integer_list_bundle_key);
    public static final String MAIN_FOLDER_ID_KEY = mContext.getString(R.string.main_folder_id_prefs);
    public static final String MAIN_FOLDER_METADATA_INFO = mContext.getString(R.string.main_folder_custom_info);
    public static final String MAIN_FOLDER_NAME = mContext.getString(R.string.main_drive_folder_title);
    public static final String NO_WARNING_SELECT_ACTIVITY_KEY = mContext.getString(R.string.no_show_select_activity_key);
    public static final String NO_WARNING_TOO_BRIGHT_KEY = mContext.getString(R.string.warning_too_bright_key);
    public static final String PALETTE_TO_UPLOAD_NAME_KEY = mContext.getString(R.string.palette_to_upload_bundle_key);
    public static final String PRIMARY_COLOR_KEY = mContext.getString(R.string.primary_color_key);
//    public static final String RECEIVER_BUNDLE_EXTRA_MESSAGE = mContext.getString(R.string.receiver_bundle_extra_message_key);
    public static final String RECEIVER_BUNDLE_IS_SUCCESS_BOOLEAN = mContext.getString(R.string.receiver_bundle_is_success_boolean_key);
    public static final String SELECT_MODE_KEY = mContext.getString(R.string.is_cursor_adapter_in_select_mode_bundle_key);
    public static final String SERVICE_BUNDLE_KEY_IS_VERIFYING = mContext.getString(R.string.service_bundle_key_is_verifying);
    public static final String UPLOAD_RESULT_KEY = mContext.getString(R.string.upload_result_prefs_key);
    public static final String URI_BUNDLE_ID_KEY = mContext.getString(R.string.image_uri_key);
    public static final String XML_METADATA_INFO = mContext.getString(R.string.xml_metadata_info);



    /*Custom property keys for Drive components*/
    public static final CustomPropertyKey CUSTOM_PROPERTY_KEY_MAIN_FOLDER = new CustomPropertyKey(CUSTOM_PROPERTY_MAIN_FOLDER, CustomPropertyKey.PUBLIC);
    public static final CustomPropertyKey CUSTOM_PROPERTY_KEY_XML = new CustomPropertyKey(CUSTOM_PROPERTY_COLORS_XML, CustomPropertyKey.PUBLIC);


    /*Integer request codes*/
    public static final int COLOR_TOO_BRIGHT_DIALOG_REQUEST = 46;
    public static final int CURSOR_ACTIVITY_REQUEST_CODE = 29;
    public static final int ENABLE_DRIVE_DIALOG_REQUEST = 4;
    public static final int IMAGE_ACTIVITY_REQUEST_RESULT_CODE = 73;
    public static final int BACKUP_TO_DRIVE_DIALOG_REQUEST = 37;
    public static final int DRIVE_REQUEST_FILE_CODE = 4;
    public static final int IMAGE_REQUEST_CODE = 46;
    public static final int INTENT_SEND_ACTION_DIALOG_REQUEST = 73;
    public static final int LOAD_FROM_IMAGE_SELECT_DIALOG_REQUEST = 58;
    public static final int NOTIFICATION_ID = 46;
    public static final int PALETTE_TO_UPLOAD_REQUEST_CODE = 37;
    public static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    public static final int SAVE_NAME_DIALOG_REQUEST = 29;
}
