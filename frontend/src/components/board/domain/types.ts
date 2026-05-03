export interface Contato {
    codigo: string;
    valor: string;
    principal: boolean;
}

export interface Lead {
    id: number;
    nome: string;
    razaoSocial: string;
    documento: string;
    origem: string;
    contatos: Contato[];
}

export interface Cliente {
    idCliente: number;
    codigo: number;
    nome: string;
    razaoSocial: string;
}

export interface PlanoAcao {
    id: number;
    ano: number;
    mes: number;
    label?: string;
    descricao?: string;
    observacao: string; // 'GARGALO' | 'OPORTUNIDADE' | metric text
    clientes: Cliente[];
}

export interface PlanoMensal {
    id: number;
    ano: number;
    mes: number;
    resumo: string;
    planos: PlanoAcao[];
}

export interface Card {
    id: number;
    nome: string;
    descricao: string;
    codigo: number | null;
    createdAt: string;
    lead: Lead | null;
    planoAcao: PlanoAcao | null;
}

export type CardType = 'lead' | 'planoAcao' | 'generic';

export interface BoardColumn {
    id: number;
    name: string;
    order: number;
    idBoard: number;
    cards: Card[];
}

export interface NormalizedState {
    columns: Record<string, Omit<BoardColumn, 'cards'>>;
    cards: Record<string, Card>;
    columnOrder: string[];
    columnCards: Record<string, string[]>;
}

export type ViewMode = 'kanban' | 'list' | 'grid' | 'timeline' | 'analytics';

export type SortField = 'createdAt' | 'nome' | 'tipo';
export type SortDir = 'asc' | 'desc';

export interface FilterState {
    search: string;
    types: CardType[];
    origens: string[];
    sortField: SortField;
    sortDir: SortDir;
}

export type BoardAction =
    | { type: 'LOAD_BOARD'; payload: BoardColumn[] }
    | { type: 'MOVE_CARD'; payload: { cardId: string; fromColumnId: string; toColumnId: string; toIndex: number } }
    | { type: 'REORDER_CARD'; payload: { columnId: string; fromIndex: number; toIndex: number } }
    | { type: 'REORDER_COLUMN'; payload: { fromIndex: number; toIndex: number } }
    | { type: 'SET_VIEW'; payload: ViewMode }
    | { type: 'SET_FILTERS'; payload: Partial<FilterState> }
    | { type: 'TOGGLE_COLUMN'; payload: string }
    | { type: 'SET_SELECTED_CARD'; payload: string | null }
    | { type: "UPDATE_COLUMN"; payload: { columnId: string; name: string } }
    | { type: "DELETE_COLUMN"; payload: string };

export interface ColumnDragInfo {
    columnId: string;
    fromIndex: number;
}


export interface BoardState {
    normalized: NormalizedState;
    viewMode: ViewMode;
    filters: FilterState;
    collapsedColumns: Set<string>;
    selectedCardId: string | null;
}

export interface DragInfo {
    cardId: string;
    sourceColumnId: string;
}
