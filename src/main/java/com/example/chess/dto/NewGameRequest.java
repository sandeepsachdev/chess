package com.example.chess.dto;

/**
 * Request body for creating a game.
 *
 * @param color      side the human plays: "white" or "black" (default white)
 * @param difficulty AI strength: "easy", "medium" or "hard" (default medium)
 */
public record NewGameRequest(String color, String difficulty) {
}
