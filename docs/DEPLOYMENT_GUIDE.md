# Deployment Guide

## Requirements

- JDK 17 or newer recommended.
- Windows, macOS, or Linux with Swing desktop support.
- Optional: JUnit 5 console standalone jar for local test execution.
- Optional: PlantUML renderer for diagrams.

## Local Desktop Deployment

1. Open a terminal in the project root.
2. Compile the project:

```powershell
javac -Xlint:all -encoding UTF-8 -d bin `
  src\chess\interfaces\*.java `
  src\chess\exceptions\*.java `
  src\chess\board\*.java `
  src\chess\pieces\*.java `
  src\chess\move\*.java `
  src\chess\audio\*.java `
  src\chess\threading\*.java `
  src\chess\network\*.java `
  src\chess\persistence\*.java `
  src\chess\pgn\*.java `
  src\chess\gui\*.java `
  src\chess\main\*.java
```

3. Run:

```powershell
java -cp bin chess.main.Main
```

## Packaging as a Jar

Compile first, then create a runnable jar:

```powershell
jar --create --file chess.jar --main-class chess.main.Main -C bin .
java -jar chess.jar
```

## Multiplayer / RMI Deployment

The project includes RMI services for moves and chat:

- Chess moves: default port `1099`, service name `ChessService`.
- Chat: default port `1100`, service name `ChessChatService`.

For LAN deployment:

1. Ensure host and clients run compatible builds.
2. Open ports `1099` and `1100` in the host firewall.
3. If clients cannot connect back to the host, start Java with:

```powershell
java -Djava.rmi.server.hostname=<host-ip> -cp bin chess.main.Main
```

4. Host creates the RMI service; clients connect using the host IP.

## Production Verification

Recommended production checks:

- Compile the application from a clean checkout.
- Launch the Swing client from the compiled classes.
- For multiplayer, verify the host and client can connect over the intended RMI port.

Example compile command:

```powershell
javac -encoding UTF-8 -d bin (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

## Operational Notes

- Always call `close()` on `ChessClient`, `ChessServer`, RMI `ChatThread`, `ChessClock`, local `ChatThread`, `MoveListenerThread`, and `SoundManager` when owned by long-lived UI or service code.
- Update Swing components from the EDT using `SwingUtilities.invokeLater`.
- Do not accept remote moves directly into the local board without `MoveValidator`.
- Save files written by the menu are plain board text; richer serialized save support lives in `chess.persistence`.
