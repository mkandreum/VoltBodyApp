// Servidor de pruebas LOCAL que imita la API de VoltBody (web/server) para probar la app en el emulador.
// Sin dependencias: `node devtools/mock-server.mjs` -> http://localhost:3000 ; en el emulador: `adb reverse tcp:3000 tcp:3000`
// y en la app (engranaje del login / Perfil > Servidor) usar http://localhost:3000
// Datos en memoria. Cuenta de prueba: ver devtools/README.md
import http from 'node:http';
import crypto from 'node:crypto';
import fs from 'node:fs';
import { resolveAlias } from '../../web/server/utils/exerciseAliases.js';
const DATASET = JSON.parse(fs.readFileSync(new URL('../../web/server/utils/exercises.json', import.meta.url), 'utf8'));
// Emparejado de GIF igual que el servidor real (tabla curada).
const enrichEx = (ex, force) => {
  if (!force && ex.gifUrl) return ex;
  const n = resolveAlias(ex.name, ex.nameEn); const it = n && DATASET.find((e) => e.name === n);
  return it ? { ...ex, gifUrl: GIF(it.gif_url) } : ex;
};

const PORT = 3000;
const GIF = (p) => `https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/${p}`;
const TEST_EMAIL = 'demo@voltbody.test';
const TEST_PASSWORD = 'Volt-demo-2026';

const today = new Date();
const day = (offset) => { const d = new Date(today); d.setDate(d.getDate() + offset); return d.toISOString().slice(0, 10); };

const ex = (id, name, group, gif, sets = 4, reps = '8-12', weight = 40, type) =>
  ({ id, name, sets, reps, weight, gifUrl: GIF(gif), muscleGroup: group, exerciseType: type,
     technique: '1. Colócate en posición estable.\n2. Baja controlando el peso.\n3. Sube de forma explosiva.' });

const routine = [
  { day: 'Lunes', focus: 'Pecho y tríceps', exercises: [
    ex('bench', 'Press de banca', 'Pecho', 'videos/0025-EIeI8Vf.gif', 4, '8-10', 60),
    ex('pushup', 'Flexiones', 'Pecho', 'videos/0662-I4hDWkc.gif', 3, '12-15', 0, 'bodyweight'),
    ex('lateral', 'Elevaciones laterales', 'Hombros', 'videos/0334-DsgkuIt.gif', 3, '12-15', 8) ] },
  { day: 'Martes', focus: 'Espalda y bíceps', exercises: [
    ex('row', 'Remo con barra', 'Espalda', 'videos/0027-eZyBC3j.gif', 4, '8-12', 50),
    ex('pulldown', 'Jalón al pecho', 'Espalda', 'videos/2330-LEprlgG.gif', 3, '10-12', 45),
    ex('curl', 'Curl con barra', 'Bíceps', 'videos/0025-EIeI8Vf.gif', 3, '10-12', 25) ] }, // GIF erroneo a proposito (press banca) para probar el re-emparejado
  { day: 'Miércoles', focus: 'Full body burn', exercises: [
    ex('goblet', 'Sentadilla goblet', 'Piernas', 'videos/1760-yn8yg1r.gif', 4, '10-12', 20),
    ex('bench', 'Press de banca', 'Pecho', 'videos/0025-EIeI8Vf.gif', 3, '8-10', 60),
    ex('row', 'Remo con barra', 'Espalda', 'videos/0027-eZyBC3j.gif', 3, '8-12', 50),
    ex('plank', 'Plancha', 'Core', 'videos/0464-CosupLu.gif', 3, '30-45s', 0, 'isometric') ] },
  { day: 'Viernes', focus: 'Piernas y core', exercises: [
    ex('goblet', 'Sentadilla goblet', 'Piernas', 'videos/1760-yn8yg1r.gif', 4, '10-12', 22),
    ex('plank', 'Plancha', 'Core', 'videos/0464-CosupLu.gif', 3, '30-45s', 0, 'isometric') ] },
];
const diet = { dailyCalories: 2300, macros: { protein: 160, carbs: 250, fat: 70 }, meals: [
  { id: 'm1', name: 'Avena con frutos rojos', time: '08:00', calories: 480, protein: 30, carbs: 60, fat: 12, description: 'Avena, yogur griego y fruta' },
  { id: 'm2', name: 'Pollo con arroz y verduras', time: '14:00', calories: 720, protein: 55, carbs: 80, fat: 18, description: 'Pechuga a la plancha' },
  { id: 'm3', name: 'Batido y tostada', time: '18:00', calories: 380, protein: 30, carbs: 40, fat: 10, description: 'Batido de proteína' },
  { id: 'm4', name: 'Salmón con boniato', time: '21:00', calories: 650, protein: 45, carbs: 50, fat: 25, description: 'Salmón al horno' } ] };

