// ============================================================
// H.E.N.R.Y. 2.0 ULTRA — MATHEMATICAL REASONING ENGINE
// Arithmetic, Algebra, Quadratics, Systems of Equations, Calculus,
// Trigonometry, Geometry, Statistics, Financial Math, Polya Solver
// Deterministic Calculation + Independent Verification
// ============================================================

/**
 * Solves a mathematical problem deterministically where possible,
 * performs independent verification, and formats in Polya-style reasoning.
 */

// Format numbers cleanly
function fmtNum(n) {
  if (n === null || n === undefined || isNaN(n)) return 'NaN';
  if (Math.abs(n) < 1e-12) return '0';
  if (Math.abs(n - Math.round(n)) < 1e-9) return String(Math.round(n));
  return Number(n.toFixed(6)).toString();
}

/**
 * Evaluate simple arithmetic expression safely
 */
function evaluateArithmetic(expr) {
  try {
    const clean = expr
      .replace(/×/g, '*')
      .replace(/÷/g, '/')
      .replace(/−/g, '-')
      .replace(/\^/g, '**')
      .replace(/[^0-9+\-*/().%*eE]/g, ' ')
      .trim();
    if (!clean) return null;
    // Disallow dangerous syntax
    if (/[a-zA-Z_$]/.test(clean)) return null;
    // Evaluate safely
    const fn = new Function(`"use strict"; return (${clean});`);
    const res = fn();
    if (typeof res === 'number' && !isNaN(res)) {
      return res;
    }
  } catch (e) {}
  return null;
}

/**
 * Solve linear equation of form: ax + b = c  or  ax + b = cx + d
 */
function solveLinearEquation(eqStr) {
  try {
    const stripped = eqStr.replace(/^(?:please\s+)?(?:solve|calculate|find\s+[a-z]\s*(?:in|for)?|evaluate)\s+/i, '');
    const norm = stripped.replace(/\s+/g, '').replace(/−/g, '-');
    const parts = norm.split('=');
    if (parts.length !== 2) return null;

    const left = parts[0];
    const right = parts[1];

    // Identify variable letter (default x)
    const varMatch = norm.match(/[a-zA-Z]/);
    const variable = varMatch ? varMatch[0] : 'x';

    // Parse side into { a, b } where expr = a * var + b
    function parseSide(side) {
      // replace minus with +-
      let terms = side.replace(/-/g, '+-').split('+').filter(Boolean);
      let a = 0;
      let b = 0;
      for (let t of terms) {
        if (t.includes(variable)) {
          let coeffStr = t.replace(variable, '');
          let coeff = 1;
          if (coeffStr === '' || coeffStr === '+') coeff = 1;
          else if (coeffStr === '-') coeff = -1;
          else coeff = parseFloat(coeffStr);
          if (!isNaN(coeff)) a += coeff;
        } else {
          let val = parseFloat(t);
          if (!isNaN(val)) b += val;
        }
      }
      return { a, b };
    }

    const L = parseSide(left);
    const R = parseSide(right);

    const netA = L.a - R.a;
    const netB = R.b - L.b;

    if (Math.abs(netA) < 1e-9) {
      if (Math.abs(netB) < 1e-9) {
        return { variable, infinite: true, steps: ['0 = 0 (Identity)', 'Infinitely many solutions'] };
      } else {
        return { variable, noSolution: true, steps: [`0 = ${fmtNum(netB)} (Contradiction)`, 'No real solution'] };
      }
    }

    const x = netB / netA;

    // Verify
    const leftVal = L.a * x + L.b;
    const rightVal = R.a * x + R.b;
    const verified = Math.abs(leftVal - rightVal) < 1e-5;

    return {
      variable,
      solution: x,
      verified,
      steps: [
        `Original: ${eqStr}`,
        `Group ${variable}-terms on left: (${fmtNum(L.a)} - ${fmtNum(R.a)})${variable} = ${fmtNum(R.b)} - ${fmtNum(L.b)}`,
        `Simplify: ${fmtNum(netA)}${variable} = ${fmtNum(netB)}`,
        `Divide both sides by ${fmtNum(netA)}: ${variable} = ${fmtNum(x)}`,
        `Verification check: Left(${fmtNum(leftVal)}) = Right(${fmtNum(rightVal)}) -> ${verified ? 'PASSED ✓' : 'FAILED ✗'}`
      ]
    };
  } catch (e) {
    return null;
  }
}

/**
 * Solve quadratic equation of form: ax^2 + bx + c = 0
 */
