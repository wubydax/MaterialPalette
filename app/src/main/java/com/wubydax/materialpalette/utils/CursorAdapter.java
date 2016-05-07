package com.wubydax.materialpalette.utils;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.wubydax.materialpalette.MainViewFragment;
import com.wubydax.materialpalette.R;
import com.wubydax.materialpalette.data.PaletteContract;

import java.util.ArrayList;

/**
 * Created by Anna Berkovitch on 31/03/2016.
 * RecyclerView adapter for cursor loader
 */
public class CursorAdapter extends RecyclerView.Adapter<CursorAdapter.MyViewHolder> {
    private Context mContext;
    private Cursor mCursor;
    private OnCursorSelectedListener mListener;
    private boolean isSelectMode;
    private ArrayList<Integer> mSelectedPositionList;

    public CursorAdapter(Context context) {
        mContext = context;
        isSelectMode = false;
        mSelectedPositionList = new ArrayList<>();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mainView = LayoutInflater.from(mContext).inflate(R.layout.cursor_item, parent, false);
        return new MyViewHolder(mainView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        onBindCursorToHolder(holder, position, getItem(position));
    }

    private void onBindCursorToHolder(MyViewHolder holder, int position, Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            holder.mPreview.setImageBitmap(Utils.getBitmap(cursor.getBlob(PaletteContract.PREVIEW_POSITION)));
            holder.mTitle.setText(cursor.getString(PaletteContract.NAME_POSITION));
            holder.mPosition = position;
            holder.mCheckBox.setVisibility(isSelectMode ? View.VISIBLE : View.GONE);
            holder.mProtectiveLayer.setVisibility(isSelectMode ? View.VISIBLE : View.GONE);
            if(!isSelectMode) {
                holder.mCheckBox.setChecked(false);
            } else {
                if(mSelectedPositionList.contains(position)) {
                    holder.mCheckBox.setChecked(true);
                } else {
                    holder.mCheckBox.setChecked(false);
                }
            }


        }


    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView mPreview;
        TextView mTitle;
        AppCompatCheckBox mCheckBox;
        int mPosition;
        View mProtectiveLayer;

        public MyViewHolder(View itemView) {
            super(itemView);
            mPreview = (ImageView) itemView.findViewById(R.id.savedBitmapPreview);
            mTitle = (TextView) itemView.findViewById(R.id.savedPaletteTitle);
            mCheckBox = (AppCompatCheckBox) itemView.findViewById(R.id.deleteCheckbox);
            mProtectiveLayer = itemView.findViewById(R.id.protectiveLayout);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                StateListAnimator stateListAnimator = AnimatorInflater.loadStateListAnimator(mContext, R.animator.on_click_elevation);
                itemView.setStateListAnimator(stateListAnimator);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Cursor cursor = getItem(mPosition);
                    if (cursor != null && !cursor.isClosed()) {
                        if(!isSelectMode) {
                            int primaryColor = cursor.getInt(PaletteContract.PRIMARY_POSITION);
                            int accentColor = cursor.getInt(PaletteContract.ACCENT_POSITION);
                            String paletteName = cursor.getString(PaletteContract.NAME_POSITION);
                            mListener.onCursorSelected(primaryColor, accentColor, paletteName);
                        } else {
                            mCheckBox.setChecked(!mCheckBox.isChecked());
                        }

                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(!isSelectMode) {
                        isSelectMode = true;
                        notifyDataSetChanged();
                        mListener.onSelectModeEnabled();
                        mCheckBox.setChecked(true);
                        if(!mSelectedPositionList.contains(mPosition)) {
                            mSelectedPositionList.add(mPosition);
                        }
                        return true;
                    }
                    return false;
                }
            });

            mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isSelectMode) {
                        Cursor cursor = getItem(mPosition);
                        long id = cursor.getLong(PaletteContract.ID_POSITION);
                        Log.d(MainViewFragment.LOG_TAG, "onBindCursorToHolder id is " + id);
                        mListener.onItemSelectedForDeletion(isChecked, id);
                        if(isChecked) {
                            if(!mSelectedPositionList.contains(mPosition)) {
                                mSelectedPositionList.add(mPosition);
                            }
                            Log.d(MainViewFragment.LOG_TAG, "onCheckedChanged list size is " + mSelectedPositionList.size());
                        } else {
                            mSelectedPositionList.remove(((Integer) mPosition));
                            Log.d(MainViewFragment.LOG_TAG, "onCheckedChanged list size is " + mSelectedPositionList.size());
                        }
                    }

                }
            });

        }
    }

    public void setSelectedPositionList(ArrayList<Integer> list) {
        mSelectedPositionList = list;
    }

    public ArrayList<Integer> getSelectedPositionList() {
        return mSelectedPositionList;
    }

    public boolean isSelectMode() {
        return isSelectMode;
    }

    public void setSelectMode(boolean isSelectMode) {
        this.isSelectMode = isSelectMode;
        if(!isSelectMode) {
            mSelectedPositionList = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    public Cursor getItem(int position) {
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.moveToPosition(position);
        }
        return mCursor;
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    public void setOnCursorSelectedListener(OnCursorSelectedListener onCursorSelectedListener) {
        mListener = onCursorSelectedListener;
    }

    public interface OnCursorSelectedListener {
        void onCursorSelected(int primaryColor, int accentColor, String paletteName);

        void onSelectModeEnabled();

        void onItemSelectedForDeletion(boolean isSelected, long id);
    }
}
