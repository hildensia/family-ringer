package com.familyringer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.VH> {

    interface OnAlertClick { void onClick(String message); }

    private final List<String> alerts;
    private final OnAlertClick listener;

    public AlertAdapter(List<String> alerts, OnAlertClick listener) {
        this.alerts = alerts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String alert = alerts.get(position);
        holder.button.setText(alert);
        holder.button.setOnClickListener(v -> listener.onClick(alert));
    }

    @Override
    public int getItemCount() { return alerts.size(); }

    static class VH extends RecyclerView.ViewHolder {
        Button button;
        VH(@NonNull View v) {
            super(v);
            button = v.findViewById(R.id.btnAlert);
        }
    }
}
