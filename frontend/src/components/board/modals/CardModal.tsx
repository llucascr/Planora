import { useEffect } from "react";
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
} from "@phosphor-icons/react";
import { classnames } from "../utils/classnames";
import { useNavigate } from "react-router-dom";

interface CardModalProps {
  card: Card;
  columnName?: string;
  onClose: () => void;
}

export function CardModal({ card, columnName, onClose }: CardModalProps) {
  const type = detectCardType(card);
  const navigate = useNavigate();

  // Close on Escape
  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  const date = card.createdAt
    ? moment(card.createdAt)
        .locale("pt-br")
        .format("DD [de] MMMM [de] YYYY [às] HH:mm")
    : null;

  const headerColor: Record<string, string> = {
    lead: "from-blue-600 to-blue-500",
    diagnostico: "from-purple-600 to-purple-500",
    planoAcao: "from-amber-600 to-amber-500",
    generic: "from-slate-600 to-slate-500",
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />

      {/* Modal */}
      <div className="relative z-10 w-full max-w-lg max-h-[90vh] flex flex-col rounded-2xl bg-card border border-border shadow-2xl overflow-hidden">
        {/* Header gradient */}
        <div
          className={classnames(
            "bg-gradient-to-r p-5 text-white shadow-lg",
            headerColor[type],
          )}
        >
          <div className="flex items-start justify-between gap-3">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <Badge
                  className="bg-white bg-opacity-20 text-white border-0"
                  size="sm"
                >
                  {getCardTypeLabel(type)}
                </Badge>
                {columnName && (
                  <Badge
                    className="bg-white bg-opacity-20 text-white border-0"
                    size="sm"
                  >
                    {columnName}
                  </Badge>
                )}
              </div>
              <h2 className="text-lg font-bold leading-snug">
                {card.nome || `${getCardTypeLabel(type)} #${card.id}`}
              </h2>
              {date && (
                <p className="text-xs text-white opacity-70 mt-1">{date}</p>
              )}
            </div>
            <button
              onClick={onClose}
              className="shrink-0 flex h-8 w-8 items-center justify-center rounded-full bg-white bg-opacity-20 hover:bg-opacity-30 transition-colors text-white"
              aria-label="Fechar"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          {/* Lead avatar + origem in header */}
          {card.lead && (
            <div className="mt-4 flex items-center gap-3">
              <Avatar
                name={card.lead.nome}
                size="md"
                className="ring-2 ring-white ring-opacity-20"
              />
              <div>
                <p className="text-sm font-bold">{card.lead.nome}</p>
                {card.lead.razaoSocial && (
                  <p className="text-xs text-white opacity-70">
                    {card.lead.razaoSocial}
                  </p>
                )}
              </div>
              {card.lead.origem && (
                <span className="ml-auto flex items-center gap-1.5 rounded-full bg-white bg-opacity-10 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white shadow-inner">
                  {getOrigemIcon(card.lead.origem)} {card.lead.origem}
                </span>
              )}
            </div>
          )}
        </div>

        {/* Body — scrollable */}
        <div className="flex-1 overflow-y-auto p-5 flex flex-col gap-6">
          {/* ─── LEAD CONTENT ─────────────────────── */}
          {card.lead && (
            <>
              {/* Contacts */}
              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground mb-3 px-1">
                  Canais de Contato
                </p>
                <div className="grid grid-cols-2 gap-3">
                  {card.lead.contatos.map((c) => (
                    <div
                      key={c.codigo}
                      className="rounded-xl bg-secondary p-3 border border-border/50"
                    >
                      <p className="text-[9px] text-muted-foreground font-bold uppercase tracking-widest mb-1.5">
                        {c.codigo}
                      </p>
                      <p className="text-xs font-semibold text-foreground truncate">
                        {c.valor || "—"}
                      </p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Fit ICP */}
              {card.lead.contatos.find((c) => c.codigo === "Fit ICP") &&
                (() => {
                  const val = parseFloat(
                    card.lead!.contatos.find((c) => c.codigo === "Fit ICP")!
                      .valor,
                  );
                  return (
                    <div className="bg-secondary p-4 rounded-xl border border-border/50">
                      <KpiIndicator
                        label="Fit ICP Score"
                        value={val}
                        max={10}
                        unit="/10"
                        variant={scoreToVariant(val)}
                        size="md"
                      />
                    </div>
                  );
                })()}
            </>
          )}

          {card.planoAcao && (
            <>
              {card.planoAcao.observacao != "OPORTUNIDADE" &&
                card.planoAcao.observacao != "GARGALO" && (
                  <div className="rounded-xl bg-violet-500 bg-opacity-5 border border-violet-500 border-opacity-20 p-4">
                    <p className="text-[10px] font-bold text-violet-500 uppercase tracking-widest mb-2">
                      Resumo
                    </p>
                    <p className="text-sm text-foreground italic leading-relaxed">
                      "{card.planoAcao.observacao}"
                    </p>
                  </div>
                )}

              {card.descricao && (
                <div>
                  <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground mb-2 px-1">
                    Análise Detalhada
                  </p>
                  <p className="text-sm text-foreground opacity-80 leading-relaxed px-1">
                    {card.descricao}
                  </p>
                </div>
              )}

              {/* Planos grouped */}
              {card.planoAcao.clientes.length > 0 && (
                <div
                  key={card.planoAcao.id}
                  className="rounded-2xl border border-border bg-card p-4 transition-all hover:bg-accent/30"
                >
                  {card.planoAcao.observacao != "" && (
                    <div className="flex items-start justify-between gap-3 mb-3">
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
                  )}

                  <div className="">
                    <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest mb-2">
                      Clientes ({card.planoAcao.clientes.length})
                    </p>
                    <div className="flex flex-col gap-1.5 h-52 overflow-auto">
                      {card.planoAcao.clientes.map((c) => (
                        <div
                          key={c.idCliente}
                          className="flex items-center gap-3 p-2.5 rounded-xl border border-border/40 bg-slate-500/5 hover:bg-slate-500/10 cursor-pointer transition-all active:scale-[0.98] group"
                          onClick={() => {
                            navigate(`/ans/cliente/${c.codigo}`);
                          }}
                        >
                          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white shadow-sm dark:bg-slate-800 border border-border/40 transition-shadow group-hover:shadow-md">
                            <Users
                              weight="duotone"
                              className="h-4.5 w-4.5 text-muted-foreground transition-colors group-hover:text-primary"
                            />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-[11px] font-bold text-foreground truncate leading-tight">
                              {c.razaoSocial || c.nome}
                            </p>
                            <p className="text-[10px] font-mono text-muted-foreground/60 leading-none mt-0.5">
                              #{c.codigo}
                            </p>
                          </div>
                          <div className="h-6 w-6 flex items-center justify-center rounded-full bg-transparent group-hover:bg-primary/10 transition-colors">
                            <ArrowRightIcon className="h-3 w-3 text-muted-foreground/40 group-hover:text-primary group-hover:opacity-100" />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </>
          )}

          {/* ─── GENERIC CONTENT ───────────────────── */}
          {type === "generic" && card.descricao && (
            <div>
              <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground mb-2 px-1">
                Descrição
              </p>
              <p className="text-sm text-foreground opacity-80 leading-relaxed px-1">
                {card.descricao}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
