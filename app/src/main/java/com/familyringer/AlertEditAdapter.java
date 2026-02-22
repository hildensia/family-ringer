package com.familyringer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertEditAdapter extends RecyclerView.Adapter<AlertEditAdapter.VH> {

    interface OnDelete { void onDelete(int pos); }

    private final List<String> alerts;
    private final OnDelete listener;

    public AlertEditAdapter(List<String> alerts, OnDelete listener) {
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert_edit, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String alert = alerts.get(position);
        holder.text.setText(alert);
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return alerts.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        ImageButton btnDelete;
        VH(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.textAlertEdit);
            btnDelete = v.findViewById(R.id.btnDeleteAlert);
        }
    }
}
