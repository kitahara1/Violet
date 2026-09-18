package com.example.violet.ui.security;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.violet.R;
import com.example.violet.data.HiddenAppsManager;

/**
 * Windows Phone Metro–styled 4-digit PIN entry and creation dialog.
 */
public class PinEntryDialog extends Dialog {

    public enum Mode {
        VERIFY,
        CREATE
    }

    public interface OnPinSuccessCallback {
        void onPinSuccess();
    }

    private final Mode initialMode;
    private final OnPinSuccessCallback callback;
    private final HiddenAppsManager hiddenAppsManager;

    private TextView tvTitle;
    private TextView tvError;
    private LinearLayout llDots;
    private View dot1, dot2, dot3, dot4;

    private final StringBuilder currentInput = new StringBuilder();
    private String firstEnteredPin = null;
    private boolean isConfirming = false;

    public PinEntryDialog(@NonNull Context context, Mode mode, OnPinSuccessCallback callback) {
        super(context);
        this.initialMode = mode;
        this.callback = callback;
        this.hiddenAppsManager = HiddenAppsManager.getInstance(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_pin_entry);

        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        initViews();
        setupKeypad();
        updateUiState();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_pin_title);
        tvError = findViewById(R.id.tv_pin_error);
        llDots = findViewById(R.id.ll_pin_dots);
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        dot4 = findViewById(R.id.dot_4);
    }

    private void setupKeypad() {
        int[] numIds = {
                R.id.btn_num_0, R.id.btn_num_1, R.id.btn_num_2, R.id.btn_num_3,
                R.id.btn_num_4, R.id.btn_num_5, R.id.btn_num_6, R.id.btn_num_7,
                R.id.btn_num_8, R.id.btn_num_9
        };

        for (int id : numIds) {
            Button btn = findViewById(id);
            if (btn != null) {
                btn.setOnClickListener(v -> appendDigit(btn.getText().toString()));
            }
        }

        View btnCancel = findViewById(R.id.btn_keypad_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        View btnBackspace = findViewById(R.id.btn_keypad_backspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> deleteLastDigit());
        }
    }

    private void updateUiState() {
        if (initialMode == Mode.VERIFY) {
            tvTitle.setText(R.string.enter_pin);
        } else {
            if (isConfirming) {
                tvTitle.setText(R.string.confirm_pin);
            } else {
                tvTitle.setText(R.string.create_pin);
            }
        }
        updateDots();
    }

    private void appendDigit(String digit) {
        if (currentInput.length() < 4) {
            currentInput.append(digit);
            tvError.setVisibility(View.INVISIBLE);
            updateDots();

            if (currentInput.length() == 4) {
                onFourDigitsEntered();
            }
        }
    }

    private void deleteLastDigit() {
        if (currentInput.length() > 0) {
            currentInput.deleteCharAt(currentInput.length() - 1);
            tvError.setVisibility(View.INVISIBLE);
            updateDots();
        }
    }

    private void updateDots() {
        int len = currentInput.length();
        dot1.setBackgroundResource(len >= 1 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
        dot2.setBackgroundResource(len >= 2 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
        dot3.setBackgroundResource(len >= 3 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
        dot4.setBackgroundResource(len >= 4 ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
    }

    private void onFourDigitsEntered() {
        String entered = currentInput.toString();

        if (initialMode == Mode.VERIFY) {
            if (hiddenAppsManager.verifyPin(entered)) {
                dismiss();
                if (callback != null) {
                    callback.onPinSuccess();
                }
            } else {
                showError(getContext().getString(R.string.pin_incorrect));
            }
        } else { // Mode.CREATE
            if (!isConfirming) {
                firstEnteredPin = entered;
                isConfirming = true;
                currentInput.setLength(0);
                updateUiState();
            } else {
                if (entered.equals(firstEnteredPin)) {
                    hiddenAppsManager.setupPin(entered);
                    dismiss();
                    if (callback != null) {
                        callback.onPinSuccess();
                    }
                } else {
                    isConfirming = false;
                    firstEnteredPin = null;
                    showError(getContext().getString(R.string.pin_mismatch));
                    updateUiState();
                }
            }
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
        currentInput.setLength(0);
        updateDots();

        // Shake animation feedback
        TranslateAnimation shake = new TranslateAnimation(0, 18, 0, 0);
        shake.setDuration(350);
        shake.setInterpolator(new android.view.animation.CycleInterpolator(4));
        llDots.startAnimation(shake);
    }
}
