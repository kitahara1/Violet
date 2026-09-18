package com.example.violet.ui.anim;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * Implements the iconic Windows Phone Metro 3D Tile Tilt effect.
 * When touched, tiles tilt inward along the 3D perspective axis based on touch location,
 * and snap back on release while preserving click and long-press event flow.
 */
public class MetroTiltTouchListener implements View.OnTouchListener {

    private static final float MAX_TILT_ANGLE = 12.0f; // Maximum degrees of tilt
    private static final float PRESS_SCALE = 0.96f;     // Depth scale
    private static final long TILT_DURATION_MS = 80;
    private static final long RELEASE_DURATION_MS = 150;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Set camera distance for authentic 3D depth perspective
        float density = v.getResources().getDisplayMetrics().density;
        v.setCameraDistance(density * 8000);

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                float width = v.getWidth();
                float height = v.getHeight();

                if (width > 0 && height > 0) {
                    float centerX = width / 2.0f;
                    float centerY = height / 2.0f;

                    // Normalized relative offsets [-1.0, 1.0]
                    float dx = (event.getX() - centerX) / centerX;
                    float dy = (event.getY() - centerY) / centerY;

                    // Clamp
                    dx = Math.max(-1.0f, Math.min(1.0f, dx));
                    dy = Math.max(-1.0f, Math.min(1.0f, dy));

                    // Invert axes according to Android 3D perspective
                    float targetRotX = -dy * MAX_TILT_ANGLE;
                    float targetRotY = dx * MAX_TILT_ANGLE;

                    animateTilt(v, targetRotX, targetRotY, PRESS_SCALE, TILT_DURATION_MS);
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                animateTilt(v, 0f, 0f, 1.0f, RELEASE_DURATION_MS);
                break;
            }
        }

        // Return false so View's onClick and onLongClick listeners continue to fire normally
        return false;
    }

    private void animateTilt(View v, float rotX, float rotY, float scale, long duration) {
        v.animate().cancel();
        v.animate()
                .rotationX(rotX)
                .rotationY(rotY)
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
