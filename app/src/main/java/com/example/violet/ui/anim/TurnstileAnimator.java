package com.example.violet.ui.anim;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.violet.R;

/**
 * Implements the sequential Windows Phone launch choreography:
 * 1. Full 3D Turnstile kinetic animation finishes first (0 - 240ms).
 * 2. Interim fake Metro splash screen appears next, displaying the app's accent color, icon, and title (240 - 560ms).
 * 3. Actual target application launches smoothly on top of the splash screen (at 560ms).
 * 4. Return to Launcher resets all views cleanly to resting state.
 */
public class TurnstileAnimator {

    private static volatile boolean isLaunching = false;

    public static boolean isLaunching() {
        return isLaunching;
    }

    public static void resetLaunchingState() {
        isLaunching = false;
    }

    /**
     * Holds references and state management for the fullscreen Windows Phone Metro splash overlay.
     */
    public static class SplashViewHolder {
        public final View root;
        public final ImageView icon;
        public final TextView label;

        public SplashViewHolder(@NonNull View root) {
            this.root = root;
            this.icon = root.findViewById(R.id.iv_splash_icon);
            this.label = root.findViewById(R.id.tv_splash_label);
        }

        public void show(@Nullable Drawable iconDrawable, @NonNull String labelText, int bgColor) {
            if (root == null) return;
            root.setBackgroundColor(bgColor);
            if (icon != null) {
                icon.setImageDrawable(iconDrawable);
            }
            if (label != null) {
                label.setText(labelText);
            }

            root.setAlpha(0f);
            root.setScaleX(0.95f);
            root.setScaleY(0.95f);
            root.setVisibility(View.VISIBLE);
            root.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .start();
        }

        public void hide() {
            if (root == null) return;
            root.animate().cancel();
            root.setVisibility(View.GONE);
            root.setAlpha(0f);
        }

        public boolean isVisible() {
            return root != null && root.getVisibility() == View.VISIBLE;
        }
    }

    /**
     * Executes the sequential launch: Full Turnstile animation first -> Fake Splash Screen -> Actual App.
     */
    public static void animateTileLaunch(
            @NonNull Activity activity,
            @NonNull View clickedTile,
            @Nullable RecyclerView rvTiles,
            @Nullable SplashViewHolder splashHolder,
            @Nullable Drawable iconDrawable,
            @NonNull String label,
            int backgroundColor,
            @NonNull Intent intent
    ) {
        if (isLaunching) return;
        isLaunching = true;

        Context context = activity;
        float density = context.getResources().getDisplayMetrics().density;

        // --- PHASE 1: Full 3D Turnstile Animation (0 - 240ms) ---
        // 1. Zoom the tapped tile forward toward the camera
        clickedTile.setCameraDistance(density * 8000f);
        clickedTile.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new AccelerateInterpolator(1.4f))
                .start();

