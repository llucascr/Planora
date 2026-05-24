import { httpClient } from "api";
import { useUI } from "context";
import {
  Sparkle,
  CheckCircle,
  XCircle,
  SpinnerGap,
  PlusIcon,
} from "@phosphor-icons/react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import type { ProjetoBoard } from "types";
import { useEffect, useState } from "react";

type JobStatus = "PROCESSING" | "COMPLETED" | "ERROR";

interface AiJob {
  id: number;
  title: string | null;
  description: string;
  status: JobStatus;
}

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
  COMPLETED: {
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
  const [previewMode, setPreviewMode] = useState(false);
  const [columns, setColumns] = useState<{ id: string; name: string }[]>([]);
  const [selectedColumnId, setSelectedColumnId] = useState<string>("");

  useEffect(() => {
    async function loadBoard() {
      try {
        const res = await httpClient.get<ProjetoBoard>(
          `/v1/kanban/board/${boardId}`,
        );
        const cols = res.columns
          .sort((a, b) => a.position - b.position)
          .map((c) => ({
            id: String(c.kanbanColumnId),
            name: c.name,
          }));
        setColumns(cols);
        if (cols.length > 0) {
          setSelectedColumnId(cols[0].id);
        }
      } catch (err) {
        console.error("Erro ao carregar colunas:", err);
      }
    }
    loadBoard();
  }, [boardId]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !selectedColumnId) return;
    setSubmitting(true);

    try {
      const endpoint = `/v1/ia?title=${encodeURIComponent(
        title.trim(),
      )}&boardId=${boardId}&columnId=${selectedColumnId}`;

      await httpClient.post(endpoint, {
        description: description.trim(),
      });

      onClose();
    } catch (err) {
      console.error(err);
      alert("Erro ao criar issue com IA.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col h-full bg-background overflow-hidden"
    >
      {/* Header Info */}
      <div className="p-5 border-b border-border bg-card shrink-0">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Adicionar título (ex: Backlog de Auth)"
              required
              autoFocus
              className="w-full bg-transparent text-xl font-bold text-foreground placeholder:text-muted-foreground/60 outline-none"
            />
          </div>
        </div>
      </div>

      {/* Main Body */}
      <div className="flex-1 overflow-y-auto p-5 flex flex-col gap-5">
        {/* Description Editor */}
        <div className="flex flex-col gap-1.5 flex-1 min-h-[300px]">
          <label className="text-[13px] font-semibold text-foreground">
            Contexto detalhado do projeto para geração do backlog
          </label>

          <div className="flex flex-col flex-1 border border-border rounded-md bg-card overflow-hidden focus-within:border-primary focus-within:ring-1 focus-within:ring-primary transition">
            {/* Tabs */}
            <div className="flex items-center gap-1 bg-secondary/50 border-b border-border px-2 py-1.5">
              <button
                type="button"
                onClick={() => setPreviewMode(false)}
                className={`px-3 py-1.5 text-[13px] font-medium rounded-md transition-colors ${
                  !previewMode
                    ? "bg-card text-foreground shadow-sm border border-border"
                    : "text-muted-foreground hover:text-foreground border border-transparent"
                }`}
              >
                Write
              </button>
              <button
                type="button"
                onClick={() => setPreviewMode(true)}
                className={`px-3 py-1.5 text-[13px] font-medium rounded-md transition-colors ${
                  previewMode
                    ? "bg-card text-foreground shadow-sm border border-border"
                    : "text-muted-foreground hover:text-foreground border border-transparent"
                }`}
              >
                Preview
              </button>
            </div>

            <div className="flex-1 relative bg-card">
              {!previewMode ? (
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Descreva o contexto..."
                  className="absolute inset-0 w-full h-full p-4 bg-transparent text-[13px] text-foreground font-mono outline-none resize-none"
                />
              ) : (
                <div
                  className="absolute inset-0 w-full h-full p-5 overflow-y-auto bg-card
                  prose prose-sm max-w-none
                  prose-headings:text-foreground prose-headings:font-bold prose-headings:border-b prose-headings:border-border prose-headings:pb-2 prose-headings:mb-3
                  prose-p:text-foreground/80 prose-p:my-2.5 prose-p:leading-relaxed
                  prose-a:text-blue-600 hover:prose-a:underline prose-a:font-medium
                  prose-code:bg-secondary prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded-md prose-code:text-foreground prose-code:text-[13px] prose-code:font-mono prose-code:before:content-none prose-code:after:content-none
                  prose-pre:bg-secondary prose-pre:text-foreground prose-pre:p-4 prose-pre:rounded-xl prose-pre:border prose-pre:border-border
                  prose-ul:my-3 prose-ol:my-3 prose-ul:pl-5 prose-ol:pl-5
                  prose-li:text-foreground/80 prose-li:my-1
                  prose-strong:text-foreground prose-strong:font-semibold
                  prose-blockquote:border-l-4 prose-blockquote:border-border prose-blockquote:text-muted-foreground prose-blockquote:pl-4 prose-blockquote:italic"
                >
                  {description ? (
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>
                      {description}
                    </ReactMarkdown>
                  ) : (
                    <p className="text-muted-foreground italic not-prose text-sm">
                      Nothing to preview
                    </p>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Column Select */}
        <div className="flex flex-col gap-1.5">
          <label className="text-[13px] font-semibold text-foreground">
            Coluna Destino
          </label>
          <select
            value={selectedColumnId}
            onChange={(e) => setSelectedColumnId(e.target.value)}
            className="px-3 py-2 bg-card border border-border rounded-md text-[13px] text-foreground outline-none focus:border-primary"
            required
          >
            {columns.length === 0 && <option value="">Carregando...</option>}
            {columns.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Footer Controls */}
      <div className="flex items-center justify-end gap-3 p-4 border-t border-border bg-card shrink-0">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 rounded-lg text-sm font-medium text-foreground hover:bg-accent transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={submitting || !title.trim() || !selectedColumnId}
          className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-bold shadow-sm hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {submitting ? (
            <SpinnerGap size={14} className="animate-spin" />
          ) : (
            <Sparkle size={14} weight="fill" />
          )}
          {submitting ? "Processando..." : "Gerar com IA"}
        </button>
      </div>
    </form>
  );
};

export const AiJobsSidebar = ({ boardId }: { boardId: number }) => {
  const ui = useUI();
  const [jobs, setJobs] = useState<AiJob[]>([]);

  useEffect(() => {
    let mounted = true;

    async function fetchJobs() {
      try {
        const data = await httpClient.get<AiJob[]>(
          `/v1/jobs?boardId=${boardId}`,
        );
        if (mounted) {
          setJobs(data);
        }
      } catch (err) {
        console.error("Erro ao carregar jobs", err);
      }
    }

    fetchJobs();
    const intervalId = setInterval(fetchJobs, 10000); // 10s polling
    return () => {
      mounted = false;
      clearInterval(intervalId);
    };
  }, [boardId]);

  function openCreateModal() {
    ui.show({
      id: "create-issue-ai",
      type: "modal",
      options: {
        titulo: "Nova backlog com IA",
      },
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
        <PlusIcon size={15} weight="bold" />
        Nova backlog com IA
      </button>
      <div>
        <p className="text-xs text-foreground/50 mb-3">Processamentos de IA</p>

        {jobs.length === 0 ? (
          <div className="flex flex-col items-center py-12 gap-3 text-foreground/40">
            <Sparkle size={40} weight="duotone" />
            <p className="text-sm">Nenhum job encontrado</p>
          </div>
        ) : (
          <ul className="space-y-2">
            {jobs.map((job) => {
              const cfg = STATUS_CONFIG[job.status] || STATUS_CONFIG["ERROR"];
              const jobTitle = job.title || "Geração de Backlog";
              return (
                <li
                  key={job.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-border bg-card gap-2"
                >
                  <span
                    className="text-sm font-medium text-foreground truncate"
                    title={jobTitle}
                  >
                    {jobTitle}
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
