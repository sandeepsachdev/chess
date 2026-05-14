package com.example.chess.model;

public record Piece(PieceType type, Color color) {

    /** Two-character code such as "wP" or "bK", used by the API/UI. */
    public String code() {
        return (color == Color.WHITE ? "w" : "b") + type.symbol();
    }

    public static Piece fromCode(String code) {
        Color color = code.charAt(0) == 'w' ? Color.WHITE : Color.BLACK;
        return new Piece(PieceType.fromSymbol(code.charAt(1)), color);
    }
}
