package chess.pgn;

import java.util.ArrayList;
import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.move.GameStateDetector;
import chess.move.GameStateDetector.GameState;
import chess.move.GameStateDetector.GameStateType;
import chess.move.Move;
import chess.move.MoveValidator;
import chess.pieces.Bishop;
import chess.pieces.King;
import chess.pieces.Knight;
import chess.pieces.Pawn;
import chess.pieces.Piece;
import chess.pieces.Queen;
import chess.pieces.Rook;

/**
 * Exports legal chess moves to PGN Standard Algebraic Notation.
 */
public class PGNExporter {

    private static final int BOARD_SIZE = ChessBoard.BOARD_SIZE;

    private final MoveValidator moveValidator;
    private final GameStateDetector gameStateDetector;

    /**
     * Creates a PGN exporter.
     */
    public PGNExporter() {
        moveValidator = new MoveValidator();
        gameStateDetector = new GameStateDetector();
    }

    /**
     * Exports moves from the standard starting position.
     *
     * @param moves legal moves in game order
     * @return PGN movetext
     */
    public String export(List<Move> moves) {
        ChessBoard chessBoard = new ChessBoard();
        chessBoard.setupStartingPosition();
        return export(chessBoard, moves, true);
    }

    /**
     * Exports moves from the supplied board position.
     *
     * @param startingBoard board position before the first move
     * @param moves legal moves in game order
     * @param whiteTurn {@code true} if white moves first
     * @return PGN movetext
     */
    public String export(ChessBoard startingBoard, List<Move> moves, boolean whiteTurn) {
        if (startingBoard == null) {
            throw new IllegalArgumentException("Starting board cannot be null.");
        }

        if (moves == null) {
            throw new IllegalArgumentException("Move list cannot be null.");
        }

        ChessBoard replayBoard = copyBoard(startingBoard);
        StringBuilder builder = new StringBuilder();
        boolean currentWhiteTurn = whiteTurn;
        int moveNumber = 1;

        for (Move move : moves) {
            if (currentWhiteTurn) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(moveNumber).append(". ");
            } else if (builder.length() == 0) {
                builder.append(moveNumber).append("... ");
            } else {
                builder.append(' ');
            }

            String notation = toSan(replayBoard, move, currentWhiteTurn);
            builder.append(notation);
            replayBoard.movePiece(copyMove(move));

            if (!currentWhiteTurn) {
                moveNumber++;
            }

            currentWhiteTurn = !currentWhiteTurn;
        }

