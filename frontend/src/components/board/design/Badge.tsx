import { classnames } from "../utils/classnames";

interface BadgeProps {
  children: React.ReactNode;
  variant?: "default" | "success" | "warning" | "danger" | "info" | "purple";
  size?: "sm" | "md";
  className?: string;
  icon?: React.ReactNode;
}

const variantClasses = {
  default:
    "bg-slate-100 text-slate-700 dark:bg-slate-700/60 dark:text-slate-300",
  success:
    "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400",
  warning:
    "bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400",
  danger: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
  info: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  purple:
    "bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400",
};

export function Badge({
  children,
  variant = "default",
  size = "sm",
  className,
  icon,
}: BadgeProps) {
  return (
    <span
      className={classnames(
        "inline-flex items-center gap-1 rounded-full font-medium leading-none",
        size === "sm" ? "px-2 py-0.5 text-[10px]" : "px-2.5 py-1 text-xs",
        variantClasses[variant],
        className,
      )}
    >
      {icon && <span className="shrink-0">{icon}</span>}
      {children}
    </span>
  );
}
