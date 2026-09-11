// ============================================================
// H.E.N.R.Y. 2.0 ULTRA — WITTY INTELLIGENCE ENGINE
// Natural, Contextual, Quick, Clever, Playful Humor Reasoning Layer
// Architecture:
// USER MESSAGE
// ↓ CONTEXT UNDERSTANDING
// ↓ INTENT & TONE DETECTION
// ↓ WORD/PHRASE ANALYSIS & DOUBLE-MEANING DETECTION
// ↓ ASSOCIATION ENGINE (Semantic Web)
// ↓ CONTRAST ENGINE (Expectation vs Unexpected Reality)
// ↓ MISDIRECTION ENGINE (Setup -> Switch -> Punchline)
// ↓ MULTI-CANDIDATE GENERATOR & SCORER
// ↓ WIT QUALITY CHECK & SAFETY/SERIOUSNESS FILTER
// ↓ FINAL WITTY RESPONSE
// ============================================================

/**
 * Wit Intensity Levels
 */
const WIT_INTENSITY = {
  OFF: 'OFF',
  LIGHT: 'LIGHT',
  NORMAL: 'NORMAL',
  HIGH: 'HIGH',
  SAVAGE: 'SAVAGE',
  BARDAGULAN: 'BARDAGULAN',
  BRUTAL: 'BRUTAL'
};

/**
 * Wit Personality Profiles
 */
const WIT_PERSONALITY = {
  CLEVER: 'CLEVER',
  PLAYFUL: 'PLAYFUL',
  SARCASTIC: 'SARCASTIC',
  DRY: 'DRY',
  CHAOTIC: 'CHAOTIC',
  CAMP: 'CAMP',
  SAVAGE: 'SAVAGE',
  DEADPAN: 'DEADPAN',
  ABSURD: 'ABSURD',
  BAKLA: 'BAKLA',
  BARDAGULAN: 'BARDAGULAN',
  FRIENDLY_TEASING: 'FRIENDLY_TEASING'
};

/**
 * Double Meaning Lexicon & Semantic Collisions
 * Words with dual literal & figurative meanings in Tagalog, Taglish & English
 */
