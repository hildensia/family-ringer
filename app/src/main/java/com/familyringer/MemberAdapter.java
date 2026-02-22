package com.familyringer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.VH> {

    interface OnMemberClick { void onClick(Member member); }

    private final List<Member> members;
    private final OnMemberClick listener;

    public MemberAdapter(List<Member> members, OnMemberClick listener) {
        this.members = members;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_member, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Member m = members.get(position);
        holder.name.setText(m.name);
        holder.emoji.setText(m.isChild() ? "👦" : "👨");
        holder.itemView.setBackgroundResource(
            m.selected ? R.drawable.bg_kid_selected : R.drawable.bg_kid_normal);
        holder.itemView.setOnClickListener(v -> listener.onClick(m));
    }

    @Override
    public int getItemCount() { return members.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, emoji;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.textKidName);
            emoji = v.findViewById(R.id.textKidEmoji);
        }
    }
}