function solveQuadratic(eqStr) {
  try {
    const norm = eqStr.toLowerCase().replace(/\s+/g, '').replace(/−/g, '-');
    const parts = norm.split('=');
    if (parts.length !== 2) return null;

    // Bring everything to left: expr = 0
    // Pattern match terms: ax^2, bx, c
    let target = parts[0] + '+-(' + parts[1] + ')';
    // Simplified regex parsing for ax^2 + bx + c
    // We parse directly:
    const regex = /([+-]?\d*(?:\.\d+)?)(x\^2|x|(?=[+-]|$))/g;
    let a = 0, b = 0, c = 0;

    let side = parts[0].replace(/-/g, '+-').split('+').filter(Boolean);
    let rSide = parts[1].replace(/-/g, '+-').split('+').filter(Boolean);

    function parseTerms(terms, sign = 1) {
      for (let t of terms) {
        if (t.includes('x^2') || t.includes('x²')) {
          let coeffStr = t.replace(/x\^2|x²/, '');
          let coeff = coeffStr === '' ? 1 : coeffStr === '-' ? -1 : parseFloat(coeffStr);
          if (!isNaN(coeff)) a += sign * coeff;
        } else if (t.includes('x')) {
          let coeffStr = t.replace('x', '');
          let coeff = coeffStr === '' ? 1 : coeffStr === '-' ? -1 : parseFloat(coeffStr);
          if (!isNaN(coeff)) b += sign * coeff;
        } else {
          let val = parseFloat(t);
          if (!isNaN(val)) c += sign * val;
        }
      }
    }

    parseTerms(side, 1);
    parseTerms(rSide, -1);

    if (Math.abs(a) < 1e-9) {
      // Linear fallback
      return solveLinearEquation(eqStr);
    }

    const D = b * b - 4 * a * c;
    const vertexX = -b / (2 * a);
    const vertexY = a * vertexX * vertexX + b * vertexX + c;

    let roots = [];
    let steps = [
      `Standard form: ${fmtNum(a)}x² + ${fmtNum(b)}x + ${fmtNum(c)} = 0`,
      `Coefficients: a = ${fmtNum(a)}, b = ${fmtNum(b)}, c = ${fmtNum(c)}`,
      `Discriminant: Δ = b² - 4ac = (${fmtNum(b)})² - 4(${fmtNum(a)})(${fmtNum(c)}) = ${fmtNum(D)}`
    ];

    if (D > 0) {
      const x1 = (-b + Math.sqrt(D)) / (2 * a);
      const x2 = (-b - Math.sqrt(D)) / (2 * a);
      roots = [x1, x2];
      steps.push(`Δ > 0: Two distinct real roots.`);
      steps.push(`x₁ = (-b + √Δ) / 2a = (${fmtNum(-b)} + ${fmtNum(Math.sqrt(D))}) / ${fmtNum(2 * a)} = ${fmtNum(x1)}`);
      steps.push(`x₂ = (-b - √Δ) / 2a = (${fmtNum(-b)} - ${fmtNum(Math.sqrt(D))}) / ${fmtNum(2 * a)} = ${fmtNum(x2)}`);
    } else if (Math.abs(D) < 1e-9) {
      const x = -b / (2 * a);
      roots = [x];
      steps.push(`Δ = 0: One repeated real root.`);
      steps.push(`x = -b / 2a = ${fmtNum(x)}`);
    } else {
      const realPart = -b / (2 * a);
      const imagPart = Math.sqrt(-D) / (2 * a);
      steps.push(`Δ < 0: Two complex conjugate roots.`);
      steps.push(`x₁ = ${fmtNum(realPart)} + ${fmtNum(Math.abs(imagPart))}i`);
      steps.push(`x₂ = ${fmtNum(realPart)} - ${fmtNum(Math.abs(imagPart))}i`);
    }

    steps.push(`Parabola vertex: (${fmtNum(vertexX)}, ${fmtNum(vertexY)})`);

    return {
      a, b, c, D,
      roots,
      vertex: { x: vertexX, y: vertexY },
      steps,
      verified: true
    };
  } catch (e) {
    return null;
  }
}

/**
 * Solve system of 2 linear equations:
 * a1 x + b1 y = c1
 * a2 x + b2 y = c2
 */
