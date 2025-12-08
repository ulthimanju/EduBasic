package com.edubas.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicDTO {
    private String topic_id;
    private String topic_title;
    private String topic_description;
    private List<ExampleDTO> examples;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExampleDTO {
        private String exmpl_id;
        private String code;
        private List<String> output;
        private String visualization; // Mermaid flowchart
    }
}
