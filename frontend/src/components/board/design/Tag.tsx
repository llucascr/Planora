import { classnames } from "../utils/classnames";

interface TagProps {
  children: React.ReactNode;
  color?: string;
  className?: string;
  onRemove?: () => void;
}

export function Tag({ children, className, onRemove }: TagProps) {
  return (
    <span
      className={classnames(
        "inline-flex items-center gap-1 rounded-md border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 px-1.5 py-0.5 text-[10px] text-slate-600 dark:text-slate-400",
        className,
      )}
    >
      {children}
      {onRemove && (
        <button
          onClick={onRemove}
          className="ml-0.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 leading-none"
          aria-label="Remove tag"
        >
          ×
        </button>
      )}
    </span>
  );
}
