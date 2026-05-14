package com.example.chess.dto;

import com.example.chess.model.Move;

/** A move expressed with algebraic squares, suitable for JSON. */
public record MoveDto(String from, String to, String promotion) {

    public static MoveDto of(Move move) {
        if (move == null) {
            return null;
        }
        String promotion = move.promotion() == null
                ? null
                : String.valueOf(Character.toLowerCase(move.promotion().symbol()));
        return new MoveDto(move.fromSquare(), move.toSquare(), promotion);
    }
}
