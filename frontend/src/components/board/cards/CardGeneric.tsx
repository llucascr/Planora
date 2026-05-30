import type { Card } from "../domain/types";
import moment from "moment";
import { Trash, PencilSimple } from "@phosphor-icons/react";
import { httpClient } from "api";
import { useState } from "react";
import { Avatar } from "../design/Avatar";

interface CardGenericProps {
  card: Card;
  compact?: boolean;
  onClick?: () => void;
  onDelete?: (cardId: string) => void;
  onEdit?: (card: Card) => void;
}

export function CardGeneric({ card, onClick, onDelete, onEdit }: CardGenericProps) {
  const [confirmDelete, setConfirmDelete] = useState(false);

  const date = card.createdAt
    ? moment(new Date(card.createdAt)).locale("pt-br").format("DD MMM")
    : null;

  async function handleDelete(e: React.MouseEvent) {
    e.stopPropagation();

    try {
      await httpClient.delete(
        `/v1/kanban/board/issue/${card.id}`
      );

      onDelete?.(String(card.id));
    } catch (err) {
      console.error("Erro ao deletar card:", err);
    }
  }

  return (
    <div onClick={onClick} className="flex flex-col gap-1.5 group">
      <div className="flex items-start justify-between gap-2">
        <p className="text-[13px] font-semibold text-foreground leading-snug">
          {card.nome || "Card sem título"}
        </p>

         <div className="flex items-center gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition">
          {confirmDelete ? (
            <>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete(e);
                }}
                className="text-xs text-red-500 hover:text-red-700 font-medium px-1 py-0.5 rounded transition-colors"
              >
                Confirmar
              </button>

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setConfirmDelete(false);
                }}
                className="text-xs text-gray-400 hover:text-gray-600 font-medium px-1 py-0.5 rounded transition-colors"
              >
                Cancelar
              </button>
            </>
          ) : (
            <>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onEdit?.(card);
                }}
                className="p-1 text-muted-foreground/60 hover:text-foreground transition-colors rounded hover:bg-accent"
                title="Editar"
              >
                <PencilSimple size={18} weight="bold" />
              </button>

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setConfirmDelete(true);
                }}
                className="p-1 text-muted-foreground/60 hover:text-red-500 transition-colors rounded hover:bg-red-500/10"
                title="Excluir"
              >
                <Trash size={18} weight="bold" />
              </button>
            </>
          )}
        </div>
      </div>
      {card.descricao && (
        <p className="text-[11px] text-muted-foreground line-clamp-2 leading-relaxed">
          {card.descricao}
        </p>
      )}

      {card.labels && card.labels.length > 0 && (
        <div className="flex flex-wrap gap-1 mt-1">
          {card.labels.map((label) => (
            <span
              key={label.name}
              className="text-[9px] font-medium px-1.5 py-0.5 rounded-full"
              style={{
                backgroundColor: `#${label.color}20`, // 20% opacity
                color: `#${label.color}`,
                border: `1px solid #${label.color}40`,
              }}
              title={label.description}
            >
              {label.name}
            </span>
          ))}
        </div>
      )}

      <div className="flex items-center justify-between mt-1">
        {date ? (
          <p className="text-[10px] text-muted-foreground">{date}</p>
        ) : (
          <div />
        )}
        
        {card.assignees && card.assignees.length > 0 && (
          <div className="flex -space-x-1">
            {card.assignees.map((assignee) => (
              <Avatar
                key={assignee.login}
                name={assignee.login}
                src={assignee.avatarUrl}
                size="xs"
                className="border border-card"
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
