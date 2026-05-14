package com.example.chess.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable chess board with full rule support: castling, en passant, promotion,
 * check / checkmate / stalemate detection. Squares are indexed [row][col] where
 * row 0 is rank 1 and col 0 is file a.
 */
public class Board {

    private static final int[][] KNIGHT_OFFSETS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] KING_OFFSETS = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
    };
    private static final int[][] BISHOP_DIRS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int[][] ROOK_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final Piece[][] squares = new Piece[8][8];
    private Color sideToMove = Color.WHITE;

    private boolean whiteCastleKingSide = true;
    private boolean whiteCastleQueenSide = true;
    private boolean blackCastleKingSide = true;
    private boolean blackCastleQueenSide = true;

    private int enPassantRow = -1;
    private int enPassantCol = -1;

    private int halfmoveClock = 0;
    private int fullmoveNumber = 1;

    public static Board initial() {
        Board board = new Board();
        PieceType[] backRank = {
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };
        for (int col = 0; col < 8; col++) {
            board.squares[0][col] = new Piece(backRank[col], Color.WHITE);
            board.squares[1][col] = new Piece(PieceType.PAWN, Color.WHITE);
            board.squares[6][col] = new Piece(PieceType.PAWN, Color.BLACK);
            board.squares[7][col] = new Piece(backRank[col], Color.BLACK);
        }
        return board;
    }

    public Board copy() {
        Board copy = new Board();
        for (int row = 0; row < 8; row++) {
            System.arraycopy(this.squares[row], 0, copy.squares[row], 0, 8);
        }
        copy.sideToMove = this.sideToMove;
        copy.whiteCastleKingSide = this.whiteCastleKingSide;
        copy.whiteCastleQueenSide = this.whiteCastleQueenSide;
        copy.blackCastleKingSide = this.blackCastleKingSide;
        copy.blackCastleQueenSide = this.blackCastleQueenSide;
        copy.enPassantRow = this.enPassantRow;
        copy.enPassantCol = this.enPassantCol;
        copy.halfmoveClock = this.halfmoveClock;
        copy.fullmoveNumber = this.fullmoveNumber;
        return copy;
    }

    public Piece pieceAt(int row, int col) {
        return squares[row][col];
    }

    public Color getSideToMove() {
        return sideToMove;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    public int getFullmoveNumber() {
        return fullmoveNumber;
    }

    private static boolean inBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    // ----------------------------------------------------------------------
    // Move application
    // ----------------------------------------------------------------------

    /** Applies {@code move} in place. The move is assumed to be legal. */
    public void applyMove(Move move) {
        Piece piece = squares[move.fromRow()][move.fromCol()];
        Piece captured = squares[move.toRow()][move.toCol()];
        boolean isPawn = piece.type() == PieceType.PAWN;

        // En passant capture: pawn moves diagonally onto the en passant square.
        boolean isEnPassant = isPawn
                && move.toCol() != move.fromCol()
                && captured == null;
        if (isEnPassant) {
            squares[move.fromRow()][move.toCol()] = null;
        }

        // Move the piece.
        squares[move.fromRow()][move.fromCol()] = null;
        if (isPawn && (move.toRow() == 0 || move.toRow() == 7)) {
            PieceType promo = move.promotion() != null ? move.promotion() : PieceType.QUEEN;
            squares[move.toRow()][move.toCol()] = new Piece(promo, piece.color());
        } else {
            squares[move.toRow()][move.toCol()] = piece;
        }

        // Castling: move the rook to the other side of the king.
        if (piece.type() == PieceType.KING && Math.abs(move.toCol() - move.fromCol()) == 2) {
            int row = move.fromRow();
            if (move.toCol() == 6) { // king side
                squares[row][5] = squares[row][7];
                squares[row][7] = null;
            } else if (move.toCol() == 2) { // queen side
                squares[row][3] = squares[row][0];
                squares[row][0] = null;
            }
        }

        updateCastlingRights(piece, move, captured);

        // En passant target square (only set when a pawn advances two squares).
        if (isPawn && Math.abs(move.toRow() - move.fromRow()) == 2) {
            enPassantRow = (move.fromRow() + move.toRow()) / 2;
            enPassantCol = move.fromCol();
        } else {
            enPassantRow = -1;
            enPassantCol = -1;
        }

        if (isPawn || captured != null || isEnPassant) {
            halfmoveClock = 0;
        } else {
            halfmoveClock++;
        }
        if (sideToMove == Color.BLACK) {
            fullmoveNumber++;
        }
        sideToMove = sideToMove.opposite();
    }

    private void updateCastlingRights(Piece piece, Move move, Piece captured) {
        if (piece.type() == PieceType.KING) {
            if (piece.color() == Color.WHITE) {
                whiteCastleKingSide = false;
                whiteCastleQueenSide = false;
            } else {
                blackCastleKingSide = false;
                blackCastleQueenSide = false;
            }
        }
        // Rook moved away from its home square.
        clearCastleRightForSquare(move.fromRow(), move.fromCol());
        // Rook captured on its home square.
        if (captured != null) {
            clearCastleRightForSquare(move.toRow(), move.toCol());
        }
    }

    private void clearCastleRightForSquare(int row, int col) {
        if (row == 0 && col == 0) {
            whiteCastleQueenSide = false;
        } else if (row == 0 && col == 7) {
            whiteCastleKingSide = false;
        } else if (row == 7 && col == 0) {
            blackCastleQueenSide = false;
        } else if (row == 7 && col == 7) {
            blackCastleKingSide = false;
        }
    }

    // ----------------------------------------------------------------------
    // Move generation
    // ----------------------------------------------------------------------

    /** All fully legal moves for the side to move. */
    public List<Move> generateLegalMoves() {
        List<Move> pseudo = generatePseudoLegalMoves(sideToMove);
        List<Move> legal = new ArrayList<>(pseudo.size());
        for (Move move : pseudo) {
            Board next = copy();
            next.applyMove(move);
            if (!next.isInCheck(sideToMove)) {
                legal.add(move);
            }
        }
        return legal;
    }

    private List<Move> generatePseudoLegalMoves(Color color) {
        List<Move> moves = new ArrayList<>(48);
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                if (piece == null || piece.color() != color) {
                    continue;
                }
                switch (piece.type()) {
                    case PAWN -> generatePawnMoves(row, col, color, moves);
                    case KNIGHT -> generateOffsetMoves(row, col, color, KNIGHT_OFFSETS, moves);
                    case KING -> {
                        generateOffsetMoves(row, col, color, KING_OFFSETS, moves);
                        generateCastlingMoves(row, col, color, moves);
                    }
                    case BISHOP -> generateSlidingMoves(row, col, color, BISHOP_DIRS, moves);
                    case ROOK -> generateSlidingMoves(row, col, color, ROOK_DIRS, moves);
                    case QUEEN -> {
                        generateSlidingMoves(row, col, color, BISHOP_DIRS, moves);
                        generateSlidingMoves(row, col, color, ROOK_DIRS, moves);
                    }
                }
            }
        }
        return moves;
    }

    private void generatePawnMoves(int row, int col, Color color, List<Move> moves) {
        int dir = color == Color.WHITE ? 1 : -1;
        int startRow = color == Color.WHITE ? 1 : 6;
        int oneAhead = row + dir;

        if (inBounds(oneAhead, col) && squares[oneAhead][col] == null) {
            addPawnMove(row, col, oneAhead, col, moves);
            int twoAhead = row + 2 * dir;
            if (row == startRow && squares[twoAhead][col] == null) {
                moves.add(new Move(row, col, twoAhead, col));
            }
        }
        for (int dc = -1; dc <= 1; dc += 2) {
            int toCol = col + dc;
            if (!inBounds(oneAhead, toCol)) {
                continue;
            }
            Piece target = squares[oneAhead][toCol];
            if (target != null && target.color() != color) {
                addPawnMove(row, col, oneAhead, toCol, moves);
            } else if (target == null && oneAhead == enPassantRow && toCol == enPassantCol) {
                moves.add(new Move(row, col, oneAhead, toCol));
            }
        }
    }

    private void addPawnMove(int fromRow, int fromCol, int toRow, int toCol, List<Move> moves) {
        if (toRow == 0 || toRow == 7) {
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.QUEEN));
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.ROOK));
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.BISHOP));
            moves.add(new Move(fromRow, fromCol, toRow, toCol, PieceType.KNIGHT));
        } else {
            moves.add(new Move(fromRow, fromCol, toRow, toCol));
        }
    }

    private void generateOffsetMoves(int row, int col, Color color, int[][] offsets, List<Move> moves) {
        for (int[] offset : offsets) {
            int toRow = row + offset[0];
            int toCol = col + offset[1];
            if (!inBounds(toRow, toCol)) {
                continue;
            }
            Piece target = squares[toRow][toCol];
            if (target == null || target.color() != color) {
                moves.add(new Move(row, col, toRow, toCol));
            }
        }
    }

    private void generateSlidingMoves(int row, int col, Color color, int[][] dirs, List<Move> moves) {
        for (int[] dir : dirs) {
            int toRow = row + dir[0];
            int toCol = col + dir[1];
            while (inBounds(toRow, toCol)) {
                Piece target = squares[toRow][toCol];
                if (target == null) {
                    moves.add(new Move(row, col, toRow, toCol));
                } else {
                    if (target.color() != color) {
                        moves.add(new Move(row, col, toRow, toCol));
                    }
                    break;
                }
                toRow += dir[0];
                toCol += dir[1];
            }
        }
    }

    private void generateCastlingMoves(int row, int col, Color color, List<Move> moves) {
        boolean kingSide = color == Color.WHITE ? whiteCastleKingSide : blackCastleKingSide;
        boolean queenSide = color == Color.WHITE ? whiteCastleQueenSide : blackCastleQueenSide;
        if (!kingSide && !queenSide) {
            return;
        }
        int homeRow = color == Color.WHITE ? 0 : 7;
        if (row != homeRow || col != 4) {
            return;
        }
        Color enemy = color.opposite();
        if (isSquareAttacked(homeRow, 4, enemy)) {
            return; // cannot castle out of check
        }
        if (kingSide
                && squares[homeRow][5] == null && squares[homeRow][6] == null
                && squares[homeRow][7] != null
                && squares[homeRow][7].type() == PieceType.ROOK
                && squares[homeRow][7].color() == color
                && !isSquareAttacked(homeRow, 5, enemy)
                && !isSquareAttacked(homeRow, 6, enemy)) {
            moves.add(new Move(homeRow, 4, homeRow, 6));
        }
        if (queenSide
                && squares[homeRow][3] == null && squares[homeRow][2] == null && squares[homeRow][1] == null
                && squares[homeRow][0] != null
                && squares[homeRow][0].type() == PieceType.ROOK
                && squares[homeRow][0].color() == color
                && !isSquareAttacked(homeRow, 3, enemy)
                && !isSquareAttacked(homeRow, 2, enemy)) {
            moves.add(new Move(homeRow, 4, homeRow, 2));
        }
    }

    // ----------------------------------------------------------------------
    // Attack / check detection
    // ----------------------------------------------------------------------

    public boolean isInCheck(Color color) {
        int[] king = findKing(color);
        if (king == null) {
            return false;
        }
        return isSquareAttacked(king[0], king[1], color.opposite());
    }

    /** Algebraic square of {@code color}'s king, or {@code null} if absent. */
    public String kingSquare(Color color) {
        int[] king = findKing(color);
        return king == null ? null : Move.square(king[0], king[1]);
    }

    private int[] findKing(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                if (piece != null && piece.type() == PieceType.KING && piece.color() == color) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    /** True if {@code (row,col)} is attacked by any piece of {@code byColor}. */
    public boolean isSquareAttacked(int row, int col, Color byColor) {
        // Pawn attacks: a pawn of byColor sits one rank "behind" the square.
        int pawnRow = byColor == Color.WHITE ? row - 1 : row + 1;
        for (int dc = -1; dc <= 1; dc += 2) {
            if (inBounds(pawnRow, col + dc)) {
                Piece p = squares[pawnRow][col + dc];
                if (p != null && p.color() == byColor && p.type() == PieceType.PAWN) {
                    return true;
                }
            }
        }
        // Knight attacks.
        if (hasAttackerAtOffsets(row, col, byColor, KNIGHT_OFFSETS, PieceType.KNIGHT)) {
            return true;
        }
        // King attacks.
        if (hasAttackerAtOffsets(row, col, byColor, KING_OFFSETS, PieceType.KING)) {
            return true;
        }
        // Bishop / queen diagonals.
        if (hasSlidingAttacker(row, col, byColor, BISHOP_DIRS, PieceType.BISHOP)) {
            return true;
        }
        // Rook / queen orthogonals.
        return hasSlidingAttacker(row, col, byColor, ROOK_DIRS, PieceType.ROOK);
    }

    private boolean hasAttackerAtOffsets(int row, int col, Color byColor, int[][] offsets, PieceType type) {
        for (int[] offset : offsets) {
            int r = row + offset[0];
            int c = col + offset[1];
            if (inBounds(r, c)) {
                Piece p = squares[r][c];
                if (p != null && p.color() == byColor && p.type() == type) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSlidingAttacker(int row, int col, Color byColor, int[][] dirs, PieceType straightType) {
        for (int[] dir : dirs) {
            int r = row + dir[0];
            int c = col + dir[1];
            while (inBounds(r, c)) {
                Piece p = squares[r][c];
                if (p != null) {
                    if (p.color() == byColor
                            && (p.type() == straightType || p.type() == PieceType.QUEEN)) {
                        return true;
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------
    // Draw detection
    // ----------------------------------------------------------------------

    public boolean hasInsufficientMaterial() {
        List<Piece> minors = new ArrayList<>();
        int whiteBishopColor = -1;
        int blackBishopColor = -1;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                if (piece == null || piece.type() == PieceType.KING) {
                    continue;
                }
                switch (piece.type()) {
                    case PAWN, ROOK, QUEEN -> {
                        return false; // sufficient material exists
                    }
                    case BISHOP -> {
                        minors.add(piece);
                        if (piece.color() == Color.WHITE) {
                            whiteBishopColor = (row + col) % 2;
                        } else {
                            blackBishopColor = (row + col) % 2;
                        }
                    }
                    case KNIGHT -> minors.add(piece);
                    default -> {
                    }
                }
            }
        }
        if (minors.isEmpty()) {
            return true; // king vs king
        }
        if (minors.size() == 1) {
            return true; // king + single minor vs king
        }
        // King + bishop vs king + bishop with both bishops on the same color.
        if (minors.size() == 2
                && minors.get(0).type() == PieceType.BISHOP
                && minors.get(1).type() == PieceType.BISHOP
                && minors.get(0).color() != minors.get(1).color()
                && whiteBishopColor == blackBishopColor) {
            return true;
        }
        return false;
    }

    /**
     * Compact position key (placement + side to move + castling + en passant)
     * used for threefold-repetition detection.
     */
    public String positionKey() {
        StringBuilder sb = new StringBuilder(80);
        for (int row = 7; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                sb.append(piece == null ? "." : piece.code());
            }
        }
        sb.append(sideToMove == Color.WHITE ? 'w' : 'b');
        sb.append(whiteCastleKingSide ? 'K' : '-');
        sb.append(whiteCastleQueenSide ? 'Q' : '-');
        sb.append(blackCastleKingSide ? 'k' : '-');
        sb.append(blackCastleQueenSide ? 'q' : '-');
        sb.append(enPassantRow).append(',').append(enPassantCol);
        return sb.toString();
    }
}
