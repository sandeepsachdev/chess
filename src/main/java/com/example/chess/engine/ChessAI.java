package com.example.chess.engine;

import com.example.chess.model.Board;
import com.example.chess.model.Color;
import com.example.chess.model.Move;
import com.example.chess.model.Piece;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Negamax search with alpha-beta pruning and MVV-LVA move ordering. The search
 * depth is supplied per call so the service can map a difficulty level onto it.
 */
@Component
public class ChessAI {

    private static final int INFINITY = 1_000_000_000;
    private static final int MATE = 1_000_000;

    /** Picks the best move for the side to move, searching to {@code depth} plies. */
    public Move chooseMove(Board board, int depth) {
        List<Move> moves = board.generateLegalMoves();
        if (moves.isEmpty()) {
            return null;
        }
        orderMoves(board, moves);

        int alpha = -INFINITY;
        int beta = INFINITY;
        int bestScore = -INFINITY;
        List<Move> bestMoves = new ArrayList<>();

        for (Move move : moves) {
            Board next = board.copy();
            next.applyMove(move);
            int score = -negamax(next, depth - 1, -beta, -alpha, 1);
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
            if (bestScore > alpha) {
                alpha = bestScore;
            }
        }
        // Random tie-break between equally good moves keeps games varied.
        return bestMoves.get((int) (Math.random() * bestMoves.size()));
    }

    private int negamax(Board board, int depth, int alpha, int beta, int ply) {
        List<Move> moves = board.generateLegalMoves();
        if (moves.isEmpty()) {
            if (board.isInCheck(board.getSideToMove())) {
                return -(MATE - ply); // prefer mating sooner / being mated later
            }
            return 0; // stalemate
        }
        if (board.hasInsufficientMaterial() || board.getHalfmoveClock() >= 100) {
            return 0;
        }
        if (depth <= 0) {
            return evaluateForSideToMove(board);
        }

        orderMoves(board, moves);
        int best = -INFINITY;
        for (Move move : moves) {
            Board next = board.copy();
            next.applyMove(move);
            int score = -negamax(next, depth - 1, -beta, -alpha, ply + 1);
            if (score > best) {
                best = score;
            }
            if (best > alpha) {
                alpha = best;
            }
            if (alpha >= beta) {
                break; // beta cut-off
            }
        }
        return best;
    }

    private int evaluateForSideToMove(Board board) {
        int whiteScore = Evaluator.evaluate(board);
        return board.getSideToMove() == Color.WHITE ? whiteScore : -whiteScore;
    }

    /** Orders captures and promotions first to make alpha-beta pruning effective. */
    private void orderMoves(Board board, List<Move> moves) {
        moves.sort((a, b) -> Integer.compare(moveScore(board, b), moveScore(board, a)));
    }

    private int moveScore(Board board, Move move) {
        int score = 0;
        Piece victim = board.pieceAt(move.toRow(), move.toCol());
        Piece attacker = board.pieceAt(move.fromRow(), move.fromCol());
        if (victim != null && attacker != null) {
            // Most Valuable Victim - Least Valuable Aggressor.
            score += 10 * Evaluator.materialValue(victim.type())
                    - Evaluator.materialValue(attacker.type());
        }
        if (move.promotion() != null) {
            score += Evaluator.materialValue(move.promotion());
        }
        return score;
    }
}
