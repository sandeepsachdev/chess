package com.example.chess.engine;

import com.example.chess.model.Board;
import com.example.chess.model.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessAITest {

    private final ChessAI ai = new ChessAI();

    @Test
    void choosesALegalMoveFromTheStartingPosition() {
        Board board = Board.initial();
        Move move = ai.chooseMove(board, 3);

        assertNotNull(move);
        assertTrue(board.generateLegalMoves().stream().anyMatch(m ->
                        m.fromRow() == move.fromRow() && m.fromCol() == move.fromCol()
                                && m.toRow() == move.toRow() && m.toCol() == move.toCol()),
                "AI move must be legal");
    }

    @Test
    void capturesAHangingQueen() {
        // White queen on d5 is attacked by a Black pawn on e6; Black to move
        // should grab it. 1. e4 e6 2. Qf3?? ... 3. Qd5 — e6 pawn can take on d5.
        Board board = Board.initial();
        board.applyMove(Move.fromAlgebraic("e2", "e4", null));
        board.applyMove(Move.fromAlgebraic("e7", "e6", null));
        board.applyMove(Move.fromAlgebraic("d1", "f3", null));
        board.applyMove(Move.fromAlgebraic("a7", "a6", null));
        board.applyMove(Move.fromAlgebraic("f3", "d5", null));

        Move move = ai.chooseMove(board, 3);
        assertNotNull(move);
        assertTrue(move.toSquare().equals("d5"), "AI should capture the hanging queen on d5");
    }
}
