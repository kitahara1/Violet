package com.example.violet.ui.apps;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.violet.R;
import com.example.violet.data.AppInfo;
import com.example.violet.data.AppRepository;
import com.example.violet.data.HiddenAppsManager;
import com.example.violet.ui.security.PinEntryDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Windows Phone Metro–styled dialog displaying hidden applications.
 * Allows launching hidden apps privately, unhiding them, or changing the PIN.
 */
public class HiddenAppsDialog extends Dialog {

    private final HiddenAppsManager hiddenAppsManager;
    private final AppRepository appRepository;

    private RecyclerView rvHiddenApps;
    private TextView tvEmpty;
    private HiddenAppsAdapter adapter;

    public HiddenAppsDialog(@NonNull Context context) {
        super(context);
        this.hiddenAppsManager = HiddenAppsManager.getInstance(context);
        this.appRepository = AppRepository.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_hidden_apps);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        initViews();
        loadHiddenApps();
    }

    private void initViews() {
        rvHiddenApps = findViewById(R.id.rv_hidden_apps);
        tvEmpty = findViewById(R.id.tv_empty_hidden);
        rvHiddenApps.setLayoutManager(new LinearLayoutManager(getContext()));

        TextView btnChangePin = findViewById(R.id.btn_change_pin);
        if (btnChangePin != null) {
            btnChangePin.setOnClickListener(v -> {
                PinEntryDialog pinDialog = new PinEntryDialog(getContext(), PinEntryDialog.Mode.CREATE, () -> {
                    Toast.makeText(getContext(), "PIN updated", Toast.LENGTH_SHORT).show();
                });
                pinDialog.show();
            });
        }
    }

    private void loadHiddenApps() {
        Set<String> hiddenPackages = hiddenAppsManager.getHiddenPackages();
        PackageManager pm = getContext().getPackageManager();
        List<HiddenAppEntry> entries = new ArrayList<>();

        for (String pkg : hiddenPackages) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                String label = pm.getApplicationLabel(ai).toString();
                Drawable icon = pm.getApplicationIcon(ai);
                entries.add(new HiddenAppEntry(pkg, label, icon));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        if (entries.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHiddenApps.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHiddenApps.setVisibility(View.VISIBLE);
        }

        adapter = new HiddenAppsAdapter(entries, new HiddenAppsAdapter.OnHiddenAppActionListener() {
            @Override
            public void onLaunch(HiddenAppEntry entry) {
                launchApp(entry.packageName);
            }

            @Override
            public void onUnhide(HiddenAppEntry entry) {
                hiddenAppsManager.unhideApp(entry.packageName);
                Toast.makeText(getContext(), R.string.app_unhidden, Toast.LENGTH_SHORT).show();
                loadHiddenApps();
            }
        });
        rvHiddenApps.setAdapter(adapter);
    }

    private void launchApp(String packageName) {
        try {
            Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(launchIntent);
                dismiss();
            } else {
                Toast.makeText(getContext(), "Cannot launch app", Toast.LENGTH_SHORT).show();
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(getContext(), "Unable to launch", Toast.LENGTH_SHORT).show();
        }
    }

    private static class HiddenAppEntry {
        final String packageName;
        final String label;
        final Drawable icon;

        HiddenAppEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private static class HiddenAppsAdapter extends RecyclerView.Adapter<HiddenAppsAdapter.ViewHolder> {

        interface OnHiddenAppActionListener {
            void onLaunch(HiddenAppEntry entry);
            void onUnhide(HiddenAppEntry entry);
        }

        private final List<HiddenAppEntry> entries;
        private final OnHiddenAppActionListener listener;

        HiddenAppsAdapter(List<HiddenAppEntry> entries, OnHiddenAppActionListener listener) {
            this.entries = entries;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hidden_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(entries.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivIcon;
            private final TextView tvName;
            private final TextView btnUnhide;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_hidden_icon);
                tvName = itemView.findViewById(R.id.tv_hidden_name);
                btnUnhide = itemView.findViewById(R.id.btn_unhide_app);
            }

            void bind(HiddenAppEntry entry, OnHiddenAppActionListener listener) {
                tvName.setText(entry.label);
                ivIcon.setImageDrawable(entry.icon);

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onLaunch(entry);
                });

                btnUnhide.setOnClickListener(v -> {
                    if (listener != null) listener.onUnhide(entry);
                });
            }
        }
    }
}
