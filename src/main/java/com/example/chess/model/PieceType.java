package com.example.chess.model;

public enum PieceType {
    PAWN('P'),
    KNIGHT('N'),
    BISHOP('B'),
    ROOK('R'),
    QUEEN('Q'),
    KING('K');

    private final char symbol;

    PieceType(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public static PieceType fromSymbol(char c) {
        for (PieceType type : values()) {
            if (type.symbol == Character.toUpperCase(c)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown piece symbol: " + c);
    }
}
