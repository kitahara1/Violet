package com.example.violet;

import com.example.violet.ui.anim.TurnstileAnimator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class TurnstileAnimatorTest {

    @Test
    public void testTurnstileAnimatorInitialState() {
        TurnstileAnimator.resetLaunchingState();
        assertFalse(TurnstileAnimator.isLaunching());
    }

    @Test
    public void testResetInstantNullSafe() {
        TurnstileAnimator.resetInstant(null);
        assertFalse(TurnstileAnimator.isLaunching());
    }

    @Test
    public void testResetAllNullSafe() {
        TurnstileAnimator.resetAll(null, null, null);
        assertFalse(TurnstileAnimator.isLaunching());
    }
}
