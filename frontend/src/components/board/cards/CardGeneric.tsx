import type { Card } from "../domain/types";
import moment from "moment";
import { Trash, PencilSimple } from "@phosphor-icons/react";
import { httpClient } from "api";
import { useState } from "react";

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
                className="text-gray-300 hover:text-gray-500 transition-colors"
              >
                <PencilSimple size={15} weight="bold" />
              </button>

              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setConfirmDelete(true);
                }}
                className="text-gray-300 hover:text-red-400 transition-colors"
              >
                <Trash size={15} weight="bold" />
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
      {date && (
        <p className="text-[10px] text-muted-foreground mt-0.5">{date}</p>
      )}
    </div>
  );
}
