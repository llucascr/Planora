import { httpClient } from "api";
import { useUI } from "context";
import {
  Sparkle,
  CheckCircle,
  XCircle,
  SpinnerGap,
  PlusIcon,
  MagicWand,
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
  {
    label: string;
    pill: string;
    dot: string;
    Icon: React.ElementType;
    spin?: boolean;
  }
> = {
  PROCESSING: {
    label: "Processando",
    pill: "text-primary bg-accent border-primary/20",
    dot: "text-primary bg-accent",
    Icon: SpinnerGap,
    spin: true,
  },
  COMPLETED: {
    label: "Concluído",
    pill: "text-emerald-700 bg-emerald-50 border-emerald-200",
    dot: "text-emerald-600 bg-emerald-50",
    Icon: CheckCircle,
  },
  ERROR: {
    label: "Erro",
    pill: "text-red-700 bg-red-50 border-red-200",
    dot: "text-red-600 bg-red-50",
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
    <form onSubmit={handleSubmit} className="flex flex-col gap-5">
      {/* Intro */}
      <div className="flex items-start gap-3 rounded-xl bg-accent/60 border border-primary/10 p-3.5">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-linear-to-br from-primary to-primary-hover shadow-sm">
          <MagicWand size={18} weight="fill" className="text-white" />
        </div>
        <p className="text-[13px] leading-relaxed text-muted-foreground">
          Dê um título e descreva o contexto do projeto. A IA gera um backlog
          estruturado de issues direto na coluna escolhida.
        </p>
      </div>

      {/* Title */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[13px] font-semibold text-foreground">
          Título da backlog
        </label>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="ex: Backlog de Autenticação"
          required
          autoFocus
          className="w-full px-3.5 py-2.5 bg-card border border-border rounded-xl text-sm text-foreground placeholder:text-muted-foreground/60 outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition"
        />
      </div>

      {/* Description Editor */}
      <div className="flex flex-col gap-1.5">
        <label className="text-[13px] font-semibold text-foreground">
          Contexto do projeto
        </label>

        <div className="flex flex-col border border-border rounded-xl bg-card overflow-hidden focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/10 transition">
          {/* Tabs */}
          <div className="flex items-center gap-1 bg-secondary/50 border-b border-border px-2 py-1.5">
            <button
              type="button"
              onClick={() => setPreviewMode(false)}
              className={`px-3 py-1.5 text-[13px] font-medium rounded-lg transition-colors ${
                !previewMode
                  ? "bg-card text-foreground shadow-sm border border-border"
                  : "text-muted-foreground hover:text-foreground border border-transparent"
              }`}
            >
              Escrever
            </button>
            <button
              type="button"
              onClick={() => setPreviewMode(true)}
              className={`px-3 py-1.5 text-[13px] font-medium rounded-lg transition-colors ${
                previewMode
                  ? "bg-card text-foreground shadow-sm border border-border"
                  : "text-muted-foreground hover:text-foreground border border-transparent"
              }`}
            >
              Prévia
            </button>
          </div>

          <div className="h-[220px] relative bg-card">
            {!previewMode ? (
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Descreva objetivos, requisitos, stack, regras de negócio..."
                className="absolute inset-0 w-full h-full p-4 bg-transparent text-[13px] text-foreground font-mono outline-none resize-none"
              />
            ) : (
              <div
                className="absolute inset-0 w-full h-full p-5 overflow-y-auto bg-card
                prose prose-sm max-w-none
                prose-headings:text-foreground prose-headings:font-bold prose-headings:border-b prose-headings:border-border prose-headings:pb-2 prose-headings:mb-3
                prose-p:text-foreground/80 prose-p:my-2.5 prose-p:leading-relaxed
                prose-a:text-primary hover:prose-a:underline prose-a:font-medium
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
                    Nada para pré-visualizar
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
          Coluna destino
        </label>
        <select
          value={selectedColumnId}
          onChange={(e) => setSelectedColumnId(e.target.value)}
          className="px-3.5 py-2.5 bg-card border border-border rounded-xl text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/10 transition cursor-pointer"
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

      {/* Footer Controls */}
      <div className="flex items-center justify-end gap-3 pt-2 border-t border-border">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2.5 rounded-xl text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={submitting || !title.trim() || !selectedColumnId}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-linear-to-br from-primary to-primary-hover text-white text-sm font-semibold shadow-sm hover:opacity-95 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:active:scale-100 transition-all"
        >
          {submitting ? (
            <SpinnerGap size={15} className="animate-spin" />
          ) : (
            <Sparkle size={15} weight="fill" />
          )}
          {submitting ? "Gerando..." : "Gerar com IA"}
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
        size: "large",
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
    <div className="flex flex-col h-full gap-5 p-4">
      {/* Hero */}
      <div className="relative overflow-hidden rounded-2xl bg-linear-to-br from-primary to-primary-hover p-5 text-white shadow-sm">
        <div className="pointer-events-none absolute -top-10 -right-8 h-32 w-32 rounded-full bg-white/10 blur-2xl" />
        <div className="relative flex items-center gap-2.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/15 backdrop-blur-sm">
            <Sparkle size={20} weight="fill" className="text-white" />
          </div>
          <h2 className="text-lg font-extrabold tracking-tight">
            Gere backlogs com IA
          </h2>
        </div>
        <p className="relative mt-3 text-sm leading-relaxed text-white/75">
          Descreva o contexto do projeto e a IA cria issues estruturadas
          automaticamente na sua board.
        </p>
      </div>

      {/* CTA */}
      <button
        onClick={openCreateModal}
        className="flex items-center justify-center gap-2 w-full py-3 rounded-xl bg-primary text-white text-sm font-semibold shadow-sm hover:bg-primary-hover active:scale-[0.99] transition-all"
      >
        <PlusIcon size={16} weight="bold" />
        Nova backlog com IA
      </button>

      {/* Jobs */}
      <div className="flex-1 min-h-0 flex flex-col">
        <div className="flex items-center justify-between mb-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Processamentos
          </p>
          {jobs.length > 0 && (
            <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-muted px-1.5 text-[11px] font-semibold text-muted-foreground">
              {jobs.length}
            </span>
          )}
        </div>

        {jobs.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-border py-12 text-muted-foreground">
            <Sparkle size={36} weight="duotone" className="text-primary/40" />
            <p className="text-sm">Nenhum processamento ainda</p>
            <p className="text-xs text-muted-foreground/70">
              Crie sua primeira backlog com IA
            </p>
          </div>
        ) : (
          <ul className="space-y-2 overflow-y-auto">
            {jobs.map((job) => {
              const cfg = STATUS_CONFIG[job.status] || STATUS_CONFIG["ERROR"];
              const jobTitle = job.title || "Geração de Backlog";
              return (
                <li
                  key={job.id}
                  className="flex items-center gap-3 p-3 rounded-xl border border-border bg-card hover:border-primary/30 transition-colors"
                >
                  <div
                    className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${cfg.dot}`}
                  >
                    <cfg.Icon
                      size={16}
                      weight="fill"
                      className={cfg.spin ? "animate-spin" : ""}
                    />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p
                      className="text-sm font-medium text-foreground truncate"
                      title={jobTitle}
                    >
                      {jobTitle}
                    </p>
                    <span
                      className={`inline-flex items-center gap-1 mt-0.5 text-[11px] font-medium px-2 py-0.5 rounded-full border ${cfg.pill}`}
                    >
                      {cfg.label}
                    </span>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
};
