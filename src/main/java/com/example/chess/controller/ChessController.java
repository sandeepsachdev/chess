package com.example.chess.controller;

import com.example.chess.dto.GameStateDto;
import com.example.chess.dto.MoveRequest;
import com.example.chess.dto.NewGameRequest;
import com.example.chess.model.Move;
import com.example.chess.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
public class ChessController {

    private final GameService gameService;

    public ChessController(GameService gameService) {
        this.gameService = gameService;
    }

    /** Creates a new game and returns its initial state. */
    @PostMapping
    public GameStateDto newGame(@RequestBody(required = false) NewGameRequest request) {
        String color = request == null ? null : request.color();
        String difficulty = request == null ? null : request.difficulty();
        return GameStateDto.from(gameService.newGame(color, difficulty), null);
    }

    /** Returns the current state of an existing game. */
    @GetMapping("/{id}")
    public GameStateDto getGame(@PathVariable String id) {
        return GameStateDto.from(gameService.getGame(id), null);
    }

    /** Applies the human's move, lets the AI reply, and returns the new state. */
    @PostMapping("/{id}/moves")
    public GameStateDto move(@PathVariable String id, @RequestBody MoveRequest request) {
        if (request == null || request.from() == null || request.to() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' and 'to' are required");
        }
        Move aiMove = gameService.applyHumanMove(id, request.from(), request.to(), request.promotion());
        return GameStateDto.from(gameService.getGame(id), aiMove);
    }
}
