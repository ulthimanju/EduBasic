package com.edubas.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelDTO {
    private String level; // beginner|intermediate|advanced|expert|master
    private String level_title;
    private List<ModuleDTO> modules;
}
