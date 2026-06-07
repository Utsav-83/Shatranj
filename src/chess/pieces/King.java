package chess.pieces;

import java.util.List;

import chess.board.ChessBoard;
import chess.board.Position;

public class King extends Piece {

    public King(boolean white) {
        super(white);
    }

    @Override
    public String getSymbol() {
        return white ? "WK" : "BK";
    }

    @Override
    public List<Position> getLegalMoves(Position currentPosition, ChessBoard chessBoard) {
        return super.getLegalMoves(currentPosition, chessBoard);
    }

    @Override
    public List<Position> getAttackPositions(Position currentPosition, ChessBoard chessBoard) {
        return super.getAttackPositions(currentPosition, chessBoard);
    }
}
