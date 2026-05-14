"use strict";

const PIECE_GLYPHS = { K: "♚", Q: "♛", R: "♜", B: "♝", N: "♞", P: "♟" };

const boardEl = document.getElementById("board");
const statusEl = document.getElementById("status");
const moveListEl = document.getElementById("move-list");
const promotionEl = document.getElementById("promotion");
const colorSelect = document.getElementById("color-select");
const difficultySelect = document.getElementById("difficulty-select");
const newGameBtn = document.getElementById("new-game-btn");

let state = null;
let selected = null;        // algebraic square string, e.g. "e2"
let busy = false;           // true while waiting on the server
let pendingPromotion = null; // { from, to } awaiting a promotion choice

function squareName(row, col) {
    return String.fromCharCode(97 + col) + (row + 1);
}

async function api(method, url, body) {
    const options = { method, headers: { "Content-Type": "application/json" } };
    if (body !== undefined) {
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        const message = data && data.message ? data.message : response.statusText;
        throw new Error(message);
    }
    return data;
}

async function startNewGame() {
    busy = true;
    statusEl.textContent = "Starting new game…";
    try {
        state = await api("POST", "/api/games", {
            color: colorSelect.value,
            difficulty: difficultySelect.value,
        });
        localStorage.setItem("chessGameId", state.gameId);
        selected = null;
        pendingPromotion = null;
    } catch (err) {
        statusEl.textContent = "Could not start game: " + err.message;
    } finally {
        busy = false;
    }
    render();
}

async function loadExistingGame(id) {
    try {
        state = await api("GET", "/api/games/" + id);
        render();
    } catch (err) {
        await startNewGame();
    }
}

async function submitMove(from, to, promotion) {
    busy = true;
    selected = null;
    pendingPromotion = null;
    statusEl.textContent = "Computer is thinking…";
    statusEl.className = "status thinking";
    render();
    try {
        state = await api("POST", "/api/games/" + state.gameId + "/moves", { from, to, promotion });
    } catch (err) {
        statusEl.textContent = err.message;
        // Refresh authoritative state so the board stays consistent.
        try {
            state = await api("GET", "/api/games/" + state.gameId);
        } catch (ignored) { /* keep last known state */ }
    } finally {
        busy = false;
    }
    render();
}

function legalMovesFrom(square) {
    if (!state || !state.legalMoves) {
        return [];
    }
    return state.legalMoves.filter((m) => m.from === square);
}

function humanToMove() {
    return state
        && state.status === "IN_PROGRESS"
        && state.turn === state.humanColor
        && !busy;
}

function onSquareClick(row, col) {
    if (!humanToMove() || pendingPromotion) {
        return;
    }
    const square = squareName(row, col);
    const piece = state.board[row][col];

    if (selected) {
        const candidates = legalMovesFrom(selected).filter((m) => m.to === square);
        if (candidates.length > 0) {
            const promotions = candidates.filter((m) => m.promotion);
            if (promotions.length > 0) {
                pendingPromotion = { from: selected, to: square };
                render();
            } else {
                submitMove(selected, square, null);
            }
            return;
        }
    }

    // Select one of the human's own pieces, otherwise clear the selection.
    if (piece && piece[0] === state.humanColor[0].toLowerCase()) {
        selected = square;
    } else {
        selected = null;
    }
    render();
}

function choosePromotion(pieceCode) {
    if (!pendingPromotion) {
        return;
    }
    const { from, to } = pendingPromotion;
    submitMove(from, to, pieceCode);
}

function statusText() {
    if (!state) {
        return "Loading…";
    }
    const youAre = state.humanColor === "WHITE" ? "White" : "Black";
    switch (state.status) {
        case "CHECKMATE":
            return state.winner === state.humanColor
                ? "Checkmate — you win!"
                : "Checkmate — the computer wins.";
        case "STALEMATE":
            return "Stalemate — it's a draw.";
        case "DRAW_INSUFFICIENT_MATERIAL":
            return "Draw — insufficient material.";
        case "DRAW_FIFTY_MOVE":
            return "Draw — fifty-move rule.";
        case "DRAW_REPETITION":
            return "Draw — threefold repetition.";
        default:
            if (busy) {
                return "Computer is thinking…";
            }
            if (state.turn === state.humanColor) {
                return state.inCheck
                    ? "Your move (" + youAre + ") — you're in check!"
                    : "Your move (" + youAre + ")";
            }
            return "Computer is thinking…";
    }
}

function render() {
    renderStatus();
    renderBoard();
    renderHistory();
    renderPromotion();
}

function renderStatus() {
    statusEl.textContent = statusText();
    let cls = "status";
    if (busy) {
        cls += " thinking";
    }
    if (state && state.status !== "IN_PROGRESS") {
        cls += " over";
    }
    statusEl.className = cls;
}

function renderBoard() {
    boardEl.innerHTML = "";
    if (!state) {
        return;
    }
    const humanWhite = state.humanColor === "WHITE";
    // White at the bottom for White, flipped for Black.
    const rows = humanWhite ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7];
    const cols = humanWhite ? [0, 1, 2, 3, 4, 5, 6, 7] : [7, 6, 5, 4, 3, 2, 1, 0];

    const targets = selected ? legalMovesFrom(selected).map((m) => m.to) : [];
    const lastMove = state.lastMove;

    for (const row of rows) {
        for (const col of cols) {
            const square = squareName(row, col);
            const cell = document.createElement("div");
            cell.className = "square " + ((row + col) % 2 === 0 ? "dark" : "light");
            cell.dataset.row = row;
            cell.dataset.col = col;

            if (square === selected) {
                cell.classList.add("selected");
            }
            if (lastMove && (square === lastMove.from || square === lastMove.to)) {
                cell.classList.add("last-move");
            }
            if (state.inCheck && square === state.checkSquare) {
                cell.classList.add("in-check");
            }

            const code = state.board[row][col];
            if (code) {
                const piece = document.createElement("span");
                piece.className = "piece " + (code[0] === "w" ? "white" : "black");
                piece.textContent = PIECE_GLYPHS[code[1]];
                cell.appendChild(piece);
            }

            if (targets.includes(square)) {
                if (code) {
                    cell.classList.add("has-target");
                }
                const dot = document.createElement("span");
                dot.className = "legal-dot";
                cell.appendChild(dot);
            }

            cell.addEventListener("click", () => onSquareClick(row, col));
            boardEl.appendChild(cell);
        }
    }
    boardEl.classList.toggle("disabled", !humanToMove());
}

function renderHistory() {
    moveListEl.innerHTML = "";
    if (!state) {
        return;
    }
    const moves = state.moveHistory;
    for (let i = 0; i < moves.length; i += 2) {
        const li = document.createElement("li");
        const white = moves[i];
        const black = moves[i + 1] ? " " + moves[i + 1] : "";
        li.textContent = white + black;
        moveListEl.appendChild(li);
    }
}

function renderPromotion() {
    if (pendingPromotion) {
        promotionEl.classList.remove("hidden");
    } else {
        promotionEl.classList.add("hidden");
    }
}

newGameBtn.addEventListener("click", startNewGame);
promotionEl.querySelectorAll("button").forEach((btn) => {
    btn.addEventListener("click", () => choosePromotion(btn.dataset.piece));
});

const savedId = localStorage.getItem("chessGameId");
if (savedId) {
    loadExistingGame(savedId);
} else {
    startNewGame();
}
