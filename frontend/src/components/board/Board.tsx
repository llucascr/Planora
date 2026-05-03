import React, { useRef, useCallback } from "react";
import {
  BoardProvider,
  useBoardState,
  useBoardDispatch,
} from "./domain/boardStore";
import { KanbanView } from "./views/KanbanView";
import { ListView } from "./views/ListView";
import { GridView } from "./views/GridView";
import { AnalyticsView } from "./views/AnalyticsView";
import { CardModal } from "./modals/CardModal";
import { FilterBar } from "./filters/FilterBar";
import type { BoardColumn, DragInfo, ViewMode } from "./domain/types";
import {
  Kanban,
  List,
  SquaresFour,
  ChartBar,
  ArrowsCounterClockwise,
} from "@phosphor-icons/react";
import { classnames } from "./utils/classnames";

export const COLUMN_COLORS = [
  "#60a5fa", // blue
  "#f59e0b", // amber
  "#34d399", // emerald
  "#a78bfa", // violet
  "#f87171", // red
  "#38bdf8", // sky
  "#fb923c", // orange
];

const VIEWS: { mode: ViewMode; icon: React.ReactNode; label: string }[] = [
  { mode: "kanban", icon: <Kanban className="h-3.5 w-3.5" />, label: "Board" },
  { mode: "list", icon: <List className="h-3.5 w-3.5" />, label: "Lista" },
  {
    mode: "grid",
    icon: <SquaresFour className="h-3.5 w-3.5" />,
    label: "Grid",
  },
  {
    mode: "analytics",
    icon: <ChartBar className="h-3.5 w-3.5" />,
    label: "Analytics",
  },
];

function BoardInner({
  onCardMove,
  onColumnMove,
  refetch,
  onCreateColumn,
}: {
  onCardMove?: (from: string, to: string, cardId: string) => void;
  onColumnMove?: (fromIndex: number, toIndex: number, columnId: string) => void;
  refetch?: () => void;
  onCreateColumn?: () => void;
}) {
  const state = useBoardState();
  const dispatch = useBoardDispatch();
  const dragRef = useRef<DragInfo | null>(null);
  const { viewMode, selectedCardId, normalized } = state;

  const setView = useCallback(
    (m: ViewMode) => dispatch({ type: "SET_VIEW", payload: m }),
    [dispatch],
  );
  const handleCardClick = useCallback(
    (cardId: string) =>
      dispatch({ type: "SET_SELECTED_CARD", payload: cardId }),
    [dispatch],
  );
  const handleCloseModal = useCallback(
    () => dispatch({ type: "SET_SELECTED_CARD", payload: null }),
    [dispatch],
  );

  const selectedCard = selectedCardId ? normalized.cards[selectedCardId] : null;
  const selectedColumnId = selectedCardId
    ? Object.entries(normalized.columnCards).find(([, ids]) =>
      ids.includes(selectedCardId),
    )?.[0]
    : null;
  const selectedColumn = selectedColumnId
    ? normalized.columns[selectedColumnId]
    : null;

  function renderView() {
    switch (viewMode) {
      case "kanban":
        return (
          <KanbanView
            dragRef={dragRef}
            onCardClick={handleCardClick}
            onCardMove={onCardMove}
            onColumnMove={onColumnMove}
            onCreateColumn={onCreateColumn}
          />
        );
      case "list":
        return (
          <ListView
            dragRef={dragRef}
            onCardClick={handleCardClick}
            onCardMove={onCardMove}
          />
        );
      case "grid":
        return (
          <GridView
            dragRef={dragRef}
            onCardClick={handleCardClick}
            onCardMove={onCardMove}
          />
        );
      case "analytics":
        return <AnalyticsView />;
    }
  }

  return (
    <div className="flex h-full w-full flex-col overflow-x-auto">
      <div className="relative p-2 z-20 flex items-center gap-3 border-b border-border backdrop-blur shrink-0 px-4">
        <div className="flex-1 min-w-0">
          <FilterBar />
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            title="Atualizar dados"
            className={classnames(
              "flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-all group",
              "bg-secondary border border-border text-foreground hover:bg-accent-hover active:scale-95 shadow-sm",
            )}
            onClick={refetch}
          >
            <ArrowsCounterClockwise className="h-3.5 w-3.5 transition-transform group-hover:rotate-180 duration-500" />
            <span className="hidden lg:inline">Atualizar</span>
          </button>

          <div className="flex items-center gap-px rounded-lg bg-secondary border border-border p-0.5">
            {VIEWS.map(({ mode, icon, label }) => (
              <button
                key={mode}
                title={label}
                onClick={() => setView(mode)}
                className={classnames(
                  "flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-medium transition-all",
                  viewMode === mode
                    ? "bg-primary text-foreground shadow-sm"
                    : "text-foreground opacity-40 hover:opacity-70",
                )}
              >
                {icon}
                <span className="hidden lg:inline">{label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      <div
        className={classnames(
          "flex-1 min-h-0",
          viewMode === "kanban"
            ? "overflow-x-auto overflow-y-hidden"
            : "overflow-y-auto px-4 py-4",
        )}
      >
        {renderView()}
      </div>

      {selectedCard && (
        <CardModal
          card={selectedCard}
          columnName={selectedColumn?.name}
          onClose={handleCloseModal}
        />
      )}
    </div>
  );
}

interface BoardProps {
  columns: BoardColumn[];
  onCardMove?: (
    fromColumnId: string,
    toColumnId: string,
    cardId: string,
  ) => void;
  onColumnMove?: (fromIndex: number, toIndex: number, columnId: string) => void;
  className?: string;
  refetch?: () => void;
  onCreateColumn?: () => void;
}

export function Board({
  columns,
  onCardMove,
  onColumnMove,
  className,
  refetch,
  onCreateColumn,
}: BoardProps) {
  return (
    <BoardProvider initialColumns={columns}>
      <div className={classnames("h-full flex flex-col", className)}>
        <BoardInner
          onCardMove={onCardMove}
          onColumnMove={onColumnMove}
          refetch={refetch}
          onCreateColumn={onCreateColumn}
        />
      </div>
    </BoardProvider>
  );
}