const users = new Map(); // email -> user
function makeUser(email, password, name) {
  const id = crypto.randomUUID();
  const u = { id, email, password, name, profile: { id: crypto.randomUUID(), userId: id }, logs: [], photos: [], weights: [], metrics: [], mealLogs: [], goals: [], achievements: [], custom: [], recovery: [] };
  users.set(email, u); return u;
}
// Usuario de prueba con datos de ejemplo
const demo = makeUser(TEST_EMAIL, TEST_PASSWORD, 'Daniel');
Object.assign(demo.profile, { age: 30, weight: 78, height: 178, gender: 'Hombre', goal: 'Ganar músculo', goalDirection: 'Ganar', goalTargetKg: 4,
  goalTimelineMonths: 4, trainingDaysPerWeek: 4, sessionMinutes: 60, theme: 'verde-negro', routine, diet,
  insights: { sleepRecommendation: 'Duerme 7-8 h', estimatedResults: 'Cambios visibles en 6 semanas', dailyQuote: 'La disciplina vence al talento.' } });
for (let i = 6; i >= 0; i--) demo.metrics.push({ date: day(-i), steps: 3500 + ((i * 1370) % 6000), distanceKm: 2.4 + i * 0.4, activeKcal: 280 + i * 30, waterMl: 750 + i * 200 });
for (let i = 0; i < 6; i++) demo.weights.push({ id: crypto.randomUUID(), date: day(-i * 4) + 'T00:00:00.000Z', weight: 78 + i * 0.3 });
const ago = (d, h = 10) => { const t = new Date(today); t.setDate(t.getDate() - d); t.setHours(h, 0, 0, 0); return t.toISOString(); };
for (const [d, w] of [[14, 50], [10, 52.5], [7, 55], [3, 57.5]]) for (let s = 0; s < 3; s++) demo.logs.push({ id: crypto.randomUUID(), date: ago(d, 10 + s), exerciseId: 'bench', weight: w, reps: 10 - s, duration: null, rpe: null, rir: 2 });
demo.mealLogs.push({ date: day(0), mealId: 'm1' });

const tokens = new Map(); // token -> email
const json = (res, code, body) => { res.writeHead(code, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }); res.end(JSON.stringify(body)); };
const readBody = (req) => new Promise((r) => { let b = ''; req.on('data', (c) => (b += c)); req.on('end', () => { try { r(b ? JSON.parse(b) : {}); } catch { r({}); } }); });
const authPayload = (u) => ({ token: (() => { const t = crypto.randomBytes(16).toString('hex'); tokens.set(t, u.email); return t; })(),
  user: { id: u.id, email: u.email, name: u.name }, profile: u.profile, routine: u.profile.routine, diet: u.profile.diet, insights: u.profile.insights });

