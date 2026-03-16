import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Plus, DotsThree } from "@phosphor-icons/react";

const projectsData: Record<
  string,
  { name: string; color: string }
> = {
  "1": { name: "Redesign do App", color: "#0E1F63" },
  "2": { name: "API de Pagamentos", color: "#3d5aad" },
  "3": { name: "Dashboard Analytics", color: "#16a34a" },
  "4": { name: "Módulo de Relatórios", color: "#dc2626" },
  "5": { name: "App Mobile", color: "#0E1F63" },
  "6": { name: "Portal do Cliente", color: "#3d5aad" },
};

type Priority = "Alta" | "Média" | "Baixa";

type Task = {
  id: number;
  title: string;
  priority: Priority;
  assignee: string;
};

type Column = {
  id: string;
  label: string;
  color: string;
  tasks: Task[];
};

const priorityConfig: Record<Priority, string> = {
  Alta: "bg-red-50 text-red-500",
  Média: "bg-amber-50 text-amber-500",
  Baixa: "bg-emerald-50 text-emerald-600",
};

const initialColumns: Column[] = [
  {
    id: "backlog",
    label: "Backlog",
    color: "bg-gray-400",
    tasks: [
      { id: 1, title: "Levantamento de requisitos", priority: "Alta", assignee: "F" },
      { id: 2, title: "Definir paleta de cores", priority: "Baixa", assignee: "M" },
    ],
  },
  {
    id: "todo",
    label: "A Fazer",
    color: "bg-blue-400",
    tasks: [
      { id: 3, title: "Criar wireframes das telas", priority: "Alta", assignee: "F" },
      { id: 4, title: "Configurar ambiente de dev", priority: "Média", assignee: "L" },
    ],
  },
  {
    id: "doing",
    label: "Em Andamento",
    color: "bg-amber-400",
    tasks: [
      { id: 5, title: "Implementar componentes base", priority: "Alta", assignee: "F" },
      { id: 6, title: "Integrar API de autenticação", priority: "Média", assignee: "M" },
      { id: 7, title: "Revisar protótipos de UI", priority: "Baixa", assignee: "L" },
    ],
  },
  {
    id: "review",
    label: "Revisão",
    color: "bg-purple-400",
    tasks: [
      { id: 8, title: "Code review do módulo de login", priority: "Alta", assignee: "F" },
    ],
  },
  {
    id: "done",
    label: "Concluído",
    color: "bg-emerald-400",
    tasks: [
      { id: 9, title: "Setup do repositório", priority: "Baixa", assignee: "L" },
      { id: 10, title: "Documentar arquitetura", priority: "Média", assignee: "M" },
    ],
  },
];

export const TarefasPage = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();

  const project = projectsData[projectId ?? ""] ?? { name: "Projeto", color: "#0E1F63" };

  return (
    <div className="flex flex-col h-full space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate("/projetos")}
            className="flex items-center justify-center w-8 h-8 rounded-lg border border-gray-200 bg-white text-gray-500 hover:bg-gray-50 transition-colors"
          >
            <ArrowLeft size={16} weight="bold" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <span
                className="w-2.5 h-2.5 rounded-full shrink-0"
                style={{ backgroundColor: project.color }}
              />
              <h1 className="text-2xl font-bold text-[#0E1F63]">{project.name}</h1>
            </div>
            <p className="text-sm text-gray-500 mt-0.5 ml-4">Kanban do projeto</p>
          </div>
        </div>
        <button className="flex items-center gap-2 bg-[#0E1F63] text-white text-sm font-medium px-4 py-2.5 rounded-xl hover:bg-[#1a2f7a] transition-colors shadow-sm">
          <Plus size={16} weight="bold" />
          Nova Tarefa
        </button>
      </div>

      {/* Kanban board */}
      <div className="flex gap-4 overflow-x-auto pb-4 flex-1 items-start">
        {initialColumns.map((col) => (
          <div
            key={col.id}
            className="flex flex-col shrink-0 w-64 bg-gray-50 rounded-2xl border border-gray-100"
          >
            {/* Column header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
              <div className="flex items-center gap-2">
                <span className={`w-2 h-2 rounded-full ${col.color}`} />
                <span className="text-sm font-semibold text-gray-700">{col.label}</span>
                <span className="text-xs text-gray-400 bg-gray-200 px-1.5 py-0.5 rounded-full font-medium">
                  {col.tasks.length}
                </span>
              </div>
              <button className="text-gray-300 hover:text-gray-500 transition-colors">
                <DotsThree size={18} weight="bold" />
              </button>
            </div>

            {/* Tasks */}
            <div className="flex flex-col gap-2 p-3">
              {col.tasks.map((task) => (
                <div
                  key={task.id}
                  className="bg-white rounded-xl p-3 shadow-sm border border-gray-100 hover:shadow-md transition-shadow cursor-pointer group"
                >
                  <p className="text-sm font-medium text-gray-800 leading-snug mb-3">
                    {task.title}
                  </p>
                  <div className="flex items-center justify-between">
                    <span
                      className={`text-[11px] font-semibold px-2 py-0.5 rounded-full ${priorityConfig[task.priority]}`}
                    >
                      {task.priority}
                    </span>
                    <div
                      className="w-6 h-6 rounded-full bg-gradient-to-br from-[#0E1F63] to-[#3d5aad] flex items-center justify-center text-white text-[10px] font-bold"
                      title={task.assignee}
                    >
                      {task.assignee}
                    </div>
                  </div>
                </div>
              ))}

              {/* Add task button */}
              <button className="flex items-center gap-1.5 w-full px-2 py-2 rounded-xl text-xs text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors">
                <Plus size={13} weight="bold" />
                Adicionar tarefa
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
