"use client";

import { GRADE_BANDS, type GradeBand } from "@/lib/words";
import { cn } from "@/lib/utils";

export function GradePicker({
  value,
  onChange,
}: {
  value: GradeBand;
  onChange: (grade: GradeBand) => void;
}) {
  const selected = GRADE_BANDS.find((band) => band.id === value);
  return (
    <fieldset>
      <legend className="font-heading text-sm font-medium text-muted-foreground">
        Pick your level
      </legend>
      <div className="mt-2 flex flex-wrap gap-2" role="radiogroup">
        {GRADE_BANDS.map((band) => {
          const active = band.id === value;
          return (
            <button
              key={band.id}
              type="button"
              role="radio"
              aria-checked={active}
              onClick={() => onChange(band.id)}
              className={cn(
                "font-heading rounded-xl border-[3px] border-ink px-4 py-1.5 text-sm font-medium transition-all cursor-pointer focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
                active
                  ? "bg-primary text-primary-foreground shadow-[0_3px_0_var(--ink)]"
                  : "bg-card text-foreground hover:-translate-y-0.5 hover:shadow-[0_3px_0_var(--ink)]",
              )}
            >
              {band.label}
            </button>
          );
        })}
      </div>
      {selected && (
        <p className="mt-2 text-sm text-muted-foreground">{selected.blurb}</p>
      )}
    </fieldset>
  );
}
