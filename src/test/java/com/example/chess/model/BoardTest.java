package com.example.chess.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Correctness tests for the rules engine. The perft counts are the well-known
 * reference values for the standard starting position and exercise move
 * generation, legality filtering, castling, en passant and promotion together.
 */
class BoardTest {

    private static long perft(Board board, int depth) {
        if (depth == 0) {
            return 1;
        }
        long nodes = 0;
        for (Move move : board.generateLegalMoves()) {
            Board next = board.copy();
            next.applyMove(move);
            nodes += perft(next, depth - 1);
        }
        return nodes;
    }

    @Test
    void perftFromStartingPosition() {
        Board board = Board.initial();
        assertEquals(20, perft(board, 1));
        assertEquals(400, perft(board, 2));
        assertEquals(8902, perft(board, 3));
        assertEquals(197281, perft(board, 4));
    }

    private static Move move(String from, String to) {
        return Move.fromAlgebraic(from, to, null);
    }

    private static Board playSequence(String... moves) {
        Board board = Board.initial();
        for (String m : moves) {
            board.applyMove(move(m.substring(0, 2), m.substring(2, 4)));
        }
        return board;
    }

    private static boolean containsMove(List<Move> moves, String from, String to) {
        int fr = from.charAt(1) - '1', fc = from.charAt(0) - 'a';
        int tr = to.charAt(1) - '1', tc = to.charAt(0) - 'a';
        return moves.stream().anyMatch(m ->
                m.fromRow() == fr && m.fromCol() == fc && m.toRow() == tr && m.toCol() == tc);
    }

    @Test
    void kingsideCastlingIsLegalAndMovesTheRook() {
        // Clear f1/g1 and the king-side knight so White can castle.
        Board board = playSequence("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6");

        assertTrue(containsMove(board.generateLegalMoves(), "e1", "g1"), "O-O should be legal");

        board.applyMove(move("e1", "g1"));
        assertEquals(new Piece(PieceType.KING, Color.WHITE), board.pieceAt(0, 6));
        assertEquals(new Piece(PieceType.ROOK, Color.WHITE), board.pieceAt(0, 5));
        assertNull(board.pieceAt(0, 7));
        assertNull(board.pieceAt(0, 4));
    }

    @Test
    void enPassantCaptureRemovesThePassedPawn() {
        // 1. e4 a6 2. e5 d5 — White can now play exd6 en passant.
        Board board = playSequence("e2e4", "a7a6", "e4e5", "d7d5");

        assertTrue(containsMove(board.generateLegalMoves(), "e5", "d6"), "en passant should be legal");

        board.applyMove(move("e5", "d6"));
        assertEquals(new Piece(PieceType.PAWN, Color.WHITE), board.pieceAt(5, 3)); // pawn on d6
        assertNull(board.pieceAt(4, 3)); // captured black pawn (was on d5) is gone
    }

    @Test
    void promotionGeneratesAllFourPiecesAndPromotes() {
        // March a White pawn to c7, then capture-promote on b8.
        Board board = playSequence("e2e4", "d7d5", "e4d5", "g8f6", "d5d6", "f6g8", "d6c7", "g8f6");

        long promotionMoves = board.generateLegalMoves().stream()
                .filter(m -> m.fromRow() == 6 && m.fromCol() == 2 && m.toRow() == 7 && m.toCol() == 1)
                .count();
        assertEquals(4, promotionMoves, "should offer queen/rook/bishop/knight");

        board.applyMove(new Move(6, 2, 7, 1, PieceType.QUEEN));
        assertEquals(new Piece(PieceType.QUEEN, Color.WHITE), board.pieceAt(7, 1));
    }

    @Test
    void foolsMateIsDetectedAsCheckmate() {
        // 1. f3 e5 2. g4 Qh4#
        Board board = playSequence("f2f3", "e7e5", "g2g4", "d8h4");

        assertTrue(board.isInCheck(Color.WHITE));
        assertTrue(board.generateLegalMoves().isEmpty(), "White should have no legal moves");
    }

    @Test
    void initialPositionIsNotCheckOrStalemate() {
        Board board = Board.initial();
        assertFalse(board.isInCheck(Color.WHITE));
        assertFalse(board.isInCheck(Color.BLACK));
        assertEquals(20, board.generateLegalMoves().size());
    }
}
