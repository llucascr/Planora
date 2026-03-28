import { classnames } from "../utils/classnames";

interface AvatarProps {
  name: string;
  size?: "xs" | "sm" | "md";
  className?: string;
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length === 0 || !parts[0]) return "?";
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

// Generate a consistent color based on name
function getAvatarColor(name: string): string {
  const colors = [
    "bg-blue-500",
    "bg-emerald-500",
    "bg-violet-500",
    "bg-rose-500",
    "bg-amber-500",
    "bg-cyan-500",
    "bg-indigo-500",
    "bg-teal-500",
  ];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}

export function Avatar({ name, size = "sm", className }: AvatarProps) {
  const initials = getInitials(name);
  const color = getAvatarColor(name);
  const sizeClass =
    size === "xs"
      ? "h-5 w-5 text-[9px]"
      : size === "sm"
        ? "h-7 w-7 text-xs"
        : "h-9 w-9 text-sm";

  return (
    <span
      className={classnames(
        "inline-flex items-center justify-center rounded-full font-semibold text-white shrink-0",
        color,
        sizeClass,
        className,
      )}
      title={name}
    >
      {initials}
    </span>
  );
}
