package com.edubas.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDTO {
    private String module_id;
    private String module_title;
    private String module_description;
    private List<TopicDTO> topics;
}
