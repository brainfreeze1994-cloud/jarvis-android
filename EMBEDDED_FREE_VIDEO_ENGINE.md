# HENRY Embedded Free Video Engine

This version renders MP4 video directly inside the Android app.

- No Python server
- No FFmpeg installation
- No Gemini/Veo
- No API key
- No paid video service
- Uses Android `MediaCodec` + `MediaMuxer`
- Produces a real H.264 MP4 file locally
- Procedural cinematic motion is generated from the user prompt

## Important
This is a **procedural local video renderer**, not a diffusion/generative-AI video model. A true generative model would require model weights and substantial device resources.

The procedural mode now uses scene-based animation rather than a title-card template: no grid, timeline, HENRY label, or topic banner is rendered into the MP4. Underwater prompts such as `Create a 30 second video of fish swimming` render an underwater scene with fish, bubbles, light rays, and swaying sea grass.

For photorealistic AI video, connect HENRY to a genuine local or hosted video-generation model. That capability is separate from the embedded renderer and must not be represented as available until the model is installed and configured.

The renderer defaults to a lightweight 640x360 landscape or 360x640 portrait pipeline at 12 fps, while generating unique motion frames at 6 fps to keep free on-device rendering practical.

Duration commands preserve their unit: `Create a 30 second video of space exploration` renders a 30-second MP4, while `Create a 2 minute video of space exploration` renders two minutes.

No setup is required beyond installing the HENRY APK.
