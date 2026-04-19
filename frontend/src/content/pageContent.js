/**
 * UI copy and static strings for the application.
 */

export const DASHBOARD_CONTENT = {
  GREETING: 'Welcome back,',
  GREETING_FALLBACK: 'there',
  SIGNED_IN_AS: 'Signed in as',
  PROFILE_TITLE: 'Your Profile',
  PROFILE_LABELS: {
    NAME: 'Name',
    EMAIL: 'Email',
    ID: 'User ID',
  },
};

export const COURSE_SELECT_CONTENT = {
  HERO: {
    EYEBROW: 'Assessment Library',
    HEADLINE: 'Pick a course and start with a clearer benchmark',
    SUBTITLE: 'Each assessment adapts as you answer, so the first choice should be the subject you want the most accurate signal on right now.',
  },
  HIGHLIGHTS: [
    { label: 'Adaptive difficulty', icon: 'Sparkles' },
    { label: 'Topic-based coverage', icon: 'BookOpen' },
    { label: 'Fast placement signal', icon: 'Layers3' },
  ],
  ASIDE: {
    LABEL: 'Available now',
    STATS: {
      COURSES: 'courses',
      TOPIC_TAGS: 'topic tags',
      UNIQUE_AREAS: 'unique areas',
    },
    NOTE: 'Choose the card that best matches your next exam goal. You can start a new assessment in one click.',
  },
  EMPTY_STATE: {
    TITLE: 'No courses are available yet',
    TEXT: 'Course assessments will appear here as soon as they are published by the exam service.',
  },
  CARD: {
    BADGE_PREFIX: 'Course',
    TOPICS_MORE: 'more',
    DESCRIPTION: 'Adaptive questions across the core concepts most likely to shape your proficiency level.',
    FOOTNOTE: 'Best when you want a fast read on where to focus your next round of study.',
    ACTION: 'Start Assessment',
    ACTION_STARTING: 'Starting...',
  },
};

export const EXAM_CONTENT = {
  ENVIRONMENT: {
    TITLE: 'Secure Exam Environment',
    DESCRIPTION: 'To ensure assessment integrity, this exam will run in <b>fullscreen mode</b>. Switching tabs, minimizing the window, or exiting fullscreen will result in a violation strike.',
    START_BTN: 'Enter Fullscreen & Start',
  },
  TERMINATED: {
    TITLE: 'Exam Terminated',
    DESCRIPTION: 'Your session has been ended due to multiple integrity violations:',
    ACTION: 'View Partial Result',
  },
  WARNING: {
    TITLE: 'Integrity Warning',
    DESCRIPTION_PREFIX: 'We detected a focus change or navigation event:',
    DESCRIPTION_STRIKES: 'Remaining strikes:',
    ACTION: 'I Understand, Resume Exam',
  },
  HEADER: {
    PROGRESS: 'Progress',
    TIME_LEFT: 'Time Left',
    ADAPTIVE_SIGNAL: 'Adaptive Signal',
    EXIT: 'Exit',
    EXITING: 'Exiting...',
  },
  QUESTION: {
    ADAPTIVE_INSIGHT: 'Adaptive Insight',
    NEXT_BTN: 'Next Question',
    RESULT_BTN: 'View Proficiency Report',
  },
  SECURITY_FOOTER: {
    PREFIX: 'Security Layer:',
    VIOLATIONS: 'Violation(s)',
    ACTIVE: 'Active',
  },
  EXIT_PROMPT: {
    TITLE: 'End Exam Early?',
    DESCRIPTION: 'Are you sure you want to end the exam now? This will finalize your current score and you won\'t be able to resume.',
    CONFIRM: 'Yes, End Exam',
    CANCEL: 'Keep Going',
  },
  EXIT_ERROR: {
    TITLE: 'Error',
    DESCRIPTION: 'Failed to end the session. Please check your connection and try again.',
    CONFIRM: 'Dismiss',
  },
};

export const RESULT_CONTENT = {
  TERMINATED_NOTICE: {
    TITLE: 'Assessment Terminated',
    DESCRIPTION_PREFIX: 'This session was ended early due to integrity violations. The proficiency level below is a <b>partial estimate</b> based on',
    DESCRIPTION_SUFFIX: 'recorded focus/navigation events.',
    CODE: 'CODE: INTEGRITY_FAIL',
  },
  HERO: {
    TERMINATED_TITLE: 'Incomplete Assessment',
    COMPLETE_TITLE: 'Assessment Complete',
    TERMINATED_SUBTITLE: 'Session ended prematurely. Reason:',
    COMPLETE_SUBTITLE: "We've analyzed your performance. Here is your proficiency profile.",
  },
  CARDS: {
    LEVEL_LABEL: 'Estimated Level',
    SCORE_LABEL: 'Proficiency Score',
  },
  INSIGHTS: {
    TITLE: 'Performance Insights',
    STRENGTH_LABEL: 'Demonstrated Strength',
    STRENGTH_EMPTY: 'Not enough data recorded',
    FOCUS_LABEL: 'Focus Areas',
    FOCUS_EMPTY_COMPLETE: 'You handled recorded topics well',
    FOCUS_EMPTY_TERMINATED: 'Assessment interrupted',
  },
  INTEGRITY: {
    TITLE: 'Assessment Integrity Notice',
    DESCRIPTION_COMPLETE: 'No significant integrity violations were detected during this session, providing a high level of confidence in the proficiency score.',
    DESCRIPTION_TERMINATED: 'Multiple violations were detected, impacting the statistical confidence of this result.',
    TRACKING_LABELS: {
      FOCUS: 'Focus Tracking',
      NAVIGATION: 'Navigation Guard',
      SESSION: 'Session Signature',
    },
  },
  ACTIONS: {
    RETRY: 'Retry Assessment',
    TRY_ANOTHER: 'Try Another Course',
    DASHBOARD: 'Return to Dashboard',
  },
};
