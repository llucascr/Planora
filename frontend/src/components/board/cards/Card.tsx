import React, { useRef } from "react";
import type { Card as CardData, DragInfo } from "../domain/types";
import { detectCardType, getCardTypeLabel } from "../domain/cardDetector";
import { CardGeneric } from "./CardGeneric";
import { useBoardDispatch } from "../domain/boardStore";
import { classnames } from "../utils/classnames";
import { useUI } from "context";
import { IssueForm } from "@/pages/Tarefas/IssueForm";

interface CardProps {
  card: CardData;
  columnId: string;
  columnIndex?: number;
  compact?: boolean;
  dragRef: React.MutableRefObject<DragInfo | null>;
  onCardClick?: (cardId: string) => void;
  onCardMove?: (from: string, to: string, cardId: string) => void;
  refetch?: () => void;
  boardId?: number;
  repository?: string;
}

function getCardCode(card: CardData): string | null {
  // if (card.codigo != null) return `#${card.codigo}`;
  return `#${card.id}`;
}

const typeBorder: Record<string, string> = {
  lead: "border-l-blue-500",
  diagnostico: "border-l-violet-500",
  planoAcao: "border-l-amber-500",
  generic: "border-l-slate-600",
};

const typeLabel: Record<string, { bg: string; text: string }> = {
  lead: { bg: "bg-blue-500/20", text: "text-blue-500" },
  diagnostico: { bg: "bg-violet-500/20", text: "text-violet-500" },
  planoAcao: { bg: "bg-amber-500/20", text: "text-amber-500" },
  generic: { bg: "bg-slate-500/20", text: "text-slate-500" },
};

export function Card({
  card,
  columnId,
  compact = false,
  dragRef,
  refetch,
  boardId,
  repository,
  onCardClick,
}: CardProps) {
  const dispatch = useBoardDispatch();
  const cardType = detectCardType(card);
  const cardId = String(card.id);
  const isDragging = useRef(false);
  const code = getCardCode(card);
  const label = typeLabel[cardType];
  const ui = useUI();

  function handleDragStart(e: React.DragEvent) {
    isDragging.current = true;
    dragRef.current = { cardId, sourceColumnId: columnId };
    e.dataTransfer.effectAllowed = "move";
    const ghost = document.createElement("div");
    ghost.style.position = "fixed";
    ghost.style.top = "-9999px";
    document.body.appendChild(ghost);
    e.dataTransfer.setDragImage(ghost, 0, 0);
    setTimeout(() => document.body.removeChild(ghost), 0);
  }

  function handleDragEnd() {
    isDragging.current = false;
  }

  function handleClick() {
    onCardClick?.(cardId);
    dispatch({ type: "SET_SELECTED_CARD", payload: cardId });
  }

  function renderContent() {
    const props = { card, compact, onClick: undefined };
    switch (cardType) {
      default:
        return (
          <CardGeneric
            {...props}
            onDelete={(cardId) => {
              dispatch({
                type: "DELETE_CARD",
                payload: {
                  cardId,
                  columnId,
                },
              });
            }}
            onEdit={(card) => {
              ui.show({
                id: "issue-form-update",
                type: "modal",
                options: {
                  titulo: "Editar Issue",
                },
                content: (
                  <IssueForm
                    action="update"
                    issue={card}
                    boardId={boardId!}
                    columnId={Number(columnId)}
                    repository={repository!}
                    refetch={refetch!}
                    onClose={() =>
                      ui.hide("modal", "issue-form-update")
                    }
                  />
                ),
              });
            }}
          />
        );
    }
  }

  return (
    <div
      draggable
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      onClick={handleClick}
      data-card-id={cardId}
      className={classnames(
        "group relative flex flex-col gap-2.5 rounded-lg z-10",
        "bg-card border border-border border-l-2",
        "px-3 pt-2.5 pb-2.5",
        typeBorder[cardType],
        "cursor-grab active:cursor-grabbing select-none",
        "transition-all duration-100",
        "hover:border-border hover:border-opacity-10 hover:bg-accent hover:shadow-md hover:shadow-opacity-10",
        "active:scale-[0.98] active:opacity-70",
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-[10px] font-mono text-foreground tracking-wide">
          {code}
        </span>
        <span
          className={classnames(
            "rounded-full px-1.5 py-0.5 text-[9px] font-semibold tracking-wide uppercase",
            label.bg,
            label.text,
          )}
        >
          {getCardTypeLabel(cardType)}
        </span>
      </div>

      {renderContent()}
    </div>
  );
}
