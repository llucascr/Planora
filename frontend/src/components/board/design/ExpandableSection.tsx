import React, { useState } from "react";
import { CaretDown, CaretRight } from "@phosphor-icons/react";
import { classnames } from "../utils/classnames";

interface ExpandableSectionProps {
  title: React.ReactNode;
  children: React.ReactNode;
  defaultOpen?: boolean;
  className?: string;
  headerClassName?: string;
  contentClassName?: string;
  badge?: React.ReactNode;
}

export function ExpandableSection({
  title,
  children,
  defaultOpen = false,
  className,
  headerClassName,
  contentClassName,
  badge,
}: ExpandableSectionProps) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className={classnames("", className)}>
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className={classnames(
          "flex w-full items-center justify-between gap-1 rounded-md px-1 py-0.5 text-left",
          "text-xs font-semibold text-slate-600 dark:text-slate-300",
          "hover:bg-slate-100 dark:hover:bg-slate-700/50 transition-colors",
          headerClassName,
        )}
      >
        <span className="flex items-center gap-1">
          {open ? (
            <CaretDown className="h-3 w-3" />
          ) : (
            <CaretRight className="h-3 w-3" />
          )}
          {title}
        </span>
        {badge && <span>{badge}</span>}
      </button>

      {open && (
        <div className={classnames("mt-1 pl-4", contentClassName)}>
          {children}
        </div>
      )}
    </div>
  );
}
