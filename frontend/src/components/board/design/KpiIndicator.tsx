import { classnames } from "../utils/classnames";

interface KpiIndicatorProps {
  label: string;
  value: string | number;
  max?: number;
  unit?: string;
  variant?: "default" | "success" | "warning" | "danger";
  size?: "sm" | "md";
  className?: string;
}

const variantBar: Record<string, string> = {
  default: "bg-blue-500",
  success: "bg-emerald-500",
  warning: "bg-amber-500",
  danger: "bg-red-500",
};

const variantText: Record<string, string> = {
  default: "text-blue-600 dark:text-blue-400",
  success: "text-emerald-600 dark:text-emerald-400",
  warning: "text-amber-600 dark:text-amber-400",
  danger: "text-red-600 dark:text-red-400",
};

/** Determine variant from a numeric score 0-10 */
export function scoreToVariant(
  score: number,
): "default" | "success" | "warning" | "danger" {
  if (score >= 8) return "success";
  if (score >= 5) return "default";
  if (score >= 3) return "warning";
  return "danger";
}

export function KpiIndicator({
  label,
  value,
  max,
  unit,
  variant = "default",
  size = "sm",
  className,
}: KpiIndicatorProps) {
  const numericValue =
    typeof value === "number" ? value : parseFloat(String(value));
  const pct =
    max && !isNaN(numericValue)
      ? Math.min(100, (numericValue / max) * 100)
      : null;

  return (
    <div className={classnames("flex flex-col gap-0.5", className)}>
      <div className="flex items-baseline justify-between gap-1">
        <span
          className={classnames(
            "font-medium",
            size === "sm" ? "text-[10px]" : "text-xs",
            "text-slate-500 dark:text-slate-400",
          )}
        >
          {label}
        </span>
        <span
          className={classnames(
            "font-bold tabular-nums",
            size === "sm" ? "text-xs" : "text-sm",
            variantText[variant],
          )}
        >
          {value}
          {unit}
        </span>
      </div>
      {pct !== null && (
        <div className="h-1.5 w-full rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden">
          <div
            className={classnames(
              "h-full rounded-full transition-all duration-500",
              variantBar[variant],
            )}
            style={{ width: `${pct}%` }}
          />
        </div>
      )}
    </div>
  );
}
