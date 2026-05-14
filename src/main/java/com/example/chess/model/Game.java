package com.example.chess.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single chess game: the live board, the side the human controls, the move
 * history and the bookkeeping needed for repetition draws.
 */
public class Game {

    private final String id;
    private final Board board;
    private final Color humanColor;
    private final int aiSearchDepth;
    private final List<String> moveHistory = new ArrayList<>();
    private final Map<String, Integer> positionCounts = new HashMap<>();
    private Move lastMove;

    public Game(String id, Color humanColor, int aiSearchDepth) {
        this.id = id;
        this.humanColor = humanColor;
        this.aiSearchDepth = aiSearchDepth;
        this.board = Board.initial();
        recordPosition();
    }

    public String getId() {
        return id;
    }

    public Board getBoard() {
        return board;
    }

    public Color getHumanColor() {
        return humanColor;
    }

    public Color getAiColor() {
        return humanColor.opposite();
    }

    public int getAiSearchDepth() {
        return aiSearchDepth;
    }

    public List<String> getMoveHistory() {
        return moveHistory;
    }

    public Move getLastMove() {
        return lastMove;
    }

    public void play(Move move) {
        board.applyMove(move);
        lastMove = move;
        moveHistory.add(move.toString());
        recordPosition();
    }

    private void recordPosition() {
        positionCounts.merge(board.positionKey(), 1, Integer::sum);
    }

    public GameStatus getStatus() {
        if (board.generateLegalMoves().isEmpty()) {
            return board.isInCheck(board.getSideToMove()) ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        }
        if (board.hasInsufficientMaterial()) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL;
        }
        if (board.getHalfmoveClock() >= 100) {
            return GameStatus.DRAW_FIFTY_MOVE;
        }
        if (positionCounts.getOrDefault(board.positionKey(), 0) >= 3) {
            return GameStatus.DRAW_REPETITION;
        }
        return GameStatus.IN_PROGRESS;
    }

    /** Winner of the game, or {@code null} for a draw or game still in progress. */
    public Color getWinner() {
        if (getStatus() == GameStatus.CHECKMATE) {
            // The side to move has been checkmated, so the other side won.
            return board.getSideToMove().opposite();
        }
        return null;
    }
}
