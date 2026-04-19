/**
 * Behavioral and visual configuration for the application.
 */

export const EXAM_CONFIG = {
  DEFAULT_TOTAL_QUESTIONS: 20,
  DEFAULT_TIME_LIMIT: 60,
  MAX_STRIKES: 2,
  AUTO_NEXT_DELAY_MS: 2000,
};

export const COURSE_CONFIG = {
  TOPIC_PREVIEW_LIMIT: 4,
};

export const VISUAL_MAPPINGS = {
  LEVEL_COLORS: {
    Expert: 'text-purple-500',
    Advanced: 'text-blue-500',
    Intermediate: 'text-green-500',
    Default: 'text-accent',
    Terminated: 'text-text-muted',
  },
  INTEGRITY_STATUS: {
    PASSED: { label: 'PASSED', colorClass: 'bg-green-500' },
    FAILED: { label: 'FAILED', colorClass: 'bg-accent' },
    VERIFIED: { label: 'VERIFIED', colorClass: 'bg-green-500' },
  },
};