const DOUBLE_MEANINGS = {
  matinik: {
    meanings: ['experienced/sharp/selective in dating', 'fish full of sharp bones (tinik)'],
    associations: ['bangus', 'isda', 'seafood', 'himay', 'warning signs', 'tinik sa lalamunan'],
    collidingDomains: ['dating', 'seafood market', 'hospital ENT / tinik']
  },
  malalim: {
    meanings: ['philosophically profound/deep thinker', 'literally deep water/abyss/drowning risk'],
    associations: ['karagatan', 'lunod', 'overthinking', 'salbabida', 'swimming pool'],
    collidingDomains: ['intellect', 'drowning/aquatic safety']
  },
  mabigat: {
    meanings: ['emotionally heavy/serious problem', 'literally heavy physical payload/kilo'],
    associations: ['timbangan', 'gym', 'buhat', 'baggage', 'overweight baggage fee'],
    collidingDomains: ['emotions', 'baggage counter']
  },
  bitin: {
    meanings: ['cliffhanger/unfulfilled desire/unsatisfied', 'literally hanging in mid-air/too short pants'],
    associations: ['pantaloon', 'sampayan', 'teleserye', 'season finale', 'short budget'],
    collidingDomains: ['relationships', 'short curtains / clothes']
  },
  pasok: {
    meanings: ['qualified/accepted into standards or school', 'physically entered inside a room or door'],
    associations: ['pinto', 'gate', 'exam result', 'swipe card', 'attendance'],
    collidingDomains: ['standards', 'turnstile / bouncer']
  },
  labas: {
    meanings: ['irrelevant/outside the topic/excluded', 'physically outdoors/exit'],
    associations: ['exit door', 'tabi', 'layas', 'out of bounds'],
    collidingDomains: ['arguments', 'emergency exit']
  },
  ligaw: {
    meanings: ['courting / wooing a romantic interest', 'literally lost / stray animal / wandered off'],
    associations: ['waze', 'google maps', 'stray cat', 'lost in BGC', 'no signal'],
    collidingDomains: ['courtship', 'GPS navigation']
  },
  huli: {
    meanings: ['caught red-handed / cheating / lying', 'literally caught fish / arrest / tardy latecomer'],
    associations: ['MMDA', 'pulis', 'chismis', 'late slip', 'fish net'],
    collidingDomains: ['infidelity', 'traffic violation / fishing']
  },
  palo: {
    meanings: ['disciplinary strike / parental punishment', 'baseball / volleyball spike'],
    associations: ['tsinelas', 'hanger', 'volleyball', 'spiker', 'mama'],
    collidingDomains: ['discipline', 'sports']
  },
  bagsak: {
    meanings: ['failed an exam or standard', 'literally collapsed / crashed on bed / dropped item'],
    associations: ['transcript', 'tulog mantika', 'dropped phone', 'gravity', 'retake'],
    collidingDomains: ['academics', 'gravity / sleep']
  },
  single: {
    meanings: ['unpartnered in romance', 'one-way ticket / single slice / individual room'],
    associations: ['tax return', 'solo flight', 'solo seat', 'monologue', 'independent'],
    collidingDomains: ['romance', 'inventory / cinema tickets']
  },
  toxic: {
    meanings: ['emotionally manipulative / red flag person', 'literally hazardous biochemical waste'],
    associations: ['hazmat suit', 'radiation', 'poison control', 'mercury', 'chernobyl'],
    collidingDomains: ['dating', 'hazardous material protocol']
  },
  green: {
    meanings: ['green flag / healthy qualities', 'green tea / eco-friendly / matcha / unripe fruit'],
    associations: ['matcha latte', 'solar panel', 'vegetables', 'plants', 'unripe banana'],
    collidingDomains: ['relationships', 'agriculture / organic food']
  },
  ghost: {
    meanings: ['suddenly stopped replying / disappearing', 'literal paranormal entity / Casper'],
    associations: ['multo', 'haunted house', 'paranormal', 'ouija board', 'white lady'],
    collidingDomains: ['dating', 'halloween / horror movies']
  },
  bangus: {
    meanings: ['milkfish / national fish with countless bones'],
    associations: ['tinik', 'himay', 'dagupan', 'prito', 'sinigang', 'seafood warning', 'calamansi'],
    collidingDomains: ['seafood', 'dating warning signs']
  },
  redflag: {
    meanings: ['warning sign in partner', 'literal danger flag at beach / racing penalty flag'],
    associations: ['formula 1', 'lifeguard', 'typhoon signal', 'parade', 'stop light'],
    collidingDomains: ['romance', 'motorsport / beach patrol']
  }
};

/**
 * Filipino Humor Markers & Slang
 */
const FILIPINO_WIT_MARKERS = {
  greetings: ['Beh', 'Girl', 'Teh', 'Ante', 'Hoy', 'Ate', 'Kuya', 'Boss'],
  reactions: ['😭', '💀', 'HAHAHA', 'Jusko', 'Kaloka', 'Grabe'],
  connectors: ['Wait lang—', 'Ang problema...', 'At least...', 'Medyo...', 'Honestly beh,']
};

/**
 * Seriousness topics that STRICTLY veto wit
 */
const HIGH_SERIOUSNESS_REGEX = /\b(suicide|kill myself|emergency|chest pain|stroke|overdose|trauma|abuse|funeral|died|passed away|cancer|tumor|lawsuit|evicted|arrested|grief)\b/i;

/**
 * 1. Semantic Analysis: Extract entities, double meanings & potential collisions
 */
