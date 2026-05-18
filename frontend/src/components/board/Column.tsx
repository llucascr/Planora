import React, { useState, useRef, useEffect } from "react";
import { DotsSixVertical, Plus, DotsThreeVertical } from "@phosphor-icons/react";
import { Card } from "./cards/Card";
import { useBoardState, useBoardDispatch } from "./domain/boardStore";
import type { DragInfo } from "./domain/types";
import { COLUMN_COLORS } from "./Board";
import { classnames } from "./utils/classnames";
import { httpClient } from "api";
import { IssueForm } from "@/pages/Tarefas/IssueForm";
import { useUI } from "context";
import type { MemberBoard } from "types";

interface ColumnProps {
  columnId: string;
  columnIndex: number;
  dragRef: React.MutableRefObject<DragInfo | null>;
  filteredCardIds: string[];
  onCardClick?: (cardId: string) => void;
  compact?: boolean;
  // Column reorder props (passed from KanbanView)
  onColumnHeaderDragStart?: (e: React.DragEvent) => void;
  onColumnHeaderDragEnd?: () => void;
  isColumnDragging?: boolean;
  onCardMove?: (from: string, to: string, cardId: string) => void;
  members: MemberBoard[];
  refetch: () => void;
}

export function Column({
  columnId,
  columnIndex,
  dragRef,
  filteredCardIds,
  onCardClick,
  compact = false,
  onColumnHeaderDragStart,
  onColumnHeaderDragEnd,
  isColumnDragging = false,
  members = [],
  onCardMove,
  refetch,
}: ColumnProps) {
  const ui = useUI();
  const state = useBoardState();
  const dispatch = useBoardDispatch();
  const column = state.normalized.columns[columnId];
  const totalCount = state.normalized.columnCards[columnId]?.length ?? 0;
  const isCollapsed = state.collapsedColumns.has(columnId);
  const [isDragOver, setIsDragOver] = useState(false);
  const [dropIndex, setDropIndex] = useState<number | null>(null);
  const columnRef = useRef<HTMLDivElement>(null);
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(column.name);
  const [openMenu, setOpenMenu] = useState(false);
  const [repository, setRepository] = useState("");

  const accentColor = COLUMN_COLORS[columnIndex % COLUMN_COLORS.length];

  if (!column) return null;

  useEffect(() => {
    async function loadBoard() {
      try {

        const response: any = await httpClient.get(
          `/v1/kanban/board/${column.idBoard}`
        );

        setRepository(response.name);

      } catch (err) {
        console.error("Erro ao buscar board:", err);
      }
    }

    loadBoard();
  }, [column.idBoard]);

  function handleToggle() {
    dispatch({ type: "TOGGLE_COLUMN", payload: columnId });
  }

  // ── Card drag-over / drop ───────────────────────────────
  function handleDragOver(e: React.DragEvent) {
    // Ignore if this is a column drag
    if (e.dataTransfer.types.includes("application/x-column-drag")) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setIsDragOver(true);

    const cards = columnRef.current?.querySelectorAll("[data-card-id]");
    if (!cards) return;
    let idx = filteredCardIds.length;
    for (let i = 0; i < cards.length; i++) {
      const rect = cards[i].getBoundingClientRect();
      if (e.clientY < rect.top + rect.height / 2) {
        idx = i;
        break;
      }
    }
    setDropIndex(idx);
  }

  function handleDragLeave(e: React.DragEvent) {
    if (!columnRef.current?.contains(e.relatedTarget as Node)) {
      setIsDragOver(false);
      setDropIndex(null);
    }
  }

  async function handleDrop(e: React.DragEvent) {
    // Ignore column drags — handled by KanbanView
    if (e.dataTransfer.types.includes("application/x-column-drag")) return;
    e.preventDefault();
    setIsDragOver(false);
    setDropIndex(null);
    if (!dragRef.current) return;
    const { cardId, sourceColumnId } = dragRef.current;

    if (sourceColumnId === columnId) {
      dragRef.current = null;
      return;
    }

    dispatch({
      type: "MOVE_CARD",
      payload: {
        cardId,
        fromColumnId: sourceColumnId,
        toColumnId: columnId,
        toIndex: dropIndex ?? filteredCardIds.length,
      },
    });

    try {
      await httpClient.patch(
        `/v1/kanban/board/${column.idBoard}/issue/move`,
        {
          issueId: Number(cardId),
          targetColumnId: Number(columnId),
        }
      );

      onCardMove?.(sourceColumnId, columnId, cardId);
    } catch (err) {
      console.error("Erro ao mover issue:", err);

      dispatch({
        type: "MOVE_CARD",
        payload: {
          cardId,
          fromColumnId: columnId,
          toColumnId: sourceColumnId,
          toIndex: 0,
        },
      });
    }
    dragRef.current = null;
  }

  async function handleSave() {
    setEditing(false);

    if (name === column.name) return;

    dispatch({
      type: "UPDATE_COLUMN",
      payload: {
        columnId,
        name,
      },
    });

    try {
      await httpClient.put(
        `/v1/kanban/board/${column.idBoard}/column/${column.id}`,
        {
          name,
          position: column.order,
        }
      );
    } catch (err) {
      dispatch({
        type: "UPDATE_COLUMN",
        payload: {
          columnId,
          name: column.name,
        },
      });
    }
  }

  async function handleDelete() {
    try {
      await httpClient.delete(
        `/v1/kanban/board/${column.idBoard}/column/${column.id}`
      );

      dispatch({
        type: "DELETE_COLUMN",
        payload: String(columnId),
      });

      setOpenMenu(false);
    } catch (err) {
      console.error("erro:", err);
    }
  }

  function handleOpenIssueModal() {
    ui.show({
      id: "issue-form-create",
      type: "modal",
      options: {
        titulo: "Nova Issue",
      },
      content: (
        <IssueForm
          action="create"
          boardId={column.idBoard}
          columnId={column.id}
          repository={repository}
          refetch={refetch}
          members={members}
          onClose={() =>
            ui.hide("modal", "issue-form-create")
          }
        />
      ),
    });
  }

  return (
    <div
      className={classnames(
        "flex flex-col rounded-xl transition-all duration-150 shrink-0 h-full",
        "bg-accent border border-border",
        "w-[400px]",
        isDragOver && "ring-1 ring-primary ring-opacity-60",
        isColumnDragging && "opacity-40",
      )}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <div className="flex items-center gap-1.5 px-2 py-2.5 select-none shrink-0">
        <div
          draggable
          onDragStart={onColumnHeaderDragStart}
          onDragEnd={onColumnHeaderDragEnd}
          className="flex h-5 w-4 cursor-grab active:cursor-grabbing items-center justify-center rounded text-primary"
          title="Arrastar para reordenar coluna"
        >
          <DotsSixVertical className="h-4 w-4 text-foreground" />
        </div>

        <span
          className="h-2.5 w-2.5 rounded-full shrink-0 ring-2 ring-border ring-opacity-20"
          style={{ backgroundColor: accentColor }}
        />

        {editing ? (
          <input
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            onBlur={handleSave}
            onKeyDown={(e) => {
              if (e.key === "Enter") handleSave();
              if (e.key === "Escape") {
                setName(column.name);
                setEditing(false);
              }
            }}
            className="flex-1 bg-transparent text-[13px] font-semibold outline-none"
          />
        ) : (
          <button
            className="flex-1 text-left text-[13px] font-semibold text-foreground opacity-90 hover:opacity-100 truncate"
            onDoubleClick={() => setEditing(true)}
            onClick={handleToggle}
          >
            {column.name}
          </button>
        )}

        <span className="text-[13px] font-bold text-foreground tabular-nums shrink-0">
          {totalCount}
        </span>

        <div className="relative">
          <button
            onClick={() => setOpenMenu((v) => !v)}
            className="p-1 rounded hover:bg-accent-hover"
          >
            <DotsThreeVertical size={16} />
          </button>

          {openMenu && (
            <div className="absolute right-0 mt-2 w-36 bg-background border border-border rounded-md shadow-lg z-50">
              <button
                className="w-full text-left px-3 py-2 text-sm hover:bg-accent-hover text-red-500"
                onMouseDown={handleDelete}
              >
                Excluir coluna
              </button>
            </div>
          )}
        </div>
      </div>

      <div
        className="h-px mx-3 mb-1 rounded-full opacity-30 shrink-0"
        style={{ backgroundColor: accentColor }}
      />
      {!isCollapsed && (
        <div
          ref={columnRef}
          className="flex flex-col gap-2 px-2 pt-1 flex-1 min-h-0 overflow-y-auto overflow-x-visible relative"
        >
          {filteredCardIds.length === 0 && !isDragOver && (
            <div className="flex items-center justify-center py-6 text-[11px] text-foreground opacity-20 italic">
              Nenhum card
            </div>
          )}

          {filteredCardIds.map((cardId, index) => {
            const card = state.normalized.cards[cardId];
            if (!card) return null;
            return (
              <React.Fragment key={cardId}>
                {isDragOver && dropIndex === index && (
                  <div
                    className="h-1 rounded-full mx-1 animate-pulse"
                    style={{ backgroundColor: accentColor }}
                  />
                )}
                <Card
                  card={card}
                  columnId={columnId}
                  columnIndex={columnIndex}
                  compact={compact}
                  dragRef={dragRef}
                  onCardClick={onCardClick}
                  refetch={refetch}
                  boardId={column.idBoard}
                  repository={repository}
                  members={members}
                />
              </React.Fragment>
            );
          })}

          {isDragOver && dropIndex === filteredCardIds.length && (
            <div
              className="h-1 rounded-full mx-1 animate-pulse"
              style={{ backgroundColor: accentColor }}
            />
          )}

          {isDragOver && filteredCardIds.length === 0 && (
            <div
              className="flex h-16 items-center justify-center rounded-lg border border-dashed text-xs opacity-50"
              style={{ borderColor: accentColor, color: accentColor }}
            >
              Soltar aqui
            </div>
          )}

          <div className="h-2 shrink-0" />
        </div>
      )}

      <div className="px-2 pb-2 pt-1 shrink-0">
        <button onClick={handleOpenIssueModal} className="flex w-full items-center gap-1.5 rounded-lg p-2 text-[11px] text-foreground hover:bg-accent-hover transition-colors">
          <Plus className="h-3 w-3 shrink-0" />
          Adicionar card
        </button>
      </div>
    </div>
  );
}
