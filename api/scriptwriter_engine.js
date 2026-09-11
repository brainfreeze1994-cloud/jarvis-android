// ============================================================
// H.E.N.R.Y. 2.0 ULTRA — SCRIPTWRITER & NARRATIVE ENGINE
// Loglines, Character Bibles, Three-Act / Save the Cat Structures,
// Screenplay Formatter, YouTube Scripts, Documentaries, Script QC
// ============================================================

/**
 * Generates compelling loglines adhering to:
 * "When [PROTAGONIST] must [GOAL], they must overcome [OBSTACLE] before [STAKES], or [CONSEQUENCE]."
 */
function generateLoglines(prompt, genre = 'Drama', count = 3) {
  const genres = {
    SciFi: { stakes: 'before the orbital tether collapses and suffocates three million colonists', obstacle: 'a rogue planetary terraforming algorithm' },
    Horror: { stakes: 'before midnight or become permanent reflections in the antique mirror', obstacle: 'a shapeshifting parasitic entity mimicking lost family members' },
    Thriller: { stakes: 'before the encrypted ledger goes live on the darknet', obstacle: 'an elite private military task force' },
    Comedy: { stakes: 'before the catastrophic live broadcast ruins their catering business', obstacle: 'an eccentric celebrity chef who lost his sense of taste' },
    Documentary: { stakes: 'before industrial silence erases the last living dialect', obstacle: 'a century of bureaucratic denial and rapid urban migration' },
    Drama: { stakes: 'before the family estate is seized by predatory debt collectors', obstacle: 'deep-seated generational secrets and estranged siblings' }
  };

  const selectedGenre = genres[genre] || genres.Drama;
  const list = [];

  for (let i = 1; i <= count; i++) {
    const variation = i === 1 ? 'High-Concept' : i === 2 ? 'Character-Driven' : 'Dark & Subversive';
    list.push({
      id: `LOGLINE_${i}`,
      conceptType: variation,
      logline: `When an overlooked specialist is forced to resolve ${prompt || 'an unprecedented crisis'}, they must overcome ${selectedGenre.obstacle} before ${selectedGenre.stakes}.`,
      protagonist: `Determined specialist facing overwhelming odds`,
      goal: `Resolve the crisis and uncover the underlying truth`,
      antagonist: selectedGenre.obstacle,
      stakes: selectedGenre.stakes,
      uniqueHook: `A fresh, claustrophobic visual perspective blending grounded realism with relentless ticking-clock tension.`
    });
  }

  return list;
}

/**
 * Character Engine: Creates complete character profiles & arcs
 */
function createCharacterProfile(name, role = 'Protagonist', archetype = 'Reluctant Expert') {
  return {
    name,
    role,
    archetype,
    age: 32,
    personality: 'Hyper-observant, guarded, dry sense of humor, deeply empathetic beneath cynical exterior',
    goal: 'Restore balance and protect their fragile found family',
    motivation: 'A lingering guilt from an unresolved error three years prior',
    fear: 'Irreversible failure while others are depending on them',
    flaw: 'Reluctant to ask for help; hyper-fixates on micro-details',
    strength: 'Exceptional pattern recognition under extreme adrenaline',
    secret: 'Carries an encrypted physical drive containing the original telemetry logs',
    arc: {
      beginningState: 'Isolated, risk-averse, functioning purely on muscle memory and regret',
      endingState: 'Transformed into a decisive, vulnerable leader willing to trust collective solidarity'
    },
    voiceIdentity: {
      tone: 'Crisp, measured, low-register cadence',
      speed: '145 words per minute',
      mannerism: 'Pauses before answering; uses precise technical metaphors'
    }
  };
}

/**
 * Estimate narration words from duration (minutes) & WPM
 */
function calculateNarrationWordCount(durationMinutes, wpm = 145) {
  const targetWords = Math.round(durationMinutes * wpm);
  return {
    durationMinutes,
    wpm,
    targetWords,
    minWords: Math.round(targetWords * 0.9),
    maxWords: Math.round(targetWords * 1.1)
  };
}

/**
 * Three-Act Structure Pipeline
 */
function buildThreeActStructure(title, premise, characters) {
  return {
    title: title || 'Untitled Narrative Project',
    premise,
    characters,
    acts: {
      act1: {
        title: 'ACT I: SETUP',
        scenes: [
          { number: 1, slugline: 'INT. WORKSHOP - NIGHT', purpose: 'Opening Image: Establish normal world, flaws, and isolation.' },
          { number: 2, slugline: 'INT. CORRIDOR - DAY', purpose: 'Inciting Incident: The anomalous message arrives; the routine shatters.' },
          { number: 3, slugline: 'EXT. PERIMETER - DUSK', purpose: 'Plot Point I: Protagonist makes irrevocable choice to cross the threshold.' }
        ]
      },
      act2: {
        title: 'ACT II: CONFRONTATION & ESCALATION',
        scenes: [
          { number: 4, slugline: 'INT. COMMAND CENTER - CONTINUOUS', purpose: 'Rising Action: First encounter with antagonist; stakes double.' },
          { number: 5, slugline: 'INT. TRANSIT TUNNEL - NIGHT', purpose: 'Midpoint: False victory turns into devastating revelation of true scope.' },
          { number: 6, slugline: 'EXT. WASTELAND - DAWN', purpose: 'All Hope is Lost / Dark Night of the Soul: The internal flaw causes collapse.' },
          { number: 7, slugline: 'INT. SAFE HOUSE - NIGHT', purpose: 'Plot Point II: Epiphany; protagonist embraces the transformed worldview.' }
        ]
      },
      act3: {
        title: 'ACT III: RESOLUTION',
        scenes: [
          { number: 8, slugline: 'INT. THE VAULT - NIGHT', purpose: 'Climax: High-stakes confrontation tests protagonist’s ultimate growth.' },
          { number: 9, slugline: 'EXT. THE HORIZON - DAY', purpose: 'Final Image: Mirroring scene 1, demonstrating permanent character transformation.' }
        ]
      }
    }
  };
}

