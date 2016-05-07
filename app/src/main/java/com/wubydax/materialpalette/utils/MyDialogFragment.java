package com.wubydax.materialpalette.utils;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.wubydax.materialpalette.R;

import java.util.Arrays;


@SuppressLint("InflateParams")
public class MyDialogFragment extends DialogFragment {
    OnDialogResultCallbackListener mListener;


    public interface OnDialogResultCallbackListener {
        void onDialogResult(int requestCode, String paletteName, boolean isCanceled);
    }

    public static MyDialogFragment newInstance(int dialogRequestCode) {
        MyDialogFragment myDialogFragment = new MyDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.DIALOG_REQUEST_CODE_KEY, dialogRequestCode);
        myDialogFragment.setArguments(bundle);
        return myDialogFragment;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle == null) {
            return null;
        }
        int requestCode = bundle.getInt(Constants.DIALOG_REQUEST_CODE_KEY);
        switch (requestCode) {
            case Constants.COLOR_TOO_BRIGHT_DIALOG_REQUEST:
                return getColorTooBrightDialog();
            case Constants.LOAD_FROM_IMAGE_SELECT_DIALOG_REQUEST:
                return getImageActivitySelectorDialog(requestCode);
            case Constants.INTENT_SEND_ACTION_DIALOG_REQUEST:
                return getImageActivitySelectorDialog(requestCode);
            case Constants.SAVE_NAME_DIALOG_REQUEST:
                return getSavePaletteDialog(requestCode);
            case Constants.ENABLE_DRIVE_DIALOG_REQUEST:
                return getEnableDriveDialog();
            case Constants.BACKUP_TO_DRIVE_DIALOG_REQUEST:
                return getBackupOptionsSelectorDialog(requestCode);
            default:
                return super.onCreateDialog(savedInstanceState);
        }

    }

    private Dialog getBackupOptionsSelectorDialog(final int requestCode) {
        String[] items = getActivity().getResources().getStringArray(R.array.drive_options_items);
        int prefPosition = Constants.mSharedPreferences.getInt(Constants.DRIVE_BACKUP_OPTION_PREF_KEY, 0);

        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.backup_options))
                .setIcon(R.drawable.ic_drive_dialog)
                .setSingleChoiceItems(items, prefPosition, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        Constants.mSharedPreferences.edit().putInt(Constants.DRIVE_BACKUP_OPTION_PREF_KEY, selectedPosition).apply();
                        mListener.onDialogResult(requestCode, null, false);
                    }
                }).create();
    }

    private Dialog getEnableDriveDialog() {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.enable_drive_dialog_title))
                .setMessage(getString(R.string.enable_drive_dialog_message))
                .setIcon(R.drawable.ic_drive_dialog)
                .setNegativeButton(getString(R.string.button_later), null)
                .setPositiveButton(getString(R.string.button_enable), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Constants.mSharedPreferences.edit().putBoolean(Constants.DRIVE_KEY, true).apply();
                    }
                })
                .create();
    }

    private Dialog getSavePaletteDialog(final int requestCode) {
        View savePaletteDialogView = LayoutInflater.from(getActivity()).inflate(R.layout.save_dialog_layout, null);
        final TextView errorText = (TextView) savePaletteDialogView.findViewById(R.id.dialogErrorText);
        EditText editText = (EditText) savePaletteDialogView.findViewById(R.id.saveDialogEditText);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    String errorMessage = null;
                    if(v.getText().toString().length() < 3) {
                        errorMessage = getString(R.string.error_name_too_long);
                    } else if (Utils.isInDb(v.getText().toString())) {
                        errorMessage = getString(R.string.error_palette_exists);
                    }
                    if(errorMessage != null) {
                        errorText.setVisibility(View.VISIBLE);
                        errorText.setText(errorMessage);
                    } else {
                        mListener.onDialogResult(requestCode, v.getText().toString(), false);
                        getDialog().dismiss();
                    }
                    return true;
                }
                return false;
            }
        });
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.save_to_db_title))
                .setView(savePaletteDialogView)
                .create();
    }

    private Dialog getColorTooBrightDialog() {
        View brightColorDialogView = LayoutInflater.from(getActivity()).inflate(R.layout.bright_color_dialog_layout, null);
        CheckBox checkBox = (CheckBox) brightColorDialogView.findViewById(R.id.noShowAgainCheckBox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Constants.mSharedPreferences.edit().putBoolean(Constants.NO_WARNING_TOO_BRIGHT_KEY, !isChecked).apply();
            }
        });
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.bright_color_selected_dialog_title))
                .setView(brightColorDialogView)
                .setNegativeButton(getString(R.string.too_bright_color_button_dismiss), null)
                .create();

    }

    private Dialog getImageActivitySelectorDialog(final int requestCode) {
        View selectImageActivityView = LayoutInflater.from(getActivity()).inflate(R.layout.image_loader_activity_chooser_dialog, null);
        final CheckBox noShowAgainCheckBox = (CheckBox) selectImageActivityView.findViewById(R.id.showNoMoreCheckBox);
        final TextView noShowSelectedText = (TextView) selectImageActivityView.findViewById(R.id.textNoShowSelected);
        noShowAgainCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                noShowSelectedText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        final ListView listView = (ListView) selectImageActivityView.findViewById(R.id.dialogSelectImageViewActivityListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.dialog_single_shoice_item, getResources().getStringArray(R.array.image_display_activity_items));
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(adapter);
        final String[] valuesArray = getResources().getStringArray(R.array.image_display_activity_values);
        listView.setItemChecked(Arrays.asList(valuesArray).indexOf(Constants.mSharedPreferences.getString(Constants.IMAGE_ACTIVITY_KEY, "1")), true);
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.select_image_activity_dialog_title))
                .setView(selectImageActivityView)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogResult(requestCode, null, true);
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int selectedPosition = listView.getCheckedItemPosition();
                        Constants.mSharedPreferences.edit().putString(Constants.IMAGE_ACTIVITY_KEY, valuesArray[selectedPosition]).apply();
                        if(noShowAgainCheckBox.isChecked()) {
                            Constants.mSharedPreferences.edit().putBoolean(Constants.NO_WARNING_SELECT_ACTIVITY_KEY, true).apply();
                        }
                        mListener.onDialogResult(requestCode, null, false);
                    }
                })
                .create();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (OnDialogResultCallbackListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnHeadlineSelectedListener");


        }
    }

    //added for older apis which do not call onAttach(Context context)
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnDialogResultCallbackListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");


        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }
}