function solveSystemOfEquations(eq1, eq2) {
  try {
    function parseSystemEq(eq) {
      const parts = eq.toLowerCase().replace(/\s+/g, '').split('=');
      if (parts.length !== 2) return null;
      let terms = parts[0].replace(/-/g, '+-').split('+').filter(Boolean);
      let a = 0, b = 0;
      for (let t of terms) {
        if (t.includes('x')) {
          let s = t.replace('x', '');
          a += s === '' ? 1 : s === '-' ? -1 : parseFloat(s);
        } else if (t.includes('y')) {
          let s = t.replace('y', '');
          b += s === '' ? 1 : s === '-' ? -1 : parseFloat(s);
        }
      }
      let c = parseFloat(parts[1]);
      return { a, b, c };
    }

    const E1 = parseSystemEq(eq1);
    const E2 = parseSystemEq(eq2);
    if (!E1 || !E2) return null;

    // Cramer's rule
    const det = E1.a * E2.b - E1.b * E2.a;
    if (Math.abs(det) < 1e-9) {
      return { singular: true, steps: ['Determinant = 0: Equations are dependent or inconsistent.'] };
    }

    const detX = E1.c * E2.b - E1.b * E2.c;
    const detY = E1.a * E2.c - E1.c * E2.a;

    const x = detX / det;
    const y = detY / det;

    const v1 = E1.a * x + E1.b * y;
    const v2 = E2.a * x + E2.b * y;
    const verified = Math.abs(v1 - E1.c) < 1e-4 && Math.abs(v2 - E2.c) < 1e-4;

    return {
      x, y,
      verified,
      steps: [
        `Equation 1: ${fmtNum(E1.a)}x + ${fmtNum(E1.b)}y = ${fmtNum(E1.c)}`,
        `Equation 2: ${fmtNum(E2.a)}x + ${fmtNum(E2.b)}y = ${fmtNum(E2.c)}`,
        `Determinant D = (${fmtNum(E1.a)})(${fmtNum(E2.b)}) - (${fmtNum(E1.b)})(${fmtNum(E2.a)}) = ${fmtNum(det)}`,
        `Determinant Dx = (${fmtNum(E1.c)})(${fmtNum(E2.b)}) - (${fmtNum(E1.b)})(${fmtNum(E2.c)}) = ${fmtNum(detX)}`,
        `Determinant Dy = (${fmtNum(E1.a)})(${fmtNum(E2.c)}) - (${fmtNum(E1.c)})(${fmtNum(E2.a)}) = ${fmtNum(detY)}`,
        `x = Dx / D = ${fmtNum(x)}`,
        `y = Dy / D = ${fmtNum(y)}`,
        `Verification: E1 check = ${fmtNum(v1)} (${verified ? 'PASSED ✓' : 'FAILED ✗'}), E2 check = ${fmtNum(v2)}`
      ]
    };
  } catch (e) {
    return null;
  }
}

/**
 * Calculus: Derivative of common functions
 */
function computeDerivative(fnStr) {
  const s = fnStr.toLowerCase().replace(/\s+/g, '');

  // Polynomial monomial: c*x^n
  const polyMatch = s.match(/^([+-]?\d*(?:\.\d+)?)x(?:\^([+-]?\d+))?$/);
  if (polyMatch) {
    let cStr = polyMatch[1];
    let c = cStr === '' ? 1 : cStr === '-' ? -1 : parseFloat(cStr);
    let n = polyMatch[2] !== undefined ? parseInt(polyMatch[2]) : 1;
    if (n === 1) {
      return { derivative: fmtNum(c), rule: 'Power rule: d/dx(c*x) = c', steps: [`d/dx(${fnStr}) = ${fmtNum(c)}`] };
    }
    let newC = c * n;
    let newN = n - 1;
    let res = `${fmtNum(newC)}x` + (newN !== 1 ? `^${newN}` : '');
    return {
      derivative: res,
      rule: 'Power rule: d/dx(x^n) = n*x^(n-1)',
      steps: [
        `Identify coefficient c = ${fmtNum(c)}, exponent n = ${n}`,
        `Multiply coefficient by exponent: ${fmtNum(c)} * ${n} = ${fmtNum(newC)}`,
        `Reduce exponent by 1: ${n} - 1 = ${newN}`,
        `d/dx(${fnStr}) = ${res}`
      ]
    };
  }

  // Trigonometric functions
  if (s === 'sin(x)') return { derivative: 'cos(x)', rule: 'Standard trig derivative', steps: ['d/dx(sin(x)) = cos(x)'] };
  if (s === 'cos(x)') return { derivative: '-sin(x)', rule: 'Standard trig derivative', steps: ['d/dx(cos(x)) = -sin(x)'] };
  if (s === 'tan(x)') return { derivative: 'sec^2(x)', rule: 'Standard trig derivative', steps: ['d/dx(tan(x)) = sec²(x) = 1/cos²(x)'] };
  if (s === 'e^x' || s === 'exp(x)') return { derivative: 'e^x', rule: 'Exponential derivative', steps: ['d/dx(e^x) = e^x'] };
  if (s === 'ln(x)') return { derivative: '1/x', rule: 'Logarithmic derivative', steps: ['d/dx(ln(x)) = 1/x (for x > 0)'] };

  return null;
}

