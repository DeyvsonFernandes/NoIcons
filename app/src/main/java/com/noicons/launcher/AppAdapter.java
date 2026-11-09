package com.noicons.launcher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.VH> {
    private List<ResolveInfo> apps;
    private final PackageManager pm;

    public AppAdapter(List<ResolveInfo> apps, PackageManager pm) {
        this.apps = apps;
        this.pm = pm;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setApps(List<ResolveInfo> novos) {
        this.apps = novos;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView nome, contador;

        VH(View v) {
            super(v);
            nome = v.findViewById(R.id.nome);
            contador = v.findViewById(R.id.contador);
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        ResolveInfo info = apps.get(position);
        String pkg = info.activityInfo.packageName;

        holder.nome.setText(info.loadLabel(pm));

        int n = NotificationStore.getCount(pkg);
        if (n > 0) {
            holder.contador.setText(n > 99 ? "99+" : String.valueOf(n));
            holder.contador.setVisibility(View.VISIBLE);
        } else {
            holder.contador.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent i = pm.getLaunchIntentForPackage(pkg);
            if (i != null) {
                NotificationStore.markSeen(pkg);
                v.getContext().startActivity(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }
}