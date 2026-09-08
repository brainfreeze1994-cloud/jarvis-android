package com.jarvis.ai;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * HenryAnimationHelper
 *
 * Central UI helper class to toggle between Lottie and hardware-accelerated
 * animations for loading and success states across activities, dialogs, and components.
 */
public class HenryAnimationHelper {

    public enum State {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Show loading spinner animation on the target HenryLottieAnimationView.
     */
    public static void showLoading(HenryLottieAnimationView animView) {
        if (animView == null) return;
        MAIN_HANDLER.post(() -> {
            animView.setVisibility(View.VISIBLE);
            animView.setAlpha(1f);
            animView.playLoadingSpinner();
        });
    }

    /**
     * Show loading animation and smoothly hide the underlying content view.
     */
    public static void showLoading(HenryLottieAnimationView animView, View contentToHide) {
        if (animView == null) return;
        MAIN_HANDLER.post(() -> {
            animView.setVisibility(View.VISIBLE);
            animView.animate().alpha(1f).setDuration(200).start();
            animView.playLoadingSpinner();

            if (contentToHide != null) {
                contentToHide.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                contentToHide.setVisibility(View.INVISIBLE);
                            }
                        })
                        .start();
            }
        });
    }

    /**
     * Show success checkmark animation.
     */
    public static void showSuccess(HenryLottieAnimationView animView, Runnable onComplete) {
        if (animView == null) {
            if (onComplete != null) MAIN_HANDLER.post(onComplete);
            return;
        }
        MAIN_HANDLER.post(() -> {
            animView.setVisibility(View.VISIBLE);
            animView.setAlpha(1f);
            animView.playSuccessCheck();

            if (onComplete != null) {
                MAIN_HANDLER.postDelayed(onComplete, 950);
            }
        });
    }

    /**
     * Show success checkmark for a given duration, then fade out the animation view.
     */
    public static void showSuccessThenHide(HenryLottieAnimationView animView, long displayDurationMs, Runnable onHidden) {
        if (animView == null) {
            if (onHidden != null) MAIN_HANDLER.post(onHidden);
            return;
        }
        MAIN_HANDLER.post(() -> {
            animView.setVisibility(View.VISIBLE);
            animView.setAlpha(1f);
            animView.playSuccessCheck();

            MAIN_HANDLER.postDelayed(() -> {
                animView.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                animView.stopAndReset();
                                animView.setVisibility(View.GONE);
                                if (onHidden != null) onHidden.run();
                            }
                        })
                        .start();
            }, Math.max(500, displayDurationMs));
        });
    }

    /**
     * Transition smoothly from loading spinner to success checkmark, then reveal content view.
     */
    public static void transitionLoadingToSuccess(HenryLottieAnimationView animView,
                                                  View contentToReveal,
                                                  long successDisplayMs,
                                                  Runnable onComplete) {
        if (animView == null) {
            if (contentToReveal != null) contentToReveal.setVisibility(View.VISIBLE);
            if (onComplete != null) MAIN_HANDLER.post(onComplete);
            return;
        }

        MAIN_HANDLER.post(() -> {
            animView.setVisibility(View.VISIBLE);
            animView.playSuccessCheck();

            MAIN_HANDLER.postDelayed(() -> {
                animView.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                animView.stopAndReset();
                                animView.setVisibility(View.GONE);

                                if (contentToReveal != null) {
                                    contentToReveal.setVisibility(View.VISIBLE);
                                    contentToReveal.setAlpha(0f);
                                    contentToReveal.animate().alpha(1f).setDuration(250).start();
                                }

                                if (onComplete != null) {
                                    onComplete.run();
                                }
                            }
                        })
                        .start();
            }, Math.max(600, successDisplayMs));
        });
    }

    /**
     * Toggle loading state: if true shows loading spinner, if false hides animation.
     */
    public static void toggleLoading(HenryLottieAnimationView animView, boolean isLoading) {
        if (animView == null) return;
        if (isLoading) {
            showLoading(animView);
        } else {
            reset(animView);
        }
    }

    /**
     * Toggle loading state with content view coordination.
     */
    public static void toggleLoading(HenryLottieAnimationView animView, View targetContent, boolean isLoading) {
        if (isLoading) {
            showLoading(animView, targetContent);
        } else {
            if (animView != null) reset(animView);
            if (targetContent != null) {
                targetContent.setVisibility(View.VISIBLE);
                targetContent.animate().alpha(1f).setDuration(200).start();
            }
        }
    }

    /**
     * Central state switcher for IDLE, LOADING, SUCCESS, and ERROR.
     */
    public static void toggleState(HenryLottieAnimationView animView, State state) {
        if (animView == null) return;
        MAIN_HANDLER.post(() -> {
            switch (state) {
                case LOADING:
                    showLoading(animView);
                    break;
                case SUCCESS:
                    showSuccess(animView, null);
                    break;
                case IDLE:
                case ERROR:
                default:
                    reset(animView);
                    break;
            }
        });
    }

    /**
     * Reset and hide animation view immediately.
     */
    public static void reset(HenryLottieAnimationView animView) {
        if (animView == null) return;
        MAIN_HANDLER.post(() -> {
            animView.stopAndReset();
            animView.setVisibility(View.GONE);
            animView.setAlpha(1f);
        });
    }
}