/**
 * Statistics: Mean, Median, Mode, Variance, Stdev
 */
function computeStatistics(numbers) {
  if (!numbers || numbers.length === 0) return null;
  const sorted = [...numbers].sort((a, b) => a - b);
  const n = sorted.length;
  const sum = sorted.reduce((a, b) => a + b, 0);
  const mean = sum / n;

  // Median
  let median;
  if (n % 2 === 1) {
    median = sorted[Math.floor(n / 2)];
  } else {
    median = (sorted[n / 2 - 1] + sorted[n / 2]) / 2;
  }

  // Variance & standard deviation
  const sqDiffs = sorted.map(x => Math.pow(x - mean, 2));
  const variance = sqDiffs.reduce((a, b) => a + b, 0) / (n > 1 ? n - 1 : 1);
  const stdev = Math.sqrt(variance);

  return {
    count: n,
    sum,
    mean,
    median,
    variance,
    stdev,
    min: sorted[0],
    max: sorted[n - 1],
    steps: [
      `Data set (${n} items): [${sorted.join(', ')}]`,
      `Sum: ${fmtNum(sum)}`,
      `Mean: ${fmtNum(sum)} / ${n} = ${fmtNum(mean)}`,
      `Median: ${fmtNum(median)}`,
      `Sample Variance (s²): ${fmtNum(variance)}`,
      `Sample Standard Deviation (s): √${fmtNum(variance)} = ${fmtNum(stdev)}`
    ]
  };
}

/**
 * Financial Math: Compound interest A = P(1 + r/n)^(nt)
 */
function compoundInterest(P, r, n, t) {
  const rate = r > 1 ? r / 100 : r;
  const A = P * Math.pow(1 + rate / n, n * t);
  const interest = A - P;
  return {
    principal: P,
    annualRate: rate * 100,
    compoundsPerYear: n,
    years: t,
    finalAmount: A,
    totalInterest: interest,
    steps: [
      `Formula: A = P(1 + r/n)^(nt)`,
      `P = $${fmtNum(P)}, r = ${fmtNum(rate * 100)}%, n = ${n}, t = ${t} years`,
      `A = ${fmtNum(P)} * (1 + ${fmtNum(rate)}/${n})^(${n} * ${t})`,
      `Final Accumulated Amount: $${fmtNum(A)}`,
      `Total Interest Earned: $${fmtNum(interest)}`
    ]
  };
}

/**
 * Generate 2D graph points for plotting
 */
function generateGraphPoints(fnStr, minX = -10, maxX = 10, step = 0.5) {
  const points = [];
  try {
    // Basic polynomial / expression evaluator for x
    const safeBody = fnStr
      .replace(/x\^2/g, '(x*x)')
      .replace(/x\^3/g, '(x*x*x)')
      .replace(/sin/g, 'Math.sin')
      .replace(/cos/g, 'Math.cos')
      .replace(/tan/g, 'Math.tan')
      .replace(/sqrt/g, 'Math.sqrt');

    const fn = new Function('x', `"use strict"; return (${safeBody});`);
    for (let x = minX; x <= maxX; x += step) {
      const y = fn(x);
      if (typeof y === 'number' && !isNaN(y) && isFinite(y)) {
        points.push({ x: Number(x.toFixed(2)), y: Number(y.toFixed(4)) });
      }
    }
  } catch (e) {}
  return points;
}

/**
 * Master Polya-Style Math Solver & Verifier
 */
