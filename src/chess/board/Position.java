package chess.board;

import java.util.Objects;

import chess.exceptions.InvalidPositionException;

/**
 * Represents a fixed row and column on an 8x8 chess board.
 * <p>
 * Rows and columns are zero-based. Row {@code 0} is the black back rank and
 * row {@code 7} is the white back rank in the default board setup.
 */
public final class Position {

    /** Number of rows and columns on a standard chess board. */
    public static final int BOARD_SIZE = 8;

    private final int row;
    private final int column;

    /**
     * Creates a position on the board.
     *
     * @param row the zero-based row, from {@code 0} to {@code 7}
     * @param column the zero-based column, from {@code 0} to {@code 7}
     * @throws IllegalArgumentException if the row or column is outside the board
     */
    public Position(int row, int column) {
        if (!isValid(row, column)) {
            throw new InvalidPositionException(row, column);
        }

        this.row = row;
        this.column = column;
    }

    /**
     * Checks whether a row and column are inside a standard chess board.
     *
     * @param row the row to check
     * @param column the column to check
     * @return {@code true} if the position is inside the board
     */
    public static boolean isValid(int row, int column) {
        return row >= 0 && row < BOARD_SIZE && column >= 0 && column < BOARD_SIZE;
    }

    /**
     * Gets the zero-based row.
     *
     * @return the row
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the zero-based column.
     *
     * @return the column
     */
    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Position)) {
            return false;
        }

        Position position = (Position) object;
        return row == position.row && column == position.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "Position{row=" + row + ", column=" + column + "}";
    }
}
