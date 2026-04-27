package com.nexora.courseservice.controller;

import com.nexora.courseservice.dto.request.CreateModuleRequest;
import com.nexora.courseservice.dto.request.ReorderRequest;
import com.nexora.courseservice.dto.request.UpdateModuleRequest;
import com.nexora.courseservice.dto.response.ModuleResponse;
import com.nexora.courseservice.service.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @PostMapping("/courses/{courseId}/modules")
    public ResponseEntity<ModuleResponse> addModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateModuleRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        return new ResponseEntity<>(moduleService.addModule(courseId, request, instructorId), HttpStatus.CREATED);
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable UUID moduleId,
            @Valid @RequestBody UpdateModuleRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        return ResponseEntity.ok(moduleService.updateModule(moduleId, request, instructorId));
    }

    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(
            @PathVariable UUID moduleId,
            @AuthenticationPrincipal UUID instructorId) {
        moduleService.deleteModule(moduleId, instructorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/courses/{courseId}/modules/reorder")
    public ResponseEntity<Void> reorderModules(
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderRequest request,
            @AuthenticationPrincipal UUID instructorId) {
        moduleService.reorderModules(courseId, request, instructorId);
        return ResponseEntity.ok().build();
    }
}
