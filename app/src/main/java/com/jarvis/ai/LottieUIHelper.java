package com.jarvis.ai;

import android.view.View;

/**
 * LottieUIHelper
 *
 * Central UI helper convenience wrapper for toggling between Lottie
 * loading and success animations. Delegates to HenryAnimationHelper.
 */
public class LottieUIHelper {

    public static void showLoading(HenryLottieAnimationView view) {
        HenryAnimationHelper.showLoading(view);
    }

    public static void showLoading(HenryLottieAnimationView view, View contentToHide) {
        HenryAnimationHelper.showLoading(view, contentToHide);
    }

    public static void showSuccess(HenryLottieAnimationView view, Runnable onComplete) {
        HenryAnimationHelper.showSuccess(view, onComplete);
    }

    public static void showSuccessThenHide(HenryLottieAnimationView view, long displayDurationMs, Runnable onHidden) {
        HenryAnimationHelper.showSuccessThenHide(view, displayDurationMs, onHidden);
    }

    public static void toggleLoading(HenryLottieAnimationView view, boolean isLoading) {
        HenryAnimationHelper.toggleLoading(view, isLoading);
    }

    public static void toggleLoading(HenryLottieAnimationView view, View targetContent, boolean isLoading) {
        HenryAnimationHelper.toggleLoading(view, targetContent, isLoading);
    }

    public static void transitionLoadingToSuccess(HenryLottieAnimationView view, View contentToReveal, long successDisplayMs, Runnable onComplete) {
        HenryAnimationHelper.transitionLoadingToSuccess(view, contentToReveal, successDisplayMs, onComplete);
    }

    public static void reset(HenryLottieAnimationView view) {
        HenryAnimationHelper.reset(view);
    }
}
