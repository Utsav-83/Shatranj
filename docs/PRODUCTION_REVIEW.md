# Production Review

## Summary

The project now compiles cleanly with `javac -Xlint:all`. The core chess model is reasonably separated from Swing, with move validation and check detection kept in `chess.move`. The highest-risk areas are direct board mutation, RMI trust boundaries, GUI event-thread safety, and missing dependency/build metadata for tests.

## Bugs Fixed

- Direct `ChessBoard.movePiece` castling now rejects castling when the king/rook rights are missing instead of risking a null rook path.
- En passant generation and simulation now require an actual adjacent enemy pawn, preventing illegal diagonal empty-square captures when the en-passant target is malformed.
- `PGNExporter.export` no longer mutates caller-provided `Move` objects while replaying moves.
- `ChessService.GameSnapshot` no longer exposes mutable internal lists to RMI clients.
- `ChessServer` now rejects a third player, invalid move coordinates, invalid promotion symbols, disconnected players, and out-of-turn submissions.
- `MultiplayerManager.hostGame` now rolls back and stops the server if host-then-join fails.
- `ChessClient` and RMI `ChatThread` now reject reconnect attempts on an already connected instance.
- `BoardPanel.refreshBoard` now marshals refresh work to the Swing event dispatch thread.
- `BoardPanel` now caches generated piece icons rather than rebuilding images on every refresh.
- `SoundManager` now ignores playback requests after close and closes audio resources in finally paths.

## Memory / Resource Leaks

- RMI server executors and exported objects are explicitly shut down by `stopServer`.
- RMI chat closes polling tasks, unbinds hosted services, and unexports the object.
- Sound clips and generated audio lines are closed even on interrupt or failure.
- Board icons are cached per symbol, avoiding repeated image allocation.
- Remaining risk: GUI-created services or clocks must be closed by their owner. If new windows add clocks, chat, or multiplayer managers, wire them into window disposal.

## Threading Issues

- Swing UI must be updated only on the EDT. Board refresh is now EDT-safe.
- Network and local worker callbacks still execute on background threads. GUI listeners must wrap UI updates in `SwingUtilities.invokeLater`.
- Worker threads are daemon threads, so they will not block JVM exit, but production callers should still call `close`.
- `ChessBoard` itself is not thread-safe. All board mutations should be serialized by the GUI/controller.

## RMI Issues

- Server-side validation now covers player count, turn order, coordinate bounds, promotion symbols, and disconnected sessions.
- RMI move validation does not yet replay the full board or prevent cheating legal-looking but impossible moves. A hardened multiplayer mode should make the server own a `ChessBoard` and validate each submitted move with `MoveValidator`.
- Java RMI may need `java.rmi.server.hostname` on multi-machine deployments.
- Registry ports must be reachable through the local firewall.

## Illegal Move Scenarios

- Core validator rejects wrong turn, empty source, same-square moves, own-piece captures, king captures, invalid piece movement, invalid promotion symbols, castling through check, self-check, and pinned-piece exposure.
- Board-level direct mutation still intentionally performs only structural move execution, not full chess validation. Use `MoveValidator` before `ChessBoard.movePiece` for user or remote input.
- Check/checkmate/stalemate are detected by analyzing all legal replies.

## GUI Issues

- Refresh work is now EDT-safe.
- Piece icons are cached for performance.
- Clicking a non-highlighted destination after selecting a piece clears selection instead of showing noisy illegal-move dialogs.
- Save/load menu currently stores board state only via text serialization, not turn, clocks, captured pieces, or visible move history. Use `chess.persistence.GameState` for richer production saves.

## Edge Cases

- Promotion supports Q/R/B/N and defaults to queen for direct board moves.
- En passant state is serialized in board text.
- Castling rights are derived from moved flags and original king/rook squares.
- Missing kings raise `CheckException`; tests and callers should create valid king-bearing positions before game-state analysis.

## Recommended Next Steps

- Add a build file such as Maven or Gradle with JUnit 5 and JaCoCo coverage.
- Convert menu save/load to use `GameState` so turn and clocks survive reload.
- Make the RMI server authoritative over board state for anti-cheat multiplayer.
- Add CI to compile, run tests, and publish coverage.
- Add integration tests for RMI host/join/send/close lifecycle.
