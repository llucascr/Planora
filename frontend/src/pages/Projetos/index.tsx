import { useState, type ReactElement } from "react";
import { useNavigate } from "react-router-dom";
import {
  FolderOpen,
  Plus,
  DotsThree,
  Circle,
  CheckCircle,
  Warning,
  MagnifyingGlass,
  FunnelSimple,
} from "@phosphor-icons/react";

type ProjectStatus = "Em andamento" | "Concluído" | "Atrasado";

type Project = {
  id: number;
  name: string;
  description: string;
  progress: number;
  status: ProjectStatus;
  color: string;
  tasks: { total: number; done: number };
  dueDate: string;
};

const projects: Project[] = [
  {
    id: 1,
    name: "Redesign do App",
    description: "Atualização da interface com novo sistema de design.",
    progress: 72,
    status: "Em andamento",
    color: "#0E1F63",
    tasks: { total: 18, done: 13 },
    dueDate: "30/03/2026",
  },
  {
    id: 2,
    name: "API de Pagamentos",
    description: "Integração com gateway de pagamentos e webhooks.",
    progress: 45,
    status: "Em andamento",
    color: "#3d5aad",
    tasks: { total: 24, done: 11 },
    dueDate: "15/04/2026",
  },
  {
    id: 3,
    name: "Dashboard Analytics",
    description: "Painel de métricas e relatórios em tempo real.",
    progress: 100,
    status: "Concluído",
    color: "#16a34a",
    tasks: { total: 12, done: 12 },
    dueDate: "01/03/2026",
  },
  {
    id: 4,
    name: "Módulo de Relatórios",
    description: "Exportação de relatórios em PDF e Excel.",
    progress: 20,
    status: "Atrasado",
    color: "#dc2626",
    tasks: { total: 10, done: 2 },
    dueDate: "10/03/2026",
  },
  {
    id: 5,
    name: "App Mobile",
    description: "Versão mobile do sistema para iOS e Android.",
    progress: 58,
    status: "Em andamento",
    color: "#0E1F63",
    tasks: { total: 30, done: 17 },
    dueDate: "20/05/2026",
  },
  {
    id: 6,
    name: "Portal do Cliente",
    description: "Área exclusiva para clientes acompanharem seus pedidos.",
    progress: 90,
    status: "Em andamento",
    color: "#3d5aad",
    tasks: { total: 15, done: 13 },
    dueDate: "25/03/2026",
  },
];

const statusConfig: Record<
  ProjectStatus,
  { label: string; icon: ReactElement; badge: string }
> = {
  "Em andamento": {
    label: "Em andamento",
    icon: <Circle size={14} weight="fill" className="text-[#3d5aad]" />,
    badge: "bg-blue-50 text-[#3d5aad]",
  },
  Concluído: {
    label: "Concluído",
    icon: <CheckCircle size={14} weight="fill" className="text-emerald-500" />,
    badge: "bg-emerald-50 text-emerald-600",
  },
  Atrasado: {
    label: "Atrasado",
    icon: <Warning size={14} weight="fill" className="text-red-500" />,
    badge: "bg-red-50 text-red-600",
  },
};

const filters = ["Todos", "Em andamento", "Concluído", "Atrasado"] as const;
type Filter = (typeof filters)[number];

export const ProjetosPage = () => {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [activeFilter, setActiveFilter] = useState<Filter>("Todos");

  const filtered = projects.filter((p) => {
    const matchesSearch = p.name.toLowerCase().includes(search.toLowerCase());
    const matchesFilter =
      activeFilter === "Todos" || p.status === activeFilter;
    return matchesSearch && matchesFilter;
  });

  const counts = {
    Todos: projects.length,
    "Em andamento": projects.filter((p) => p.status === "Em andamento").length,
    Concluído: projects.filter((p) => p.status === "Concluído").length,
    Atrasado: projects.filter((p) => p.status === "Atrasado").length,
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#0E1F63]">Projetos</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Gerencie e acompanhe todos os seus projetos.
          </p>
        </div>
        <button className="flex items-center gap-2 bg-[#0E1F63] text-white text-sm font-medium px-4 py-2.5 rounded-xl hover:bg-[#1a2f7a] transition-colors shadow-sm">
          <Plus size={16} weight="bold" />
          Novo Projeto
        </button>
      </div>

      {/* Search + Filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <MagnifyingGlass
            size={16}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
          />
          <input
            type="text"
            placeholder="Buscar projetos..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2.5 text-sm bg-white border border-gray-200 rounded-xl outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
          />
        </div>
        <div className="flex items-center gap-1 bg-white border border-gray-200 rounded-xl p-1 shadow-sm">
          <FunnelSimple size={16} className="text-gray-400 ml-2 shrink-0" />
          {filters.map((f) => (
            <button
              key={f}
              onClick={() => setActiveFilter(f)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors whitespace-nowrap ${
                activeFilter === f
                  ? "bg-[#0E1F63] text-white"
                  : "text-gray-500 hover:text-gray-700"
              }`}
            >
              {f}
              <span
                className={`text-xs px-1.5 py-0.5 rounded-full font-semibold ${
                  activeFilter === f
                    ? "bg-white/20 text-white"
                    : "bg-gray-100 text-gray-500"
                }`}
              >
                {counts[f]}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Projects Grid */}
      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-gray-400">
          <FolderOpen size={48} weight="duotone" className="mb-3 opacity-40" />
          <p className="text-sm font-medium">Nenhum projeto encontrado</p>
          <p className="text-xs mt-1">Tente ajustar os filtros ou a busca</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map((project) => {
            const s = statusConfig[project.status];
            return (
              <div
                key={project.id}
                onClick={() => navigate(`/projetos/${project.id}/tarefas`)}
                className="bg-white border border-gray-100 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow cursor-pointer group"
              >
                {/* Card Header */}
                <div className="flex items-start justify-between mb-3">
                  <span
                    className={`flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full ${s.badge}`}
                  >
                    {s.icon}
                    {s.label}
                  </span>
                  <button className="text-gray-300 hover:text-gray-500 transition-colors opacity-0 group-hover:opacity-100">
                    <DotsThree size={20} weight="bold" />
                  </button>
                </div>

                {/* Name + Description */}
                <div className="mb-4">
                  <h3 className="text-sm font-semibold text-gray-800 leading-tight mb-1">
                    {project.name}
                  </h3>
                  <p className="text-xs text-gray-400 leading-relaxed line-clamp-2">
                    {project.description}
                  </p>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between text-xs text-gray-400 pt-3 border-t border-gray-50">
                  <span>
                    {project.tasks.done}/{project.tasks.total} tarefas
                  </span>
                  <span>Prazo: {project.dueDate}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
