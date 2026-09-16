package com.jarvis.ai;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ViewModel responsible for user input state, attachment management,
 * multi-turn conversation history, and orchestrating communication with
 * the Gemini API pipeline (JarvisApi.askV20) with graceful web search
 * and offline-first fallback.
 */
public class ChatViewModel extends AndroidViewModel {

    private static final String PREFS = "jarvis_prefs";
    private static final String KEY_HIS = "history";
    private static final int MAX_HISTORY_ITEMS = 80;

    // ── Observable UI State ───────────────────────────────────────────────────
    private final MutableLiveData<List<Message>> _messages = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<Message>> messages = _messages;

    private final MutableLiveData<OrbView.OrbState> _orbState = new MutableLiveData<>(OrbView.OrbState.IDLE);
    public final LiveData<OrbView.OrbState> orbState = _orbState;

    private final MutableLiveData<String> _thinkingHint = new MutableLiveData<>("WHAT CAN I DO FOR YOU, SIR?");
    public final LiveData<String> thinkingHint = _thinkingHint;

    private final MutableLiveData<Boolean> _isTyping = new MutableLiveData<>(false);
    public final LiveData<Boolean> isTyping = _isTyping;

    private final MutableLiveData<Boolean> _isSending = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSending = _isSending;

    private final MutableLiveData<String> _inputText = new MutableLiveData<>("");
    public final LiveData<String> inputText = _inputText;

    private final MutableLiveData<AttachmentUiState> _attachmentState = new MutableLiveData<>(new AttachmentUiState());
    public final LiveData<AttachmentUiState> attachmentState = _attachmentState;

    private final MutableLiveData<List<String>> _followUpChips = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<String>> followUpChips = _followUpChips;

    private final MutableLiveData<String> _moodPhrase = new MutableLiveData<>();
    public final LiveData<String> moodPhrase = _moodPhrase;

    // One-shot events
    public final SingleLiveEvent<SpeechEvent> speechEvent = new SingleLiveEvent<>();
    public final SingleLiveEvent<String[]> transitActionsEvent = new SingleLiveEvent<>();
    public final SingleLiveEvent<UAELawHelper.LegalCategory> legalActionsEvent = new SingleLiveEvent<>();
    public final SingleLiveEvent<String> toastEvent = new SingleLiveEvent<>();
    public final SingleLiveEvent<Void> scrollToBottomEvent = new SingleLiveEvent<>();

    // ── Internal State ────────────────────────────────────────────────────────
    private final List<Message> internalMessages = new ArrayList<>();
    private final List<HistoryItem> history = new ArrayList<>();
    private int typingPos = -1;

