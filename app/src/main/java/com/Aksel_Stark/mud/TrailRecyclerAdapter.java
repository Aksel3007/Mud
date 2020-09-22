package com.Aksel_Stark.mud;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrailRecyclerAdapter extends RecyclerView.Adapter<TrailRecyclerAdapter.ViewHolder> {

    private List<Trail> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    TrailRecyclerAdapter(Context context, List<Trail> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    public void updateTrailList(List<Trail> data){
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.trail_recycler_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String TrailName = mData.get(position).name;
        holder.myTextView.setText(TrailName);

        Double precip = mData.get(position).precipLastDay;
        holder.Precipitation.setText("Rain last 24h: "+precip+" inches");
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;
        TextView Precipitation;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.TrailName);
            Precipitation = itemView.findViewById(R.id.PrecipTextView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // Get name of trail with id
    String getItem(int id) {
        return mData.get(id).getName();
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}