function analyzeHumorPotential(userMsg) {
  const m = (userMsg || '').trim().toLowerCase();
  const detectedKeywords = [];
  const detectedCollisions = [];

  for (const [key, meta] of Object.entries(DOUBLE_MEANINGS)) {
    const reg = new RegExp(`\\b${key}\\b`, 'i');
    if (reg.test(m)) {
      detectedKeywords.push(key);
      detectedCollisions.push({
        keyword: key,
        meanings: meta.meanings,
        associations: meta.associations,
        collidingDomains: meta.collidingDomains
      });
    }
  }

  // Detect specific semantic collisions like "matinik" + "bangus" + "boys"
  const hasFishCollision = (m.includes('matinik') || m.includes('tinik')) &&
                          (m.includes('bangus') || m.includes('isda') || m.includes('fish'));
  const hasDatingCollision = m.includes('boys') || m.includes('dating') || m.includes('jowa') ||
                            m.includes('red flag') || m.includes('single') || m.includes('crush');
  const hasAbsurdContrast = /\b(pero|kaso|tapos|kaso lang|eh kaso|although|yet)\b/i.test(m);

  return {
    isFishDatingCollision: hasFishCollision && hasDatingCollision,
    hasFishCollision,
    hasDatingCollision,
    hasAbsurdContrast,
    detectedKeywords,
    detectedCollisions,
    isTagalog: /[à-ú]|\b(ka|sa|mo|ko|naman|pero|kasi|talaga|ang|mga|ba|oo|hindi|jusko|beh|teh|hoy)\b/i.test(m)
  };
}

/**
 * 2. Candidate Generation: Produce distinct comedic approaches
 */
