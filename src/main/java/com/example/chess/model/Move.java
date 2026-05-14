package com.example.chess.model;

/**
 * A move from one square to another. Squares are 0-based: row 0 is rank 1
 * (White's back rank) and col 0 is file a. {@code promotion} is non-null only
 * when a pawn reaches the last rank.
 */
public record Move(int fromRow, int fromCol, int toRow, int toCol, PieceType promotion) {

    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this(fromRow, fromCol, toRow, toCol, null);
    }

    public static Move fromAlgebraic(String from, String to, PieceType promotion) {
        return new Move(rank(from), file(from), rank(to), file(to), promotion);
    }

    private static int file(String square) {
        return square.charAt(0) - 'a';
    }

    private static int rank(String square) {
        return square.charAt(1) - '1';
    }

    public String fromSquare() {
        return square(fromRow, fromCol);
    }

    public String toSquare() {
        return square(toRow, toCol);
    }

    public static String square(int row, int col) {
        return "" + (char) ('a' + col) + (char) ('1' + row);
    }

    @Override
    public String toString() {
        return fromSquare() + toSquare() + (promotion != null ? Character.toLowerCase(promotion.symbol()) : "");
    }
}
