package com.example.violet.ui.apps;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.violet.R;
import com.example.violet.data.AccentColor;
import com.example.violet.data.LauncherPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Windows Phone–style alphabet grid jump modal.
 * Shows # and A-Z in a 4-column grid. Active letters are highlighted with the accent color.
 */
public class AlphabetJumpDialog extends Dialog {

    public interface OnLetterSelectedListener {
        void onLetterSelected(String letter);
    }

    private final Set<String> activeLetters;
    private final OnLetterSelectedListener listener;
    private final AccentColor accentColor;

    public AlphabetJumpDialog(@NonNull Context context, Set<String> activeLetters, OnLetterSelectedListener listener) {
        super(context);
        this.activeLetters = activeLetters;
        this.listener = listener;
        this.accentColor = LauncherPreferences.getInstance(context).getAccentColor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_alphabet_jump);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        RecyclerView rvGrid = findViewById(R.id.rv_alphabet_grid);
        rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 4));

        List<String> letters = buildAlphabetList();
        rvGrid.setAdapter(new AlphabetAdapter(letters, activeLetters, accentColor, letter -> {
            dismiss();
            if (listener != null) {
                listener.onLetterSelected(letter);
            }
        }));
    }

    private List<String> buildAlphabetList() {
        List<String> list = new ArrayList<>();
        list.add("#");
        for (char c = 'A'; c <= 'Z'; c++) {
            list.add(String.valueOf(c));
        }
        return list;
    }

    private static class AlphabetAdapter extends RecyclerView.Adapter<AlphabetAdapter.ViewHolder> {
        private final List<String> letters;
        private final Set<String> activeLetters;
        private final AccentColor accentColor;
        private final OnLetterSelectedListener clickListener;

        AlphabetAdapter(List<String> letters, Set<String> activeLetters, AccentColor accentColor, OnLetterSelectedListener clickListener) {
            this.letters = letters;
            this.activeLetters = activeLetters;
            this.accentColor = accentColor;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alphabet_jump_tile, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String letter = letters.get(position);
            boolean isActive = activeLetters != null && activeLetters.contains(letter);
            holder.bind(letter, isActive, accentColor, clickListener);
        }

        @Override
        public int getItemCount() {
            return letters.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final FrameLayout root;
            private final TextView tvLetter;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                root = itemView.findViewById(R.id.jump_tile_root);
                tvLetter = itemView.findViewById(R.id.tv_jump_letter);
            }

            void bind(String letter, boolean isActive, AccentColor accentColor, OnLetterSelectedListener listener) {
                tvLetter.setText(letter);

                if (isActive) {
                    if (accentColor != null && root.getBackground() != null) {
                        root.getBackground().mutate().setColorFilter(accentColor.getColorInt(), PorterDuff.Mode.SRC_IN);
                    }
                    tvLetter.setTextColor(Color.WHITE);
                    itemView.setEnabled(true);
                    itemView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onLetterSelected(letter);
                        }
                    });
                } else {
                    if (root.getBackground() != null) {
                        root.getBackground().mutate().setColorFilter(Color.parseColor("#26888888"), PorterDuff.Mode.SRC_IN);
                    }
                    tvLetter.setTextColor(Color.parseColor("#4DFFFFFF"));
                    itemView.setEnabled(false);
                    itemView.setOnClickListener(null);
                }
            }
        }
    }
}