    // Pending attachments
    private String pendingImageBase64;
    private String pendingImageUriStr;
    private final List<String> pendingImagesBase64 = new ArrayList<>();
    private final List<Uri> pendingImagesUris = new ArrayList<>();
    private final List<String> lastAnalyzedImagesBase64 = new ArrayList<>();
    private String pendingPdfText;
    private String pendingDocScanQuestion;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ioExecutor = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();
    private final ChatPersistenceRepository persistenceRepo;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.persistenceRepo = ChatPersistenceRepository.getInstance(application);
        updateMoodPhrase();
    }

    // ── Attachment State Model ────────────────────────────────────────────────
    public static class AttachmentUiState {
        public final boolean hasAttachment;
        public final int count;
        public final Uri previewUri;
        public final String label;
        public final String sublabel;
        public final String hint;
        public final boolean isPdf;

        public AttachmentUiState() {
            this.hasAttachment = false;
            this.count = 0;
            this.previewUri = null;
            this.label = "";
            this.sublabel = "";
            this.hint = "Command HENRY…";
            this.isPdf = false;
        }

        public AttachmentUiState(int count, Uri previewUri, String label, String sublabel, String hint, boolean isPdf) {
            this.hasAttachment = count > 0 || isPdf;
            this.count = count;
            this.previewUri = previewUri;
            this.label = label;
            this.sublabel = sublabel;
            this.hint = hint;
            this.isPdf = isPdf;
        }
    }

    // ── Speech Event Model ────────────────────────────────────────────────────
    public static class SpeechEvent {
        public final String text;
        public final String emotion;

        public SpeechEvent(String text, String emotion) {
            this.text = text;
            this.emotion = emotion;
        }
    }

    // ── Input State Operations ────────────────────────────────────────────────
    public void setInputText(String text) {
        _inputText.setValue(text);
    }

    public String getInputText() {
        return _inputText.getValue();
    }

    public void setPendingDocScanQuestion(String question) {
        this.pendingDocScanQuestion = question;
    }

    public String getPendingDocScanQuestion() {
        return pendingDocScanQuestion;
    }

    public void clearPendingDocScanQuestion() {
        this.pendingDocScanQuestion = null;
    }

    public boolean hasPendingAttachment() {
        return pendingImageBase64 != null || !pendingImagesBase64.isEmpty() || pendingPdfText != null;
    }

    public String getPendingPdfText() {
        return pendingPdfText;
    }

    public void setPdfAttachment(String pdfText, int pages) {
        this.pendingPdfText = pdfText;
        this.pendingImageBase64 = null;
        this.pendingImageUriStr = null;
        this.pendingImagesBase64.clear();
        this.pendingImagesUris.clear();

        AttachmentUiState state = new AttachmentUiState(
                0,
                null,
                "PDF Document attached (" + pages + " pages)",
                "Tap to manage or add attachments",
                "Ask about the PDF…",
                true
        );
        _attachmentState.setValue(state);
    }

    public void clearAttachment() {
        pendingImageBase64 = null;
        pendingImageUriStr = null;
        pendingImagesBase64.clear();
        pendingImagesUris.clear();
        pendingPdfText = null;
        _attachmentState.setValue(new AttachmentUiState());
    }

    public void encodeImageAsync(Uri uri) {
        if (uri == null) return;
        encodeImagesAsync(Collections.singletonList(uri), false);
    }

    public void encodeImagesAsync(List<Uri> uris, boolean append) {
        if (uris == null || uris.isEmpty()) return;

        ioExecutor.execute(() -> {
            Context ctx = getApplication().getApplicationContext();
            List<String> b64List = new ArrayList<>();
            List<Uri> validUris = new ArrayList<>();

            for (Uri uri : uris) {
                if (uri == null) continue;
                try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
                    if (is == null) continue;
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    if (bmp == null) continue;
                    int w = bmp.getWidth(), h = bmp.getHeight(), maxPx = 768;
                    if (w > maxPx || h > maxPx) {
                        float s = Math.min((float) maxPx / w, (float) maxPx / h);
                        bmp = Bitmap.createScaledBitmap(bmp, (int) (w * s), (int) (h * s), true);
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, 72, baos);
                    String b64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                    b64List.add(b64);
                    validUris.add(uri);
                } catch (Exception ignored) {}
            }

            mainHandler.post(() -> {
                if (b64List.isEmpty()) {
                    toastEvent.setValue("Failed to load selected image(s)");
                    return;
                }

                if (!append) {
                    pendingImagesBase64.clear();
                    pendingImagesUris.clear();
                }
                for (int i = 0; i < b64List.size() && pendingImagesBase64.size() < 10; i++) {
                    pendingImagesBase64.add(b64List.get(i));
                    pendingImagesUris.add(validUris.get(i));
                }

                if (!pendingImagesBase64.isEmpty()) {
                    pendingImageBase64 = pendingImagesBase64.get(0);
                    pendingImageUriStr = pendingImagesUris.get(0).toString();
                }
                pendingPdfText = null;

                int count = pendingImagesUris.size();
                Uri preview = count > 0 ? pendingImagesUris.get(0) : null;
                String label = count == 1 ? "1 image attached" : count + " images attached";
                String sublabel = "Tap to add more photos or PDF";
                String hint = count > 1 ? "Ask about these " + count + " attachments…" : "Ask about this image…";

                _attachmentState.setValue(new AttachmentUiState(count, preview, label, sublabel, hint, false));

                if (count > 1) {
                    toastEvent.setValue(count + " attachments ready for multi-attachment analysis.");
                } else {
                    toastEvent.setValue("Image attached.");
                }
            });
        });
    }

    // ── History & Persistence Operations ──────────────────────────────────────
    public List<HistoryItem> getHistory() {
        return new ArrayList<>(history);
    }

    public void loadHistory(Runnable onLoaded) {
        persistenceRepo.loadHistory(MAX_HISTORY_ITEMS, (historyItems, messageList) -> {
            if (historyItems != null && !historyItems.isEmpty()) {
                history.clear();
                history.addAll(historyItems);
                internalMessages.clear();
                internalMessages.addAll(messageList);
                _messages.setValue(new ArrayList<>(internalMessages));
                scrollToBottomEvent.call();
                if (onLoaded != null) onLoaded.run();
            } else {
                // Fallback to legacy SharedPreferences if Room was uninitialized
                SharedPreferences prefs = getApplication().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                String json = prefs.getString(KEY_HIS, null);
                if (json != null && !json.isEmpty()) {
                    try {
                        Type type = new TypeToken<List<HistoryItem>>() {}.getType();
                        List<HistoryItem> saved = gson.fromJson(json, type);
                        if (saved != null) {
                            history.clear();
                            history.addAll(saved);
                            internalMessages.clear();
                            List<HistoryItem> vis = saved.size() > 20
                                    ? saved.subList(saved.size() - 20, saved.size()) : saved;
                            for (HistoryItem item : vis) {
                                internalMessages.add(new Message(
                                        "user".equals(item.role) ? Message.TYPE_USER : Message.TYPE_JARVIS, item.text));
                            }
                            _messages.setValue(new ArrayList<>(internalMessages));
                            scrollToBottomEvent.call();
                        }
                    } catch (Exception ignored) {}
                }
                if (onLoaded != null) onLoaded.run();
            }
        });
    }

    public void saveHistory() {
        List<HistoryItem> toSave = history.size() > MAX_HISTORY_ITEMS
                ? history.subList(history.size() - MAX_HISTORY_ITEMS, history.size()) : history;
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HIS, gson.toJson(toSave)).apply();

        // Also persist to Room Database asynchronously
        try {
            if (!history.isEmpty()) {
                HistoryItem last = history.get(history.size() - 1);
                persistenceRepo.saveMessage(
                        new Message("user".equals(last.role) ? Message.TYPE_USER : Message.TYPE_JARVIS, last.text),
                        last.role,
                        false
                );
            }
        } catch (Exception ignored) {}
    }

    public void clearChat() {
        history.clear();
        internalMessages.clear();
        _messages.setValue(new ArrayList<>());
        clearAttachment();

        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HIS).apply();
        persistenceRepo.clearAll(null);
    }

    // ── Message Manipulation ──────────────────────────────────────────────────
    public void addUserMessage(String text) {
        String clean = stripEmotionTag(text);
        if (clean.isEmpty()) return;
        internalMessages.add(new Message(Message.TYPE_USER, clean));
        _messages.setValue(new ArrayList<>(internalMessages));
        scrollToBottomEvent.call();
    }

    public void addJarvisMessage(String text) {
        String clean = stripEmotionTag(text);
        if (clean.isEmpty()) return;
        internalMessages.add(new Message(Message.TYPE_JARVIS, clean));
        _messages.setValue(new ArrayList<>(internalMessages));
        scrollToBottomEvent.call();
    }

    public void addUrlImageMessage(String text, String imageUrl) {
        String clean = stripEmotionTag(text);
        internalMessages.add(new Message(Message.TYPE_URL_IMAGE, clean, null, imageUrl));
        _messages.setValue(new ArrayList<>(internalMessages));
        scrollToBottomEvent.call();
    }

    public void addHistoryItem(String role, String text) {
        history.add(new HistoryItem(role, stripEmotionTag(text)));
        saveHistory();
    }

    public void setOrbState(OrbView.OrbState state) {
        _orbState.setValue(state);
    }

    public void setThinkingHint(String hint) {
        _thinkingHint.setValue(hint != null ? hint.toUpperCase(Locale.US) : "THINKING…");
    }

    public void showTyping(String intentType) {
        String hint;
        switch (intentType != null ? intentType : "chat") {
            case "search":        hint = "Searching the web…";                  break;
            case "news":          hint = "Fetching latest news…";               break;
            case "crypto":        hint = "Checking live prices…";               break;
            case "forex":         hint = "Getting exchange rates…";             break;
            case "math":          hint = "Calculating…";                        break;
            case "vision":        hint = "Analysing image…";                    break;
            case "multi_vision":  hint = "Synthesizing multiple attachments…";  break;
            case "cybersecurity": hint = "Analyzing security architecture…";    break;
            case "finance":       hint = "Modeling financial metrics…";         break;
            case "medical":       hint = "Reviewing clinical literature…";      break;
            case "witty":         hint = "Calibrating wit & comebacks…";        break;
            case "roast":         hint = "Generating witty roast…";             break;
            case "reason":        hint = "Thinking step by step…";              break;
            case "transit":       hint = "Planning your route…";                break;
            case "legal":         hint = "Checking UAE law…";                   break;
            default:              hint = "Thinking…";                           break;
        }
        setThinkingHint(hint);
        _isTyping.setValue(true);

        // Add typing placeholder item if not already present
        if (typingPos < 0 || typingPos >= internalMessages.size()
                || internalMessages.get(typingPos).type != Message.TYPE_TYPING) {
            internalMessages.add(new Message(Message.TYPE_TYPING, ""));
            typingPos = internalMessages.size() - 1;
            _messages.setValue(new ArrayList<>(internalMessages));
            scrollToBottomEvent.call();
        }
    }

    public void hideTyping() {
        _isTyping.setValue(false);
        setThinkingHint("WHAT CAN I DO FOR YOU, SIR?");
        if (typingPos >= 0 && typingPos < internalMessages.size()) {
            internalMessages.remove(typingPos);
            typingPos = -1;
            _messages.setValue(new ArrayList<>(internalMessages));
        }
    }

    public void updateMoodPhrase() {
        HenryMood.Mood mood = HenryMood.getCurrentMood();
        _moodPhrase.setValue(HenryMood.getStatusPhrase(mood));
    }

    // ── Core Gemini Chat Pipeline Orchestration ───────────────────────────────
    public void executeChatPipeline(
            @NonNull String userText,
            @NonNull String responseMode,
            @NonNull UserProfile userProfile
    ) {
        if (userText.trim().isEmpty()) return;

        final String cleanUserText = userText.trim();
        _isSending.setValue(true);

        // 1. Add user message to UI and history
        addUserMessage(cleanUserText);
        history.add(new HistoryItem("user", cleanUserText));

        // 2. Classify intent
        final boolean isTransit = TransitHelper.isTransitQuery(cleanUserText);
        final boolean isLegal = UAELawHelper.isLegalQuery(cleanUserText);
        final String[] transitRoute = isTransit ? TransitHelper.extractRoute(cleanUserText) : null;
        final UAELawHelper.LegalCategory legalCat = isLegal ? UAELawHelper.classify(cleanUserText) : null;

        String intentType = (pendingImageBase64 != null) ? "vision"
                : isTransit ? "transit"
                : isLegal ? "legal"
                : JarvisApi.classifyIntent(cleanUserText);

        setOrbState(OrbView.OrbState.THINKING);
        showTyping(intentType);

        // 3. Prepare attachments
        String imageB64 = pendingImageBase64;
        List<String> imagesB64 = new ArrayList<>(pendingImagesBase64);
        clearAttachment();

        if (!imagesB64.isEmpty()) {
            lastAnalyzedImagesBase64.clear();
            lastAnalyzedImagesBase64.addAll(imagesB64);
        } else if (imageB64 != null && !imageB64.isEmpty()) {
            lastAnalyzedImagesBase64.clear();
            lastAnalyzedImagesBase64.add(imageB64);
        } else if (!lastAnalyzedImagesBase64.isEmpty()) {
            // Check follow-up queries about previously analyzed images (e.g. 'What kind of phone is that?', 'Sino ang pipiliin mo sa tatlo?')
            String low = cleanUserText.toLowerCase(Locale.US);
            if (low.matches(".*(sino|pipiliin|which|tatlo|three|first|second|third|picture|photo|image|suit|jacket|wall|guy|man|girl|compare|them|both|these|those|who|alin|pili|kanila|ano masasabi|phone|device|model|brand|that|this|it|screen|display|what kind|what is|tell me more|details|specs|look|see|holding|hand|background|color|price|foldable|folding|camera|read|text|zoom).*")
                || (history.size() >= 2 && !low.matches(".*(timer|alarm|weather|calculate|waze|map|search|news|crypto|stock).*"))) {
                imagesB64 = new ArrayList<>(lastAnalyzedImagesBase64);
                if (imageB64 == null && !imagesB64.isEmpty()) imageB64 = imagesB64.get(0);
                intentType = "vision";
            }
        }

        // 4. Multi-turn History
        List<HistoryItem> apiHistory = new ArrayList<>(history);

        // 5. Memory and Relationship context
        Context appContext = getApplication().getApplicationContext();
        SmartMemory.learnFromMessage(appContext, cleanUserText);
        String relCtx = RelationshipBrain.buildContext(appContext);

        boolean useTournament = isImportantQuery(cleanUserText);
        boolean useChain = isDeepReasoningQuery(cleanUserText);

        final String offlineQueryText = cleanUserText;
        final String offlineQueryIntent = intentType;

        // 6. Gemini API Execution
        JarvisApi.askV20(
                apiHistory,
                imageB64,
                imagesB64,
                responseMode,
                userProfile,
                intentType,
                appContext,
                "neutral",
                relCtx.isEmpty() ? null : relCtx,
                useTournament,
                useChain,
                new JarvisApi.Callback() {
                    @Override
                    public void onSuccess(String reply, String imageUrl, List<String> followUps) {
                        mainHandler.post(() -> {
                            hideTyping();
                            _isSending.setValue(false);
                            String cleanReply = stripEmotionTag(reply);

                            // Fallback: If model returned an unverified refusal for a web search query
                            if (HenryWebSearch.isRefusal(cleanReply) && HenryWebSearch.isSearchQuery(cleanUserText)) {
                                showTyping("search");
                                HenryWebSearch.search(cleanUserText, new HenryWebSearch.SearchCallback() {
                                    @Override
                                    public void onSearchResult(String summary, String source, List<String> sources) {
                                        mainHandler.post(() -> {
                                            hideTyping();
                                            String verifiedAnswer = stripEmotionTag(summary);
                                            history.add(new HistoryItem("model", verifiedAnswer));
                                            addJarvisMessage(verifiedAnswer);
                                            speechEvent.setValue(new SpeechEvent(verifiedAnswer, "informative"));
                                            saveHistory();
                                            updateMoodPhrase();
                                            setOrbState(OrbView.OrbState.IDLE);
                                        });
                                    }

                                    @Override
                                    public void onError(String reason) {
                                        mainHandler.post(() -> {
                                            hideTyping();
                                            String fallback = stripEmotionTag(
                                                    HenryOfflineBrain.generateOfflineResponse(cleanUserText, offlineQueryIntent, appContext)
                                            );
                                            history.add(new HistoryItem("model", fallback));
                                            addJarvisMessage(fallback);
                                            speechEvent.setValue(new SpeechEvent(fallback, "neutral"));
                                            saveHistory();
                                            updateMoodPhrase();
                                            setOrbState(OrbView.OrbState.IDLE);
                                        });
                                    }
                                });
                                return;
                            }

                            String emotion = extractEmotion(reply);
                            history.add(new HistoryItem("model", cleanReply));

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                addUrlImageMessage(cleanReply, imageUrl);
                                speechEvent.setValue(new SpeechEvent("Here is your generated image, sir.", "proud"));
                            } else {
                                String toShow = (!cleanReply.trim().isEmpty())
                                        ? cleanReply
                                        : "I am right here, sir. How may I assist you?";
                                addJarvisMessage(toShow);
                                speechEvent.setValue(new SpeechEvent(toShow, emotion));
                            }

                            // Transit & Legal Actions
                            if (isTransit && transitRoute != null) {
                                transitActionsEvent.setValue(transitRoute);
                            }
                            if (isLegal && legalCat != null) {
                                legalActionsEvent.setValue(legalCat);
                            }

                            // Follow-up chips
                            if (followUps != null && !followUps.isEmpty()) {
                                _followUpChips.setValue(followUps);
                            }

                            // Cache AI response asynchronously
                            persistenceRepo.cacheAiResponse(
                                    offlineQueryText,
                                    cleanReply,
                                    emotion,
                                    offlineQueryIntent,
                                    imageUrl
                            );

                            saveHistory();
                            updateMoodPhrase();
                            setOrbState(OrbView.OrbState.IDLE);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> {
                            hideTyping();
                            _isSending.setValue(false);

                            ioExecutor.execute(() -> {
                                AiResponseCacheEntity cached = persistenceRepo.lookupCachedResponseSync(offlineQueryText);
                                String offlineReply;
                                if (cached != null && cached.responseText != null && !cached.responseText.isEmpty()) {
                                    offlineReply = cached.responseText;
                                } else {
                                    offlineReply = HenryOfflineBrain.generateOfflineResponse(
                                            offlineQueryText,
                                            offlineQueryIntent,
                                            appContext
                                    );
                                }

                                String emotion = extractEmotion(offlineReply);
                                String cleanReply = stripEmotionTag(offlineReply);

                                mainHandler.post(() -> {
                                    history.add(new HistoryItem("model", cleanReply));
                                    addJarvisMessage(cleanReply);
                                    speechEvent.setValue(new SpeechEvent(cleanReply, emotion));
                                    saveHistory();
                                    updateMoodPhrase();
                                    setOrbState(OrbView.OrbState.IDLE);
                                });
                            });
                        });
                    }
                }
        );
    }

    // ── Helper Utility Methods ────────────────────────────────────────────────
    public static String stripEmotionTag(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\[emotion:[^\\]]*\\]\\s*", "")
                   .replaceAll("(?i)\\[emotion[^\\]]*\\]\\s*", "")
                   .replaceAll("(?i)^\\[(neutral|warm|concerned|excited|amused|serious|proud|playful|curious|thoughtful|empathetic|sarcastic|surprised|determined|friendly|happy|sad|analytical|mysterious|reverent|witty)\\]\\s*", "")
                   .trim();
    }

    public static String extractEmotion(String text) {
        if (text == null) return "neutral";
        Matcher m = Pattern.compile("(?i)\\[(?:emotion:)?(neutral|warm|concerned|excited|amused|serious|proud|playful|curious|thoughtful|empathetic|sarcastic|surprised|determined|friendly|happy|sad|analytical|mysterious|reverent|witty)\\]").matcher(text);
        if (m.find()) return m.group(1).trim().toLowerCase(Locale.US);
        return "neutral";
    }

    private boolean isImportantQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);
        return t.matches(".*\\b(should i invest|is it safe|medical|diagnosis|symptoms|treatment|doctor|hospital|legal advice|court|financial decision|mortgage|loan|insurance|life-changing|career|should i quit|should i move)\\b.*");
    }

    private boolean isDeepReasoningQuery(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.US);
        return t.matches(".*\\b(explain deeply|step by step|walk me through|break it down|analyse|analyze in detail|compare thoroughly|pros and cons of|help me understand|how exactly does)\\b.*")
                && text.length() > 40;
    }
}
