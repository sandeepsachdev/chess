package com.example.chess.dto;

/**
 * Request body for a move.
 *
 * @param from      origin square in algebraic notation, e.g. "e2"
 * @param to        destination square, e.g. "e4"
 * @param promotion promotion piece when a pawn reaches the last rank:
 *                  "q", "r", "b" or "n" (optional, defaults to queen)
 */
public record MoveRequest(String from, String to, String promotion) {
}