        return builder.toString();
    }

    /**
     * Converts a single legal move to SAN using the supplied board state.
     *
     * @param chessBoard board before the move
     * @param move move to convert
     * @param whiteTurn side to move
     * @return SAN notation
     */
    public String toSan(ChessBoard chessBoard, Move move, boolean whiteTurn) {
        if (chessBoard == null || move == null) {
            throw new IllegalArgumentException("Board and move are required.");
        }

        moveValidator.validateMove(move, chessBoard, whiteTurn);

        Piece movingPiece = chessBoard.getPiece(move.getSource());
        if (movingPiece instanceof King && move.isCastling()) {
            return castleNotation(move) + getCheckSuffix(chessBoard, move, whiteTurn);
        }

        Piece destinationPiece = chessBoard.getPiece(move.getDestination());
        boolean enPassantCapture = isEnPassantCapture(chessBoard, move, movingPiece);
        boolean capture = destinationPiece != null || enPassantCapture;
        StringBuilder builder = new StringBuilder();

        if (!(movingPiece instanceof Pawn)) {
            builder.append(pieceLetter(movingPiece));
            builder.append(disambiguation(chessBoard, move, movingPiece, whiteTurn));
        } else if (capture) {
            builder.append(fileName(move.getSource().getColumn()));
        }

        if (capture) {
            builder.append('x');
        }

        builder.append(squareName(move.getDestination()));

        if (movingPiece instanceof Pawn && isPromotionDestination(move.getDestination(), movingPiece.isWhite())) {
            String promotionSymbol = move.getPromotionSymbol() == null ? "Q" : move.getPromotionSymbol();
            builder.append('=').append(promotionSymbol);
        }

        builder.append(getCheckSuffix(chessBoard, move, whiteTurn));
        return builder.toString();
    }

    private String getCheckSuffix(ChessBoard chessBoard, Move move, boolean whiteTurn) {
        ChessBoard copy = copyBoard(chessBoard);
        Move replayMove = copyMove(move);
        copy.movePiece(replayMove);
        GameState gameState = gameStateDetector.analyze(copy, !whiteTurn);

        if (gameState.getType() == GameStateType.CHECKMATE) {
            return "#";
        }

        if (gameState.getType() == GameStateType.CHECK) {
            return "+";
        }

        return "";
    }

    private String disambiguation(ChessBoard chessBoard, Move move, Piece movingPiece, boolean whiteTurn) {
        List<Position> matchingSources = new ArrayList<>();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                Position source = new Position(row, column);
                Piece candidate = chessBoard.getPiece(source);

                if (candidate == null || candidate == movingPiece || candidate.isWhite() != movingPiece.isWhite()
                        || !samePieceType(candidate, movingPiece)) {
                    continue;
                }

                Move candidateMove = new Move(source, move.getDestination());
                candidateMove.setPromotionSymbol(move.getPromotionSymbol());

                if (moveValidator.isLegalMove(candidateMove, chessBoard, whiteTurn)) {
                    matchingSources.add(source);
                }
            }
        }

        if (matchingSources.isEmpty()) {
            return "";
        }

        boolean sameFile = false;
        boolean sameRank = false;

        for (Position source : matchingSources) {
            sameFile = sameFile || source.getColumn() == move.getSource().getColumn();
            sameRank = sameRank || source.getRow() == move.getSource().getRow();
        }

        if (!sameFile) {
            return String.valueOf(fileName(move.getSource().getColumn()));
        }

        if (!sameRank) {
            return String.valueOf(rankName(move.getSource().getRow()));
        }

        return String.valueOf(fileName(move.getSource().getColumn())) + rankName(move.getSource().getRow());
    }

    private ChessBoard copyBoard(ChessBoard chessBoard) {
        ChessBoard copy = new ChessBoard();
        copy.deserializeGame(chessBoard.serializeGame());
        return copy;
    }

    private Move copyMove(Move move) {
        Move copy = new Move(move.getSource(), move.getDestination());
        copy.setPromotionSymbol(move.getPromotionSymbol());
        return copy;
    }

    private boolean isEnPassantCapture(ChessBoard chessBoard, Move move, Piece movingPiece) {
        return movingPiece instanceof Pawn && chessBoard.getEnPassantTarget() != null
                && chessBoard.getEnPassantTarget().equals(move.getDestination())
                && chessBoard.getPiece(move.getDestination()) == null
                && Math.abs(move.getDestination().getColumn() - move.getSource().getColumn()) == 1;
    }

    private boolean isPromotionDestination(Position destination, boolean whitePawn) {
        return whitePawn ? destination.getRow() == 0 : destination.getRow() == BOARD_SIZE - 1;
    }

    private String castleNotation(Move move) {
        return move.getDestination().getColumn() > move.getSource().getColumn() ? "O-O" : "O-O-O";
    }

    private String squareName(Position position) {
        return String.valueOf(fileName(position.getColumn())) + rankName(position.getRow());
    }

    private char fileName(int column) {
        return (char) ('a' + column);
    }

    private char rankName(int row) {
        return (char) ('1' + (BOARD_SIZE - 1 - row));
    }

    private String pieceLetter(Piece piece) {
        if (piece instanceof Knight) {
            return "N";
        }
        if (piece instanceof Bishop) {
            return "B";
        }
        if (piece instanceof Rook) {
            return "R";
        }
        if (piece instanceof Queen) {
            return "Q";
        }
        if (piece instanceof King) {
            return "K";
        }
        return "";
    }

    private boolean samePieceType(Piece first, Piece second) {
        return first.getClass().equals(second.getClass());
    }
}
