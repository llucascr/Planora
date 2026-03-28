import React from "react";
import { useBoardState } from "../domain/boardStore";
import { useAllFilteredCards } from "../filters/useFilters";
import { Card } from "../cards/Card";
import type { DragInfo } from "../domain/types";

interface GridViewProps {
  dragRef: React.MutableRefObject<DragInfo | null>;
  onCardClick?: (cardId: string) => void;
  onCardMove?: (from: string, to: string, cardId: string) => void;
}

export function GridView({ dragRef, onCardClick, onCardMove }: GridViewProps) {
  const state = useBoardState();
  const allCards = useAllFilteredCards();

  // Find column for each card
  const cardToColumn: Record<string, string> = {};
  for (const colId of state.normalized.columnOrder) {
    for (const cid of state.normalized.columnCards[colId] ?? []) {
      cardToColumn[cid] = colId;
    }
  }

  return (
    <div className="flex-1 overflow-y-auto px-2 pb-4">
      <div className="columns-1 sm:columns-2 lg:columns-3 xl:columns-4 gap-4">
        {allCards.map((card) => {
          const colId = cardToColumn[String(card.id)];
          if (!colId) return null;
          return (
            <div key={card.id} className="break-inside-avoid mb-4">
              <Card
                card={card}
                columnId={colId}
                compact={false}
                dragRef={dragRef}
                onCardClick={onCardClick}
                onCardMove={onCardMove}
              />
            </div>
          );
        })}
        {allCards.length === 0 && (
          <div className="col-span-full flex flex-col items-center justify-center py-24 text-muted-foreground opacity-40">
            <p className="text-sm font-medium">Nenhum card encontrado</p>
          </div>
        )}
      </div>
    </div>
  );
}
