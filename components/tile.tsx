"use client";

import {
  Children,
  createContext,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type ReactNode,
} from "react";

import { cn } from "@/lib/utils";

export type TileSize = "xs" | "sm" | "md" | "lg";

const SIZE_CLASSES: Record<TileSize, string> = {
  xs: "h-8 w-8 text-sm rounded-lg border-2 shadow-[0_3px_0_var(--ink)]",
  sm: "h-10 w-10 text-lg",
  md: "h-12 w-12 text-2xl",
  lg: "h-14 w-14 text-3xl",
};

const SIZE_PX: Record<TileSize, number> = { xs: 32, sm: 40, md: 48, lg: 56 };

/* --------------------------------------------------------------------------
 * Fitting a word onto one line
 *
 * Fixed tile sizes wrap a word wherever the flex row happens to run out of
 * room, which leaves orphan letters on the next line and is hard to read.
 * TileRow instead measures the space it has and shrinks the tiles so the whole
 * word sits on one line. Only when that would push the letters below a legible
 * size does it split — and then into even rows, so "pronunciation" reads as
 * 7 + 6 rather than 9 + 4.
 * ----------------------------------------------------------------------- */

/** Full-size tile; matches the old "md". */
const TILE_MAX = 48;
/** Smallest tile a young reader can still read comfortably. */
const TILE_FLOOR = 28;
/** Absolute floor, only reached when even three rows will not fit. */
const TILE_HARD_MIN = 18;
/** Gaps tighten alongside the tiles so narrow screens buy back some room. */
const GAP_LG = 6;
const GAP_SM = 4;
/** Past this, splitting the word does more harm than shrinking it. */
const MAX_ROWS = 3;

export interface TileFit {
  /** Edge length of each tile, in CSS pixels. */
  px: number;
  /** Gap between tiles, in CSS pixels. */
  gap: number;
  /** How many tiles go on each line. */
  perRow: number;
}

function sizeFor(perRow: number, width: number, extra: number) {
  const roomy = (width - extra - (perRow - 1) * GAP_LG) / perRow;
  if (roomy >= 36) return { px: Math.min(TILE_MAX, roomy), gap: GAP_LG };
  const tight = (width - extra - (perRow - 1) * GAP_SM) / perRow;
  return { px: Math.min(TILE_MAX, tight), gap: GAP_SM };
}

/**
 * Lay `count` tiles into `width` pixels, reserving `extra` pixels for anything
 * else sharing the line (the "+" and "=" in Ending Machine, say).
 */
export function fitTiles(count: number, width: number, extra = 0): TileFit {
  if (count <= 0) return { px: TILE_MAX, gap: GAP_LG, perRow: 1 };
  // Before the first measurement, fall back to the static heuristic.
  if (width <= 0) {
    const guess = count > 10 ? "xs" : count > 7 ? "sm" : "md";
    return { px: SIZE_PX[guess], gap: GAP_LG, perRow: count };
  }
  for (let rows = 1; rows <= MAX_ROWS; rows++) {
    const perRow = Math.ceil(count / rows);
    const { px, gap } = sizeFor(perRow, width, extra);
    if (px >= TILE_FLOOR || rows === MAX_ROWS) {
      return { px: Math.max(TILE_HARD_MIN, Math.floor(px)), gap, perRow };
    }
  }
  /* c8 ignore next */
  throw new Error("unreachable");
}

const useIsomorphicLayoutEffect =
  typeof window === "undefined" ? useEffect : useLayoutEffect;

/** Measures an element and reports a tile layout that fits inside it. */
export function useTileFit(count: number, extra = 0) {
  const ref = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(0);

  useIsomorphicLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    setWidth(el.getBoundingClientRect().width);
    const observer = new ResizeObserver((entries) => {
      setWidth(entries[0].contentRect.width);
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return { ref, fit: fitTiles(count, width, extra) };
}

const TileFitContext = createContext<TileFit | null>(null);

/** Lets a caller that lays out its own rows still size the tiles inside them. */
export function TileFitProvider({
  fit,
  children,
}: {
  fit: TileFit;
  children: ReactNode;
}) {
  return (
    <TileFitContext.Provider value={fit}>{children}</TileFitContext.Provider>
  );
}

/**
 * A row of tiles sized to fit the available width, split into even rows only
 * when the tiles would otherwise be too small to read. Each child is one tile.
 */
export function TileRow({
  children,
  className,
  extra = 0,
  ...props
}: {
  children: ReactNode;
  className?: string;
  /** Pixels on the line taken by non-tile content, if any. */
  extra?: number;
} & Omit<React.HTMLAttributes<HTMLDivElement>, "children">) {
  const items = Children.toArray(children);
  const { ref, fit } = useTileFit(items.length, extra);

  const rows: ReturnType<typeof Children.toArray>[] = [];
  for (let i = 0; i < items.length; i += fit.perRow) {
    rows.push(items.slice(i, i + fit.perRow));
  }

  return (
    <div ref={ref} className={cn("w-full", className)} {...props}>
      <TileFitProvider fit={fit}>
        {rows.map((row, i) => (
          <div
            key={i}
            className="flex justify-center"
            style={{ gap: fit.gap, marginTop: i > 0 ? fit.gap : undefined }}
          >
            {row}
          </div>
        ))}
      </TileFitProvider>
    </div>
  );
}

/**
 * Inline geometry wins over the size classes; cosmetic bits (radius, border,
 * shadow) stay classes so callers can still override them.
 */
function fitStyles(fit: TileFit | null) {
  if (!fit) return { style: undefined, className: undefined };
  return {
    style: {
      width: fit.px,
      height: fit.px,
      fontSize: Math.round(fit.px * 0.5),
    },
    className:
      fit.px < 36 ? "rounded-lg border-2 shadow-[0_3px_0_var(--ink)]" : "",
  };
}

export function Tile({
  children,
  size = "md",
  className,
}: {
  children?: ReactNode;
  size?: TileSize;
  className?: string;
}) {
  const fit = useContext(TileFitContext);
  const fitted = fitStyles(fit);
  return (
    <span
      className={cn(
        "tile",
        fit ? fitted.className : SIZE_CLASSES[size],
        className,
      )}
      style={fitted.style}
    >
      {children}
    </span>
  );
}

export function TileButton({
  size = "md",
  className,
  children,
  style,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { size?: TileSize }) {
  const fit = useContext(TileFitContext);
  const fitted = fitStyles(fit);
  return (
    <button
      type="button"
      className={cn(
        "tile tile-press cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:cursor-default disabled:opacity-35 disabled:shadow-none disabled:translate-y-1",
        fit ? fitted.className : SIZE_CLASSES[size],
        className,
      )}
      style={{ ...fitted.style, ...style }}
      {...props}
    >
      {children}
    </button>
  );
}
