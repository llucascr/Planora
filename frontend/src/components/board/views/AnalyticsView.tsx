import React from "react";
import { useBoardState } from "../domain/boardStore";
import { detectCardType, getCardTypeLabel } from "../domain/cardDetector";
import type { CardType } from "../domain/types";
import {
  ChartBar,
  Users,
  Target,
  FileText,
  TrendUp,
} from "@phosphor-icons/react";

const TYPE_ICONS: Record<CardType, React.ReactNode> = {
  lead: <Users weight="duotone" className="h-5 w-5" />,
  planoAcao: <Target weight="duotone" className="h-5 w-5" />,
  generic: <FileText weight="duotone" className="h-5 w-5" />,
};

const TYPE_COLORS: Record<CardType, string> = {
  lead: "bg-blue-500",
  planoAcao: "bg-amber-500",
  generic: "bg-slate-500",
};

export function AnalyticsView() {
  const state = useBoardState();
  const { normalized } = state;

  // Count by type
  const typeCounts: Record<CardType, number> = {
    lead: 0,
    planoAcao: 0,
    generic: 0,
  };
  const total = Object.values(normalized.cards).length;

  for (const card of Object.values(normalized.cards)) {
    const t = detectCardType(card);
    typeCounts[t]++;
  }

  // Count by column
  const columnStats = normalized.columnOrder.map((colId) => {
    const col = normalized.columns[colId];
    const ids = normalized.columnCards[colId] ?? [];
    const byType: Record<CardType, number> = {
      lead: 0,
      planoAcao: 0,
      generic: 0,
    };
    for (const cid of ids) {
      const card = normalized.cards[cid];
      if (card) byType[detectCardType(card)]++;
    }
    return { colId, nome: col.nome, count: ids.length, byType };
  });

  return (
    <div className="flex-1 overflow-y-auto px-6 pb-8">
      <div className="max-w-5xl mx-auto flex flex-col gap-10 pt-6">
        {/* Summary KPI cards */}
        <div>
          <h3 className="text-[11px] font-bold uppercase tracking-[0.2em] text-muted-foreground mb-4 px-1">
            Visão Geral por Tipo
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {(Object.keys(typeCounts) as CardType[]).map((type) => {
              const count = typeCounts[type];
              const pct = total ? Math.round((count / total) * 100) : 0;
              const color = TYPE_COLORS[type];

              return (
                <div
                  key={type}
                  className="group relative flex flex-col gap-4 rounded-2xl border border-border bg-card p-5 transition-all hover:border-primary/30 hover:shadow-xl hover:shadow-black/5"
                >
                  <div className="flex items-center justify-between">
                    <div
                      className={`flex h-10 w-10 items-center justify-center rounded-xl bg-opacity-10 ${color} ${color.replace("bg-", "text-")} shadow-sm`}
                    >
                      {TYPE_ICONS[type]}
                    </div>
                    <span className="text-xl font-black text-foreground tabular-nums">
                      {count}
                    </span>
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-foreground">
                      {getCardTypeLabel(type)}
                    </p>
                    <div className="mt-3">
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider">
                          Distribuição
                        </span>
                        <span className="text-[10px] font-bold text-foreground">
                          {pct}%
                        </span>
                      </div>
                      <div className="h-1.5 w-full rounded-full bg-secondary overflow-hidden">
                        <div
                          className={`h-full rounded-full ${color} transition-all duration-1000 ease-out`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Column breakdown */}
        <div className="grid grid-cols-1 lg:grid-cols-1 gap-10">
          <div>
            <h3 className="text-[11px] font-bold uppercase tracking-[0.2em] text-muted-foreground mb-4 px-1">
              Desempenho por Coluna
            </h3>
            <div className="space-y-3">
              {columnStats.map(({ colId, nome, count, byType }) => {
                return (
                  <div
                    key={colId}
                    className="group rounded-2xl border border-border bg-card p-5 transition-all hover:bg-accent/30"
                  >
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-1.5 rounded-full bg-primary opacity-20 group-hover:opacity-100 transition-opacity" />
                        <span className="text-sm font-bold text-foreground">
                          {nome}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-muted-foreground font-medium">
                          Total:
                        </span>
                        <span className="text-sm font-black text-foreground tabular-nums">
                          {count}
                        </span>
                      </div>
                    </div>

                    {/* Stacked bar */}
                    <div className="relative flex h-3 w-full rounded-full bg-secondary overflow-hidden mb-5">
                      {(Object.keys(byType) as CardType[]).map((t) =>
                        byType[t] > 0 ? (
                          <div
                            key={t}
                            className={`${TYPE_COLORS[t]} h-full transition-all duration-500`}
                            style={{
                              width: `${(byType[t] / Math.max(count, 1)) * 100}%`,
                            }}
                            title={`${getCardTypeLabel(t)}: ${byType[t]}`}
                          />
                        ) : null,
                      )}
                      {count === 0 && (
                        <div className="flex-1 bg-secondary opacity-50" />
                      )}
                    </div>

                    {/* Legend Labels */}
                    <div className="flex gap-4 flex-wrap">
                      {(Object.keys(byType) as CardType[]).map((t) =>
                        byType[t] > 0 ? (
                          <div
                            key={t}
                            className="flex items-center gap-2 rounded-full border border-border bg-background/50 px-2.5 py-1 transition-colors hover:bg-background"
                          >
                            <span
                              className={`h-2 w-2 rounded-full ${TYPE_COLORS[t]}`}
                            />
                            <span className="text-[10px] font-semibold text-foreground uppercase tracking-wider">
                              {getCardTypeLabel(t)}
                            </span>
                            <span className="text-[10px] font-black text-foreground/50 tabular-nums border-l border-border pl-1.5 ml-0.5">
                              {byType[t]}
                            </span>
                          </div>
                        ) : null,
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Total Summary Footer */}
        <div className="relative overflow-hidden rounded-3xl bg-primary p-8 text-foreground shadow-2xl shadow-primary/20">
          <div className="absolute top-0 right-0 -mr-16 -mt-16 h-64 w-64 rounded-full bg-white opacity-10 blur-3xl" />
          <div className="relative flex items-center justify-between gap-6">
            <div className="flex items-center gap-6">
              <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white bg-opacity-20 backdrop-blur-md shadow-inner text-white">
                <TrendUp weight="bold" className="h-8 w-8" />
              </div>
              <div>
                <p className="text-4xl font-black tracking-tight tabular-nums">
                  {total}
                </p>
                <p className="text-sm font-medium opacity-80 uppercase tracking-widest">
                  Total de cards no workflow
                </p>
              </div>
            </div>
            <div className="hidden md:block">
              <ChartBar weight="fill" className="h-24 w-24 opacity-50" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
