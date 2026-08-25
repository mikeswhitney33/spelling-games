import { cn } from "@/lib/utils";
import type { ButtonHTMLAttributes, ReactNode } from "react";

export type TileSize = "xs" | "sm" | "md" | "lg";

const SIZE_CLASSES: Record<TileSize, string> = {
  xs: "h-8 w-8 text-sm rounded-lg border-2 shadow-[0_3px_0_var(--ink)]",
  sm: "h-10 w-10 text-lg",
  md: "h-12 w-12 text-2xl",
  lg: "h-14 w-14 text-3xl",
};

/** Pick a tile size that keeps long words on screen. */
export function tileSizeForWord(word: string): TileSize {
  if (word.length > 10) return "xs";
  if (word.length > 7) return "sm";
  return "md";
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
  return (
    <span className={cn("tile", SIZE_CLASSES[size], className)}>{children}</span>
  );
}

export function TileButton({
  size = "md",
  className,
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { size?: TileSize }) {
  return (
    <button
      type="button"
      className={cn(
        "tile tile-press cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:cursor-default disabled:opacity-35 disabled:shadow-none disabled:translate-y-1",
        SIZE_CLASSES[size],
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}
