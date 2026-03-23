export { Board } from "./Board";
export {
  BoardProvider,
  useBoardState,
  useBoardDispatch,
} from "./domain/boardStore";
export type { BoardColumn, Card, CardType, ViewMode } from "./domain/types";
export { detectCardType, getCardTypeLabel } from "./domain/cardDetector";
