# Java Chess

Production-ready Swing chess application with a rule-aware board model, legal move validation, check/checkmate detection, save/load support, PGN import/export helpers, local threading utilities, and optional RMI-based multiplayer/chat services.

## Features

- Standard chess setup and board serialization.
- Legal moves for pawns, rooks, knights, bishops, queens, and kings.
- Castling, en passant, pawn promotion, check, checkmate, stalemate, pins, undo, and redo.
- Swing GUI with move highlighting, captured pieces, move history, save/load, and promotion dialog.
- RMI multiplayer services for synchronized move exchange.
- RMI chat service and local asynchronous chat/move/clock workers.
- JUnit 5 test suite under `test/chess`.

## Project Layout

```text
src/chess/board        Board, square, and position model
src/chess/pieces       Piece hierarchy
src/chess/move         Move generation, validation, history, check state
src/chess/gui          Swing user interface
src/chess/network      RMI multiplayer and chat services
src/chess/threading    Local async clock/chat/move helpers
src/chess/persistence  Serializable save/load model
src/chess/pgn          PGN import/export helpers
src/chess/audio        Async sound manager
test/chess             JUnit 5 tests
docs                   Review notes, diagrams, deployment guide
```

## Build

Compile from the project root with Maven:

```powershell
mvn compile
```

Or use the helper script:

```powershell
.\scripts\compile.ps1
```

## Run

```powershell
.\scripts\run.ps1
```

## Test

Run the JUnit 5 test suite with Maven:

```powershell
mvn test
```

Or use the helper script:

```powershell
.\scripts\test.ps1
```

## Documentation

- [Production Review](docs/PRODUCTION_REVIEW.md)
- [Deployment Guide](docs/DEPLOYMENT_GUIDE.md)
- [UML Class Diagram](docs/UML_CLASS_DIAGRAM.puml)
- [Move Sequence Diagram](docs/SEQUENCE_MOVE.puml)
- [Use Case Diagram](docs/USE_CASE_DIAGRAM.puml)

PlantUML files can be rendered with PlantUML in an IDE plugin or command line renderer.
