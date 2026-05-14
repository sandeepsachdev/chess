package com.example.chess.service;

import com.example.chess.engine.ChessAI;
import com.example.chess.model.Board;
import com.example.chess.model.Color;
import com.example.chess.model.Game;
import com.example.chess.model.Move;
import com.example.chess.model.PieceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Owns the lifecycle of chess games and drives the AI's replies. */
@Service
public class GameService {

    private final ChessAI chessAI;
    private final ConcurrentMap<String, Game> games = new ConcurrentHashMap<>();

    public GameService(ChessAI chessAI) {
        this.chessAI = chessAI;
    }

    public Game newGame(String color, String difficulty) {
        Color humanColor = "black".equalsIgnoreCase(color) ? Color.BLACK : Color.WHITE;
        int depth = searchDepthFor(difficulty);
        Game game = new Game(UUID.randomUUID().toString(), humanColor, depth);
        games.put(game.getId(), game);

        // If the human plays Black, the AI (White) opens the game.
        if (humanColor == Color.BLACK) {
            playAiMove(game);
        }
        return game;
    }

    public Game getGame(String id) {
        Game game = games.get(id);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found: " + id);
        }
        return game;
    }

    /**
     * Applies the human's move and, if the game continues, the AI's reply.
     *
     * @return the AI's reply move, or {@code null} if the AI did not move
     */
    public Move applyHumanMove(String id, String from, String to, String promotionCode) {
        Game game = getGame(id);
        if (game.getStatus().isGameOver()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already over");
        }
        Board board = game.getBoard();
        if (board.getSideToMove() != game.getHumanColor()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "It is not your turn");
        }

        Move move = resolveMove(board, from, to, promotionCode);
        game.play(move);

        if (!game.getStatus().isGameOver() && board.getSideToMove() == game.getAiColor()) {
            return playAiMove(game);
        }
        return null;
    }

    private Move playAiMove(Game game) {
        Move aiMove = chessAI.chooseMove(game.getBoard(), game.getAiSearchDepth());
        if (aiMove != null) {
            game.play(aiMove);
        }
        return aiMove;
    }

    private Move resolveMove(Board board, String from, String to, String promotionCode) {
        int fromRow, fromCol, toRow, toCol;
        try {
            fromCol = file(from);
            fromRow = rank(from);
            toCol = file(to);
            toRow = rank(to);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid square notation");
        }
        PieceType requestedPromotion = parsePromotion(promotionCode);

        Move chosen = null;
        for (Move legal : board.generateLegalMoves()) {
            if (legal.fromRow() != fromRow || legal.fromCol() != fromCol
                    || legal.toRow() != toRow || legal.toCol() != toCol) {
                continue;
            }
            if (legal.promotion() == null) {
                return legal; // non-promotion move; unambiguous
            }
            // Promotion move: match the requested piece, defaulting to a queen.
            PieceType target = requestedPromotion != null ? requestedPromotion : PieceType.QUEEN;
            if (legal.promotion() == target) {
                chosen = legal;
            }
        }
        if (chosen == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Illegal move");
        }
        return chosen;
    }

    private static int file(String square) {
        int col = square.charAt(0) - 'a';
        if (col < 0 || col > 7 || square.length() != 2) {
            throw new IllegalArgumentException("bad square");
        }
        return col;
    }

    private static int rank(String square) {
        int row = square.charAt(1) - '1';
        if (row < 0 || row > 7) {
            throw new IllegalArgumentException("bad square");
        }
        return row;
    }

    private static PieceType parsePromotion(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return switch (code.trim().toUpperCase()) {
            case "Q", "QUEEN" -> PieceType.QUEEN;
            case "R", "ROOK" -> PieceType.ROOK;
            case "B", "BISHOP" -> PieceType.BISHOP;
            case "N", "KNIGHT" -> PieceType.KNIGHT;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid promotion piece");
        };
    }

    private static int searchDepthFor(String difficulty) {
        if (difficulty == null) {
            return 3;
        }
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> 2;
            case "hard" -> 4;
            default -> 3;
        };
    }

    public List<Move> legalMoves(Game game) {
        return game.getBoard().generateLegalMoves();
    }
}
