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
        <CircleNotch size={32} className="animate-spin text-[#0E1F63]" />
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
    if (count === 0) return "#f3f4f6";
    const pct = count / maxCount;
    if (pct < 0.25) return "#bfdbfe";
    if (pct < 0.5) return "#93c5fd";
    if (pct < 0.75) return "#3d5aad";
    return "#0E1F63";
  };

  const statCards = [
    {
      label: "Boards Ativos",
      value: stats?.activeBoardsCount ?? 0,
      icon: FolderOpen,
      bg: "bg-[#0E1F63]",
    },
    {
      label: "Issues Atribuídas (30d)",
      value: stats?.assignedIssuesLast30Days ?? 0,
      icon: CheckSquare,
      bg: "bg-[#3d5aad]",
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
          <h1 className="text-2xl font-bold text-[#0E1F63]">
            {greeting} 👋
          </h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Veja o resumo dos seus projetos e atividades recentes.
          </p>
        </div>
        <div className="hidden sm:flex items-center gap-2 bg-white border border-gray-200 rounded-xl px-4 py-2 text-sm text-gray-500 shadow-sm">
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
            className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100 flex flex-col gap-3"
          >
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-500">{label}</span>
              <div
                className={`w-9 h-9 rounded-xl ${bg} flex items-center justify-center`}
              >
                <Icon size={18} weight="duotone" className="text-white" />
              </div>
            </div>
            <p className="text-3xl font-bold text-gray-800">{value}</p>
          </div>
        ))}
      </div>

      {/* Monthly Progress + Commits */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold text-gray-800">
              Progresso Mensal
            </h2>
            <span className="text-xs text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
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
                    <stop offset="5%" stopColor="#0E1F63" stopOpacity={0.15} />
                    <stop offset="95%" stopColor="#0E1F63" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient
                    id="gradFechadas"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop offset="5%" stopColor="#3d5aad" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#3d5aad" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis
                  dataKey="dia"
                  tick={{ fontSize: 11, fill: "#9ca3af" }}
                  axisLine={false}
                  tickLine={false}
                  interval="preserveStartEnd"
                />
                <YAxis
                  tick={{ fontSize: 12, fill: "#9ca3af" }}
                  axisLine={false}
                  tickLine={false}
                />
                <Tooltip
                  contentStyle={{
                    borderRadius: "12px",
                    border: "1px solid #e5e7eb",
                    fontSize: 12,
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="abertas"
                  name="Abertas"
                  stroke="#0E1F63"
                  strokeWidth={2}
                  fill="url(#gradAbertas)"
                />
                <Area
                  type="monotone"
                  dataKey="fechadas"
                  name="Fechadas"
                  stroke="#3d5aad"
                  strokeWidth={2}
                  fill="url(#gradFechadas)"
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm">
              Sem dados disponíveis
            </div>
          )}
        </div>

        {/* Recent Commits */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
          <h2 className="text-base font-semibold text-gray-800 mb-4">
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
                      className="text-[#3d5aad]"
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-gray-700 truncate leading-tight">
                      {commit.message}
                    </p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-xs text-gray-400 truncate">
                        {commit.repositoryName}
                      </span>
                      <span className="text-xs text-gray-300">·</span>
                      <span className="text-xs text-gray-400 shrink-0">
                        {formatDate(commit.date)}
                      </span>
                    </div>
                  </div>
                  <a
                    href={commit.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-gray-300 hover:text-[#3d5aad] transition-colors shrink-0 mt-0.5"
                  >
                    <ArrowSquareOut size={14} />
                  </a>
                </li>
              ))}
            </ul>
          ) : (
            <div className="flex items-center justify-center h-32 text-gray-400 text-sm">
              Sem commits recentes
            </div>
          )}
        </div>
      </div>

      {/* Activity Heatmap */}
      <div className="bg-white rounded-2xl px-5 py-4 shadow-sm border border-gray-100">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold text-gray-800">
            Atividade no GitHub
          </h2>
          <div className="flex items-center gap-4 text-xs text-gray-500">
            <span>
              <span className="font-semibold text-[#0E1F63]">{totalEvents}</span> eventos
            </span>
            <span>
              <span className="font-semibold text-[#0E1F63]">{activeDays}</span>/30 dias ativos
            </span>
            {peakEntry && (
              <span>
                pico: <span className="font-semibold text-[#0E1F63]">{peakEntry.count}</span> em {formatDate(peakEntry.date)}
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
                      className="w-3 h-3 flex items-center justify-center text-[9px] text-gray-400"
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
                          className="w-3 h-3 rounded-[2px] cursor-default transition-opacity hover:opacity-70"
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
              <span className="text-[9px] text-gray-400">−</span>
              {["#f3f4f6", "#bfdbfe", "#93c5fd", "#3d5aad", "#0E1F63"].map((color) => (
                <div
                  key={color}
                  className="w-3 h-3 rounded-[2px]"
                  style={{ backgroundColor: color }}
                />
              ))}
              <span className="text-[9px] text-gray-400">+</span>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center h-10 text-gray-400 text-xs">
            Sem atividade registrada
          </div>
        )}
      </div>
    </div>
  );
};
