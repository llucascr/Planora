import React, { useRef, useState } from "react";
import { Column } from "../Column";
import { useBoardState, useBoardDispatch } from "../domain/boardStore";
import { useFilteredColumnCards } from "../filters/useFilters";
import type { DragInfo, ColumnDragInfo } from "../domain/types";
import { COLUMN_COLORS } from "../Board";
import { classnames } from "../utils/classnames";

interface KanbanViewProps {
  dragRef: React.MutableRefObject<DragInfo | null>;
  onCardClick?: (cardId: string) => void;
  onCardMove?: (from: string, to: string, cardId: string) => void;
  onColumnMove?: (fromIndex: number, toIndex: number, columnId: string) => void;
  onCreateColumn?: () => void;
  refetch: () => void;
}

function KanbanColumn({
  columnId,
  columnIndex,
  dragRef,
  onCardClick,
  // Column reorder props
  // columnDragRef: colDragRef,
  draggingColIdx,
  dropTargetIdx,
  onColumnDragStart,
  onColumnDragOver,
  onColumnDrop,
  onColumnDragEnd,
  onCardMove,
  refetch,
}: {
  columnId: string;
  columnIndex: number;
  dragRef: React.MutableRefObject<DragInfo | null>;
  onCardClick?: (id: string) => void;
  columnDragRef: React.MutableRefObject<ColumnDragInfo | null>;
  draggingColIdx: number | null;
  dropTargetIdx: number | null;
  onColumnDragStart: (e: React.DragEvent, colId: string, idx: number) => void;
  onColumnDragOver: (e: React.DragEvent, idx: number) => void;
  onColumnDrop: (e: React.DragEvent, toIdx: number) => void;
  onColumnDragEnd: () => void;
  onCardMove?: (from: string, to: string, cardId: string) => void;
  refetch: () => void;
}) {
  const filteredCardIds = useFilteredColumnCards(columnId);
  const isDragging = draggingColIdx === columnIndex;
  const isDropTarget = dropTargetIdx === columnIndex;

  return (
    <div
      className={classnames(
        "relative shrink-0 h-full transition-all duration-150",
        isDragging && "opacity-40 scale-[0.98]",
      )}
      onDragOver={(e) => onColumnDragOver(e, columnIndex)}
      onDrop={(e) => onColumnDrop(e, columnIndex)}
    >
      {/* Drop indicator — left side */}
      {isDropTarget &&
        draggingColIdx !== null &&
        draggingColIdx !== columnIndex - 1 && (
          <div
            className="absolute -left-2 top-0 bottom-0 w-1 rounded-full z-30 animate-pulse"
            style={{
              backgroundColor:
                COLUMN_COLORS[columnIndex % COLUMN_COLORS.length],
            }}
          />
        )}

      <Column
        columnId={columnId}
        columnIndex={columnIndex}
        dragRef={dragRef}
        filteredCardIds={filteredCardIds}
        onCardClick={onCardClick}
        onCardMove={onCardMove}
        onColumnHeaderDragStart={(e) =>
          onColumnDragStart(e, columnId, columnIndex)
        }
        onColumnHeaderDragEnd={onColumnDragEnd}
        isColumnDragging={isDragging}
        refetch={refetch}
      />
    </div>
  );
}

export function KanbanView({
  dragRef,
  onCardClick,
  onCardMove,
  onColumnMove,
  onCreateColumn,
  refetch,
}: KanbanViewProps) {
  const state = useBoardState();
  const dispatch = useBoardDispatch();
  const { columnOrder } = state.normalized;

  // Column drag state
  const columnDragRef = useRef<ColumnDragInfo | null>(null);
  const [draggingColIdx, setDraggingColIdx] = useState<number | null>(null);
  const [dropTargetIdx, setDropTargetIdx] = useState<number | null>(null);

  function handleColumnDragStart(
    e: React.DragEvent,
    colId: string,
    idx: number,
  ) {
    // Signal this is a column drag (not a card drag)
    e.dataTransfer.setData("application/x-column-drag", colId);
    e.dataTransfer.effectAllowed = "move";
    columnDragRef.current = { columnId: colId, fromIndex: idx };
    setDraggingColIdx(idx);
  }

  function handleColumnDragOver(e: React.DragEvent, toIdx: number) {
    if (!e.dataTransfer.types.includes("application/x-column-drag")) return;
    e.preventDefault();
    e.stopPropagation();
    e.dataTransfer.dropEffect = "move";
    setDropTargetIdx(toIdx);
  }

  function handleColumnDrop(e: React.DragEvent, toIdx: number) {
    if (!e.dataTransfer.types.includes("application/x-column-drag")) return;
    e.preventDefault();
    e.stopPropagation();
    if (!columnDragRef.current) return;
    const { fromIndex } = columnDragRef.current;
    if (fromIndex !== toIdx) {
      const colId = columnDragRef.current.columnId;
      dispatch({
        type: "REORDER_COLUMN",
        payload: { fromIndex, toIndex: toIdx },
      });
      onColumnMove?.(fromIndex, toIdx, colId);
    }
    columnDragRef.current = null;
    setDraggingColIdx(null);
    setDropTargetIdx(null);
  }

  function handleColumnDragEnd() {
    columnDragRef.current = null;
    setDraggingColIdx(null);
    setDropTargetIdx(null);
  }

  return (
    <div className="flex h-full items-stretch gap-4 overflow-x-auto overflow-y-hidden px-4 pt-4 pb-2">
      {columnOrder.map((colId, index) => (
        <KanbanColumn
          key={colId}
          columnId={colId}
          columnIndex={index}
          dragRef={dragRef}
          onCardClick={onCardClick}
          columnDragRef={columnDragRef}
          draggingColIdx={draggingColIdx}
          dropTargetIdx={dropTargetIdx}
          onColumnDragStart={handleColumnDragStart}
          onColumnDragOver={handleColumnDragOver}
          onColumnDrop={handleColumnDrop}
          onColumnDragEnd={handleColumnDragEnd}
          onCardMove={onCardMove}
          refetch={refetch}
        />
      ))}

      {/* Add column placeholder */}
      <div className="shrink-0 w-[272px] flex items-start pt-0">
        <button
          className="flex w-full items-center gap-2 rounded-xl border border-dashed border-border px-4 py-3 text-xs text-muted-foreground hover:border-border hover:text-foreground hover:bg-accent/50 transition-all self-start"
          onClick={onCreateColumn}
        >
          <span className="text-lg font-light leading-none">+</span>
          Nova coluna
        </button>
      </div>
    </div>
  );
}
