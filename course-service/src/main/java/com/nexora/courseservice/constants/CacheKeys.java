package com.nexora.courseservice.constants;

import java.util.UUID;

public final class CacheKeys {
    private CacheKeys() {}

    public static String enrollment(UUID studentId, UUID courseId) {
        return "course:enrollment:" + studentId + ":" + courseId;
    }

    public static String progress(UUID studentId, UUID courseId) {
        return "course:progress:" + studentId + ":" + courseId;
    }

    public static String catalogPage(int pageNum, int pageSize, String keyword) {
        return "course:catalog:page:" + pageNum + ":" + pageSize + ":"
               + (keyword == null ? "all" : keyword.toLowerCase());
    }

    public static String examScore(UUID studentId, UUID examId) {
        return "course:examscore:" + studentId + ":" + examId;
    }
}