/**
 * YouTube Script Engine: Hook, Promise, Context, Escalation, Payoff, CTA
 */
function buildYouTubeScript(topic, targetMinutes = 5) {
  const wordsConfig = calculateNarrationWordCount(targetMinutes);

  return {
    format: 'YOUTUBE_PRODUCTION_SCRIPT',
    topic,
    targetDuration: `${targetMinutes} minutes (${wordsConfig.targetWords} estimated words)`,
    sections: [
      {
        section: 'HOOK',
        durationSec: 15,
        visual: 'Fast-paced kinetic montage; sharp sound design; high-contrast macro visuals.',
        narration: `Within the next 60 seconds, everything you thought you knew about ${topic} is going to change permanently.`
      },
      {
        section: 'PROMISE & PREVIEW',
        durationSec: 30,
        visual: 'On-screen graphic timeline; split-screen comparative data overlay.',
        narration: `By the end of this breakdown, you will understand the exact mechanism behind this phenomenon—and why it matters right now.`
      },
      {
        section: 'CONTEXT & THE HIDDEN TRUTH',
        durationSec: 60,
        visual: 'Archival footage and clean infographic motion graphics.',
        narration: `To see how we arrived here, we have to look back at the critical mistake made when this was first introduced...`
      },
      {
        section: 'CORE ESCALATION & CASE STUDY',
        durationSec: 120,
        visual: 'Dynamic screen recordings, side-by-side performance benchmarks, detailed diagram highlights.',
        narration: `Notice what happens when we stress-test this premise under real-world conditions. The failure mode isn't subtle; it's systemic.`
      },
      {
        section: 'THE PAYOFF & ACTIONABLE TAKEAWAY',
        durationSec: 60,
        visual: 'Crisp summary card; key principles animated sequentially with checkmarks.',
        narration: `Here is the golden rule you can apply immediately to eliminate this bottleneck in your own workflow.`
      },
      {
        section: 'CALL TO ACTION (CTA)',
        durationSec: 15,
        visual: 'Subtle interactive prompt; end-screen cards linking to deep-dive follow-up.',
        narration: `If this breakdown unlocked a new perspective for you, leave your thoughts below, and explore our full analytical masterclass linked right here.`
      }
    ]
  };
}

/**
 * Screenplay Formatter: Generates standardized screenplay layout
 */
function formatScreenplay(scenes) {
  let output = 'FADE IN:\n\n';

  for (const sc of scenes) {
    output += `${sc.slugline || 'INT. UNKNOWN - DAY'}\n\n`;
    if (sc.action) {
      output += `${sc.action}\n\n`;
    }
    if (sc.dialogues && Array.isArray(sc.dialogues)) {
      for (const d of sc.dialogues) {
        output += `                ${(d.character || 'CHARACTER').toUpperCase()}\n`;
        if (d.parenthetical) {
          output += `            (${d.parenthetical})\n`;
        }
        output += `        ${d.lines || ''}\n\n`;
      }
    }
    output += `                                                            CUT TO:\n\n`;
  }

  output += 'FADE OUT.\n';
  return output;
}

/**
 * Script Quality Control (QC) Analyzer
 */
function evaluateScriptQuality(scriptText, options = {}) {
  const text = scriptText || '';
  const wordCount = text.split(/\s+/).filter(Boolean).length;
  const sceneCount = (text.match(/INT\.|EXT\./g) || []).length || 1;
  const dialogueCount = (text.match(/[A-Z]{3,}\s*\n\s*[A-Za-z]/g) || []).length;

  const checks = [
    { name: 'Slugline Formatting', passed: /INT\.|EXT\./.test(text), weight: 15, note: 'Proper industry standard scene headings' },
    { name: 'Show Don’t Tell Ratio', passed: !/\b(he felt very sad|she was thinking about)\b/i.test(text), weight: 20, note: 'Visual behavior over pure internal narration' },
    { name: 'Dialogue Naturalness', passed: dialogueCount > 0, weight: 20, note: 'Engaging character speech cues with subtext' },
    { name: 'Narrative Conflict & Stakes', passed: /\b(must|before|consequence|threat|risk|deadly|critical|clock)\b/i.test(text), weight: 25, note: 'Explicit stakes driving protagonist action' },
    { name: 'Pacing & Density', passed: wordCount >= 80, weight: 20, note: 'Sufficient scene substance and visual progression' }
  ];

  let score = 0;
  for (const c of checks) {
    if (c.passed) score += c.weight;
  }

  return {
    qualityScore: score,
    status: score >= 85 ? 'EXCELLENT' : score >= 65 ? 'GOOD' : 'NEEDS_REVISION',
    wordCount,
    sceneCount,
    dialogueBeats: dialogueCount,
    checks,
    recommendations: score < 85 ? [
      'Sharpen visual action verbs in slugline descriptions',
      'Increase character subtext to reduce exposition'
    ] : ['Script meets high professional narrative standards ready for table read or storyboard!']
  };
}

module.exports = {
  generateLoglines,
  createCharacterProfile,
  calculateNarrationWordCount,
  buildThreeActStructure,
  buildYouTubeScript,
  formatScreenplay,
  evaluateScriptQuality
};
