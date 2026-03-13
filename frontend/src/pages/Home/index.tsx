import {
  FolderOpen,
  CheckSquare,
  ClockCountdown,
  TrendUp,
  DotsThree,
  Circle,
  CheckCircle,
  Warning,
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

const progressData = [
  { mes: "Out", tarefas: 12, concluidas: 8 },
  { mes: "Nov", tarefas: 18, concluidas: 14 },
  { mes: "Dez", tarefas: 15, concluidas: 10 },
  { mes: "Jan", tarefas: 22, concluidas: 16 },
  { mes: "Fev", tarefas: 20, concluidas: 18 },
  { mes: "Mar", tarefas: 25, concluidas: 21 },
];

const recentProjects = [
  { name: "Redesign do App", progress: 72, status: "Em andamento", color: "#0E1F63" },
  { name: "API de Pagamentos", progress: 45, status: "Em andamento", color: "#3d5aad" },
  { name: "Dashboard Analytics", progress: 100, status: "Concluído", color: "#16a34a" },
  { name: "Módulo de Relatórios", progress: 20, status: "Atrasado", color: "#dc2626" },
];

const recentTasks = [
  { title: "Revisar protótipos de UI", project: "Redesign do App", done: true },
  { title: "Configurar endpoints REST", project: "API de Pagamentos", done: false },
  { title: "Escrever testes unitários", project: "API de Pagamentos", done: false },
  { title: "Exportar relatório mensal", project: "Módulo de Relatórios", done: false },
  { title: "Deploy em produção", project: "Dashboard Analytics", done: true },
];

const stats = [
  {
    label: "Projetos Ativos",
    value: "8",
    icon: FolderOpen,
    change: "+2 este mês",
    positive: true,
    bg: "bg-[#0E1F63]",
  },
  {
    label: "Tarefas Abertas",
    value: "34",
    icon: CheckSquare,
    change: "-5 esta semana",
    positive: true,
    bg: "bg-[#3d5aad]",
  },
  {
    label: "Em Andamento",
    value: "12",
    icon: ClockCountdown,
    change: "3 com prazo hoje",
    positive: false,
    bg: "bg-amber-500",
  },
  {
    label: "Concluídas",
    value: "89",
    icon: TrendUp,
    change: "+21 este mês",
    positive: true,
    bg: "bg-emerald-500",
  },
];

const statusIcon = (status: string) => {
  if (status === "Concluído")
    return <CheckCircle size={16} weight="fill" className="text-emerald-500" />;
  if (status === "Atrasado")
    return <Warning size={16} weight="fill" className="text-red-500" />;
  return <Circle size={16} weight="fill" className="text-[#3d5aad]" />;
};

export const HomePage = () => {
  const hour = new Date().getHours();
  const greeting =
    hour < 12 ? "Bom dia" : hour < 18 ? "Boa tarde" : "Boa noite";

  return (
    <div className="space-y-6">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#0E1F63]">
            {greeting}, Fillipe 👋
          </h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Veja o resumo dos seus projetos e tarefas de hoje.
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
        {stats.map(({ label, value, icon: Icon, change, positive, bg }) => (
          <div
            key={label}
            className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100 flex flex-col gap-3"
          >
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-500">{label}</span>
              <div className={`w-9 h-9 rounded-xl ${bg} flex items-center justify-center`}>
                <Icon size={18} weight="duotone" className="text-white" />
              </div>
            </div>
            <div>
              <p className="text-3xl font-bold text-gray-800">{value}</p>
              <p className={`text-xs mt-1 ${positive ? "text-emerald-500" : "text-amber-500"}`}>
                {change}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* Chart + Recent Tasks */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">

        {/* Chart */}
        <div className="lg:col-span-2 bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold text-gray-800">Progresso Mensal</h2>
            <span className="text-xs text-gray-400 bg-gray-100 px-3 py-1 rounded-full">
              Últimos 6 meses
            </span>
          </div>
          <ResponsiveContainer width="100%" height={220}>
            <AreaChart data={progressData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="gradTarefas" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#0E1F63" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#0E1F63" stopOpacity={0} />
                </linearGradient>
                <linearGradient id="gradConcluidas" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3d5aad" stopOpacity={0.2} />
                  <stop offset="95%" stopColor="#3d5aad" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="mes" tick={{ fontSize: 12, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 12, fill: "#9ca3af" }} axisLine={false} tickLine={false} />
              <Tooltip
                contentStyle={{ borderRadius: "12px", border: "1px solid #e5e7eb", fontSize: 12 }}
              />
              <Area
                type="monotone"
                dataKey="tarefas"
                name="Abertas"
                stroke="#0E1F63"
                strokeWidth={2}
                fill="url(#gradTarefas)"
              />
              <Area
                type="monotone"
                dataKey="concluidas"
                name="Concluídas"
                stroke="#3d5aad"
                strokeWidth={2}
                fill="url(#gradConcluidas)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Recent Tasks */}
        <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold text-gray-800">Tarefas Recentes</h2>
            <button className="text-gray-400 hover:text-gray-600 transition-colors">
              <DotsThree size={20} weight="bold" />
            </button>
          </div>
          <ul className="space-y-3">
            {recentTasks.map((task) => (
              <li key={task.title} className="flex items-start gap-3">
                <div className="mt-0.5 shrink-0">
                  {task.done
                    ? <CheckCircle size={18} weight="fill" className="text-emerald-500" />
                    : <Circle size={18} weight="regular" className="text-gray-300" />
                  }
                </div>
                <div className="min-w-0">
                  <p className={`text-sm font-medium truncate ${task.done ? "line-through text-gray-400" : "text-gray-700"}`}>
                    {task.title}
                  </p>
                  <p className="text-xs text-gray-400 truncate">{task.project}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Recent Projects */}
      <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-base font-semibold text-gray-800">Projetos Recentes</h2>
          <button className="text-xs text-[#0E1F63] font-medium hover:underline">Ver todos</button>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {recentProjects.map((project) => (
            <div
              key={project.name}
              className="border border-gray-100 rounded-xl p-4 hover:shadow-md transition-shadow cursor-pointer"
            >
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-1.5 text-xs text-gray-500">
                  {statusIcon(project.status)}
                  <span>{project.status}</span>
                </div>
                <button className="text-gray-300 hover:text-gray-500 transition-colors">
                  <DotsThree size={18} weight="bold" />
                </button>
              </div>
              <p className="text-sm font-semibold text-gray-800 mb-3 leading-tight">
                {project.name}
              </p>
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs text-gray-400">
                  <span>Progresso</span>
                  <span className="font-medium" style={{ color: project.color }}>
                    {project.progress}%
                  </span>
                </div>
                <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all duration-500"
                    style={{ width: `${project.progress}%`, backgroundColor: project.color }}
                  />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
};
