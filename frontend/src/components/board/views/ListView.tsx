import React from "react";
import { useBoardState, useBoardDispatch } from "../domain/boardStore";
import { useFilteredColumnCards } from "../filters/useFilters";
import { Card } from "../cards/Card";
import type { DragInfo } from "../domain/types";
import type { MemberBoard } from "types";

interface ListViewProps {
  dragRef: React.MutableRefObject<DragInfo | null>;
  onCardClick?: (cardId: string) => void;
  onCardMove?: (from: string, to: string, cardId: string) => void;
  members?: MemberBoard[];
  boardId?: number;
  repository?: string;
  refetch?: () => void;
}

function ListColumnSection({
  columnId,
  dragRef,
  onCardClick,
  onCardMove,
  members = [],
  boardId,
  repository,
  refetch,
}: {
  columnId: string;
  dragRef: React.MutableRefObject<DragInfo | null>;
  onCardClick?: (id: string) => void;
  onCardMove?: (from: string, to: string, cardId: string) => void;
  members?: MemberBoard[];
  boardId?: number;
  repository?: string;
  refetch?: () => void;
}) {
  const state = useBoardState();
  const dispatch = useBoardDispatch();
  const column = state.normalized.columns[columnId];
  const filteredCardIds = useFilteredColumnCards(columnId);
  const isCollapsed = state.collapsedColumns.has(columnId);

  return (
    <div className="mb-4">
      <button
        className="flex w-full items-center gap-2 mb-2 px-1 group"
        onClick={() => dispatch({ type: "TOGGLE_COLUMN", payload: columnId })}
      >
        <div className="flex-1 h-px bg-border group-hover:bg-primary/30 transition-colors" />
        <span className="shrink-0 text-xs font-bold text-muted-foreground uppercase tracking-wider group-hover:text-foreground transition-colors">
          {column?.nome}
        </span>
        <span className="shrink-0 rounded-full bg-primary/10 px-1.5 text-[10px] font-bold text-primary">
          {filteredCardIds.length}
        </span>
        <div className="flex-1 h-px bg-border group-hover:bg-primary/30 transition-colors" />
      </button>

      {!isCollapsed && (
        <div className="flex flex-col gap-2 max-w-2xl mx-auto -z-10">
          {filteredCardIds.map((cardId) => {
            const card = state.normalized.cards[cardId];
            if (!card) return null;
            return (
              <Card
                key={cardId}
                card={card}
                columnId={columnId}
                compact={true}
                dragRef={dragRef}
                onCardClick={onCardClick}
                onCardMove={onCardMove}
                members={members}
                boardId={boardId}
                repository={repository}
                refetch={refetch}
              />
            );
          })}
          {filteredCardIds.length === 0 && (
            <p className="text-center text-xs text-muted-foreground py-10 italic">
              Nenhum card nesta etapa
            </p>
          )}
        </div>
      )}
    </div>
  );
}

export function ListView({
  dragRef,
  onCardClick,
  onCardMove,
  members = [],
  boardId,
  repository,
  refetch,
}: ListViewProps) {
  const state = useBoardState();
  return (
    <div className="flex-1 overflow-y-auto px-2 pb-4">
      {state.normalized.columnOrder.map((colId) => (
        <ListColumnSection
          key={colId}
          columnId={colId}
          dragRef={dragRef}
          onCardClick={onCardClick}
          onCardMove={onCardMove}
          members={members}
          boardId={boardId}
          repository={repository}
          refetch={refetch}
        />
      ))}
    </div>
  );
}
