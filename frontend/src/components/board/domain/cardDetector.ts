import type { Card, CardType } from './types';

/**
 * detectCardType
 * 
 * Priority order:
 * 1. Has lead → 'lead'
 * 2. Has planoMensal with planos that have clientes → 'planoAcao'
 * 3. Has planoMensal → 'diagnostico'
 * 4. Fallback → 'generic'
 */
export function detectCardType(_: Card): CardType {
    return 'issues';
}
/**
 * getCardTypeLabel
 * Human-readable label for display in UI
 */
export function getCardTypeLabel(type: CardType): string {
    const labels: Record<CardType, string> = {
        issues: 'Issue',
    };
    return labels[type];
}

/**
 * getCardTypeColor
 * Returns tailwind color class prefix per type
 */
export function getCardTypeColor(type: CardType): string {
    const colors: Record<CardType, string> = {
        issues: 'slate',
    };
    return colors[type];
}

/**
 * getOrigemIcon
 * Maps an origem string to an emoji icon
 */
export function getOrigemIcon(origem: string): string {
    const map: Record<string, string> = {
        whatsapp: '💬',
        email: '📧',
        telefone: '📞',
        indicacao: '🤝',
        linkedin: '💼',
        instagram: '📸',
        site: '🌐',
    };
    return map[origem?.toLowerCase()] ?? '📋';
}

/**
 * getOrigemColor
 * Returns a CSS color class for origem badges
 */
export function getOrigemBadgeClass(origem: string): string {
    const map: Record<string, string> = {
        whatsapp: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
        email: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
        telefone: 'bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300',
        indicacao: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
        linkedin: 'bg-sky-100 text-sky-800 dark:bg-sky-900/30 dark:text-sky-400',
        instagram: 'bg-pink-100 text-pink-800 dark:bg-pink-900/30 dark:text-pink-400',
        site: 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400',
    };
    return map[origem?.toLowerCase()] ?? 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300';
}