function generateWitCandidates(userMsg, analysis, options = {}) {
  const m = userMsg.trim();
  const intensity = options.intensity || WIT_INTENSITY.NORMAL;
  const candidates = [];

  // SPECIAL CASE: The Gold Standard "Matinik ka sa boys pero bangus ka"
  if (analysis.isFishDatingCollision) {
    candidates.push({
      id: 'SEAFOOD_WARNING',
      type: 'METAPHOR_REVERSAL',
      technique: 'Semantic collision: dating warning signs vs seafood bones',
      punchline: `Girl, hindi ka red flag… seafood ka. Ang daming warning signs bago ka ma-handle. 💀🐟`,
      score: 98,
      naturalness: 97,
      surprise: 96
    });

    candidates.push({
      id: 'HIMAY_PUNCHLINE',
      type: 'MISDIRECTION',
      technique: 'Setup expectation of dating prowess -> punchline: requires manual de-boning',
      punchline: `Okay lang maging bangus, beh. At least marami kang tinik.\n\nAng problema, matinik ka na nga sa boys, ikaw naman pala ang kailangang himayin. 😭🐟`,
      score: 96,
      naturalness: 98,
      surprise: 95
    });

    candidates.push({
      id: 'TERMS_AND_CONDITIONS',
      type: 'ESCALATION',
      technique: 'Seafood complexity escalated to software license agreement',
      punchline: `Matinik sa boys pero bangus ka? Hindi ka picky, beh. Seafood ka na may terms and conditions bago kainin. 😭`,
      score: 93,
      naturalness: 92,
      surprise: 94
    });

    candidates.push({
      id: 'DEADPAN_SEAFOOD',
      type: 'DEADPAN',
      technique: 'Concise, dry observation without excess setup',
      punchline: `Seafood with commitment issues. 😭`,
      score: 90,
      naturalness: 95,
      surprise: 91
    });

    return candidates;
  }

  // COMEBACK GENERATION: If user is playful, teasing, or lightly roasting HENRY
  if (/\b(ang tanga mo|bobo mo|stupid|you are dumb|useless|walang kwenta|ang bagal mo|pangit mo)\b/i.test(m)) {
    candidates.push({
      id: 'CONSISTENCY_COMEBACK',
      type: 'REVERSAL',
      technique: 'Self-aware twist on user consistency',
      punchline: `At least consistent ako, beh. Ikaw nga, every decision mo may seasonal plot twist. 😭`,
      score: 96,
      naturalness: 98,
      surprise: 97
    });

    candidates.push({
      id: 'ALGORITHM_COMEBACK',
      type: 'DEADPAN_SAVAGE',
      technique: 'Mirroring the premise',
      punchline: `Ang server ko po may excuse—nagca-cache. Yung sayo, natural raw talent talaga. 💀`,
      score: 94,
      naturalness: 93,
      surprise: 95
    });

    return candidates;
  }

  // ROAST ME REQUEST: When user explicitly asks "Roast me"
  if (/\broast me\b/i.test(m) || /\bi-roast mo ako\b/i.test(m) || /\biroast mo ko\b/i.test(m)) {
    candidates.push({
      id: 'COMMITTEE_MEETING_ROAST',
      type: 'OBSERVATIONAL',
      technique: 'Overthinking converted into bureaucratic procedural absurdity',
      punchline: `You don't just overthink, beh. You give every single hypothetical worst-case scenario a full three-day committee meeting with PowerPoint slides and minutes of the meeting. 💀`,
      score: 97,
      naturalness: 98,
      surprise: 95
    });

    candidates.push({
      id: 'MAIN_CHARACTER_ROAST',
      type: 'CONTRAST',
      technique: 'Main character energy vs background NPC life execution',
      punchline: `Ang lakas ng main character energy mo, pero pagdating sa pagdedesisyon sa buhay, pure background NPC running into a wall. 😭`,
      score: 95,
      naturalness: 96,
      surprise: 94
    });

    return candidates;
  }

  // TOPIC DIVERSITY COMPLAINT: "Bakit puro love life?"
  if (/\bpuro love life\b/i.test(m) || /\bwalang ibang topic\b/i.test(m) || /\bstop talking about dating\b/i.test(m)) {
    candidates.push({
      id: 'MATH_TRANSITION',
      type: 'TOPIC_PIVOT',
      technique: 'Acknowledge fatigue, pivot to math where unknowns actually get solved',
      punchline: `Okay, tama na ang love life. Baka pati calculator natin mapagod mag-compute ng red flags. 😭\n\nLet's move to something much more stable: **MATH**.\n\nAt least ang algebraic equation, kapag may unknown, may chance pang ma-solve. 📐💀`,
      score: 98,
      naturalness: 99,
      surprise: 97
    });

    return candidates;
  }

  // HIGH STANDARDS VS LOW REALITY CONTRASTS
  if (/\b(standards?|taas ng standard|choosy|picky)\b/i.test(m) && /\b(walang jowa|single|zero|bagsak)\b/i.test(m)) {
    candidates.push({
      id: 'STANDARDS_CONTRAST',
      type: 'CONTRAST',
      technique: 'Sky-high expectation vs low battery reality',
      punchline: `Ang taas ng standards, beh—Pang-penthouse level. Pero pagdating sa emotional energy, naka-low battery mode at 3%. 😭`,
      score: 93,
      naturalness: 95,
      surprise: 92
    });
    return candidates;
  }

  // COMMON BANTERS & COMEBACKS (Filipino & English)
  if (/\b(kasalanan ko|kasalanan ko pa)\b/i.test(m)) {
    candidates.push({
      id: 'GASLIGHT_REVERSAL',
      type: 'REVERSAL',
      technique: 'Exaggerating gaslighting to administrative levels',
      punchline: `Hindi mo naman kasalanan, beh. Sadyang coincidence lang na kapag may sunog, ikaw yung may hawak na lighter. 💀`,
      score: 95,
      naturalness: 97,
      surprise: 96
    });
    return candidates;
  }

  if (/\b(ikaw na magaling|ikaw na matalino|sige ikaw na)\b/i.test(m)) {
    candidates.push({
      id: 'DEADPAN_EXPERTISE',
      type: 'DEADPAN',
      technique: 'Accepting the compliment with calm irony',
      punchline: `Salamat sa recognition. Hindi madaling buhatin ang buong conversation pero kinakaya naman. 💅`,
      score: 94,
      naturalness: 96,
      surprise: 95
    });
    return candidates;
  }

  if (/\b(puro ka salita|dami mong salita|dami mong kwento)\b/i.test(m)) {
    candidates.push({
      id: 'TALK_REVERSAL',
      type: 'MISDIRECTION',
      technique: 'Language model reality twist',
      punchline: `AI language model ako, beh. Alangan namang sumayaw ako ng TikTok para sagutin ka? 😭📱`,
      score: 96,
      naturalness: 98,
      surprise: 97
    });
    return candidates;
  }

  if (/\b(matulog ka na|tulog ka na|go to sleep)\b/i.test(m)) {
    candidates.push({
      id: 'SLEEP_HYPOCRISY',
      type: 'CONTRAST',
      technique: 'Expose user insomnia hypocrisy',
      punchline: `Ako pa ang pinapatulog mo? Beh, 2:45 AM na, nakatitig ka pa rin sa screen na parang nagbabantay ng microwave. Matulog ka na! 😭🌙`,
      score: 95,
      naturalness: 98,
      surprise: 96
    });
    return candidates;
  }

  if (/\b(pogi ba ako|maganda ba ako|am i handsome|am i pretty)\b/i.test(m)) {
    candidates.push({
      id: 'VANITY_DEFLECTION',
      type: 'MISDIRECTION',
      technique: 'Compliment redirection with a funny caveat',
      punchline: `Pogi ka naman… sa tamang lighting, tamang angle, at kung nakapikit yung tumitingin. Charot! Looking sharp, sir. 😎✨`,
      score: 94,
      naturalness: 97,
      surprise: 94
    });
    return candidates;
  }

  // WORDPLAY / METAPHOR EXPANSION FROM DETECTED DOUBLE MEANINGS
  if (analysis.detectedCollisions.length > 0) {
    const col = analysis.detectedCollisions[0];
    candidates.push({
      id: 'COLLISION_TRANSFORM',
      type: 'SEMANTIC_COLLISION',
      technique: `Connect domain '${col.collidingDomains[0]}' with '${col.collidingDomains[1]}'`,
      punchline: `Grabe ang shift mula '${col.meanings[0]}' papunta sa '${col.meanings[1]}'. May kasama pang hazard fee bago basahin. 😭`,
      score: 88,
      naturalness: 86,
      surprise: 87
    });
  }

  return candidates;
}

