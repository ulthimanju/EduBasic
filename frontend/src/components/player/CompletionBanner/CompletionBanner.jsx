import React from 'react';
import { Link } from 'react-router-dom';
import { useCompletionStatus } from '../../../hooks/useEnrollment';
import { Trophy } from 'lucide-react';
import styles from './CompletionBanner.module.css';

export default function CompletionBanner({ courseId }) {
  const { data } = useCompletionStatus(courseId);

  if (!data?.isCompleted) return null;

  return (
    <div className={styles.banner}>
      <div className={styles.message}>
        <Trophy size={20} />
        <span>🎉 Congratulations! You've completed this course!</span>
      </div>
      <Link to="/my-courses" className={styles.link}>
        View Certificate
      </Link>
    </div>
  );
}
