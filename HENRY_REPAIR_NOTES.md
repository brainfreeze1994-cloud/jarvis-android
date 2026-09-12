# HENRY Repair Notes

This archive is an incremental repair of the existing Android project.

## Changes
- Math requests now default to STEP_BY_STEP unless the user explicitly requests QUICK.
- Math UI path uses `fullExplanation` whenever available.
- Witty detection now recognizes natural combinations of fish/thorn/dating terms instead of treating a regex-like string as a literal `contains()` pattern.
- ChemistryActivity is declared in AndroidManifest using the class that BrainActivity explicitly launches.
- System Diagnostic math checks require explanation text as well as correct numeric answers.
- Video audit is explicitly scoped to planning/storyboard verification and does not claim that a rendered video exists.

## Important limitation
The existing project does not contain a configured real AI video-generation provider. This archive does not fake one. A real provider/API must be configured to generate actual clips and final videos.
