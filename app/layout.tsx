import type { Metadata } from "next";
import { Fredoka, Nunito } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const fredoka = Fredoka({
  variable: "--font-fredoka",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const nunito = Nunito({
  variable: "--font-nunito",
  subsets: ["latin"],
  weight: ["400", "600", "700", "800"],
});

export const metadata: Metadata = {
  title: "Spell It! — Spelling Games for Kids",
  description:
    "Free spelling games for kids from kindergarten through middle school — grade-level word lists, playful letter tiles, and practice that feels like recess.",
};

function LogoTiles() {
  const letters = ["S", "P", "E", "L", "L"];
  const tones = [
    "bg-coral-soft",
    "bg-sun-soft",
    "bg-leaf-soft",
    "bg-sky-soft",
    "bg-card",
  ];
  return (
    <span className="inline-flex items-end gap-1" aria-hidden="true">
      {letters.map((letter, i) => (
        <span
          key={i}
          className={`tile h-8 w-8 text-base ${tones[i]} ${i % 2 === 1 ? "-rotate-3" : "rotate-2"}`}
        >
          {letter}
        </span>
      ))}
    </span>
  );
}

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      className={`${fredoka.variable} ${nunito.variable} h-full antialiased`}
    >
      <body className="min-h-full flex flex-col">
        <header className="border-b-[3px] border-ink/10">
          <div className="mx-auto flex w-full max-w-5xl items-center justify-between px-4 py-3 sm:px-6">
            <Link
              href="/"
              className="flex items-center gap-3 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4"
            >
              <LogoTiles />
              <span className="font-heading text-xl font-semibold tracking-wide">
                it!
              </span>
              <span className="sr-only">Spell It! home</span>
            </Link>
            <nav className="flex items-center gap-4">
              <Link
                href="/#games"
                className="font-heading text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                All games
              </Link>
              <Link
                href="/settings"
                className="font-heading text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                Word lists
              </Link>
            </nav>
          </div>
        </header>
        <main className="flex-1">{children}</main>
        <footer className="border-t-[3px] border-ink/10 py-6">
          <p className="mx-auto max-w-5xl px-4 text-center text-sm text-muted-foreground sm:px-6">
            Spell It! is a free practice site for young spellers. No accounts,
            no ads — just words.
          </p>
        </footer>
      </body>
    </html>
  );
}