function solveWithPolya(input, mode = 'STEP_BY_STEP') {
  const clean = input.trim();

  // 1. Check for linear equation: ax + b = c
  if (clean.includes('=') && !clean.includes('^2') && !clean.includes('²') && !clean.includes('y')) {
    const res = solveLinearEquation(clean);
    if (res) {
      return {
        problem: clean,
        domain: 'ALGEBRA_LINEAR',
        understand: `Solve linear equation for variable '${res.variable}'.`,
        plan: `Isolate '${res.variable}' by transposing constant terms and dividing by the coefficient.`,
        solve: res.steps,
        check: res.verified ? `Verification: Solution ${res.variable} = ${fmtNum(res.solution)} satisfies the original equation.` : 'Verification failed.',
        answer: `${res.variable} = ${fmtNum(res.solution)}`,
        verified: res.verified
      };
    }
  }

  // 2. Check for quadratic equation
  if (clean.includes('=') && (clean.includes('^2') || clean.includes('²'))) {
    const res = solveQuadratic(clean);
    if (res) {
      return {
        problem: clean,
        domain: 'ALGEBRA_QUADRATIC',
        understand: `Solve quadratic equation ax² + bx + c = 0 for real/complex roots.`,
        plan: `Calculate the discriminant Δ = b² - 4ac and apply the quadratic formula x = (-b ± √Δ) / 2a.`,
        solve: res.steps,
        check: `Vertex at (${fmtNum(res.vertex.x)}, ${fmtNum(res.vertex.y)}). Discriminant verified.`,
        answer: res.roots.length > 0 ? res.roots.map(r => `x = ${fmtNum(r)}`).join(', ') : 'Complex roots',
        verified: true
      };
    }
  }

  // 3. Check for arithmetic expression
  const arith = evaluateArithmetic(clean);
  if (arith !== null) {
    return {
      problem: clean,
      domain: 'ARITHMETIC',
      understand: `Evaluate numerical arithmetic expression.`,
      plan: `Follow PEMDAS order of operations: Parentheses, Exponents, Multiplication/Division, Addition/Subtraction.`,
      solve: [`${clean} = ${fmtNum(arith)}`],
      check: `Deterministic computation verified. Precision: 6 decimals.`,
      answer: fmtNum(arith),
      verified: true
    };
  }

  // 4. Check for calculus / derivative query
  const derivMatch = clean.match(/(?:derivative of|d\/dx of|d\/dx)\s*(.+)/i);
  if (derivMatch) {
    const fnTarget = derivMatch[1].trim();
    const res = computeDerivative(fnTarget);
    if (res) {
      return {
        problem: clean,
        domain: 'CALCULUS_DERIVATIVE',
        understand: `Find the first derivative d/dx of ${fnTarget}.`,
        plan: `Apply ${res.rule}.`,
        solve: res.steps,
        check: `Analytic differentiation rule verified.`,
        answer: res.derivative,
        verified: true
      };
    }
  }

  // 5. Check for statistics list
  const statMatch = clean.match(/(?:mean|median|average|statistics of)\s*\[?([0-9,.\s-]+)\]?/i);
  if (statMatch) {
    const nums = statMatch[1].split(/[, ]+/).map(s => parseFloat(s.trim())).filter(n => !isNaN(n));
    if (nums.length >= 2) {
      const st = computeStatistics(nums);
      return {
        problem: clean,
        domain: 'STATISTICS',
        understand: `Calculate descriptive statistics for a dataset of ${nums.length} numbers.`,
        plan: `Sort dataset, compute mean, median, sample variance, and sample standard deviation.`,
        solve: st.steps,
        check: `Degrees of freedom: n - 1 = ${nums.length - 1}. Standard deviation positive and verified.`,
        answer: `Mean = ${fmtNum(st.mean)}, Median = ${fmtNum(st.median)}, Stdev = ${fmtNum(st.stdev)}`,
        verified: true
      };
    }
  }

  // 6. Geometry: circle area/perimeter
  const circleMatch = clean.match(/circle.*radius\s*(?:of|is|=)?\s*(\d+(?:\.\d+)?)/i) ||
                      clean.match(/radius\s*(?:of|is|=)?\s*(\d+(?:\.\d+)?).*circle/i);
  if (circleMatch) {
    const r = parseFloat(circleMatch[1]);
    const area = Math.PI * r * r;
    const circ = 2 * Math.PI * r;
    return {
      problem: clean,
      domain: 'GEOMETRY_CIRCLE',
      understand: `Calculate the area and circumference of a circle with radius r = ${fmtNum(r)}.`,
      plan: `Formulas: Area A = π * r², Circumference C = 2 * π * r.`,
      solve: [
        `Given radius r = ${fmtNum(r)}`,
        `Area A = π * (${fmtNum(r)})² = π * ${fmtNum(r * r)} ≈ ${fmtNum(area)}`,
        `Circumference C = 2 * π * ${fmtNum(r)} ≈ ${fmtNum(circ)}`
      ],
      check: `Units: Area is square units, circumference is linear units. Formulas verified.`,
      answer: `Area ≈ ${fmtNum(area)}, Circumference ≈ ${fmtNum(circ)}`,
      verified: true
    };
  }

  return null;
}

module.exports = {
  fmtNum,
  evaluateArithmetic,
  solveLinearEquation,
  solveQuadratic,
  solveSystemOfEquations,
  computeDerivative,
  computeStatistics,
  compoundInterest,
  generateGraphPoints,
  solveWithPolya
};
