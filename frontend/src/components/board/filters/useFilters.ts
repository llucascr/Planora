import { useMemo } from 'react';
import { useBoardState } from '../domain/boardStore';
import { detectCardType } from '../domain/cardDetector';
import type { Card } from '../domain/types';
import moment from 'moment';

export function useFilteredColumnCards(columnId: string): string[] {
    const state = useBoardState();
    const { filters, normalized } = state;
    const allCardIds = normalized.columnCards[columnId] ?? [];

    return useMemo(() => {
        let ids = [...allCardIds];

        if (filters.search.trim()) {
            const q = filters.search.trim().toLowerCase();
            ids = ids.filter((cid) => {
                const card = normalized.cards[cid];
                if (!card) return false;
                const searchable = [
                    card.nome,
                    card.descricao,
                    card.lead?.nome,
                    card.lead?.razaoSocial,
                    card.lead?.origem,
                    moment(`${card.planoAcao?.ano}-${card.planoAcao?.mes}-01`).format('YYYY-MM'),
                    ...(card.lead?.contatos?.map((c) => c.valor) ?? []),
                ]
                    .filter(Boolean)
                    .join(' ')
                    .toLowerCase();
                return searchable.includes(q);
            });
        }

        // Filter by type
        if (filters.types.length > 0) {
            ids = ids.filter((cid) => {
                const card = normalized.cards[cid];
                return card ? filters.types.includes(detectCardType(card)) : false;
            });
        }

        // Filter by origem
        if (filters.origens.length > 0) {
            ids = ids.filter((cid) => {
                const card = normalized.cards[cid];
                if (!card?.lead?.origem) return false;
                return filters.origens.includes(card.lead.origem.toLowerCase());
            });
        }

        // Sort
        ids.sort((a, b) => {
            const ca = normalized.cards[a];
            const cb = normalized.cards[b];
            if (!ca || !cb) return 0;

            let valA: string | number = '';
            let valB: string | number = '';

            if (filters.sortField === 'createdAt') {
                valA = ca.createdAt ?? '';
                valB = cb.createdAt ?? '';
            } else if (filters.sortField === 'nome') {
                valA = (ca.lead?.nome ?? ca.nome ?? '').toLowerCase();
                valB = (cb.lead?.nome ?? cb.nome ?? '').toLowerCase();
            } else if (filters.sortField === 'tipo') {
                valA = detectCardType(ca);
                valB = detectCardType(cb);
            }

            if (valA < valB) return filters.sortDir === 'asc' ? -1 : 1;
            if (valA > valB) return filters.sortDir === 'asc' ? 1 : -1;
            return 0;
        });

        return ids;
    }, [allCardIds, filters, normalized.cards]);
}

/** Returns all cards across all columns, filtered */
export function useAllFilteredCards(): Card[] {
    const state = useBoardState();
    const { normalized, filters } = state;

    return useMemo(() => {
        return Object.values(normalized.cards).filter((card) => {
            if (filters.search.trim()) {
                const q = filters.search.trim().toLowerCase();
                const searchable = [card.nome, card.descricao, card.lead?.nome, card.lead?.razaoSocial]
                    .filter(Boolean).join(' ').toLowerCase();
                if (!searchable.includes(q)) return false;
            }
            if (filters.types.length > 0 && !filters.types.includes(detectCardType(card))) return false;
            if (filters.origens.length > 0 && !filters.origens.includes(card.lead?.origem?.toLowerCase() ?? '')) return false;
            return true;
        });
    }, [normalized.cards, filters]);
}
