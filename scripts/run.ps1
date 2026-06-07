$ErrorActionPreference = "Stop"

mvn -q compile
java -cp target/classes chess.main.Main
