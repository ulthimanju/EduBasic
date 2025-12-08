package com.edubas.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDatasetDTO {
    private String course_id;
    private List<LevelDTO> levels;
}
