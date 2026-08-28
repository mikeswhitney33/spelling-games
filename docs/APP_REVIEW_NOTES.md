# App Review Information — Spell It! (com.skdaddle.spellit)

Paste the "Notes" section below into App Store Connect → App Review Information →
Notes for every submission. The rest of this file is the working checklist behind it.

Submitted build: **1.0 (1)**, uploaded 2026-08-27, built with Xcode 26.5 /
iOS 26.5 SDK, arm64, minimum iOS 17.0, universal (iPhone + iPad).

---

## Notes (paste into App Store Connect)

**No account is required.** There is no login, no registration, no demo credentials,
no paid content, no subscriptions, and no in-app purchases. Launch the app and every
feature is immediately available offline.

**What the app is.** Spell It! is a collection of twelve short spelling games for
children in kindergarten through middle school: Daily Bee, Word Scramble, Missing
Letters, Listen & Spell, Spot the Word, Flash Spell, Fix the Sentence, Ending
Machine, Mini Crossword, Word Search, Memory Match, and Balloon Pop. Every game has
four levels (Grades K–1, 2–3, 4–5, 6+) and plays a 10-word round with stars, a second
try after a miss, and a "words to practice" recap.

**Target audience and value.** Children roughly ages 5–13, and the parents and
teachers who set them up. Spelling practice is usually a worksheet or a flashcard
stack; the problem is that repetition without variety loses kids fast. Spell It!
turns the same 10-word list into twelve different game shapes — sounding out, letter
order, proofreading, whole-word recall, clue solving — so practice stays interesting.
Parents and teachers can also build their own word lists (for example, this week's
class spelling list) and every game will draw from it.

**How to reach the main features.**
1. Launch the app — the home screen shows all twelve games in a grid.
2. Tap any game tile to play a round. Level is chosen inside the game and remembered.
3. Tap the gear icon ("Word lists") in the top-right of the home screen for the word
   list manager: pick the active list, tap "New list" to create a custom one, or
   swipe a built-in list to duplicate it as a starting point.
4. Listen & Spell and the "Hear it" buttons speak the word using Apple's on-device
   speech synthesizer — please make sure the device is not on silent/low volume.

**External services, tools, and platforms: none.** The app contains no networking
code and makes no network requests. It works fully offline in airplane mode. There
are no third-party SDKs, no analytics, no advertising, no authentication provider, no
payment processor, and no AI or LLM services. All word data ships inside the app
bundle. The only system framework used for content is AVFoundation's
`AVSpeechSynthesizer` (on-device text-to-speech, playback only — no recording, no
microphone use).

**Permissions.** The app requests no permissions at all. No location, contacts,
camera, microphone, photos, notifications, or App Tracking Transparency prompts
exist in the app.

**Data collection: none.** The app collects and transmits no data. The only stored
state is the chosen level, the daily-challenge streak, and any custom word lists the
user creates, all kept in `UserDefaults` on the device.

**User-generated content.** Custom word lists a parent or teacher types in are stored
only on that device. Nothing is uploaded, shared between users, or visible to anyone
else, so there is no feed, no messaging, and no reporting/blocking surface to
demonstrate.

**Regional differences: none.** The app behaves identically in every region and
storefront. It is English (en-US) only, has no geo-gated features, no region-specific
content, and no server that could vary by region. Speech uses the device's en-US
system voice.

**Regulated industry / third-party material: none.** The app is not in a regulated
industry. All word lists, hints, and sentences were written by the developer for this
app. The "wrong spellings" shown in Spot the Word and Fix the Sentence are generated
by an algorithm, then filtered against a dictionary-derived guard list and an
explicit blocked-words list so that a generated misspelling can never be a real word
or an inappropriate word.

**Devices tested before submission.** iPhone 13 Pro running iOS 26.6 (physical
device); <!-- TODO: add any other physical devices, e.g. iPad model + iPadOS
version --> plus the iPhone 17 and iPad simulators on Xcode 26.5.

---

## Screen recording — shot list

Apple requires a recording captured **on a physical device running the latest OS**,
starting from app launch. Record on the iPhone 13 Pro (iOS 26.6) with Control Center →
Screen Recording, or plug the device into the Mac and use QuickTime → File → New Movie
Recording → select the iPhone as camera source. Turn on the ringer so the spoken words
are audible; enable microphone-off (system audio only is fine).

Aim for 2–4 minutes covering:

1. **Launch from the home screen** — tap the Spell It! icon, show the app opening
   cold. (Do not start from an already-running app.)
2. **Home screen scroll** — the twelve game tiles and the four level cards.
3. **Word Scramble** — play at least three words, including one wrong answer to show
   the second-try behavior, then reach the round recap with stars.
4. **Listen & Spell** — show the "Hear it" button speaking a word (audible), type the
   answer.
5. **Spot the Word** or **Fix the Sentence** — shows the misspelling-based games.
6. **Mini Crossword** or **Word Search** — shows a generated-grid game.
7. **Daily Bee** — the daily round and the streak counter.
8. **Word lists (gear icon)** — create a new custom list, add a word with a hint,
   set it active, then start a game using it. This is the only place users enter
   content.
9. **Airplane mode on, play one more round** — optional but strongly recommended:
   proves the app is fully offline and uses no external services.

There is nothing to record for: account registration/login/deletion (none exist),
purchases or subscriptions (none exist), content reporting/blocking (no shared
content), and permission prompts (none are requested).

---

## Open items to confirm before replying

- [ ] **iPad testing.** The binary is universal (`TARGETED_DEVICE_FAMILY = 1,2`), so
      App Review will run it on an iPad. Test on a physical iPad and add the model +
      iPadOS version to the notes above, or drop iPad support from the target.
- [ ] **Privacy policy URL.** Required in App Store Connect, and required to be shown
      in-app if the Kids Category is enabled. The website has no privacy page today.
- [ ] **Kids Category.** Confirm whether it is enabled in App Store Connect. The app
      already satisfies its rules (no third-party analytics/ads, no external links,
      no purchases, no data collection).
- [ ] **Screenshots.** Guideline 2.3.3 — make sure the App Store screenshots show
      actual gameplay, not just the home/title screen.
- [ ] **Privacy manifest.** The app has no `PrivacyInfo.xcprivacy`. It uses a
      required-reason API (`UserDefaults`, reason `CA92.1`). Not what this rejection
      is about, but cheap to add.
