import React from 'react';
import { Link } from 'react-router-dom';
import ProgressBar from '../ProgressBar/ProgressBar';
import StatusBadge from '../StatusBadge/StatusBadge';
import styles from './CourseCard.module.css';

/**
 * Shared course card component used in catalog and student dashboard.
 * 
 * @param {object} props
 * @param {object} props.course - Course data
 * @param {'catalog' | 'enrolled'} props.variant - Display variant
 */
export default function CourseCard({ course, variant = 'catalog' }) {
  const { 
    id, 
    title, 
    description, 
    thumbnailUrl, 
    totalModules, 
    totalLessons,
    overallProgressPercent,
    completedLessons,
    passedExams,
    totalRequiredExams,
    enrollmentStatus
  } = course;

  const isEnrolled = variant === 'enrolled';

  return (
    <div className={styles.card} data-variant={variant}>
      <div className={styles.thumbnail}>
        {thumbnailUrl ? (
          <img src={thumbnailUrl} alt={title} />
        ) : (
          <div className={styles.placeholder} />
        )}
      </div>
      
      <div className={styles.body}>
        <h3 className={styles.title}>{title}</h3>
        
        {!isEnrolled && <p className={styles.desc}>{description}</p>}
        
        {isEnrolled && (
          <>
            <div className={styles.progressSection}>
              <div className={styles.progressLabel}>
                <span>Overall Progress</span>
                <span>{overallProgressPercent}%</span>
              </div>
              <ProgressBar percent={overallProgressPercent} />
            </div>
            <div className={styles.stats}>
              <span>{completedLessons}/{totalLessons} lessons</span>
              <span>{passedExams}/{totalRequiredExams} exams</span>
            </div>
          </>
        )}

        <div className={styles.footer}>
          {!isEnrolled && (
            <span className={styles.meta}>
              {totalModules} modules · {totalLessons} lessons
            </span>
          )}
          
          {isEnrolled && <StatusBadge status={enrollmentStatus} />}
          
          <Link 
            to={isEnrolled ? `/courses/${id}/learn` : `/courses/${id}`} 
            className={styles.cta}
          >
            {isEnrolled 
              ? (enrollmentStatus === 'COMPLETED' ? 'Review' : 'Continue') 
              : 'View Course'
            }
          </Link>
        </div>
      </div>
    </div>
  );
}
