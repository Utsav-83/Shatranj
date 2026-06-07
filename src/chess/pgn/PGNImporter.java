package chess.pgn;

import java.util.ArrayList;
import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;
import chess.move.Move;
import chess.move.MoveValidator;
import chess.pieces.Pawn;
import chess.pieces.Piece;

/**
 * Imports and validates PGN movetext.
 */
public class PGNImporter {

    private static final int BOARD_SIZE = ChessBoard.BOARD_SIZE;
    private static final String RESULT_PATTERN = "1-0|0-1|1/2-1/2|\\*";

    private final MoveValidator moveValidator;
    private final PGNExporter exporter;

    /**
     * Creates a PGN importer.
     */
    public PGNImporter() {
        moveValidator = new MoveValidator();
        exporter = new PGNExporter();
    }

    /**
     * Imports PGN from the standard starting position.
     *
     * @param pgn PGN text
     * @return legal moves represented by the PGN
     */
    public List<Move> importMoves(String pgn) {
        ChessBoard chessBoard = new ChessBoard();
        chessBoard.setupStartingPosition();
        return importMoves(pgn, chessBoard, true);
    }

    /**
     * Imports PGN from the supplied starting position.
     *
     * @param pgn PGN text
     * @param chessBoard board to replay onto
     * @param whiteTurn {@code true} if white moves first
     * @return legal moves represented by the PGN
     */
    public List<Move> importMoves(String pgn, ChessBoard chessBoard, boolean whiteTurn) {
        if (pgn == null || pgn.trim().isEmpty()) {
            throw new IllegalArgumentException("PGN cannot be empty.");
        }

        if (chessBoard == null) {
            throw new IllegalArgumentException("Chess board cannot be null.");
        }

        List<String> tokens = tokenizeMovetext(pgn);
        List<Move> importedMoves = new ArrayList<>();
        boolean currentWhiteTurn = whiteTurn;

        for (String token : tokens) {
            if (token.matches(RESULT_PATTERN)) {
                break;
            }

            Move move = findMoveForNotation(chessBoard, token, currentWhiteTurn);
            importedMoves.add(copyMove(move));
            chessBoard.movePiece(move);
            currentWhiteTurn = !currentWhiteTurn;
        }

        return importedMoves;
    }

    /**
     * Imports PGN and returns the final board state.
     *
     * @param pgn PGN text
     * @return board after all imported moves
     */
    public ChessBoard importToBoard(String pgn) {
        ChessBoard chessBoard = new ChessBoard();
        chessBoard.setupStartingPosition();
        importMoves(pgn, chessBoard, true);
        return chessBoard;
    }

    /**
     * Validates PGN notation by replaying every move.
     *
     * @param pgn PGN text
     * @return {@code true} if the notation is legal
     */
    public boolean isValid(String pgn) {
        try {
            importMoves(pgn);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private Move findMoveForNotation(ChessBoard chessBoard, String token, boolean whiteTurn) {
        List<Move> legalMoves = generateLegalMoves(chessBoard, whiteTurn);
        String requested = normalizeNotation(token);

        for (Move move : legalMoves) {
            String generated = normalizeNotation(exporter.toSan(chessBoard, move, whiteTurn));
            if (generated.equals(requested)) {
                return move;
            }
        }

        throw new IllegalArgumentException("Illegal or invalid PGN move: " + token);
    }

    private List<Move> generateLegalMoves(ChessBoard chessBoard, boolean whiteTurn) {
        List<Move> moves = new ArrayList<>();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                Position source = new Position(row, column);
                Piece piece = chessBoard.getPiece(source);

                if (piece == null || piece.isWhite() != whiteTurn) {
                    continue;
                }

                for (Position destination : moveValidator.getLegalMovesForTurn(source, chessBoard, whiteTurn)) {
                    if (piece instanceof Pawn && isPromotionDestination(destination, piece.isWhite())) {
                        addPromotionMoves(moves, source, destination);
                    } else {
                        moves.add(new Move(source, destination));
                    }
                }
            }
        }

        return moves;
    }

    private void addPromotionMoves(List<Move> moves, Position source, Position destination) {
        String[] promotionSymbols = {"Q", "R", "B", "N"};

        for (String promotionSymbol : promotionSymbols) {
            Move move = new Move(source, destination);
            move.setPromotionSymbol(promotionSymbol);
            moves.add(move);
        }
    }

    private List<String> tokenizeMovetext(String pgn) {
        String movetext = pgn;
        movetext = movetext.replaceAll("(?s)\\{.*?\\}", " ");
        movetext = movetext.replaceAll("(?m)^\\s*\\[.*?\\]\\s*$", " ");
        movetext = movetext.replaceAll(";[^\\r\\n]*", " ");
        movetext = movetext.replaceAll("\\$\\d+", " ");
        movetext = movetext.replaceAll("\\(", " ( ");
        movetext = removeVariations(movetext);
        movetext = movetext.replaceAll("\\d+\\.(\\.\\.)?", " ");

        String[] rawTokens = movetext.trim().split("\\s+");
        List<String> tokens = new ArrayList<>();

        for (String rawToken : rawTokens) {
            String token = rawToken.trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private String removeVariations(String movetext) {
        StringBuilder builder = new StringBuilder();
        int depth = 0;

        for (int index = 0; index < movetext.length(); index++) {
            char character = movetext.charAt(index);

            if (character == '(') {
                depth++;
                continue;
            }

            if (character == ')') {
                if (depth > 0) {
                    depth--;
                }
                continue;
            }

            if (depth == 0) {
                builder.append(character);
            }
        }

        return builder.toString();
    }

    private String normalizeNotation(String notation) {
        String normalized = notation.trim();
        normalized = normalized.replace('0', 'O');
        normalized = normalized.replaceAll("[!?]+$", "");
        normalized = normalized.replaceAll("e\\.p\\.$", "");
        normalized = normalized.replaceAll("\\s+", "");
        return normalized;
    }

    private Move copyMove(Move move) {
        Move copy = new Move(move.getSource(), move.getDestination());
        copy.setPromotionSymbol(move.getPromotionSymbol());
        return copy;
    }

    private boolean isPromotionDestination(Position destination, boolean whitePawn) {
        return whitePawn ? destination.getRow() == 0 : destination.getRow() == BOARD_SIZE - 1;
    }
}
