package com.example.chess.model;

public enum GameStatus {
    IN_PROGRESS,
    CHECKMATE,
    STALEMATE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_FIFTY_MOVE,
    DRAW_REPETITION;

    public boolean isGameOver() {
        return this != IN_PROGRESS;
    }
}
