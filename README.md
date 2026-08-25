# Spell It!

A collection of free spelling games for kids, from kindergarten through middle school. Built with Next.js (static export), Tailwind CSS, and shadcn/ui — no backend, no accounts, no ads.

## Games

| Game | What it practices |
| --- | --- |
| **Word Scramble** | Letter order — unscramble mixed-up tiles to rebuild the word |
| **Missing Letters** | Tricky letters — fill the gaps from a letter bank |
| **Listen & Spell** | Sounding out — hear a word (speech synthesis) and type it |
| **Spot the Word** | Proofreading — pick the one real spelling out of four |
| **Balloon Pop** | Letter patterns — guess letters before all six balloons pop (hangman, minus the hanging) |

Every game has four levels (Grades K–1, 2–3, 4–5, 6+) with word lists mixing early sight words and the words kids most often misspell. Rounds are 10 words with stars, streaks, a second try on misses, and a "words to practice" recap.

## Develop

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
```

Outputs a fully static site to `out/`.

## Deploy

Deployed on Vercel — import the repo at [vercel.com/new](https://vercel.com/new) and every push to `main` deploys automatically, zero config. The site is a plain static export (`out/`), so it can also be dropped onto any static host.

## Adding words

Word lists live in [lib/words.ts](lib/words.ts) — each entry is a word plus a kid-friendly clue. Add entries to any grade band and every game picks them up.
