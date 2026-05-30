import React, { createContext, useContext, useReducer } from "react";
import type {
  BoardColumn,
  BoardState,
  BoardAction,
  NormalizedState,
  FilterState,
} from "./types";

export function normalizeBoard(columns: BoardColumn[]): NormalizedState {
  const normalized: NormalizedState = {
    columns: {},
    cards: {},
    columnOrder: [],
    columnCards: {},
  };

  const sorted = [...columns].sort((a, b) => a.order - b.order);

  for (const col of sorted) {
    const colId = String(col.id);
    normalized.columnOrder.push(colId);
    normalized.columns[colId] = {
      id: col.id,
      name: col.name,
      order: col.order,
      idBoard: col.idBoard,
    };
    normalized.columnCards[colId] = [];

    for (const card of col.cards ?? []) {
      const cardId = String(card.id);
      normalized.cards[cardId] = card;
      normalized.columnCards[colId].push(cardId);
    }
  }

  return normalized;
}

const initialFilterState: FilterState = {
  search: "",
  types: [],
  origens: [],
  sortField: "createdAt",
  sortDir: "desc",
};

const initialState: BoardState = {
  normalized: {
    columns: {},
    cards: {},
    columnOrder: [],
    columnCards: {},
  },
  viewMode: "kanban",
  filters: initialFilterState,
  collapsedColumns: new Set(),
  selectedCardId: null,
};

function boardReducer(state: BoardState, action: BoardAction): BoardState {
  switch (action.type) {
    case "LOAD_BOARD": {
      return {
        ...state,
        normalized: normalizeBoard(action.payload),
      };
    }

    case "MOVE_CARD": {
      const { cardId, fromColumnId, toColumnId, toIndex } = action.payload;
      const fromCards = [...(state.normalized.columnCards[fromColumnId] ?? [])];
      const toCards =
        fromColumnId === toColumnId
          ? fromCards
          : [...(state.normalized.columnCards[toColumnId] ?? [])];

      // Remove from source
      const srcIdx = fromCards.indexOf(cardId);
      if (srcIdx === -1) return state;
      fromCards.splice(srcIdx, 1);

      // Insert at destination
      const destCards = fromColumnId === toColumnId ? fromCards : toCards;
      const clampedIndex = Math.min(toIndex, destCards.length);
      destCards.splice(clampedIndex, 0, cardId);

      return {
        ...state,
        normalized: {
          ...state.normalized,
          columnCards: {
            ...state.normalized.columnCards,
            [fromColumnId]: fromCards,
            [toColumnId]: fromColumnId === toColumnId ? fromCards : destCards,
          },
        },
      };
    }

    case "REORDER_CARD": {
      const { columnId, fromIndex, toIndex } = action.payload;
      const cards = [...(state.normalized.columnCards[columnId] ?? [])];
      const [removed] = cards.splice(fromIndex, 1);
      cards.splice(toIndex, 0, removed);
      return {
        ...state,
        normalized: {
          ...state.normalized,
          columnCards: {
            ...state.normalized.columnCards,
            [columnId]: cards,
          },
        },
      };
    }

    case "REORDER_COLUMN": {
      const { fromIndex, toIndex } = action.payload;
      const order = [...state.normalized.columnOrder];
      const [moved] = order.splice(fromIndex, 1);
      order.splice(toIndex, 0, moved);
      return {
        ...state,
        normalized: {
          ...state.normalized,
          columnOrder: order,
        },
      };
    }

    case "SET_VIEW": {
      return { ...state, viewMode: action.payload };
    }

    case "SET_FILTERS": {
      return {
        ...state,
        filters: { ...state.filters, ...action.payload },
      };
    }

    case "TOGGLE_COLUMN": {
      const next = new Set(state.collapsedColumns);
      if (next.has(action.payload)) {
        next.delete(action.payload);
      } else {
        next.add(action.payload);
      }
      return { ...state, collapsedColumns: next };
    }

    case "SET_SELECTED_CARD": {
      return { ...state, selectedCardId: action.payload };
    }

    case "UPDATE_COLUMN": {
      const { columnId, name } = action.payload;

      return {
        ...state,
        normalized: {
          ...state.normalized,
          columns: {
            ...state.normalized.columns,
            [columnId]: {
              ...state.normalized.columns[columnId],
              name,
            },
          },
        },
      };
    }

    case "DELETE_CARD": {
      const { cardId, columnId } = action.payload;

      const newColumnCards = {
        ...state.normalized.columnCards,
        [columnId]: state.normalized.columnCards[columnId].filter(
          (id) => id !== cardId
        ),
      };

      const newCards = { ...state.normalized.cards };
      delete newCards[cardId];

      return {
        ...state,
        normalized: {
          ...state.normalized,
          cards: newCards,
          columnCards: newColumnCards,
        },
      };
    }

    case "DELETE_COLUMN": {
      const columnId = String(action.payload);

      const newColumns = { ...state.normalized.columns };
      const newColumnOrder = state.normalized.columnOrder.filter(
        (id) => id !== columnId
      );
      const newColumnCards = { ...state.normalized.columnCards };

      delete newColumns[columnId];
      delete newColumnCards[columnId];

      return {
        ...state,
        normalized: {
          ...state.normalized,
          columns: newColumns,
          columnOrder: newColumnOrder,
          columnCards: newColumnCards,
        },
      };
    }

    default:
      return state;
  }
}

type Dispatch = React.Dispatch<BoardAction>;

export const BoardStateContext = createContext<BoardState>(initialState);
export const BoardDispatchContext = createContext<Dispatch>(() => { });

interface BoardProviderProps {
  children: React.ReactNode;
  initialColumns?: BoardColumn[];
}

export function BoardProvider({
  children,
  initialColumns,
}: BoardProviderProps) {
  const [state, dispatch] = useReducer(boardReducer, initialState, (s) => {
    if (initialColumns && initialColumns.length > 0) {
      return { ...s, normalized: normalizeBoard(initialColumns) };
    }
    return s;
  });

  return (
    <BoardStateContext.Provider value={state}>
      <BoardDispatchContext.Provider value={dispatch}>
        {children}
      </BoardDispatchContext.Provider>
    </BoardStateContext.Provider>
  );
}

export function useBoardState() {
  return useContext(BoardStateContext);
}

export function useBoardDispatch() {
  return useContext(BoardDispatchContext);
}

export function useColumnCards(columnId: string): string[] {
  const { normalized } = useBoardState();
  return normalized.columnCards[columnId] ?? [];
}
