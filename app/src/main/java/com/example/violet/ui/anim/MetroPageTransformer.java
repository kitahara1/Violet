package com.example.violet.ui.anim;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Windows Phone–style lightweight horizontal parallax page transformer.
 * Gives subtle depth separation when swiping between Start and All Apps with zero GPU overhead.
 */
public class MetroPageTransformer implements ViewPager2.PageTransformer {

    private static final float PARALLAX_FACTOR = 0.35f;

    @Override
    public void transformPage(@NonNull View page, float position) {
        int pageWidth = page.getWidth();

        if (position < -1) { // [-Infinity, -1): Off-screen to the left
            page.setAlpha(0f);
        } else if (position <= 0) { // [-1, 0]: Sliding out to the left (Start Screen)
            page.setAlpha(1f - Math.abs(position) * 0.4f);
            page.setTranslationX(pageWidth * -position * PARALLAX_FACTOR);
        } else if (position <= 1) { // (0, 1]: Sliding in from the right (All Apps)
            page.setAlpha(1f);
            page.setTranslationX(0f);
        } else { // (1, +Infinity]: Off-screen to the right
            page.setAlpha(0f);
        }
    }
}