http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://x'); const p = url.pathname; const m = req.method;
  console.log(m, p);
  if (m === 'OPTIONS') return json(res, 204, {});
  if (p === '/api/ping') return json(res, 200, { status: 'ok' });
  const body = ['POST', 'PUT'].includes(m) ? await readBody(req) : {};
  if (p === '/api/auth/login' && m === 'POST') {
    const u = users.get(body.email); if (!u || u.password !== body.password) return json(res, 401, { error: 'Invalid credentials' });
    return json(res, 200, authPayload(u));
  }
  if (p === '/api/auth/register' && m === 'POST') {
    if (!body.email || !body.password || body.password.length < 6) return json(res, 400, { error: 'Invalid request', details: 'Password must have at least 6 characters' });
    if (users.has(body.email)) return json(res, 400, { error: 'User already exists' });
    return json(res, 200, authPayload(makeUser(body.email, body.password, body.name)));
  }
  const tok = (req.headers.authorization || '').split(' ')[1]; const u = users.get(tokens.get(tok));
  if (!u) return json(res, 401, { error: 'Invalid token' });
  const P = u.profile;
  if (p === '/api/user/me') return json(res, 200, { id: u.id, email: u.email, name: u.name, profile: P });
  if (p === '/api/profile' && m === 'GET') return json(res, 200, P);
  if (p === '/api/profile' && m === 'PUT') { for (const [k, v] of Object.entries(body)) if (v !== undefined) P[k] = v; return json(res, 200, P); }
  if (p === '/api/workout/logs' && m === 'GET') return json(res, 200, [...u.logs].reverse());
  if (p === '/api/workout/logs' && m === 'POST') { const l = { id: crypto.randomUUID(), ...body }; u.logs.push(l); return json(res, 200, l); }
  const logId = p.match(/^\/api\/workout\/logs\/(.+)$/)?.[1];
  if (logId && m === 'PUT') { const l = u.logs.find((x) => x.id === logId); if (!l) return json(res, 404, { error: 'Not found' }); Object.assign(l, body); return json(res, 200, l); }
  if (logId && m === 'DELETE') { u.logs = u.logs.filter((x) => x.id !== logId); return json(res, 200, { ok: true }); }
  if (p === '/api/workout/photos') { if (m === 'POST') u.photos.push({ id: crypto.randomUUID(), ...body }); return json(res, 200, m === 'POST' ? u.photos.at(-1) : u.photos); }
  if (p === '/api/workout/weight-logs') {
    if (m === 'POST') { const d = String(body.date).slice(0, 10) + 'T00:00:00.000Z'; u.weights = u.weights.filter((w) => w.date !== d); u.weights.push({ id: crypto.randomUUID(), date: d, weight: body.weight }); }
    return json(res, 200, m === 'POST' ? u.weights.at(-1) : [...u.weights].sort((a, b) => a.date.localeCompare(b.date)));
  }
  if (p === '/api/data/all') return json(res, 200, { metrics: u.metrics, mealLogs: u.mealLogs, goals: u.goals, achievements: u.achievements, customWorkout: u.custom, recovery: u.recovery });
  const mDate = p.match(/^\/api\/data\/metrics\/(.+)$/)?.[1];
  if (mDate) { let row = u.metrics.find((x) => x.date === mDate); if (!row) { row = { date: mDate }; u.metrics.push(row); } Object.assign(row, body); return json(res, 200, row); }
  if (p === '/api/data/meals') { u.mealLogs = u.mealLogs.filter((x) => !(x.date === body.date && x.mealId === body.mealId)); if (body.eaten) u.mealLogs.push({ date: body.date, mealId: body.mealId }); return json(res, 200, { ok: true }); }
  if (p === '/api/data/goals') { u.goals = u.goals.filter((g) => g.weekStart !== body.weekStart).concat((body.goals || []).map((g) => ({ weekStart: body.weekStart, goalId: g.id, label: g.label, done: g.done }))); return json(res, 200, { ok: true }); }
  if (p === '/api/data/achievements') { if (!u.achievements.find((a) => a.id === body.id)) u.achievements.push({ id: body.id, unlockedAt: body.unlockedAt || new Date().toISOString() }); return json(res, 200, u.achievements.find((a) => a.id === body.id)); }
  if (p === '/api/data/custom-workout') { u.custom = body.exercises || []; return json(res, 200, { ok: true }); }
  const rDate = p.match(/^\/api\/data\/recovery\/(.+)$/)?.[1];
  if (rDate) { u.recovery = u.recovery.filter((x) => x.date !== rDate).concat({ date: rDate, ...body }); return json(res, 200, { date: rDate, ...body }); }
  if (p === '/api/ai/generate-plan') return json(res, 200, { routine, diet, insights: P.insights, fallback: true, details: 'Plan de prueba (servidor local)' });
  if (p === '/api/ai/generate-day-meals') return json(res, 200, { meals: body.diet.meals.map((m) => ({ ...m, name: 'Menú nuevo · ' + m.time, description: 'Menú regenerado (servidor de prueba)' })) });
  if (p === '/api/ai/generate-alternative-meal') return json(res, 200, { ...body.oldMeal, id: body.oldMeal.id + '-alt', name: 'Bowl de quinoa y pavo', description: 'Alternativa equivalente (servidor de prueba)' });
  if (p === '/api/ai/generate-progress-report') return json(res, 200, { overallScore: 72, progressPercent: 40, consistencyPercent: 65, nutritionPercent: 58, trainingExecutionPercent: 70, weeksToVisibleChange: 5, summary: 'Vas por buen camino.', improvements: ['Más proteína'], nextActions: ['Sube 2,5 kg en press'] });
  if (p === '/api/ai/enrich-routine') return json(res, 200, { routine: (body.routine || []).map((d) => ({ ...d, exercises: (d.exercises || []).map((e) => enrichEx(e, body.force === true)) })) });
  json(res, 404, { error: 'Resource not found' });
}).listen(PORT, '127.0.0.1', () => console.log(`Mock VoltBody API en http://localhost:${PORT}`));