        // 2. Swivel surrounding visible tiles backward around vertical hinge with feathered cascade
        if (rvTiles != null) {
            int childCount = rvTiles.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = rvTiles.getChildAt(i);
                if (child == clickedTile) continue;

                child.setCameraDistance(density * 8000f);
                child.setPivotX(0f);
                child.setPivotY(child.getHeight() * 0.5f);

                int childPos = rvTiles.getChildAdapterPosition(child);
                int stagger = (childPos >= 0) ? Math.min(60, (childPos % 4) * 15 + (childPos / 4) * 12) : 0;

                child.animate()
                        .rotationY(-60f)
                        .translationX(-dpToPx(context, 35))
                        .alpha(0f)
                        .setDuration(200)
                        .setStartDelay(stagger)
                        .setInterpolator(new AccelerateInterpolator(1.2f))
                        .start();
            }
        }

        // --- PHASE 2: Fake Splash Screen (Revealed ONLY after Turnstile finishes at 240ms) ---
        clickedTile.postDelayed(() -> {
            if (!isLaunching) return;
            if (splashHolder != null) {
                splashHolder.show(iconDrawable, label, backgroundColor);
            }
        }, 240);

        // --- PHASE 3: Launch Target Application over Splash Screen (at 560ms) ---
        clickedTile.postDelayed(() -> {
            if (!isLaunching) return;
            try {
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        activity,
                        R.anim.turnstile_app_enter,
                        0
                );
                activity.startActivity(intent, options.toBundle());
                if (Build.VERSION.SDK_INT >= 34) {
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.turnstile_app_enter, 0);
                } else {
                    activity.overridePendingTransition(R.anim.turnstile_app_enter, 0);
                }
            } catch (Exception e) {
                if (splashHolder != null) {
                    splashHolder.hide();
                }
                resetInstant(rvTiles);
                resetLaunchingState();
            }
        }, 560);
    }

    /**
     * Executes the sequential launch from All Apps: Turnstile peel first -> Fake Splash Screen -> Actual App.
     */
    public static void animateListLaunch(
            @NonNull Activity activity,
            @NonNull View clickedRow,
            @Nullable RecyclerView rvAppList,
            @Nullable SplashViewHolder splashHolder,
            @Nullable Drawable iconDrawable,
            @NonNull String label,
            int backgroundColor,
            @NonNull Intent intent
    ) {
        if (isLaunching) return;
        isLaunching = true;

        Context context = activity;
        float density = context.getResources().getDisplayMetrics().density;

        // --- PHASE 1: Full Turnstile Peel Animation (0 - 240ms) ---
        clickedRow.setCameraDistance(density * 8000f);
        clickedRow.setPivotX(0f);
        clickedRow.setPivotY(clickedRow.getHeight() * 0.5f);
        clickedRow.animate()
                .rotationY(-45f)
                .translationX(-dpToPx(context, 40))
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(new AccelerateInterpolator(1.3f))
                .start();

        if (rvAppList != null) {
            int clickedIndex = -1;
            int childCount = rvAppList.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (rvAppList.getChildAt(i) == clickedRow) {
                    clickedIndex = i;
                    break;
                }
            }

            for (int i = 0; i < childCount; i++) {
                View child = rvAppList.getChildAt(i);
                if (child == clickedRow) continue;

                child.setCameraDistance(density * 8000f);
                child.setPivotX(0f);
                child.setPivotY(child.getHeight() * 0.5f);

                int distance = (clickedIndex >= 0) ? Math.abs(i - clickedIndex) : i;
                int stagger = Math.min(60, distance * 15);

                child.animate()
                        .rotationY(-65f)
                        .translationX(-dpToPx(context, 50))
                        .alpha(0f)
                        .setDuration(200)
                        .setStartDelay(stagger)
                        .setInterpolator(new AccelerateInterpolator(1.2f))
                        .start();
            }
        }

        // --- PHASE 2: Fake Splash Screen (Revealed ONLY after Turnstile finishes at 240ms) ---
        clickedRow.postDelayed(() -> {
            if (!isLaunching) return;
            if (splashHolder != null) {
                splashHolder.show(iconDrawable, label, backgroundColor);
            }
        }, 240);

        // --- PHASE 3: Launch Target Application over Splash Screen (at 560ms) ---
        clickedRow.postDelayed(() -> {
            if (!isLaunching) return;
            try {
                ActivityOptions options = ActivityOptions.makeCustomAnimation(
                        activity,
                        R.anim.turnstile_app_enter,
                        0
                );
                activity.startActivity(intent, options.toBundle());
                if (Build.VERSION.SDK_INT >= 34) {
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.turnstile_app_enter, 0);
                } else {
                    activity.overridePendingTransition(R.anim.turnstile_app_enter, 0);
                }
            } catch (Exception e) {
                if (splashHolder != null) {
                    splashHolder.hide();
                }
                resetInstant(rvAppList);
                resetLaunchingState();
            }
        }, 560);
    }

    /**
     * Resets both Start screen and Apps list states cleanly to their neutral resting state.
     */
    public static void resetAll(
            @Nullable RecyclerView rvTiles,
            @Nullable RecyclerView rvAppList,
            @Nullable SplashViewHolder splashHolder
    ) {
        isLaunching = false;
        if (splashHolder != null) {
            splashHolder.hide();
        }
        resetInstant(rvTiles);
        resetInstant(rvAppList);
    }

    /**
     * Resets view properties instantly to default neutral state without animation.
     */
    public static void resetInstant(@Nullable RecyclerView rv) {
        if (rv == null) return;
        int childCount = rv.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = rv.getChildAt(i);
            child.animate().cancel();
            child.setRotationY(0f);
            child.setTranslationX(0f);
            child.setAlpha(1f);
            child.setScaleX(1f);
            child.setScaleY(1f);
        }
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
