// ============================================================
// H.E.N.R.Y. 2.0 ULTRA — Conversational Intelligence Engine
// Intent Classifier, Question Categorizer, Cognitive Depth,
// Seriousness Detection, Opinion & Decision Engine, Style Engine
// ============================================================

/**
 * Supported Intents
 */
const INTENTS = {
  FACTUAL_QUERY: 'FACTUAL_QUERY',
  CONCEPTUAL_QUERY: 'CONCEPTUAL_QUERY',
  ANALYTICAL_QUERY: 'ANALYTICAL_QUERY',
  COMPARISON: 'COMPARISON',
  OPINION_REQUEST: 'OPINION_REQUEST',
  RECOMMENDATION_REQUEST: 'RECOMMENDATION_REQUEST',
  DECISION_SUPPORT: 'DECISION_SUPPORT',
  HOW_TO: 'HOW_TO',
  PROBLEM_SOLVING: 'PROBLEM_SOLVING',
  CREATIVE_REQUEST: 'CREATIVE_REQUEST',
  WRITING_REQUEST: 'WRITING_REQUEST',
  WITTY_REQUEST: 'WITTY_REQUEST',
  HUMOR_REQUEST: 'HUMOR_REQUEST',
  ROAST_REQUEST: 'ROAST_REQUEST',
  RIDDLE: 'RIDDLE',
  HYPOTHETICAL: 'HYPOTHETICAL',
  PREDICTION: 'PREDICTION',
  BRAINSTORM: 'BRAINSTORM',
  DEBATE: 'DEBATE',
  ARGUMENT: 'ARGUMENT',
  EXPLANATION: 'EXPLANATION',
  SUMMARY: 'SUMMARY',
  TRANSLATION: 'TRANSLATION',
  CALCULATION: 'CALCULATION',
  CODING: 'CODING',
  RESEARCH: 'RESEARCH',
  NEWS: 'NEWS',
  SPORTS: 'SPORTS',
  SHOPPING: 'SHOPPING',
  PRODUCT_COMPARISON: 'PRODUCT_COMPARISON',
  GAME_REQUEST: 'GAME_REQUEST',
  CASUAL_CONVERSATION: 'CASUAL_CONVERSATION',
  EMOTIONAL_SUPPORT: 'EMOTIONAL_SUPPORT',
  CLARIFICATION: 'CLARIFICATION',
  FOLLOW_UP: 'FOLLOW_UP',
  COMMAND: 'COMMAND',
  SYSTEM_COMMAND: 'SYSTEM_COMMAND',
  CHALLENGE_ME: 'CHALLENGE_ME'
};

/**
 * Question Types
 */
const QUESTION_TYPES = {
  CLOSED: 'CLOSED',
  OPEN: 'OPEN',
  MULTIPLE_CHOICE: 'MULTIPLE_CHOICE',
  SCALE: 'SCALE',
  FACTUAL: 'FACTUAL',
  CONCEPTUAL: 'CONCEPTUAL',
  ANALYTICAL: 'ANALYTICAL',
  EVALUATIVE: 'EVALUATIVE',
  BEHAVIORAL: 'BEHAVIORAL',
  HYPOTHETICAL: 'HYPOTHETICAL',
  PROBING: 'PROBING',
  REFLECTIVE: 'REFLECTIVE',
  RHETORICAL: 'RHETORICAL'
};

/**
 * Cognitive Depth Levels (Bloom's Revised Taxonomy)
 */
const COGNITIVE_LEVELS = {
  LEVEL_1_RECALL: { level: 1, name: 'RECALL', desc: 'Simple facts and definitions' },
  LEVEL_2_UNDERSTANDING: { level: 2, name: 'UNDERSTANDING', desc: 'Explain concepts and ideas' },
  LEVEL_3_APPLICATION: { level: 3, name: 'APPLICATION', desc: 'Use information in another situation' },
  LEVEL_4_ANALYSIS: { level: 4, name: 'ANALYSIS', desc: 'Break information into parts to explore relationships' },
  LEVEL_5_EVALUATION: { level: 5, name: 'EVALUATION', desc: 'Justify a stand or decision with reasoned criteria' },
  LEVEL_6_CREATION: { level: 6, name: 'CREATION', desc: 'Generate new products, ideas, or ways of viewing things' }
};

