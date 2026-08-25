import Link from "next/link";
import { Puzzle, Search, Shuffle, Volume2 } from "lucide-react";
import type { ComponentType } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { COLOR_STYLES, GAMES } from "@/lib/games";
import { GRADE_BANDS } from "@/lib/words";
import { cn } from "@/lib/utils";

const GAME_ICONS: Record<string, ComponentType<{ className?: string }>> = {
  "word-scramble": Shuffle,
  "missing-letters": Puzzle,
  "listen-and-spell": Volume2,
  "spot-the-word": Search,
};

const HERO_TILES = [
  { letter: "s", tone: "bg-coral-soft", tilt: "-rotate-6" },
  { letter: "p", tone: "bg-sun-soft", tilt: "rotate-3" },
  { letter: "e", tone: "bg-leaf-soft", tilt: "-rotate-2" },
  { letter: "l", tone: "bg-sky-soft", tilt: "rotate-6" },
  { letter: "l", tone: "bg-card", tilt: "-rotate-3" },
  { letter: "i", tone: "bg-sun-soft", tilt: "rotate-2" },
  { letter: "n", tone: "bg-coral-soft", tilt: "-rotate-3" },
  { letter: "g", tone: "bg-leaf-soft", tilt: "rotate-6" },
];

export default function Home() {
  return (
    <div>
      {/* Hero on ruled notebook paper */}
      <section className="ruled-paper border-b-[3px] border-ink/10">
        <div className="mx-auto max-w-5xl px-4 py-14 text-center sm:px-6 sm:py-20">
          <div
            className="flex flex-wrap items-end justify-center gap-1.5 sm:gap-2"
            aria-hidden="true"
          >
            {HERO_TILES.map((tile, i) => (
              <span
                key={i}
                className={cn(
                  "tile h-12 w-12 text-2xl sm:h-16 sm:w-16 sm:text-4xl",
                  tile.tone,
                  tile.tilt,
                )}
              >
                {tile.letter}
              </span>
            ))}
          </div>
          <h1 className="font-heading mx-auto mt-8 max-w-2xl text-4xl font-semibold sm:text-5xl">
            Practice that feels like recess.
          </h1>
          <p className="mx-auto mt-4 max-w-xl text-lg text-muted-foreground">
            Four quick games, four levels — from first words like{" "}
            <em className="font-semibold not-italic text-coral">cat</em> to
            champion stumpers like{" "}
            <em className="font-semibold not-italic text-sky">mischievous</em>.
            Pick your grade and go.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Button
              size="lg"
              className="font-heading text-base"
              nativeButton={false}
              render={<Link href="/#games" />}
            >
              Pick a game
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="font-heading text-base bg-card"
              nativeButton={false}
              render={<Link href="/#levels" />}
            >
              How levels work
            </Button>
          </div>
        </div>
      </section>

      {/* Games */}
      <section id="games" className="scroll-mt-6">
        <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
          <h2 className="font-heading text-2xl font-semibold sm:text-3xl">
            Choose your game
          </h2>
          <p className="mt-1 text-muted-foreground">
            Every round is 10 words. Earn stars, build streaks, and collect
            words to practice.
          </p>
          <div className="mt-6 grid gap-5 sm:grid-cols-2">
            {GAMES.map((game) => {
              const Icon = GAME_ICONS[game.slug];
              const colors = COLOR_STYLES[game.color];
              return (
                <Link
                  key={game.slug}
                  href={`/games/${game.slug}/`}
                  className="group rounded-xl focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
                >
                  <Card
                    className={cn(
                      "h-full border-t-8 transition-transform group-hover:-translate-y-1",
                      colors.borderT,
                    )}
                  >
                    <CardContent className="flex h-full flex-col gap-3 pt-5">
                      <div className="flex items-center gap-3">
                        <span
                          className={cn("tile h-12 w-12", colors.soft)}
                          aria-hidden="true"
                        >
                          <Icon className="h-6 w-6" />
                        </span>
                        <div>
                          <h3 className="font-heading text-xl font-semibold">
                            {game.title}
                          </h3>
                          <p
                            className={cn(
                              "font-heading text-sm font-medium",
                              colors.text,
                            )}
                          >
                            {game.tagline}
                          </p>
                        </div>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {game.description}
                      </p>
                      <div className="mt-auto flex flex-wrap gap-1.5">
                        {game.skills.map((skill) => (
                          <Badge key={skill} variant="secondary">
                            {skill}
                          </Badge>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              );
            })}
          </div>
        </div>
      </section>

      {/* Levels */}
      <section id="levels" className="scroll-mt-6 bg-secondary/60">
        <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
          <h2 className="font-heading text-2xl font-semibold sm:text-3xl">
            A level for every speller
          </h2>
          <p className="mt-1 text-muted-foreground">
            Change your level any time from inside a game — your pick is
            remembered on this device.
          </p>
          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {GRADE_BANDS.map((band, i) => (
              <div
                key={band.id}
                className="rounded-xl border-[3px] border-ink bg-card p-4 shadow-[0_4px_0_var(--ink)]"
              >
                <span
                  className={cn(
                    "tile h-10 w-14 text-base",
                    ["bg-coral-soft", "bg-sun-soft", "bg-leaf-soft", "bg-sky-soft"][i],
                  )}
                  aria-hidden="true"
                >
                  {band.short}
                </span>
                <h3 className="font-heading mt-3 text-lg font-semibold">
                  {band.label}
                </h3>
                <p className="mt-1 text-sm text-muted-foreground">{band.blurb}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* For grown-ups */}
      <section className="mx-auto max-w-5xl px-4 py-12 sm:px-6">
        <h2 className="font-heading text-2xl font-semibold">For grown-ups</h2>
        <p className="mt-2 max-w-2xl text-muted-foreground">
          Word lists mix early sight words with the words kids most often
          misspell at each grade. Wrong answers get a second try, then the
          correct spelling is shown in tiles — and every round ends with a
          &ldquo;words to practice&rdquo; list you can review together. No
          accounts, no tracking, and it works on phones and tablets.
        </p>
      </section>
    </div>
  );
}
