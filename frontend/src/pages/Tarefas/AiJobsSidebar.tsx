import { useState } from "react";
import { httpClient } from "api";
import { useUI } from "context";
import {
  Sparkle,
  CheckCircle,
  XCircle,
  SpinnerGap,
  Plus,
} from "@phosphor-icons/react";

type JobStatus = "PROCESSING" | "DONE" | "ERROR";

interface AiJob {
  id: number;
  name: string;
  status: JobStatus;
}

const MOCK_JOBS: AiJob[] = [
  { id: 1, name: "Geração de backlog — Sprint 3", status: "DONE" },
  { id: 2, name: "Análise de issues duplicadas", status: "PROCESSING" },
  { id: 3, name: "Sugestão de prioridades", status: "PROCESSING" },
  { id: 4, name: "Resumo do projeto", status: "ERROR" },
];

const STATUS_CONFIG: Record<
  JobStatus,
  { label: string; className: string; Icon: React.ElementType; spin?: boolean }
> = {
  PROCESSING: {
    label: "Processando",
    className: "text-blue-700 bg-blue-50 border-blue-200",
    Icon: SpinnerGap,
    spin: true,
  },
  DONE: {
    label: "Concluído",
    className: "text-green-700 bg-green-50 border-green-200",
    Icon: CheckCircle,
  },
  ERROR: {
    label: "Erro",
    className: "text-red-700 bg-red-50 border-red-200",
    Icon: XCircle,
  },
};

const CreateIssueForm = ({
  boardId,
  onClose,
}: {
  boardId: number;
  onClose: () => void;
}) => {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setSubmitting(true);
    try {
      await httpClient.post("/v1/kanban/board/issue/create", {
        boardId,
        title: title.trim(),
        description: description.trim(),
      });
      onClose();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 p-1">
      <p className="text-xs text-foreground/50">
        Informe o título e a descrição. A IA irá processar e criar a issue no
        board.
      </p>

      <div className="flex flex-col gap-1.5">
        <label className="text-xs font-semibold text-foreground">
          Título <span className="text-red-500">*</span>
        </label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Ex: Criar tela de login"
          required
          autoFocus
          className="px-3 py-2 rounded-lg border border-border bg-background text-sm outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition"
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label className="text-xs font-semibold text-foreground">
          Descrição
        </label>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Descreva o contexto para a IA gerar a issue..."
          rows={5}
          className="px-3 py-2 rounded-lg border border-border bg-background text-sm outline-none focus:border-[#3d5aad] focus:ring-2 focus:ring-[#3d5aad]/10 transition resize-none"
        />
      </div>

      <div className="flex gap-2 justify-end pt-1">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 rounded-lg text-sm font-medium text-foreground border border-border hover:bg-accent transition-colors"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={submitting || !title.trim()}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-white text-sm font-medium hover:bg-[#1a2f7a] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {submitting ? (
            <SpinnerGap size={14} className="animate-spin" />
          ) : (
            <Sparkle size={14} weight="fill" />
          )}
          {submitting ? "Gerando..." : "Gerar com IA"}
        </button>
      </div>
    </form>
  );
};

export const AiJobsSidebar = ({ boardId }: { boardId: number }) => {
  const ui = useUI();

  function openCreateModal() {
    ui.show({
      id: "create-issue-ai",
      type: "modal",
      options: { titulo: "Nova Issue com IA" },
      content: (
        <CreateIssueForm
          boardId={boardId}
          onClose={() => ui.hide("modal", "create-issue-ai")}
        />
      ),
    });
  }

  return (
    <div className="flex flex-col h-full p-4 gap-4">
      <button
        onClick={openCreateModal}
        className="flex items-center justify-center gap-2 w-full py-2.5 rounded-lg bg-primary text-white text-sm font-medium hover:bg-[#1a2f7a] transition-colors"
      >
        <Plus size={15} weight="bold" />
        Nova Issue com IA
      </button>

      <div>
        <p className="text-xs text-foreground/50 mb-3">Processamentos de IA</p>

        {MOCK_JOBS.length === 0 ? (
          <div className="flex flex-col items-center py-12 gap-3 text-foreground/40">
            <Sparkle size={40} weight="duotone" />
            <p className="text-sm">Nenhum job encontrado</p>
          </div>
        ) : (
          <ul className="space-y-2">
            {MOCK_JOBS.map((job) => {
              const cfg = STATUS_CONFIG[job.status];
              return (
                <li
                  key={job.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-border bg-card gap-2"
                >
                  <span className="text-sm font-medium text-foreground truncate">
                    {job.name}
                  </span>
                  <span
                    className={`flex items-center gap-1 text-xs px-2 py-0.5 rounded-full border shrink-0 ${cfg.className}`}
                  >
                    <cfg.Icon
                      size={11}
                      weight="bold"
                      className={cfg.spin ? "animate-spin" : ""}
                    />
                    {cfg.label}
                  </span>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
};
