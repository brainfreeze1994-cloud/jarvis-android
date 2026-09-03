package com.jarvis.ai;

import android.content.Context;
import java.util.List;

/**
 * HENRY AI Gateway API
 * Wraps network reasoning calls and provides automatic failover to HenryOfflineBrain
 * when network is unavailable or remote servers are unreachable.
 */
public class HenryApi {

    public interface Callback {
        void onSuccess(String reply, String imageUrl, List<String> followUps);
        void onError(String error);
    }

    public static void askV20(List<HistoryItem> history, String imageBase64,
                             String responseMode, UserProfile profile,
                             String queryType, Context memCtx,
                             String emotionState, String relationshipContext,
                             boolean enableTournament, boolean enableChainThinking,
                             Callback cb) {
        JarvisApi.askV20(history, imageBase64, responseMode, profile, queryType, memCtx,
                emotionState, relationshipContext, enableTournament, enableChainThinking,
                new JarvisApi.Callback() {
                    @Override
                    public void onSuccess(String reply, String imageUrl, List<String> followUps) {
                        cb.onSuccess(reply, imageUrl, followUps);
                    }

                    @Override
                    public void onError(String error) {
                        // If network resolution error, gracefully generate offline intelligence
                        if (error != null && (error.contains("Unable to resolve host") ||
                                              error.contains("Failed to connect") ||
                                              error.contains("Network error") ||
                                              error.contains("timeout"))) {
                            String lastQuery = "";
                            if (history != null && !history.isEmpty()) {
                                for (int i = history.size() - 1; i >= 0; i--) {
                                    if ("user".equalsIgnoreCase(history.get(i).role)) {
                                        lastQuery = history.get(i).text;
                                        break;
                                    }
                                }
                            }
                            String offlineReply = HenryOfflineBrain.generateOfflineResponse(lastQuery, queryType, memCtx);
                            cb.onSuccess(offlineReply, null, null);
                        } else {
                            cb.onError(error);
                        }
                    }
                });
    }

    public static String classifyIntent(String msg) {
        return JarvisApi.classifyIntent(msg);
    }
}
