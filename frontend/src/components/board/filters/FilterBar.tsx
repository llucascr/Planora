import React, { useState, useRef, useEffect } from "react";
import {
  MagnifyingGlass,
  Sliders,
  X,
  CaretDown,
  ArrowsDownUp,
  SortDescending,
  SortAscending,
  Target,
  FileText,
} from "@phosphor-icons/react";
import { useBoardState, useBoardDispatch } from "../domain/boardStore";
import type { CardType } from "../domain/types";
import { classnames } from "../utils/classnames";

const CARD_TYPES: {
  type: CardType;
  label: string;
  color: string;
  icon: React.ReactNode;
}[] = [
  {
    type: "generic",
    label: "Genérico",
    color: "#94a3b8",
    icon: <FileText className="h-3 w-3" />,
  },
];

// ─── Sort options ────────────────────────────────────────────
type SortOption = {
  field: string;
  dir: "asc" | "desc";
  label: string;
  icon: React.ReactNode;
};
const SORT_OPTIONS: SortOption[] = [
  {
    field: "createdAt",
    dir: "desc",
    label: "Mais recente",
    icon: <SortDescending className="h-3 w-3" />,
  },
  {
    field: "createdAt",
    dir: "asc",
    label: "Mais antigo",
    icon: <SortAscending className="h-3 w-3" />,
  },
  {
    field: "nome",
    dir: "asc",
    label: "Nome A → Z",
    icon: <SortAscending className="h-3 w-3" />,
  },
  {
    field: "nome",
    dir: "desc",
    label: "Nome Z → A",
    icon: <SortDescending className="h-3 w-3" />,
  },
];

// ─── Hook: collect all unique origens from the board ─────────
function useAvailableOrigens(): string[] {
  const state = useBoardState();
  const origens = new Set<string>();
  for (const card of Object.values(state.normalized.cards)) {
    if (card.lead?.origem) origens.add(card.lead.origem);
  }
  return Array.from(origens).sort();
}

// ─── Active Filter Chips ──────────────────────────────────────
function FilterChip({
  label,
  color,
  onRemove,
}: {
  label: string;
  color?: string;
  onRemove: () => void;
}) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full border border-border bg-card pl-1.5 pr-1 py-0.5 text-[11px] font-medium text-foreground transition hover:bg-accent-hover">
      {color && (
        <span
          className="h-1.5 w-1.5 rounded-full shrink-0"
          style={{ backgroundColor: color }}
        />
      )}
      {label}
      <button
        onClick={onRemove}
        className="ml-0.5 flex h-3.5 w-3.5 items-center justify-center rounded-full hover:bg-border transition-colors"
      >
        <X className="h-2.5 w-2.5" />
      </button>
    </span>
  );
}

