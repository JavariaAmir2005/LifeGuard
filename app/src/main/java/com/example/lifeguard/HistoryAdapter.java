package com.example.lifeguard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    List<History> list;

    public HistoryAdapter(List<History> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new Holder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        History item = list.get(pos);
        h.tvType.setText(item.type);
        h.tvTime.setText(item.time);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvType, tvTime;
        Holder(View v) {
            super(v);
            tvType = v.findViewById(R.id.tvType);
            tvTime = v.findViewById(R.id.tvTime);
        }
    }
}
