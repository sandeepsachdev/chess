# Spring Boot Chess

A chess game where you play against the computer, built with Spring Boot. The
backend contains a full chess rules engine plus a negamax + alpha-beta AI; the
frontend is a self-contained HTML/CSS/JS board served as static content.

## Features

- Complete chess rules: castling, en passant, promotion, check, checkmate,
  stalemate, plus draws by insufficient material, the fifty-move rule and
  threefold repetition.
- Built-in AI (negamax with alpha-beta pruning, MVV-LVA move ordering and
  piece-square-table evaluation). Three difficulty levels map to search depth.
- Play as White or Black; the board flips to your perspective.
- Click-to-move UI with legal-move highlighting and a promotion picker.
- No database or external services — games are kept in memory.

## Run locally

Requirements: Java 21 and Maven 3.9+.

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080>.

To build and run the jar directly:

```bash
mvn clean package
java -jar target/chess.jar
```

## Run with Docker

```bash
docker build -t spring-boot-chess .
docker run -p 8080:8080 spring-boot-chess
```

The app listens on the port given by the `PORT` environment variable and
defaults to `8080`.

## Deploy to Render

This repo includes a `render.yaml` blueprint and a `Dockerfile`, so deployment
is one of:

- **Blueprint:** in the Render dashboard choose *New → Blueprint* and point it
  at this repository. Render reads `render.yaml` and provisions a free Docker
  web service automatically.
- **Manual:** *New → Web Service*, connect the repo, and select the *Docker*
  runtime. Render builds the `Dockerfile` and injects `PORT` for you.

The health check path is `/`, which serves the game UI.

## API

| Method | Path                      | Description                                  |
|--------|---------------------------|----------------------------------------------|
| POST   | `/api/games`              | Create a game. Body: `{ "color": "white\|black", "difficulty": "easy\|medium\|hard" }` (both optional). |
| GET    | `/api/games/{id}`         | Fetch the current game state.                |
| POST   | `/api/games/{id}/moves`   | Play a move. Body: `{ "from": "e2", "to": "e4", "promotion": "q" }` (`promotion` optional). |

A game-state response includes the board, whose turn it is, game status, the
last move, the AI's reply move, the move history and every legal move for the
side to move.

## Project layout

```
src/main/java/com/example/chess
├── model      # Board, Piece, Move, Game — rules engine
├── engine     # ChessAI (search) and Evaluator
├── service    # GameService — game lifecycle, drives the AI
├── dto        # request/response records
└── controller # ChessController — REST endpoints
src/main/resources/static  # index.html, style.css, app.js
```
