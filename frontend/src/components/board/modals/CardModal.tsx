import { useEffect, useState } from "react";
import type { Card } from "../domain/types";
import {
  detectCardType,
  getCardTypeLabel,
  getOrigemIcon,
} from "../domain/cardDetector";
import { Avatar } from "../design/Avatar";
import { Badge } from "../design/Badge";
import { KpiIndicator, scoreToVariant } from "../design/KpiIndicator";
import moment from "moment";
import {
  X,
  Warning,
  TrendUp,
  Users,
  ArrowRightIcon,
  Code,
  TextT,
  Circle,
  Tag,
  CalendarBlank,
  Columns,
} from "@phosphor-icons/react";
import { useNavigate } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { PencilSimple, Trash } from "@phosphor-icons/react";
import { IssueForm } from "../../../pages/Tarefas/IssueForm";
import type { MemberBoard } from "../../../types";

interface CardModalProps {
  card: Card;
  columnName?: string;
  onClose: () => void;
  onDelete?: () => void;
  boardId?: number;
  columnId?: number;
  repository?: string;
  githubOwnerName?: string;
  members?: MemberBoard[];
  refetch?: () => void;
}

export function CardModal({
  card,
  columnName,
  onClose,
  onDelete,
  boardId,
  columnId,
  repository,
  githubOwnerName,
  members,
  refetch,
}: CardModalProps) {
  const type = detectCardType(card);
  const navigate = useNavigate();
  const [markdownView, setMarkdownView] = useState(true);
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  const timeAgo = card.createdAt
    ? moment(card.createdAt).locale("pt-br").fromNow()
    : null;

  const fullDate = card.createdAt
    ? moment(card.createdAt)
        .locale("pt-br")
        .format("DD [de] MMMM [de] YYYY [às] HH:mm")
    : null;

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Sidebar panel */}
      <div className="relative z-10 ml-auto h-full w-full max-w-[70%] xl:max-w-[1000px] flex flex-col bg-card border-l border-border shadow-2xl overflow-hidden animate-slide-in-right">
        {/* Top bar */}
        <div className="flex items-center justify-between px-4 py-2.5 border-b border-border bg-secondary shrink-0">
          <div className="flex items-center gap-1.5 text-[13px] text-muted-foreground min-w-0">
            <span className="font-mono text-muted-foreground">#{card.id}</span>
            {columnName && (
              <>
                <span className="mx-1 opacity-40">·</span>
                <span className="truncate text-foreground/70">
                  {columnName}
                </span>
              </>
            )}
          </div>
          <button
            onClick={onClose}
            className="shrink-0 flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
            aria-label="Fechar"
          >
            <X size={15} weight="bold" />
          </button>
        </div>

        {/* Issue header */}
        <div className="px-5 pt-5 pb-4 border-b border-border shrink-0">
          <h1 className="text-[17px] font-semibold text-foreground leading-snug mb-3 wrap-break-word">
            {card.nome || `${getCardTypeLabel(type)} #${card.id}`}
          </h1>

          <div className="flex items-center flex-wrap gap-x-2 gap-y-1.5">
            {/* Open state badge — GitHub green */}
            <span className="inline-flex items-center gap-1.5 pl-2.5 pr-3 py-[5px] rounded-full text-[12px] font-semibold bg-[#1a7f37] text-white leading-none">
              <svg
                width="14"
                height="14"
                viewBox="0 0 16 16"
                fill="currentColor"
                aria-hidden="true"
              >
                <path d="M8 9.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z" />
                <path d="M8 0a8 8 0 1 1 0 16A8 8 0 0 1 8 0ZM1.5 8a6.5 6.5 0 1 0 13 0 6.5 6.5 0 0 0-13 0Z" />
              </svg>
              Aberto
            </span>

            {timeAgo && (
              <span
                className="text-[13px] text-muted-foreground"
                title={fullDate ?? undefined}
              >
                criado {timeAgo}
              </span>
            )}
          </div>

          {/* Lead avatar in header */}
          {card.lead && (
            <div className="mt-4 flex items-center gap-2.5 pt-4 border-t border-border">
              <Avatar name={card.lead.nome} size="md" />
              <div className="min-w-0">
                <p className="text-[13px] font-semibold text-foreground leading-tight">
                  {card.lead.nome}
                </p>
                {card.lead.razaoSocial && (
                  <p className="text-[12px] text-muted-foreground truncate">
                    {card.lead.razaoSocial}
                  </p>
                )}
              </div>
              {card.lead.origem && (
                <span className="ml-auto flex items-center gap-1 rounded-full border border-border bg-secondary px-2.5 py-1 text-[11px] font-semibold text-muted-foreground">
                  {getOrigemIcon(card.lead.origem)}
                  {card.lead.origem}
                </span>
              )}
            </div>
          )}
        </div>

        {isEditing &&
        boardId &&
        columnId &&
        repository &&
        githubOwnerName &&
        members ? (
          <div className="flex-1 overflow-y-auto p-4 bg-card">
            <IssueForm
              action="update"
              issue={card}
              boardId={boardId}
              columnId={columnId}
              repository={repository}
              githubOwnerName={githubOwnerName}
              members={members}
              refetch={() => {
                refetch?.();
                setIsEditing(false);
              }}
              onClose={() => setIsEditing(false)}
            />
          </div>
        ) : (
          <div className="flex-1 overflow-hidden flex flex-col md:flex-row">
            <div className="flex-1 overflow-y-auto divide-y divide-border min-w-0 md:border-r md:border-border pb-8">
              {!card.lead && !card.planoAcao && card.descricao && (
                <div className="px-5 py-4">
                  <div className="flex items-center justify-between mb-3">
                    <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
                      Descrição
                    </p>
                    <button
                      onClick={() => setMarkdownView((v) => !v)}
                      className="flex items-center gap-1 text-[11px] font-medium text-muted-foreground hover:text-foreground transition-colors rounded-md px-2 py-0.5 border border-border hover:bg-secondary"
                      title={
                        markdownView
                          ? "Ver texto bruto"
                          : "Ver markdown renderizado"
                      }
                    >
                      {markdownView ? (
                        <>
                          <Code size={11} /> Raw
                        </>
                      ) : (
                        <>
                          <TextT size={11} /> Preview
                        </>
                      )}
                    </button>
                  </div>

                  {markdownView ? (
                    <div
                      className="prose prose-sm max-w-none
                      prose-headings:text-foreground prose-headings:font-semibold prose-headings:border-b prose-headings:border-border prose-headings:pb-1
                      prose-p:text-foreground/80 prose-p:my-3 prose-p:leading-relaxed
                      prose-a:text-blue-600 prose-a:no-underline hover:prose-a:underline
                      prose-code:bg-secondary prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded-md prose-code:text-foreground prose-code:text-[85%] prose-code:font-mono prose-code:before:content-none prose-code:after:content-none
                      prose-pre:bg-secondary prose-pre:border prose-pre:border-border prose-pre:rounded-md
                      prose-ul:my-3 prose-ol:my-3 prose-ul:pl-5 prose-ol:pl-5
                      prose-li:text-foreground/80 prose-li:my-1
                      prose-blockquote:border-l-4 prose-blockquote:border-border prose-blockquote:text-muted-foreground prose-blockquote:pl-4 prose-blockquote:not-italic
                      prose-hr:border-border
                      prose-strong:text-foreground prose-strong:font-semibold
                      prose-img:rounded-md prose-img:border prose-img:border-border
                      prose-table:text-sm prose-th:bg-secondary prose-th:border prose-th:border-border prose-td:border prose-td:border-border"
                    >
                      <ReactMarkdown remarkPlugins={[remarkGfm]}>
                        {card.descricao}
                      </ReactMarkdown>
                    </div>
                  ) : (
                    <pre className="text-[13px] text-foreground/80 leading-relaxed whitespace-pre-wrap font-mono bg-secondary rounded-md p-4 border border-border">
                      {card.descricao}
                    </pre>
                  )}
                </div>
              )}

              {card.lead && (
                <>
                  <div className="px-5 py-4">
                    <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground mb-3">
                      Canais de Contato
                    </p>
                    <div className="grid grid-cols-2 gap-2.5">
                      {card.lead.contatos.map((c) => (
                        <div
                          key={c.codigo}
                          className="rounded-lg bg-secondary p-3 border border-border"
                        >
                          <p className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest mb-1.5">
                            {c.codigo}
                          </p>
                          <p className="text-[13px] font-medium text-foreground truncate">
                            {c.valor || "—"}
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>

                  {card.lead.contatos.find((c) => c.codigo === "Fit ICP") &&
                    (() => {
                      const val = parseFloat(
                        card.lead!.contatos.find((c) => c.codigo === "Fit ICP")!
                          .valor,
                      );
                      return (
                        <div className="px-5 py-4">
                          <div className="bg-secondary p-4 rounded-lg border border-border">
                            <KpiIndicator
                              label="Fit ICP Score"
                              value={val}
                              max={10}
                              unit="/10"
                              variant={scoreToVariant(val)}
                              size="md"
                            />
                          </div>
                        </div>
                      );
                    })()}
                </>
              )}

              {card.planoAcao && (
                <>
                  {card.planoAcao.observacao !== "OPORTUNIDADE" &&
                    card.planoAcao.observacao !== "GARGALO" && (
                      <div className="px-5 py-4">
                        <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground mb-2">
                          Resumo
                        </p>
                        <p className="text-[13px] text-foreground/80 italic leading-relaxed">
                          "{card.planoAcao.observacao}"
                        </p>
                      </div>
                    )}

                  {card.descricao && (
                    <div className="px-5 py-4">
                      <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground mb-2">
                        Análise Detalhada
                      </p>
                      <p className="text-[13px] text-foreground/80 leading-relaxed">
                        {card.descricao}
                      </p>
                    </div>
                  )}

                  {card.planoAcao.clientes.length > 0 && (
                    <div className="px-5 py-4">
                      <div className="flex items-center justify-between mb-3">
                        <p className="text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
                          Clientes
                        </p>
                        <div className="flex gap-1.5">
                          {card.planoAcao.observacao === "GARGALO" && (
                            <Badge
                              variant="danger"
                              icon={<Warning className="h-2.5 w-2.5" />}
                              size="sm"
                            >
                              Gargalo
                            </Badge>
                          )}
                          {card.planoAcao.observacao === "OPORTUNIDADE" && (
                            <Badge
                              variant="success"
                              icon={<TrendUp className="h-2.5 w-2.5" />}
                              size="sm"
                            >
                              Oportunidade
                            </Badge>
                          )}
                        </div>
                      </div>

                      <div className="flex flex-col gap-1.5 max-h-52 overflow-auto">
                        {card.planoAcao.clientes.map((c) => (
                          <div
                            key={c.idCliente}
                            className="flex items-center gap-3 p-2.5 rounded-lg border border-border bg-secondary hover:bg-accent cursor-pointer transition-colors group"
                            onClick={() => navigate(`/ans/cliente/${c.codigo}`)}
                          >
                            <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-card border border-border">
                              <Users
                                weight="duotone"
                                className="h-4 w-4 text-muted-foreground group-hover:text-foreground transition-colors"
                              />
                            </div>
                            <div className="flex-1 min-w-0">
                              <p className="text-[12px] font-semibold text-foreground truncate">
                                {c.razaoSocial || c.nome}
                              </p>
                              <p className="text-[11px] font-mono text-muted-foreground">
                                #{c.codigo}
                              </p>
                            </div>
                            <ArrowRightIcon className="h-3.5 w-3.5 text-muted-foreground/40 group-hover:text-foreground/60 shrink-0" />
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>

            <div className="w-full md:w-64 lg:w-72 shrink-0 overflow-y-auto bg-secondary/30 p-5 flex flex-col gap-6">
              {/* Actions */}
              {(boardId || onDelete) && (
                <div className="flex gap-2 mb-2">
                  {boardId && (
                    <button
                      onClick={() => setIsEditing(true)}
                      className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg border border-border bg-card hover:bg-accent text-sm font-semibold text-foreground transition-colors shadow-sm"
                    >
                      <PencilSimple size={16} /> Editar
                    </button>
                  )}
                  {onDelete && (
                    <button
                      onClick={onDelete}
                      className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg border border-red-500/30 bg-red-500/10 hover:bg-red-500/20 text-red-600 text-sm font-semibold transition-colors shadow-sm"
                      title="Excluir"
                    >
                      <Trash size={16} />
                    </button>
                  )}
                </div>
              )}

              {/* Assignees */}
              <div>
                <div className="flex items-center gap-2 mb-2.5">
                  <Users size={14} className="text-muted-foreground" />
                  <p className="text-[12px] font-semibold text-foreground">
                    Assignees
                  </p>
                </div>
                {card.assignees && card.assignees.length > 0 ? (
                  <div className="flex flex-col gap-2.5">
                    {card.assignees.map((assignee) => (
                      <div
                        key={assignee.login}
                        className="flex items-center gap-2.5"
                      >
                        <Avatar
                          name={assignee.login}
                          src={assignee.avatarUrl}
                          size="sm"
                        />
                        <span className="text-[13px] text-foreground font-medium">
                          {assignee.login}
                        </span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <span className="text-[13px] text-muted-foreground">
                    Nenhum responsável
                  </span>
                )}
              </div>

              <hr className="border-border" />

              {/* Labels */}
              <div>
                <div className="flex items-center gap-2 mb-2.5">
                  <Tag size={14} className="text-muted-foreground" />
                  <p className="text-[12px] font-semibold text-foreground">
                    Labels
                  </p>
                </div>
                {card.labels && card.labels.length > 0 ? (
                  <div className="flex flex-wrap gap-1.5">
                    {card.labels.map((label) => (
                      <span
                        key={label.name}
                        className="text-[11px] font-medium px-2 py-0.5 rounded-full"
                        style={{
                          backgroundColor: `#${label.color}20`,
                          color: `#${label.color}`,
                          border: `1px solid #${label.color}40`,
                        }}
                        title={label.description}
                      >
                        {label.name}
                      </span>
                    ))}
                  </div>
                ) : (
                  <span className="text-[13px] text-muted-foreground">
                    Nenhum label
                  </span>
                )}
              </div>

              <hr className="border-border" />

              {/* Status / Column */}
              <div>
                <div className="flex items-center gap-2 mb-2.5">
                  <Columns size={14} className="text-muted-foreground" />
                  <p className="text-[12px] font-semibold text-foreground">
                    Status
                  </p>
                </div>
                {columnName ? (
                  <div className="flex items-center gap-2">
                    <Circle
                      size={10}
                      weight="fill"
                      className="text-[#1a7f37] shrink-0"
                    />
                    <span className="text-[13px] text-foreground font-medium">
                      {columnName}
                    </span>
                  </div>
                ) : (
                  <span className="text-[13px] text-muted-foreground">
                    Sem status definido
                  </span>
                )}
              </div>

              <hr className="border-border" />

              {/* Date */}
              {fullDate && (
                <div>
                  <div className="flex items-center gap-2 mb-2.5">
                    <CalendarBlank
                      size={14}
                      className="text-muted-foreground"
                    />
                    <p className="text-[12px] font-semibold text-foreground">
                      Criado em
                    </p>
                  </div>
                  <time
                    className="text-[13px] text-muted-foreground"
                    title={fullDate}
                    dateTime={card.createdAt}
                  >
                    {fullDate}
                  </time>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
