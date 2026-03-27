package com.planora.backend.controller;

import com.planora.backend.model.kanban.dto.KanbanBoardRequest;
import com.planora.backend.repository.KanbanBoardRepository;
import com.planora.backend.service.KanbanBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/kanban")
public class KanbanController {

    private final KanbanBoardService kanbanBoardService;

    @PostMapping("/board")
    public ResponseEntity<?> createKanbanBoard(
            @RequestHeader("token") String token,
            @RequestBody KanbanBoardRequest request,
            @RequestParam Long userId) {
        kanbanBoardService.createKanbanBoard(token, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
