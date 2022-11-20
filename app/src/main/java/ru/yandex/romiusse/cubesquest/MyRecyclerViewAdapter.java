package ru.yandex.romiusse.cubesquest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {

    private ArrayList<ArrayList<String>> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    MyRecyclerViewAdapter(Context context, ArrayList<ArrayList<String>> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recycler_view_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.description.setText(mData.get(position).get(0));
        switch (mData.get(position).get(1)) {
            case "answerInput":
                holder.image.setBackgroundResource(R.drawable.ic_answerinput);
                break;
            case "textInput":
                holder.image.setBackgroundResource(R.drawable.ic_textinput);
                break;
            case "slides":
                holder.image.setBackgroundResource(R.drawable.ic_description);
                break;
            case "mapFlag":
                holder.image.setBackgroundResource(R.drawable.ic_mapflag);
                break;
        }
        holder.id.setText(mData.get(position).get(2));
        holder.to.setText(mData.get(position).get(3));
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView description;
        ImageView image;
        TextView id;
        TextView to;

        ViewHolder(View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.recyclerViewDescription);
            image = itemView.findViewById(R.id.recyclerViewImage);
            id = itemView.findViewById(R.id.recyclerViewId);
            to = itemView.findViewById(R.id.recyclerViewTo);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    List<String> getItem(int id) {
        return mData.get(id);
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