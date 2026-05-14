package com.example.chess.dto;

import com.example.chess.model.Board;
import com.example.chess.model.Color;
import com.example.chess.model.Game;
import com.example.chess.model.GameStatus;
import com.example.chess.model.Move;
import com.example.chess.model.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Full snapshot of a game sent to the browser.
 *
 * <p>{@code board[row][col]} uses row 0 = rank 1 and col 0 = file a; each cell
 * is a piece code such as "wP" / "bK", or {@code null} for an empty square.
 */
public record GameStateDto(
        String gameId,
        String[][] board,
        String turn,
        String humanColor,
        String aiColor,
        String status,
        String winner,
        boolean inCheck,
        String checkSquare,
        MoveDto lastMove,
        MoveDto aiMove,
        List<String> moveHistory,
        List<MoveDto> legalMoves
) {

    public static GameStateDto from(Game game, Move aiMove) {
        Board board = game.getBoard();
        String[][] squares = new String[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.pieceAt(row, col);
                squares[row][col] = piece == null ? null : piece.code();
            }
        }

        GameStatus status = game.getStatus();
        Color sideToMove = board.getSideToMove();
        boolean inCheck = board.isInCheck(sideToMove);
        String checkSquare = inCheck ? board.kingSquare(sideToMove) : null;

        List<MoveDto> legalMoves = new ArrayList<>();
        if (!status.isGameOver()) {
            for (Move move : board.generateLegalMoves()) {
                legalMoves.add(MoveDto.of(move));
            }
        }

        Color winner = game.getWinner();

        return new GameStateDto(
                game.getId(),
                squares,
                sideToMove.name(),
                game.getHumanColor().name(),
                game.getAiColor().name(),
                status.name(),
                winner == null ? null : winner.name(),
                inCheck,
                checkSquare,
                MoveDto.of(game.getLastMove()),
                MoveDto.of(aiMove),
                List.copyOf(game.getMoveHistory()),
                legalMoves
        );
    }
}
