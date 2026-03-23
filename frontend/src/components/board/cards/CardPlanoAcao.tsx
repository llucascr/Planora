import type { Card } from "../domain/types";
import { ExpandableSection } from "../design/ExpandableSection";
import moment from "moment";
import { Warning, TrendUp, Users } from "@phosphor-icons/react";
import { Badge } from "../design/Badge";

interface CardPlanoAcaoProps {
  card: Card;
  compact?: boolean;
  onClick?: () => void;
}

export function CardPlanoAcao({ card, compact = false }: CardPlanoAcaoProps) {
  const { planoAcao } = card;
  if (!planoAcao) return null;

  const totalClientes = planoAcao.clientes.length;

  const monthName = moment(new Date(planoAcao.ano, planoAcao.mes - 1))
    .locale("pt-br")
    .format("MMMM YYYY");

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-start justify-between gap-3">
        <div className="flex gap-1.5">
          {planoAcao.observacao === "GARGALO" && (
            <Badge
              variant="danger"
              icon={<Warning className="h-2.5 w-2.5" />}
              size="sm"
            >
              Gargalo
            </Badge>
          )}
          {planoAcao.observacao === "OPORTUNIDADE" && (
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

      {/* Title */}
      <p className="text-[13px] font-bold text-foreground leading-snug line-clamp-2">
        {card.nome || "Plano de Ação"}
      </p>

      <p className="text-sm text-foreground opacity-80 leading-relaxed px-1">
        {card.descricao}
      </p>

      {totalClientes > 0 && (
        <div className="grid grid-cols-3 gap-1.5">
          <div className="flex flex-col items-center rounded-md bg-blue-500/10 py-1 px-1">
            <span className="text-sm font-black text-blue-500">
              {totalClientes}
            </span>
            <span className="text-[9px] text-blue-500/70 font-medium">
              Clientes
            </span>
          </div>
        </div>
      )}
      {/* Planos preview (non-compact) */}
      {!compact && totalClientes > 0 && (
        <ExpandableSection
          title={
            <span className="text-muted-foreground">
              Planos c/ clientes ({totalClientes})
            </span>
          }
          headerClassName="hover:bg-white/5"
        >
          <div className="flex flex-col gap-1.5 mt-1">
            <div key={planoAcao.id} className="rounded-md bg-white/5 p-1.5">
              <div className="flex items-center gap-1 mb-0.5">
                {planoAcao.observacao === "GARGALO" ? (
                  <Warning className="h-2.5 w-2.5 text-red-500 shrink-0" />
                ) : (
                  <TrendUp className="h-2.5 w-2.5 text-emerald-500 shrink-0" />
                )}
                {planoAcao.label && (
                  <span className="text-[10px] font-semibold text-foreground truncate">
                    {planoAcao.label}
                  </span>
                )}
              </div>

              {planoAcao.clientes.slice(0, 2).map((c) => (
                <p
                  key={c.idCliente}
                  className="flex items-center gap-1 text-[10px] text-muted-foreground truncate"
                >
                  <Users className="h-2.5 w-2.5 shrink-0" />
                  {c.razaoSocial || c.nome}
                </p>
              ))}
              {planoAcao.clientes.length > 2 && (
                <p className="text-[10px] text-muted-foreground">
                  +{planoAcao.clientes.length - 2} clientes
                </p>
              )}
            </div>
          </div>
        </ExpandableSection>
      )}
      {monthName && (
        <p className="text-[10px] text-muted-foreground">{monthName}</p>
      )}
    </div>
  );
}