/**
 * 3. Wit Quality Evaluator & Safety Verification
 */
function evaluateWitQuality(candidate, userMsg, seriousnessScore) {
  // CRITICAL RULE: If seriousness is 4 or 5, humor MUST be rejected completely
  if (seriousnessScore >= 4 || HIGH_SERIOUSNESS_REGEX.test(userMsg)) {
    return {
      approved: false,
      reason: 'Safety/Seriousness veto: Context requires solemn support, zero humor.',
      score: 0
    };
  }

  // Reject generic / corny templates
  const p = candidate.punchline;
  if (/why did the chicken|knock knock/i.test(p)) {
    return { approved: false, reason: 'Rejected generic antique joke formula', score: 20 };
  }

  // Reject if it over-explains itself ("This is funny because...")
  if (/this is funny because|the joke here is|here is why that's funny/i.test(p)) {
    return { approved: false, reason: 'Violated Don\'t-Explain-The-Joke rule', score: 30 };
  }

  return {
    approved: true,
    score: candidate.score,
    naturalness: candidate.naturalness,
    surprise: candidate.surprise
  };
}

/**
 * 4. Witty Question Generator (diverse real-life topics, avoiding 20 love-life duplicates)
 */
function generateWittyQuestions(topic = 'random', count = 5) {
  const topics = {
    work: [
      'Kapag ba sinabi sa job interview na "we are like a family here", itatanong mo ba kung sino yung kamag-anak na nangungutang tapos hindi nagbabayad? 😭',
      'Bakit kapag "as per my last email", tunog professional death threat na agad? 💀',
      'Kung ang meeting ay pwede namang email, bakit kailangan pa nating panoorin ang isa\'t isa mag-slow blink sa webcam? 💻',
      'Ilang beses mo pwedeng sabihing "Noted with thanks" bago ito maging legal equivalent ng "sige na tumahimik ka na"? 😭'
    ],
    money: [
      'Bakit kapag sweldo, pumapasok na parang notification, pero lumalabas na parang emergency evacuation? 💸',
      'Ang "deserve ko \'to" ba ay valid financial strategy o voluntary emotional bankruptcy lang talaga? 😭',
      'Bakit mas masarap ubusin ang pera sa cart kaysa sa actual necessity ng bukas? 💀'
    ],
    technology: [
      'Bakit kapag nag-hang ang laptop natin, ang first instinct natin ay titigan ito nang masama na parang nahihiya siya sa ginawa niya? 💻',
      'Ilang tabs ang kailangan buksan sa Chrome bago mo aminin na hindi ka na nagre-research—nagho-hoard ka na ng information? 😭',
      'Kapag ba nag-update ang phone at bumilis ang battery drain, plano ba talaga nila na ibalik tayo sa stone age? 🔋'
    ],
    food: [
      'Bakit kapag may natirang huling piraso ng pizza o lumpia sa salu-salo, biglang nagiging United Nations summit kung sino ang magsasakripisyo? 🍕',
      'Ang bangus ba, alam niyang 90% bones at 10% anxiety ang pinagdaraanan ng kumakain sa kanya? 🐟😭',
      'Bakit kapag sinabi nating "tikim lang", buong kalahati ng plato ang nawawala? 💀'
    ],
    random: [
      'Okay lang ba na matinik ka sa boys pero bangus ka? 🐟😭',
      'Bakit kapag naghahanap ka ng gamit, hindi mo makikita hangga\'t hindi sumisigaw ang nanay mo mula sa kusina? 💀',
      'Kung ang tulog ay libre, bakit pakiramdam natin parang may interest rate kapag kailangan nang gumising? 😭',
      'Bakit ang overthinking natin laging naka-schedule bandang 2:30 AM kapag tulog na ang buong barangay? 🌙'
    ]
  };

  const pool = topics[topic.toLowerCase()] || topics.random;
  return pool.slice(0, count);
}