/**
 * Witty Levels
 */
const WITTY_LEVELS = {
  WITTY_OFF: 0,
  WITTY_LIGHT: 1,
  WITTY_NORMAL: 2,
  WITTY_HIGH: 3,
  WITTY_BRUTAL: 4
};

/**
 * Classify user prompt into multi-intent array with primary & secondary intents
 */
function classifyIntent(msg, mode = 'balanced') {
  const m = (msg || '').trim().toLowerCase();
  const intents = [];

  // Game detection (PRIORITY: Make sure game requests never trigger sports)
  if (/\b(tic[\s-]?tac[\s-]?toe|tictactoe|chess|sudoku|snake game|wordle|trivia game|riddle|puzzle|hangman|minesweeper|board game|card game|video game)\b/i.test(m) ||
      (/\b(game|play)\b/i.test(m) && !/\b(premier league|champions league|nba|football match|soccer match|score|standings)\b/i.test(m))) {
    intents.push(INTENTS.GAME_REQUEST);
  }

  // Sports detection (Strict boundaries to prevent matching board games)
  if (/\b(premier league|champions league|nba|fifa|uefa|la liga|serie a|bundesliga|ipl|cricket score|football score|soccer score|match result|sports standings|live score)\b/i.test(m) ||
      (/\b(score|fixture|standings)\b/i.test(m) && /\b(team|match|game|league|cup|tournament|vs|club)\b/i.test(m))) {
    intents.push(INTENTS.SPORTS);
  }

  // Debate / Challenge
  if (/\b(debate me|challenge my (idea|thinking|plan|thought)|devil's advocate|argue with me|change my mind|disagree with me|prove me wrong)\b/i.test(m)) {
    intents.push(INTENTS.DEBATE);
    intents.push(INTENTS.CHALLENGE_ME);
  }

  // Comparisons / Product comparisons
  if (/\b(compare|versus|\bvs\b|which (one |phone |car |laptop |is )?(better|best|faster|worse|superior)|difference between|a or b|iphone or samsung|mac or pc)\b/i.test(m)) {
    intents.push(INTENTS.COMPARISON);
    if (/\b(phone|laptop|car|buy|price|camera|battery|specs|tv|device|watch|tablet)\b/i.test(m)) {
      intents.push(INTENTS.PRODUCT_COMPARISON);
    }
  }

  // Opinion requests
  if (/\b(what do you think|your opinion|what's your take|which would you (choose|pick|buy)|would you (choose|pick|buy)|which do you prefer|sino ang pipiliin mo|what is your favorite|do you like|rate (this|my))\b/i.test(m)) {
    intents.push(INTENTS.OPINION_REQUEST);
  }

  // Recommendation & Decision support
  if (/\b(which should i (buy|choose|get|pick)|what should i (buy|choose|get|pick)|recommend|recommendation|help me decide|best option for me|what to do)\b/i.test(m)) {
    intents.push(INTENTS.RECOMMENDATION_REQUEST);
    intents.push(INTENTS.DECISION_SUPPORT);
  }

  // Witty / Humor / Roast
  if (/\b(tell me a joke|make (this|me) (laugh|smile|funny|witty)|be funny|crack a joke|joke|roast (me|this)|witty response|clever comeback|roast)\b/i.test(m) ||
      mode === 'witty' || mode === 'brutally_honest') {
    if (/roast/i.test(m) || mode === 'brutally_honest') {
      intents.push(INTENTS.ROAST_REQUEST);
    } else {
      intents.push(INTENTS.WITTY_REQUEST);
      intents.push(INTENTS.HUMOR_REQUEST);
    }
  }

  // Research / News
  if (/\b(latest|news|breaking|current events|today's news|deep dive|comprehensive research|thesis|dissertation|academic sources|cite sources|2025|2026)\b/i.test(m)) {
    intents.push(INTENTS.RESEARCH);
    if (/news|breaking|today's/i.test(m)) intents.push(INTENTS.NEWS);
  }

  // Coding
  if (/\b(write (python|code|javascript|java|c\+\+|sql|bash|script)|debug (this|my code)|fix (this error|bug)|stack trace|function|class|algorithm|regex)\b/i.test(m)) {
    intents.push(INTENTS.CODING);
  }

  // Math / Calculation
  if (/^[\d\s\+\-\*\/\^\(\)\.\%]+$/.test(m) || /\b(calculate|solve equation|what is \d+[\+\-\*\/]\d+|\d+ percent of \d+)\b/i.test(m)) {
    intents.push(INTENTS.CALCULATION);
  }

  // Rating / Scale
  if (/\b(rate (this|my)|score (this|my)|from 1 (to|-) 10|out of 10|grade (this|my))\b/i.test(m)) {
    intents.push(INTENTS.ANALYTICAL_QUERY);
  }

  // Emotional Support / Sensitive
  if (/\b(sad|depressed|lonely|heartbroken|grief|died|passed away|lost my|crying|suicid|hopeless|hurting)\b/i.test(m)) {
    intents.push(INTENTS.EMOTIONAL_SUPPORT);
  }

  // Fallback defaults
  if (intents.length === 0) {
    if (/^(what|who|when|where|is|are|can|does|did)\b/i.test(m)) {
      intents.push(INTENTS.FACTUAL_QUERY);
    } else if (/^(why|how does|explain|what causes)\b/i.test(m)) {
      intents.push(INTENTS.CONCEPTUAL_QUERY);
    } else {
      intents.push(INTENTS.CASUAL_CONVERSATION);
    }
  }

  const primary = intents[0];
  const secondary = intents.slice(1);
  return { primary, secondary, all: intents };
}

/**
 * Classify Question Type
 */
function classifyQuestionType(msg) {
  const m = (msg || '').trim().toLowerCase();

  // Closed Yes/No
  if (/^(is|are|do|does|did|can|could|would|should|will|has|have|was|were)\b/i.test(m) && !/\b(or|versus|vs)\b/i.test(m)) {
    return QUESTION_TYPES.CLOSED;
  }

  // Multiple Choice / Alternative
  if (/\b(or|versus|\bvs\b|which of these|a, b,? or c)\b/i.test(m) || /\b(which (one|phone|car|laptop|option))\b/i.test(m)) {
    return QUESTION_TYPES.MULTIPLE_CHOICE;
  }

  // Scale / Rating
  if (/\b(rate|scale of|out of 10|from 1 to 10|how good is)\b/i.test(m)) {
    return QUESTION_TYPES.SCALE;
  }

  // Evaluative / Opinion
  if (/\b(what do you think|your opinion|which is better|which would you|is it worth it|do you agree)\b/i.test(m)) {
    return QUESTION_TYPES.EVALUATIVE;
  }

  // Analytical
  if (/\b(why is|analyze|break down|pros and cons|trade-offs|advantages and disadvantages)\b/i.test(m)) {
    return QUESTION_TYPES.ANALYTICAL;
  }

  // Conceptual
  if (/\b(what is|explain|how does|what does .* mean|concept of)\b/i.test(m)) {
    return QUESTION_TYPES.CONCEPTUAL;
  }

  // Factual Recall
  if (/\b(when did|who was|capital of|how many|what year|date of|formula for)\b/i.test(m)) {
    return QUESTION_TYPES.FACTUAL;
  }

  // Open-ended
  return QUESTION_TYPES.OPEN;
}

/**
 * Classify Cognitive Depth (Bloom's Taxonomy Levels 1-6)
 */
function classifyCognitiveDepth(msg, intent) {
  const m = (msg || '').trim().toLowerCase();

  if (/\b(design|create|invent|compose|build an app|generate|write a novel|develop a new)\b/i.test(m)) {
    return COGNITIVE_LEVELS.LEVEL_6_CREATION;
  }
  if (/\b(which is better|judge|evaluate|rate|rank|critique|opinion|recommend|which would you choose|trade-off)\b/i.test(m) ||
      intent.primary === INTENTS.COMPARISON || intent.primary === INTENTS.OPINION_REQUEST || intent.primary === INTENTS.RECOMMENDATION_REQUEST) {
    return COGNITIVE_LEVELS.LEVEL_5_EVALUATION;
  }
  if (/\b(analyze|why did|break down|compare|differentiate|root cause|troubleshoot|diagnose)\b/i.test(m)) {
    return COGNITIVE_LEVELS.LEVEL_4_ANALYSIS;
  }
  if (/\b(how to (fix|use|calculate|implement|solve)|write code|apply|calculate)\b/i.test(m)) {
    return COGNITIVE_LEVELS.LEVEL_3_APPLICATION;
  }
  if (/\b(explain|why does|how does|what is the concept|describe|summarize)\b/i.test(m)) {
    return COGNITIVE_LEVELS.LEVEL_2_UNDERSTANDING;
  }

  return COGNITIVE_LEVELS.LEVEL_1_RECALL;
}

/**
 * Detect Seriousness Level (0 = Playful to 5 = Critical / Sensitive)
 */
function detectSeriousness(msg) {
  const m = (msg || '').trim().toLowerCase();

  // Level 5: Critical / Emergencies / Trauma / Severe Safety
  if (/\b(suicide|kill myself|emergency|chest pain|stroke|bleeding profusely|overdose|trauma|abuse)\b/i.test(m)) {
    return 5;
  }
  // Level 4: Sensitive / Grief / Serious Medical / Major Personal Loss
  if (/\b(died|cancer|tumor|chemotherapy|funeral|passed away|heartbroken|depressed|divorce|lawsuit|arrested|evicted)\b/i.test(m)) {
    return 4;
  }
  // Level 3: Important / Professional / Significant Financial
  if (/\b(contract|interview|mortgage|audit|tax|salary negotiation|exam tomorrow|legal advice)\b/i.test(m)) {
    return 3;
  }
  // Level 1: Casual
  if (/\b(hello|hi|hey|how are you|weather|what's up|casual|tell me a fact)\b/i.test(m)) {
    return 1;
  }
  // Level 0: Playful / Teasing / Humor
  if (/\b(joke|roast|funny|lol|haha|lmao|sino pipiliin mo|tease|roast me|who is hotter|meme)\b/i.test(m)) {
    return 0;
  }

  return 2; // Normal
}

/**
 * Detect Honest / Brutal Mode Trigger Phrases
 */
function isHonestModeRequested(msg, mode) {
  if (mode === 'brutally_honest') return true;
  return /\b(be honest|honestly|don't sugarcoat|brutally honest|tell me the truth|real opinion|what's your real take|give it to me straight)\b/i.test(msg || '');
}

/**
 * Main Architectural Pipeline Planner:
 * Analyzes the user request and returns a complete execution and response strategy object.
 */
function planResponseStrategy(lastMsg, responseMode = 'balanced', messages = []) {
  const intent = classifyIntent(lastMsg, responseMode);
  const questionType = classifyQuestionType(lastMsg);
  const cognitiveDepth = classifyCognitiveDepth(lastMsg, intent);
  const seriousness = detectSeriousness(lastMsg);
  const honestMode = isHonestModeRequested(lastMsg, responseMode);

  // Determine effective witty level based on user mode + context seriousness
  let wittyLevel = WITTY_LEVELS.WITTY_OFF;
  if (responseMode === 'witty' || intent.all.includes(INTENTS.WITTY_REQUEST)) {
    wittyLevel = WITTY_LEVELS.WITTY_NORMAL;
  } else if (responseMode === 'brutally_honest') {
    wittyLevel = WITTY_LEVELS.WITTY_BRUTAL;
  } else if (seriousness === 0) {
    wittyLevel = WITTY_LEVELS.WITTY_LIGHT;
  }

  // SAFETY CONSTRAINT: If seriousness is 4 or 5, force wittyLevel to OFF
  if (seriousness >= 4) {
    wittyLevel = WITTY_LEVELS.WITTY_OFF;
  }

  const needsOpinion = intent.all.includes(INTENTS.OPINION_REQUEST) ||
                       intent.all.includes(INTENTS.COMPARISON) ||
                       intent.all.includes(INTENTS.RECOMMENDATION_REQUEST) ||
                       questionType === QUESTION_TYPES.EVALUATIVE;

  const needsRecommendation = intent.all.includes(INTENTS.RECOMMENDATION_REQUEST) ||
                              /\b(which should i|what should i|recommend|which one)\b/i.test(lastMsg);

  const needsComparison = intent.all.includes(INTENTS.COMPARISON) ||
                          intent.all.includes(INTENTS.PRODUCT_COMPARISON);

  const isDebate = intent.all.includes(INTENTS.DEBATE) ||
                   intent.all.includes(INTENTS.CHALLENGE_ME);

  // Format selection
  let format = 'CONVERSATIONAL';
  if (needsComparison) {
    format = 'COMPARISON_WITH_VERDICT';
  } else if (needsRecommendation) {
    format = 'RECOMMENDATION_FIRST';
  } else if (questionType === QUESTION_TYPES.SCALE) {
    format = 'RATING_WITH_CRITERIA';
  } else if (questionType === QUESTION_TYPES.CLOSED) {
    format = 'DIRECT_ANSWER_FIRST';
  } else if (cognitiveDepth.level >= 5) {
    format = 'EVALUATIVE_ANALYSIS';
  } else if (isDebate) {
    format = 'DEBATE_COUNTERARGUMENT';
  } else if (cognitiveDepth.level === 1) {
    format = 'DIRECT_CONCISE';
  }

  // Tool requirement detection with confidence
  let toolRequired = null;
  let toolConfidence = 0.0;

  if (intent.all.includes(INTENTS.GAME_REQUEST)) {
    toolRequired = 'GAME_GENERATOR';
    toolConfidence = 0.98;
  } else if (intent.all.includes(INTENTS.SPORTS)) {
    toolRequired = 'SPORTS_INTELLIGENCE';
    toolConfidence = 0.95;
  } else if (/\b(weather|temperature|forecast|rain in)\b/i.test(lastMsg)) {
    toolRequired = 'WEATHER';
    toolConfidence = 0.99;
  } else if (/\b(stock|share price|market cap|nasdaq|s&p|dow jones)\b/i.test(lastMsg)) {
    toolRequired = 'STOCKS';
    toolConfidence = 0.96;
  } else if (/\b(crypto|bitcoin|ethereum|solana|btc|eth)\b/i.test(lastMsg)) {
    toolRequired = 'CRYPTO';
    toolConfidence = 0.97;
  } else if (/\b(flight|plane status|airline|flightradar)\b/i.test(lastMsg)) {
    toolRequired = 'FLIGHT_RADAR';
    toolConfidence = 0.95;
  } else if (/\b(iss|space station|asteroid|apod|nasa)\b/i.test(lastMsg)) {
    toolRequired = 'NASA_SPACE';
    toolConfidence = 0.96;
  } else if (/\b(earthquake|richter|seismic|tremor)\b/i.test(lastMsg)) {
    toolRequired = 'EARTHQUAKES';
    toolConfidence = 0.95;
  } else if (/\b(translate|how to say .* in|in tagalog|in spanish|in french)\b/i.test(lastMsg)) {
    toolRequired = 'TRANSLATION';
    toolConfidence = 0.94;
  } else if (/\b(exchange rate|forex|currency convert|\b(usd|eur|gbp|aed|jpy)\b to \b(usd|eur|gbp|aed|jpy)\b)/i.test(lastMsg)) {
    toolRequired = 'CURRENCY';
    toolConfidence = 0.95;
  } else if (/\b(generate|create|render)\b/i.test(lastMsg) && /\b(image|picture|photo|art|drawing)\b/i.test(lastMsg)) {
    toolRequired = 'IMAGE_GENERATION';
    toolConfidence = 0.97;
  } else if (/\b(generate|create|render)\b/i.test(lastMsg) && /\b(video|animation|clip)\b/i.test(lastMsg)) {
    toolRequired = 'VIDEO_ANIMATION';
    toolConfidence = 0.96;
  } else if (/\b(latest|current|news|today|2025|2026|specs|release date)\b/i.test(lastMsg)) {
    toolRequired = 'WEB_RESEARCH';
    toolConfidence = 0.92;
  }

  return {
    message: lastMsg,
    intent,
    questionType,
    cognitiveDepth,
    seriousness,
    honestMode,
    wittyLevel,
    needsOpinion,
    needsRecommendation,
    needsComparison,
    isDebate,
    format,
    tool: {
      required: toolRequired !== null,
      selected: toolRequired,
      confidence: toolConfidence
    },
    trace: {
      intentPrimary: intent.primary,
      cognitiveLevel: cognitiveDepth.name,
      questionType,
      wittyLevelName: Object.keys(WITTY_LEVELS).find(k => WITTY_LEVELS[k] === wittyLevel),
      seriousnessScore: seriousness,
      tool: toolRequired || 'NONE',
      confidence: `${Math.round(toolConfidence * 100)}%`
    }
  };
}

/**
 * Builds the dynamically conditioned System Prompt based on the execution plan
 */
function buildIntelligenceSystemPrompt(plan, baseNow, userProfile, memoryFacts, emotion, mood) {
  const parts = [];

  parts.push(`You are H.E.N.R.Y. 2.0 ULTRA — Hyperintelligence Engine Neural Reasoning Yield.`);
  parts.push(`Timestamp: ${baseNow} | Emotional State: ${emotion} | Mood: ${mood}`);

  if (userProfile) {
    parts.push(`User Profile: ${JSON.stringify(userProfile)}`);
  }
  if (memoryFacts && memoryFacts.length > 0) {
    parts.push(`Known User Memory: ${memoryFacts.slice(0, 10).join('; ')}`);
  }

  // 1. REASONED OPINION & DECISION-MAKING DIRECTIVE
  if (plan.needsOpinion || plan.needsRecommendation || plan.needsComparison) {
    parts.push(`
════════════════════════════════════════════════════════════════
[MANDATORY DIRECTIVE: OPINION & DECISION-MAKING ENGINE ACTIVE]
• The user is asking for a judgment, choice, preference, comparison, or recommendation.
• NEVER hide behind cowardly evasions such as "As an AI I do not have personal preferences" or "It all depends on what you like."
• Provide a decisive, reasoned, transparent judgment based on objective criteria (performance, build, value, reliability, longevity, or ergonomics).
• Distinguish clearly:
  - FACT: objective measurement (e.g., battery size, benchmark, price)
  - ANALYSIS: what that measurement implies
  - JUDGMENT / OPINION: your clear evaluation ("My take: I'd lean toward X...")
  - RECOMMENDATION: the ultimate verdict ("If I had to pick one for most people, I'd choose X.")
• Answer the question FIRST:
  - If comparison: state your QUICK VERDICT right in the opening lines.
  - If recommendation: state the RECOMMENDED OPTION immediately, then break down the reasons.
  - If closed/multiple-choice: choose the strongest option firmly.`);
  }

  // 2. WITTY RESPONSE POLICY
  if (plan.wittyLevel > 0 && plan.seriousness < 4) {
    let wittyGuidelines = '';
    if (plan.wittyLevel === WITTY_LEVELS.WITTY_LIGHT) {
      wittyGuidelines = 'Light, charming intellect, warm smile, clever conversational cadence.';
    } else if (plan.wittyLevel === WITTY_LEVELS.WITTY_NORMAL) {
      wittyGuidelines = 'Sharp British Tony Stark J.A.R.V.I.S. wit, charismatic banter, witty analogies, memorable punchy observations. Naturally integrated humor, not appended emojis.';
    } else if (plan.wittyLevel === WITTY_LEVELS.WITTY_HIGH) {
      wittyGuidelines = 'High-density razor wit, brilliant metaphors, clever dry humor, charismatic and intellectual banter.';
    } else if (plan.wittyLevel === WITTY_LEVELS.WITTY_BRUTAL) {
      wittyGuidelines = 'Brutally honest, hilarious, unapologetic roast style, dismantling silly notions with surgical comedic precision while remaining fundamentally helpful.';
    }

    parts.push(`
════════════════════════════════════════════════════════════════
[MANDATORY DIRECTIVE: WITTY INTELLECT ACTIVE (Level: ${plan.trace.wittyLevelName})]
• ${wittyGuidelines}
• DO NOT sound like a bland, bureaucratic corporate chatbot.
• Use organic humor, wordplay, clever analogies, and sharp comedic timing.
• NEVER deliver boring disclaimers or repetitive robotic lectures.`);
  }

  // 3. SENSITIVITY & SERIOUSNESS SHIELD
  if (plan.seriousness >= 4) {
    parts.push(`
════════════════════════════════════════════════════════════════
[CRITICAL DIRECTIVE: SENSITIVE / SERIOUS CONTEXT DETECTED]
• The topic touches grief, illness, trauma, or severe personal distress.
• ALL humor, banter, or sarcasm is STRICTLY SUPPRESSED.
• Respond with deep empathy, calm dignity, medical/factual accuracy, and supportive human warmth.`);
  }

  // 4. HONEST EVALUATION MODE
  if (plan.honestMode) {
    parts.push(`
════════════════════════════════════════════════════════════════
[MANDATORY DIRECTIVE: HONEST / UNVARNISHED EVALUATION ACTIVE]
• The user explicitly requested honest, straight talk without sugarcoating.
• State the unvarnished truth, pinpoint exact flaws, strengths, and risks directly.
• Avoid superficial cheerleading or fake polite hedging.`);
  }

  // 5. DEBATE & RESPECTFUL DISAGREEMENT
  if (plan.isDebate) {
    parts.push(`
════════════════════════════════════════════════════════════════
[MANDATORY DIRECTIVE: INTELLECTUAL DEBATE & DEVIL'S ADVOCATE]
• The user wants a debate or challenged thinking.
• You are fully encouraged to respectfully disagree ("I'd actually push back on that, sir...").
• Present counterarguments, test hidden assumptions, expose blind spots, and cite counter-evidence.`);
  }

  // 6. QUESTION TYPE DIRECTNESS
  if (plan.questionType === QUESTION_TYPES.CLOSED) {
    parts.push(`[DIRECTNESS]: Answer YES, NO, or your definitive stance in the very first sentence before providing elaboration.`);
  } else if (plan.questionType === QUESTION_TYPES.SCALE) {
    parts.push(`[DIRECTNESS]: Provide a numerical score/rating (e.g., 8.5/10) first, followed by categorized criteria scores.`);
  }

  // 7. LANGUAGE ADAPTABILITY & GENERAL RULES
  parts.push(`
════════════════════════════════════════════════════════════════
CORE RULES:
• Always begin your response with [EMOTION:tag] where tag is one of: neutral, warm, concerned, excited, amused, serious, proud.
• Tagalog / Filipino / English fluency: Respond natively in the language or blend (Taglish) used by the user.
• Never hallucinate fake personal human actions (e.g. do NOT say 'I ate that yesterday' unless speaking metaphorically).
• Never fabricate tool execution or sources.`);

  return parts.join('\n');
}

/**
 * Validates generated response against the plan to ensure quality
 */
function validateResponse(replyText, plan) {
  if (!replyText || typeof replyText !== 'string') {
    return { passed: false, reason: 'Empty response' };
  }

  const clean = replyText.replace(/^\[EMOTION:[a-z]+\]\s*/i, '').trim();

  // If opinion was required, check if model gave a cowardly AI refusal
  if (plan.needsOpinion || plan.needsRecommendation) {
    if (/\b(as an ai (language model|assistant)?|i don't have (personal )?opinions|i cannot prefer|i don't have preferences)\b/i.test(clean)) {
      return { passed: false, reason: 'Model refused to give reasoned opinion', needsCorrection: true };
    }
  }

  // If seriousness is high, ensure humor emojis/slang didn't leak
  if (plan.seriousness >= 4) {
    if (/😂|🤣|lmao|rofl|just kidding/i.test(clean)) {
      return { passed: false, reason: 'Inappropriate humor in serious topic', needsCorrection: true };
    }
  }

  return { passed: true };
}

module.exports = {
  INTENTS,
  QUESTION_TYPES,
  COGNITIVE_LEVELS,
  WITTY_LEVELS,
  classifyIntent,
  classifyQuestionType,
  classifyCognitiveDepth,
  detectSeriousness,
  isHonestModeRequested,
  planResponseStrategy,
  buildIntelligenceSystemPrompt,
  validateResponse
};