export function FilterBar() {
  const state = useBoardState();
  const dispatch = useBoardDispatch();
  const { filters } = state;
  const [panelOpen, setPanelOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);
  const availableOrigens = useAvailableOrigens();

  useEffect(() => {
    function handler(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setPanelOpen(false);
      }
    }
    if (panelOpen) document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, [panelOpen]);

  function toggleType(t: CardType) {
    const has = filters.types.includes(t);
    dispatch({
      type: "SET_FILTERS",
      payload: {
        types: has
          ? filters.types.filter((x) => x !== t)
          : [...filters.types, t],
      },
    });
  }

  function toggleOrigem(o: string) {
    const has = filters.origens.includes(o);
    dispatch({
      type: "SET_FILTERS",
      payload: {
        origens: has
          ? filters.origens.filter((x) => x !== o)
          : [...filters.origens, o],
      },
    });
  }

  function setSort(field: string, dir: "asc" | "desc") {
    dispatch({
      type: "SET_FILTERS",
      payload: { sortField: field as any, sortDir: dir },
    });
  }

  function clearAll() {
    dispatch({
      type: "SET_FILTERS",
      payload: {
        search: "",
        types: [],
        origens: [],
        sortField: "createdAt",
        sortDir: "desc",
      },
    });
  }

  const activeCount =
    filters.types.length + filters.origens.length + (filters.search ? 1 : 0);
  const currentSort =
    SORT_OPTIONS.find(
      (s) => s.field === filters.sortField && s.dir === filters.sortDir,
    ) ?? SORT_OPTIONS[0];
  const isDefaultSort =
    filters.sortField === "createdAt" && filters.sortDir === "desc";
  const hasActiveFilters = activeCount > 0;

  return (
    <div className="flex items-center gap-2 flex-wrap min-w-0 z-50">
      <div className="relative flex items-center">
        <MagnifyingGlass className="pointer-events-none absolute left-2.5 h-3 w-3 text-muted-foreground" />
        <input
          type="text"
          value={filters.search}
          onChange={(e) =>
            dispatch({
              type: "SET_FILTERS",
              payload: { search: e.target.value },
            })
          }
          placeholder="Buscar cards..."
          className="h-8 w-44 rounded-lg border border-border bg-input pl-7 pr-3 text-xs text-foreground placeholder:text-muted-foreground focus:outline-none focus:border-blue-500/50 focus:bg-white/8 transition-all"
        />
        {filters.search && (
          <button
            onClick={() =>
              dispatch({ type: "SET_FILTERS", payload: { search: "" } })
            }
            className="absolute right-2 text-muted-foreground hover:text-foreground transition-colors"
          >
            <X className="h-3 w-3" />
          </button>
        )}
      </div>

      <div className="relative" ref={panelRef}>
        <button
          onClick={() => setPanelOpen(!panelOpen)}
          className={classnames(
            "inline-flex items-center gap-1.5 rounded-lg border px-2.5 h-8 text-xs font-medium transition-all",
            panelOpen
              ? "border-blue-500/50 bg-blue-500/10 text-blue-500"
              : "border-border bg-input text-foreground hover:border-border hover:text-foreground",
          )}
        >
          <Sliders className="h-3 w-3" />
          Filtros
          {activeCount > 0 && (
            <span className="flex h-4 min-w-4 items-center justify-center rounded-full bg-blue-500 px-1 text-[9px] font-bold text-foreground">
              {activeCount}
            </span>
          )}
          <CaretDown
            className={classnames(
              "h-3 w-3 transition-transform",
              panelOpen && "rotate-180",
            )}
          />
        </button>

        {panelOpen && (
          <div className="absolute left-0 top-[calc(100%+12px)] z-50 w-72 rounded-xl border border-border bg-card shadow-2xl">
            <div className="flex items-center justify-between border-b border-border px-3 py-2">
              <span className="text-[11px] font-bold text-foreground uppercase tracking-wider">
                Filtros
              </span>
              {(hasActiveFilters || !isDefaultSort) && (
                <button
                  onClick={clearAll}
                  className="text-[10px] text-muted-foreground hover:text-red-500 transition-colors"
                >
                  Limpar tudo
                </button>
              )}
            </div>

            <div className="px-3 py-2.5 border-b border-border">
              <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                Tipo de card
              </p>
              <div className="grid grid-cols-2 gap-1.5">
                {CARD_TYPES.map(({ type, label, color, icon }) => {
                  const active = filters.types.includes(type);
                  return (
                    <button
                      key={type}
                      onClick={() => toggleType(type)}
                      className={classnames(
                        "flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-[11px] font-medium transition-all text-left",
                        active
                          ? "text-foreground"
                          : "text-muted-foreground hover:text-foreground hover:bg-border",
                      )}
                      style={
                        active
                          ? {
                              backgroundColor: color + "22",
                              color,
                              borderColor: color + "44",
                            }
                          : {}
                      }
                    >
                      <span style={{ color: active ? color : undefined }}>
                        {icon}
                      </span>
                      {label}
                      {active && (
                        <span
                          className="ml-auto h-1.5 w-1.5 rounded-full"
                          style={{ backgroundColor: color }}
                        />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {availableOrigens.length > 0 && (
              <div className="px-3 py-2.5 border-b border-border">
                <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                  Origem
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {availableOrigens.map((o) => {
                    const active = filters.origens.includes(o);
                    return (
                      <button
                        key={o}
                        onClick={() => toggleOrigem(o)}
                        className={classnames(
                          "rounded-full px-2 py-0.5 text-[10px] font-medium border transition-all capitalize",
                          active
                            ? "bg-blue-500/20 border-blue-500/50 text-blue-400"
                            : "border-border text-muted-foreground hover:border-border hover:text-foreground",
                        )}
                      >
                        {o}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            <div className="px-3 py-2.5">
              <p className="text-[10px] font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                Ordenação
              </p>
              <div className="flex flex-col gap-0.5">
                {SORT_OPTIONS.map((opt) => {
                  const active =
                    opt.field === filters.sortField &&
                    opt.dir === filters.sortDir;
                  return (
                    <button
                      key={`${opt.field}-${opt.dir}`}
                      onClick={() => setSort(opt.field, opt.dir)}
                      className={classnames(
                        "flex items-center gap-2 rounded-lg px-2 py-1.5 text-[11px] transition-all text-left",
                        active
                          ? "bg-blue-500/15 text-blue-400"
                          : "text-muted-foreground hover:text-foreground hover:bg-border",
                      )}
                    >
                      {opt.icon}
                      {opt.label}
                      {active && (
                        <span className="ml-auto h-1.5 w-1.5 rounded-full bg-blue-400" />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        )}
      </div>

      {!isDefaultSort && (
        <span className="inline-flex items-center gap-1 rounded-full border border-border bg-card px-2 py-0.5 text-[10px] text-muted-foreground">
          <ArrowsDownUp className="h-2.5 w-2.5" />
          {currentSort.label}
        </span>
      )}

      {filters.types.map((t) => {
        const cfg = CARD_TYPES.find((c) => c.type === t)!;
        return (
          <FilterChip
            key={t}
            label={cfg?.label ?? t}
            color={cfg?.color}
            onRemove={() => toggleType(t)}
          />
        );
      })}
      {filters.origens.map((o) => (
        <FilterChip key={o} label={o} onRemove={() => toggleOrigem(o)} />
      ))}

      {(hasActiveFilters || !isDefaultSort) && (
        <button
          onClick={clearAll}
          className="text-[10px] text-muted-foreground hover:text-red-400 transition-colors underline underline-offset-2"
        >
          Limpar
        </button>
      )}
    </div>
  );
}