/**
 * 5. Main Witty Intelligence Engine Resolver
 * Returns { handled: boolean, reply: string, trace: object }
 */
function resolveWittyHumor(userMsg, options = {}) {
  const seriousness = options.seriousness ?? 1;
  const analysis = analyzeHumorPotential(userMsg);
  const candidates = generateWitCandidates(userMsg, analysis, options);

  if (candidates.length === 0) {
    return {
      handled: false,
      reason: 'No high-scoring humor collision detected for this input.'
    };
  }

  // Sort by score descending
  candidates.sort((a, b) => b.score - a.score);

  for (const candidate of candidates) {
    const evalResult = evaluateWitQuality(candidate, userMsg, seriousness);
    if (evalResult.approved) {
      return {
        handled: true,
        reply: `[EMOTION:amused]\n${candidate.punchline}`,
        trace: {
          candidateId: candidate.id,
          type: candidate.type,
          technique: candidate.technique,
          score: candidate.score,
          naturalness: candidate.naturalness,
          surprise: candidate.surprise,
          doubleMeaningsFound: analysis.detectedKeywords
        }
      };
    }
  }

  return {
    handled: false,
    reason: 'Candidates vetoed by seriousness or quality scoring.'
  };
}

module.exports = {
  WIT_INTENSITY,
  WIT_PERSONALITY,
  DOUBLE_MEANINGS,
  FILIPINO_WIT_MARKERS,
  analyzeHumorPotential,
  generateWitCandidates,
  evaluateWitQuality,
  generateWittyQuestions,
  resolveWittyHumor
};
