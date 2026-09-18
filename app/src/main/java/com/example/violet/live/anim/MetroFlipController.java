package com.example.violet.live.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * Manages the authentic Windows Phone / Windows 10 Mobile 3D vertical tumble flip animation.
 * Features perspective camera distance, smooth two-phase rotation, update cascade support,
 * and randomized stagger intervals to prevent CPU spikes. Fully lifecycle-safe with zero background work.
 */
public class MetroFlipController {

    private static final Random RANDOM = new Random();
    private static final int FLIP_HALF_DURATION_MS = 320;

    private final View rootContainer;
    private final View frontFace;
    private final View backFace;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isBackShowing = false;
    private boolean isRunning = false;
    private Runnable flipRunnable;
    private ObjectAnimator activeAnimator;

    public MetroFlipController(View rootContainer, View frontFace, View backFace) {
        this.rootContainer = rootContainer;
        this.frontFace = frontFace;
        this.backFace = backFace;

        // Configure 3D camera distance for natural perspective without distortion
        float density = rootContainer.getResources().getDisplayMetrics().density;
        rootContainer.setCameraDistance(density * 8000f);
    }

    public void startFlipping() {
        if (isRunning) {
            // If already marked running but no flip is currently scheduled, ensure one is queued
            if (flipRunnable == null && activeAnimator == null) {
                int initialDelay = 4000 + RANDOM.nextInt(6000);
                scheduleNextFlip(initialDelay);
            }
            return;
        }
        isRunning = true;

        // Random stagger between 4 and 10 seconds for organic staggered home screen motion
        int initialDelay = 4000 + RANDOM.nextInt(6000);
        scheduleNextFlip(initialDelay);
    }

    public void stopFlipping() {
        isRunning = false;
        if (flipRunnable != null) {
            handler.removeCallbacks(flipRunnable);
            flipRunnable = null;
        }
        if (activeAnimator != null) {
            activeAnimator.cancel();
            activeAnimator = null;
        }

        // Reset cleanly to front face
        rootContainer.setRotationX(0f);
        frontFace.setVisibility(View.VISIBLE);
        backFace.setVisibility(View.GONE);
        isBackShowing = false;
    }

    /**
     * Triggers an immediate or staggered 3D tumble flip to showcase updated live tile content.
     * Guaranteed to flip regardless of previous idle schedule, and ensures multiple updating tiles
     * cascade cleanly across the screen without cancelling each other.
     *
     * @param staggerDelayMs Stagger delay before starting rotation (e.g. 0ms, 160ms, 320ms).
     * @param onFlipHalfway  Callback executed at 90-degree fold (edge-on) to bind fresh content.
     */
    public void triggerUpdateFlip(int staggerDelayMs, @Nullable Runnable onFlipHalfway) {
        isRunning = true;
        if (flipRunnable != null) {
            handler.removeCallbacks(flipRunnable);
            flipRunnable = null;
        }
        if (activeAnimator != null) {
            activeAnimator.cancel();
            activeAnimator = null;
            rootContainer.setRotationX(0f);
        }

        flipRunnable = () -> executeFlipWithUpdate(onFlipHalfway);
        handler.postDelayed(flipRunnable, Math.max(0, staggerDelayMs));
    }

    private void scheduleNextFlip(int delayMs) {
        if (!isRunning) return;

        flipRunnable = this::executeFlip;
        handler.postDelayed(flipRunnable, delayMs);
    }

    private void executeFlip() {
        if (!isRunning) return;

        if (!rootContainer.isAttachedToWindow()) {
            // Reschedule so the flip is not permanently lost if briefly detached during layout/scroll
            scheduleNextFlip(1500);
            return;
        }

        // Phase 1: Rotate from 0 to 90 degrees (folding away)
        activeAnimator = ObjectAnimator.ofFloat(rootContainer, View.ROTATION_X, 0f, 90f);
        activeAnimator.setDuration(FLIP_HALF_DURATION_MS);
        activeAnimator.setInterpolator(new AccelerateInterpolator());
        activeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimator = null;
                if (!isRunning) return;

                // Toggle visible face halfway through
                isBackShowing = !isBackShowing;
                if (isBackShowing) {
                    frontFace.setVisibility(View.GONE);
                    backFace.setVisibility(View.VISIBLE);
                } else {
                    frontFace.setVisibility(View.VISIBLE);
                    backFace.setVisibility(View.GONE);
                }

                // Phase 2: Rotate from -90 to 0 degrees (unfolding)
                activeAnimator = ObjectAnimator.ofFloat(rootContainer, View.ROTATION_X, -90f, 0f);
                activeAnimator.setDuration(FLIP_HALF_DURATION_MS);
                activeAnimator.setInterpolator(new DecelerateInterpolator());
                activeAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator anim) {
                        activeAnimator = null;
                        if (isRunning) {
                            // Cycle repeats every 8 to 14 seconds
                            int nextDelay = 8000 + RANDOM.nextInt(6000);
                            scheduleNextFlip(nextDelay);
                        }
                    }
                });
                activeAnimator.start();
            }
        });
        activeAnimator.start();
    }

    private void executeFlipWithUpdate(@Nullable Runnable onFlipHalfway) {
        if (!isRunning) return;

        if (!rootContainer.isAttachedToWindow()) {
            if (onFlipHalfway != null) {
                onFlipHalfway.run();
            }
            scheduleNextFlip(1500);
            return;
        }

        // Phase 1: Rotate from current rotation (or 0) to 90 degrees (folding away old face)
        float startRot = rootContainer.getRotationX();
        activeAnimator = ObjectAnimator.ofFloat(rootContainer, View.ROTATION_X, startRot, 90f);
        activeAnimator.setDuration(FLIP_HALF_DURATION_MS);
        activeAnimator.setInterpolator(new AccelerateInterpolator());
        activeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                activeAnimator = null;
                if (!isRunning) return;

                // At 90 degrees (edge-on invisible), update content and reveal back face
                if (onFlipHalfway != null) {
                    onFlipHalfway.run();
                }

                isBackShowing = true;
                frontFace.setVisibility(View.GONE);
                backFace.setVisibility(View.VISIBLE);

                // Phase 2: Unfold from -90 to 0 degrees revealing the updated live content
                activeAnimator = ObjectAnimator.ofFloat(rootContainer, View.ROTATION_X, -90f, 0f);
                activeAnimator.setDuration(FLIP_HALF_DURATION_MS);
                activeAnimator.setInterpolator(new DecelerateInterpolator());
                activeAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator anim) {
                        activeAnimator = null;
                        if (isRunning) {
                            // Next idle flip in 8 to 14 seconds
                            int nextDelay = 8000 + RANDOM.nextInt(6000);
                            scheduleNextFlip(nextDelay);
                        }
                    }
                });
                activeAnimator.start();
            }
        });
        activeAnimator.start();
    }
}
