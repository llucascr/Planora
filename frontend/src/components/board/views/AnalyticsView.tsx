import { useBoardState } from "../domain/boardStore";
import {
  FileText,
  TrendUp,
  CheckCircle,
  ClockCounterClockwise,
  User,
  Tag,
  Kanban,
} from "@phosphor-icons/react";
import { classnames } from "../utils/classnames";

export function AnalyticsView() {
  const state = useBoardState();
  const { normalized } = state;

  const total = Object.values(normalized.cards).length;

  // 1. Column Metrics (Backlog vs Done)
  const columnsCount = normalized.columnOrder.length;
  const backlogColId = columnsCount > 0 ? normalized.columnOrder[0] : null;
  const doneColId =
    columnsCount > 1 ? normalized.columnOrder[columnsCount - 1] : null;

  const backlogCount = backlogColId
    ? normalized.columnCards[backlogColId]?.length || 0
    : 0;
  const doneCount = doneColId
    ? normalized.columnCards[doneColId]?.length || 0
    : 0;

  // 2. Assignee Workload
  const assigneesMap: Record<
    string,
    { login: string; avatarUrl: string; count: number }
  > = {};

  // 3. Labels Distribution
  const labelsMap: Record<
    string,
    { name: string; color: string; count: number }
  > = {};

  // Parse cards to calculate assignees and labels
  for (const card of Object.values(normalized.cards)) {
    // Assignees
    if (card.assignees && card.assignees.length > 0) {
      card.assignees.forEach((assignee) => {
        if (!assigneesMap[assignee.login]) {
          assigneesMap[assignee.login] = {
            login: assignee.login,
            avatarUrl: assignee.avatarUrl,
            count: 0,
          };
        }
        assigneesMap[assignee.login].count++;
      });
    } else {
      // Unassigned
      if (!assigneesMap["unassigned"]) {
        assigneesMap["unassigned"] = {
          login: "Unassigned",
          avatarUrl: "",
          count: 0,
        };
      }
      assigneesMap["unassigned"].count++;
    }

    // Labels
    if (card.labels && card.labels.length > 0) {
      card.labels.forEach((label) => {
        if (!labelsMap[label.name]) {
          labelsMap[label.name] = {
            name: label.name,
            color: label.color,
            count: 0,
          };
        }
        labelsMap[label.name].count++;
      });
    } else {
      // No label
      if (!labelsMap["none"]) {
        labelsMap["none"] = {
          name: "Sem Label",
          color: "808080", // default gray
          count: 0,
        };
      }
      labelsMap["none"].count++;
    }
  }

  // Sort Assignees
  const sortedAssignees = Object.values(assigneesMap).sort(
    (a, b) => b.count - a.count,
  );

  // Sort Labels
  const sortedLabels = Object.values(labelsMap).sort(
    (a, b) => b.count - a.count,
  );

  // Column Flow Array
  const columnFlow = normalized.columnOrder.map((colId) => {
    const col = normalized.columns[colId];
    const count = normalized.columnCards[colId]?.length || 0;
    return {
      colId,
      nome: col?.name || "Desconhecido",
      count,
    };
  });

  return (
    <div className="flex-1 overflow-y-auto px-6 pb-8 bg-background">
      <div className="max-w-6xl mx-auto flex flex-col gap-10 pt-6">
        {/* TOP KPIs */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Total Issues */}
          <div className="group relative overflow-hidden rounded-3xl bg-primary p-6 text-primary-foreground shadow-lg hover:shadow-primary/30 transition-all">
            <div className="absolute top-0 right-0 -mr-16 -mt-16 h-48 w-48 rounded-full bg-white opacity-10 blur-2xl" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-semibold opacity-80 uppercase tracking-widest mb-1">
                  Total de Issues
                </p>
                <p className="text-5xl font-black tracking-tight tabular-nums">
                  {total}
                </p>
              </div>
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/20 backdrop-blur-md">
                <FileText weight="duotone" className="h-6 w-6 text-white" />
              </div>
            </div>
            <div className="mt-4 flex items-center gap-2 text-sm font-medium text-white/80">
              <TrendUp weight="bold" />
              <span>Geral no Board</span>
            </div>
          </div>

          {/* Backlog */}
          <div className="group relative overflow-hidden rounded-3xl border border-border bg-card p-6 shadow-sm hover:shadow-md transition-all">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm font-bold text-muted-foreground uppercase tracking-widest mb-1">
                  A Fazer (Backlog)
                </p>
                <p className="text-4xl font-black text-foreground tabular-nums">
                  {backlogCount}
                </p>
              </div>
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary text-secondary-foreground">
                <ClockCounterClockwise weight="duotone" className="h-6 w-6" />
              </div>
            </div>
            <div className="mt-4 h-1.5 w-full rounded-full bg-secondary overflow-hidden">
              <div
                className="h-full rounded-full bg-amber-500"
                style={{
                  width: `${total ? (backlogCount / total) * 100 : 0}%`,
                }}
              />
            </div>
          </div>

          {/* Done */}
          <div className="group relative overflow-hidden rounded-3xl border border-border bg-card p-6 shadow-sm hover:shadow-md transition-all">
            <div className="flex items-start justify-between">
              <div>
                <p className="text-sm font-bold text-muted-foreground uppercase tracking-widest mb-1">
                  Concluídas
                </p>
                <p className="text-4xl font-black text-foreground tabular-nums">
                  {doneCount}
                </p>
              </div>
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary text-secondary-foreground">
                <CheckCircle
                  weight="duotone"
                  className="h-6 w-6 text-emerald-500"
                />
              </div>
            </div>
            <div className="mt-4 h-1.5 w-full rounded-full bg-secondary overflow-hidden">
              <div
                className="h-full rounded-full bg-emerald-500"
                style={{ width: `${total ? (doneCount / total) * 100 : 0}%` }}
              />
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
          {/* ASSIGNEES WORKLOAD */}
          <div>
            <div className="flex items-center gap-2 mb-4 px-1">
              <User weight="bold" className="text-muted-foreground" />
              <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground">
                Carga de Trabalho (Assignees)
              </h3>
            </div>
            <div className="rounded-2xl border border-border bg-card p-2 flex flex-col gap-1">
              {sortedAssignees.length === 0 && (
                <div className="p-6 text-center text-sm text-muted-foreground">
                  Nenhuma issue encontrada.
                </div>
              )}
              {sortedAssignees.map((assignee) => {
                const pct = total ? (assignee.count / total) * 100 : 0;
                return (
                  <div
                    key={assignee.login}
                    className="flex items-center gap-4 rounded-xl p-3 hover:bg-accent/50 transition-colors"
                  >
                    {assignee.avatarUrl ? (
                      <img
                        src={assignee.avatarUrl}
                        alt={assignee.login}
                        className="h-10 w-10 rounded-full bg-secondary object-cover border border-border"
                      />
                    ) : (
                      <div className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary border border-border text-muted-foreground">
                        <User weight="fill" className="h-5 w-5" />
                      </div>
                    )}
                    <div className="flex-1">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-bold text-foreground">
                          {assignee.login}
                        </span>
                        <span className="text-sm font-black text-foreground tabular-nums">
                          {assignee.count}
                        </span>
                      </div>
                      <div className="h-1.5 w-full rounded-full bg-secondary overflow-hidden">
                        <div
                          className={classnames(
                            "h-full rounded-full transition-all duration-1000",
                            assignee.login === "Unassigned"
                              ? "bg-slate-400"
                              : "bg-primary",
                          )}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* LABELS DISTRIBUTION */}
          <div>
            <div className="flex items-center gap-2 mb-4 px-1">
              <Tag weight="bold" className="text-muted-foreground" />
              <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground">
                Adoção de Labels
              </h3>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5">
              <div className="flex flex-wrap gap-3">
                {sortedLabels.length === 0 && (
                  <div className="text-sm text-muted-foreground w-full text-center py-4">
                    Nenhuma label encontrada.
                  </div>
                )}
                {sortedLabels.map((label) => {
                  const hex = label.color.startsWith("#")
                    ? label.color
                    : `#${label.color}`;
                  return (
                    <div
                      key={label.name}
                      className="flex items-center gap-2.5 rounded-full border border-border bg-background/50 px-3 py-1.5 transition-all hover:bg-accent hover:border-border/80"
                    >
                      <span
                        className="h-2.5 w-2.5 rounded-full shadow-sm"
                        style={{ backgroundColor: hex }}
                      />
                      <span className="text-xs font-bold text-foreground">
                        {label.name}
                      </span>
                      <span className="text-xs font-black text-muted-foreground border-l border-border pl-2 tabular-nums">
                        {label.count}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>

        {/* COLUMN DISTRIBUTION (FLOW) */}
        <div>
          <div className="flex items-center gap-2 mb-4 px-1">
            <Kanban weight="bold" className="text-muted-foreground" />
            <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground">
              Visão do Fluxo (Colunas)
            </h3>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {columnFlow.map((col, idx) => {
              const pct = total ? (col.count / total) * 100 : 0;
              return (
                <div
                  key={col.colId}
                  className="flex flex-col gap-3 rounded-2xl border border-border bg-card p-5 hover:border-primary/30 transition-all"
                >
                  <div className="flex items-center gap-2 text-muted-foreground">
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-secondary text-[10px] font-bold">
                      {idx + 1}
                    </span>
                    <span
                      className="text-sm font-semibold truncate"
                      title={col.nome}
                    >
                      {col.nome}
                    </span>
                  </div>
                  <div className="flex items-end justify-between">
                    <span className="text-3xl font-black tabular-nums">
                      {col.count}
                    </span>
                  </div>
                  <div className="mt-1 h-1 w-full rounded-full bg-secondary overflow-hidden">
                    <div
                      className="h-full rounded-full bg-primary/60"
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
