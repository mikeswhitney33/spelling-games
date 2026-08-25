# Spell It!

A collection of free spelling games for kids, from kindergarten through middle school. Built with Next.js (static export), Tailwind CSS, and shadcn/ui — no backend, no accounts, no ads.

## Games

| Game | What it practices |
| --- | --- |
| **Word Scramble** | Letter order — unscramble mixed-up tiles to rebuild the word |
| **Missing Letters** | Tricky letters — fill the gaps from a letter bank |
| **Listen & Spell** | Sounding out — hear a word (speech synthesis) and type it |
| **Spot the Word** | Proofreading — pick the one real spelling out of four |

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

## Deploy to GitHub Pages

1. Push this repo to GitHub.
2. In the repo settings, go to **Settings → Pages** and set **Source** to **GitHub Actions**.
3. Push to `main` — the included workflow ([.github/workflows/deploy.yml](.github/workflows/deploy.yml)) builds and deploys automatically.

The workflow sets the base path to `/<repo-name>` for project sites and to `/` for `<user>.github.io` repos, so it works for either without changes. For a local build that mirrors a project-site deploy:

```bash
NEXT_PUBLIC_BASE_PATH=/spelling-games npm run build
```

## Adding words

Word lists live in [lib/words.ts](lib/words.ts) — each entry is a word plus a kid-friendly clue. Add entries to any grade band and every game picks them up.
