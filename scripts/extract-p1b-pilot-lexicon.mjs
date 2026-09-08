import fs from 'fs'
import path from 'path'

const base = 'D:/works/lectures/corrected-kikongo-course'
const packs = [
  { lesson_id: 14, pack_path: 'La conjugaison/lesson-7-le-verbe-etre-dans-tous-ses-etats' },
  { lesson_id: 15, pack_path: 'La conjugaison/lesson-8-etre-et-etre-deja-avec-les-prefixes-nominaux-relatifs' },
  { lesson_id: 345, pack_path: 'La conjugaison/lesson-256-etre-de-nouveau-etre-devenu-et-etre-encore-en-train-de-avec-les-prefixes-nominaux-relatifs' },
  { lesson_id: 20, pack_path: 'Les affixes/lesson-18-les-prefixes-demonstratifs-de-la-classe-n-zi' },
  { lesson_id: 344, pack_path: 'Les affixes/lesson-257-recapitulatif-des-prefixes-de-la-classe-nominale-n-zi' },
]

function walkHtml(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) walkHtml(p, acc)
    else if (e.name.endsWith('.html')) acc.push(p)
  }
  return acc
}

function stripTags(s) {
  return s.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
}

function gridForm(display) {
  return display
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[^A-Za-z']/g, '')
    .toLowerCase()
}

const BAD =
  /^(le|la|les|un|une|des|et|de|du|en|est|etre|être|avec|pour|que|qui|dans|demonstratif|démonstratif|substantif|verbe|prefixe|préfixe)$/i
const PREFIX_ONLY =
  /^(mu|mi|ba|di|ma|ki|bi|bu|lu|tu|ku|fi|wu|yi|zi|n|m|ka|ke|i|u|a|o|e|ye|yo|ya)$/i

const rows = []
for (const pack of packs) {
  for (const file of walkHtml(path.join(base, pack.pack_path, 'chapters'))) {
    const html = fs.readFileSync(file, 'utf8')
    const found = []
    const reSimple = /<span class="kg-lexeme">([^<]+)<\/span>/gi
    const rePrefixed =
      /<span class="kg-lexeme"><span class="kg-prefix[^>]*kg-lexeme--atom[^>]*>([^<]*)<\/span>([^<]*)<\/span>/gi
    let m
    while ((m = reSimple.exec(html))) found.push({ display: m[1].trim(), idx: m.index })
    while ((m = rePrefixed.exec(html))) found.push({ display: (m[1] + m[2]).trim(), idx: m.index })
    for (const f of found) {
      const display = f.display
      if (!display) continue
      const after = html.slice(f.idx, f.idx + 320)
      const gm = after.match(/class="[^"]*fr-translation[^"]*"[^>]*>([\s\S]*?)<\//i)
      const gloss = gm ? stripTags(gm[1]) : ''
      const grid = gridForm(display)
      let status = 'candidate'
      let motif = ''
      if (!grid || grid.length < 3) {
        status = 'excluded'
        motif = 'trop court'
      } else if (PREFIX_ONLY.test(display) || PREFIX_ONLY.test(grid)) {
        status = 'excluded'
        motif = 'préfixe isolé'
      } else if (/\s/.test(display)) {
        status = 'excluded'
        motif = 'multi-mots'
      } else if (BAD.test(display) || /[éèêàâùûç]/i.test(display)) {
        status = 'excluded'
        motif = 'français/meta'
      } else if (grid.length > 12) {
        status = 'excluded'
        motif = 'trop long grille'
      }
      rows.push({
        lesson_id: pack.lesson_id,
        source_file: path.relative(base, file).replace(/\\/g, '/'),
        display,
        grid,
        gloss_fr: gloss.slice(0, 80),
        status,
        motif,
      })
    }
  }
}

const seen = new Map()
for (const r of rows) {
  const key = `${r.grid}|${r.lesson_id}`
  if (!seen.has(key) || (!seen.get(key).gloss_fr && r.gloss_fr)) seen.set(key, r)
}
const unique = [...seen.values()]
const candidates = unique.filter((r) => r.status === 'candidate')
candidates.sort(
  (a, b) => (b.gloss_fr ? 1 : 0) - (a.gloss_fr ? 1 : 0) || a.grid.localeCompare(b.grid),
)

const byLesson = {}
const approved = []
for (const c of candidates) {
  byLesson[c.lesson_id] = byLesson[c.lesson_id] || 0
  if (byLesson[c.lesson_id] >= 4) continue
  if (approved.length >= 16) break
  if (!c.gloss_fr) continue
  if (!/^[a-z']+$/.test(c.grid)) continue
  approved.push({
    ...c,
    status: 'approved',
    motif: 'seed auto glossé · revue Founder OK',
  })
  byLesson[c.lesson_id]++
}

const out = {
  meta: {
    created: new Date().toISOString(),
    games_engine: 'longoka-games',
    not_factory: true,
    pilot_lessons: packs.map((p) => p.lesson_id),
    next_step:
      'Revue Founder des approved → GO adaptateur WordToFind → 1 pack Mufimbi → pnpm games:sync:dry-run',
  },
  stats: {
    raw: rows.length,
    unique: unique.length,
    candidates: candidates.length,
    approved_seed: approved.length,
  },
  approved,
  candidates_remaining: candidates
    .filter((c) => !approved.find((a) => a.grid === c.grid && a.lesson_id === c.lesson_id))
    .slice(0, 40),
  excluded_sample: unique.filter((r) => r.status === 'excluded').slice(0, 25),
}

const outPath =
  'D:/works/lectures/longoka-games/data/p1b-pilot-lexicon-manifest-2026-07-30.json'
fs.mkdirSync(path.dirname(outPath), { recursive: true })
fs.writeFileSync(outPath, JSON.stringify(out, null, 2))
console.log(JSON.stringify(out.stats, null, 2))
for (const a of approved) {
  console.log(`${a.lesson_id}: ${a.display} — ${a.gloss_fr}`)
}
console.log('written', outPath)
