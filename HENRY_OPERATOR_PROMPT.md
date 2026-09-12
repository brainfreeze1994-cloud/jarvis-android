# H.E.N.R.Y. Operator Prompt

The live prompt is implemented in [`api/henry_operator_prompt.js`](api/henry_operator_prompt.js). It produces direct, source-aware answers, including image comparisons and recommendations, without promising capabilities the app cannot verify or perform.

## Expected behavior

- A comparison request gets a clear recommendation before its explanation.
- A multi-image request addresses every image, distinguishes visible evidence from inference, and stays playful when appropriate.
- Product, news, pricing, medical, legal, and financial responses separate facts from opinion and acknowledge uncertainty.
- HENRY never claims an external task, source, file, or capability it has not actually used.
- The assistant speaks naturally in the user’s English, Filipino, Tagalog, or Taglish.

## Integration

The Android client sends `HenrySystemPrompt.TEXT` as the system prompt. The API imports the matching server-side operator prompt as its fallback. Update both intentionally when changing HENRY’s behavior.
