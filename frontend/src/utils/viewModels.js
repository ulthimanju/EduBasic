import { VISUAL_MAPPINGS } from '../config/pageConfig';

/**
 * Derives the visual class for a given proficiency level.
 */
export const getLevelColorClass = (level, isTerminated) => {
  if (isTerminated) return VISUAL_MAPPINGS.LEVEL_COLORS.Terminated;
  return VISUAL_MAPPINGS.LEVEL_COLORS[level] || VISUAL_MAPPINGS.LEVEL_COLORS.Default;
};

/**
 * Derives user display name from name string.
 */
export const getUserFirstName = (name, fallback) => {
  return name?.split(' ')[0] ?? fallback;
};

/**
 * derives course topics display info.
 */
export const getCourseTopicsInfo = (topics, limit) => {
  const visibleTopics = topics.slice(0, limit);
  const hiddenCount = Math.max(topics.length - limit, 0);
  return { visibleTopics, hiddenCount };
};
