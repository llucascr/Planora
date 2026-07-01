import { useState, useEffect } from "react";
import {
  FolderOpen,
  CheckSquare,
  GitPullRequest,
  GitCommit,
  ArrowSquareOut,
  CircleNotch,
  GitMerge,
} from "@phosphor-icons/react";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { httpClient, ENDPOINTS } from "api";
import type {
  DashboardStats,
  ActivityDayEntry,
  CommitHistoryEntry,
  MonthlyProgressEntry,
} from "api";

/* Brand colors consumed by JS (recharts / inline styles) */
const BRAND = "#7c3aed";
const BRAND_SOFT = "#a78bfa";
const HEATMAP_SCALE = ["#f0eef6", "#ede9fe", "#c4b5fd", "#8b5cf6", "#6d28d9"];

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "short",
  });

export const HomePage = () => {
  const hour = new Date().getHours();
  const greeting =
    hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite";

  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [activity, setActivity] = useState<ActivityDayEntry[]>([]);
  const [commits, setCommits] = useState<CommitHistoryEntry[]>([]);
  const [progress, setProgress] = useState<MonthlyProgressEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      httpClient.get<DashboardStats>(ENDPOINTS.v1.dashboard.stats),
      httpClient.get<ActivityDayEntry[]>(ENDPOINTS.v1.dashboard.activity),
      httpClient.get<CommitHistoryEntry[]>(ENDPOINTS.v1.dashboard.commits),
      httpClient.get<MonthlyProgressEntry[]>(ENDPOINTS.v1.dashboard.progress),
    ])
      .then(([statsData, activityData, commitsData, progressData]) => {
        setStats(statsData);
        setActivity(activityData);
        setCommits(commitsData);
        setProgress(progressData);
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <CircleNotch size={32} className="animate-spin text-primary" />
      </div>
    );
  }

  const activityMap = new Map(activity.map((a) => [a.date, a.count]));
  const maxCount = Math.max(...activity.map((a) => a.count), 1);
  const totalEvents = activity.reduce((sum, a) => sum + a.count, 0);
  const activeDays = activity.filter((a) => a.count > 0).length;
  const peakEntry = activity.length > 0
    ? activity.reduce((max, a) => (a.count > max.count ? a : max))
    : null;

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const last30Days = Array.from({ length: 30 }, (_, i) => {
    const d = new Date(today);
    d.setDate(today.getDate() - (29 - i));
    const dateStr = d.toISOString().split("T")[0];
    return { date: dateStr, count: activityMap.get(dateStr) ?? 0 };
  });

  const startPad = new Date(last30Days[0].date).getDay();
  const paddedDays: ({ date: string; count: number } | null)[] = [
    ...Array<null>(startPad).fill(null),
    ...last30Days,
  ];
  const weeks: ({ date: string; count: number } | null)[][] = [];
  for (let i = 0; i < paddedDays.length; i += 7) {
    weeks.push(paddedDays.slice(i, i + 7));
  }

  const heatmapColor = (count: number) => {
    if (count === 0) return HEATMAP_SCALE[0];
    const pct = count / maxCount;
    if (pct < 0.25) return HEATMAP_SCALE[1];
    if (pct < 0.5) return HEATMAP_SCALE[2];
    if (pct < 0.75) return HEATMAP_SCALE[3];
    return HEATMAP_SCALE[4];
  };

  const statCards = [
    {
      label: "Boards Ativos",
      value: stats?.activeBoardsCount ?? 0,
      icon: FolderOpen,
      bg: "bg-primary",
    },
    {
      label: "Issues Atribuídas (30d)",
      value: stats?.assignedIssuesLast30Days ?? 0,
      icon: CheckSquare,
      bg: "bg-[#6366f1]",
    },
    {
      label: "PRs Abertos",
      value: stats?.openPRsCount ?? 0,
      icon: GitPullRequest,
      bg: "bg-amber-500",
    },
    {
      label: "PRs Mergeados (30d)",
      value: stats?.mergedPRsLast30Days ?? 0,
      icon: GitMerge,
      bg: "bg-emerald-500",
    },
  ];

  const progressChartData = progress.map((p) => ({
    dia: formatDate(p.date),
    abertas: p.opened,
    fechadas: p.closed,
  }));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-foreground">
            {greeting}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Veja o resumo dos seus projetos e atividades recentes.
          </p>
        </div>
        <div className="hidden sm:flex items-center gap-2 bg-card border border-border rounded-xl px-4 py-2 text-sm text-muted-foreground shadow-sm">
          {new Date().toLocaleDateString("pt-BR", {
            weekday: "long",
            day: "numeric",
            month: "long",
          })}
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map(({ label, value, icon: Icon, bg }) => (
          <div
            key={label}
            className="bg-card rounded-2xl p-5 shadow-sm border border-border flex flex-col gap-3"
          >
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">{label}</span>
              <div
                className={`w-9 h-9 rounded-xl ${bg} flex items-center justify-center`}
              >
                <Icon size={18} weight="fill" className="text-white" />
              </div>
            </div>
            <p className="text-3xl font-extrabold text-foreground">{value}</p>
          </div>
        ))}
      </div>

      {/* Monthly Progress + Commits */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 bg-card rounded-2xl p-5 shadow-sm border border-border">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold text-foreground">
              Progresso Mensal
            </h2>
            <span className="text-xs text-muted-foreground bg-muted px-3 py-1 rounded-full">
              Últimos 30 dias
            </span>
          </div>
          {progressChartData.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart
                data={progressChartData}
                margin={{ top: 4, right: 4, left: -20, bottom: 0 }}
              >
                <defs>
                  <linearGradient id="gradAbertas" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor={BRAND} stopOpacity={0.25} />
                    <stop offset="95%" stopColor={BRAND} stopOpacity={0} />
                  </linearGradient>
                  <linearGradient
                    id="gradFechadas"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop offset="5%" stopColor={BRAND_SOFT} stopOpacity={0.25} />
                    <stop offset="95%" stopColor={BRAND_SOFT} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0eef6" />
                <XAxis
                  dataKey="dia"
                  tick={{ fontSize: 11, fill: "#8a8798" }}
                  axisLine={false}
                  tickLine={false}
                  interval="preserveStartEnd"
                />
                <YAxis
                  tick={{ fontSize: 12, fill: "#8a8798" }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  contentStyle={{
                    borderRadius: "12px",
                    border: "1px solid #eceaf2",
                    fontSize: 12,
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="abertas"
                  name="Abertas"
                  stroke={BRAND}
                  strokeWidth={2}
                  fill="url(#gradAbertas)"
                />
                <Area
                  type="monotone"
                  dataKey="fechadas"
                  name="Fechadas"
                  stroke={BRAND_SOFT}
                  strokeWidth={2}
                  fill="url(#gradFechadas)"
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[220px] text-muted-foreground text-sm">
              Sem dados disponíveis
            </div>
          )}
        </div>

        {/* Recent Commits */}
        <div className="bg-card rounded-2xl p-5 shadow-sm border border-border">
          <h2 className="text-base font-semibold text-foreground mb-4">
            Commits Recentes
          </h2>
          {commits.length > 0 ? (
            <ul className="space-y-3">
              {commits.slice(0, 6).map((commit) => (
                <li key={commit.sha} className="flex items-start gap-2.5">
                  <div className="mt-0.5 shrink-0">
                    <GitCommit
                      size={16}
                      weight="fill"
                      className="text-primary"
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-foreground truncate leading-tight">
                      {commit.message}
                    </p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-xs text-muted-foreground truncate font-mono">
                        {commit.repositoryName}
                      </span>
                      <span className="text-xs text-border">·</span>
                      <span className="text-xs text-muted-foreground shrink-0">
                        {formatDate(commit.date)}
                      </span>
                    </div>
                  </div>
                  <a
                    href={commit.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-muted-foreground hover:text-primary transition-colors shrink-0 mt-0.5"
                  >
                    <ArrowSquareOut size={14} />
                  </a>
                </li>
              ))}
            </ul>
          ) : (
            <div className="flex items-center justify-center h-32 text-muted-foreground text-sm">
              Sem commits recentes
            </div>
          )}
        </div>
      </div>

      {/* Activity Heatmap */}
      <div className="bg-card rounded-2xl px-5 py-4 shadow-sm border border-border">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold text-foreground">
            Atividade no GitHub
          </h2>
          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <span>
              <span className="font-semibold text-primary">{totalEvents}</span> eventos
            </span>
            <span>
              <span className="font-semibold text-primary">{activeDays}</span>/30 dias ativos
            </span>
            {peakEntry && (
              <span>
                pico: <span className="font-semibold text-primary">{peakEntry.count}</span> em {formatDate(peakEntry.date)}
              </span>
            )}
          </div>
        </div>

        {activity.length > 0 ? (
          <div className="flex items-end gap-3">
            <div className="overflow-x-auto flex-1">
              <div className="flex gap-[3px]">
                {/* Day-of-week labels */}
                <div className="flex flex-col gap-[3px] mr-0.5 shrink-0">
                  {["D", "S", "T", "Q", "Q", "S", "S"].map((d, i) => (
                    <div
                      key={i}
                      className="w-3 h-3 flex items-center justify-center text-[9px] text-muted-foreground"
                    >
                      {i % 2 === 1 ? d : ""}
                    </div>
                  ))}
                </div>

                {/* Week columns */}
                {weeks.map((week, wi) => (
                  <div key={wi} className="flex flex-col gap-[3px] shrink-0">
                    {Array.from({ length: 7 }, (_, di) => {
                      const day = week[di] ?? null;
                      return (
                        <div
                          key={di}
                          title={
                            day
                              ? `${formatDate(day.date)}: ${day.count} evento${day.count !== 1 ? "s" : ""}`
                              : undefined
                          }
                          className="w-3 h-3 rounded-[3px] cursor-default transition-opacity hover:opacity-70"
                          style={{
                            backgroundColor: day
                              ? heatmapColor(day.count)
                              : "transparent",
                          }}
                        />
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>

            {/* Legend */}
            <div className="flex items-center gap-1 shrink-0 pb-0.5">
              <span className="text-[9px] text-muted-foreground">−</span>
              {HEATMAP_SCALE.map((color) => (
                <div
                  key={color}
                  className="w-3 h-3 rounded-[3px]"
                  style={{ backgroundColor: color }}
                />
              ))}
              <span className="text-[9px] text-muted-foreground">+</span>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center h-10 text-muted-foreground text-xs">
            Sem atividade registrada
          </div>
        )}
      </div>
    </div>
  );
};
