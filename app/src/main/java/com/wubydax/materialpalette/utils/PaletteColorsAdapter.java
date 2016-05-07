package com.wubydax.materialpalette.utils;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wubydax.materialpalette.MyApplication;
import com.wubydax.materialpalette.R;

import java.util.List;

/**
 * Created by Anna Berkovitch on 27/03/2016.
 */
public class PaletteColorsAdapter extends RecyclerView.Adapter<PaletteColorsAdapter.MyViewHolder> {
    private Context mContext;
    private List<Integer> mList;
    private OnColorSelectedListener mListener;
    int mClickNumber;

    public PaletteColorsAdapter(List<Integer> list) {
        mContext = MyApplication.getContext();
        mList = list;
        mClickNumber = 0;
    }

    public  interface OnColorSelectedListener {
        void onColorSelected(int color, boolean isPrimary);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener){
        mListener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mainView = LayoutInflater.from(mContext).inflate(R.layout.swatch_item, parent, false);
        return new MyViewHolder(mainView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.mCardView.setCardBackgroundColor(mList.get(position));
        holder.mPosition = position;
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        CardView mCardView;
        int mPosition;


        public MyViewHolder(View itemView) {
            super(itemView);
            mCardView = (CardView) itemView;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickNumber++;
                    boolean isPrimary = mClickNumber % 2 != 0;
                    mListener.onColorSelected(mList.get(mPosition), isPrimary);
                }
            });
        }
    }
}
