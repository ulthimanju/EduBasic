import React, { useState, useMemo } from 'react';
import { useMyCourses } from '../../hooks/useEnrollment';
import Navbar from '../../components/layout/Navbar/Navbar';
import SkeletonCard from '../../components/common/SkeletonCard/SkeletonCard';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';
import ProgressBar from '../../components/common/ProgressBar/ProgressBar';
import StatusBadge from '../../components/common/StatusBadge/StatusBadge';
import { Link } from 'react-router-dom';
import styles from './MyCoursesPage.module.css';

export default function MyCoursesPage() {
  const [filter, setFilter] = useState('all');
  const { data: courses, isLoading, isError, refetch } = useMyCourses();

  const filtered = useMemo(() => {
    if (!courses) return [];
    return courses.filter((c) => {
      if (filter === 'in_progress') return c.enrollmentStatus === 'ACTIVE' && c.overallProgressPercent < 100;
      if (filter === 'completed')   return c.enrollmentStatus === 'COMPLETED' || c.overallProgressPercent === 100;
      return c.enrollmentStatus !== 'DROPPED';
    });
  }, [courses, filter]);

  return (
    <div className={styles.container}>
      <Navbar />
      <main className={styles.main}>
        <header className={styles.header}>
          <h1 className={styles.title}>My Learning</h1>
          <div className={styles.tabs}>
            <button 
              className={`${styles.tab} ${filter === 'all' ? styles.activeTab : ''}`}
              onClick={() => setFilter('all')}
            >
              All Courses
            </button>
            <button 
              className={`${styles.tab} ${filter === 'in_progress' ? styles.activeTab : ''}`}
              onClick={() => setFilter('in_progress')}
            >
              In Progress
            </button>
            <button 
              className={`${styles.tab} ${filter === 'completed' ? styles.activeTab : ''}`}
              onClick={() => setFilter('completed')}
            >
              Completed
            </button>
          </div>
        </header>

        {isError && <ErrorBanner onRetry={refetch} />}

        <div className={styles.grid}>
          {isLoading ? (
            Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} />)
          ) : (
            filtered.map(course => (
              <MyCourseSummaryCard key={course.id} course={course} />
            ))
          )}
          {!isLoading && filtered.length === 0 && (
            <div className={styles.empty}>
              <p>No courses found in this category.</p>
              <Link to="/courses" className={styles.browseBtn}>Browse Catalog</Link>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

function MyCourseSummaryCard({ course }) {
  const { id, title, thumbnailUrl, overallProgressPercent, completedLessons, totalLessons, passedExams, totalRequiredExams, enrollmentStatus } = course;

  return (
    <div className={styles.card}>
      <div className={styles.cardThumb}>
        {thumbnailUrl ? (
          <img src={thumbnailUrl} alt={title} />
        ) : (
          <div className={styles.placeholder} />
        )}
      </div>
      <div className={styles.cardBody}>
        <h3 className={styles.cardTitle}>{title}</h3>
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
        <div className={styles.cardFooter}>
          <StatusBadge status={enrollmentStatus} />
          <Link to={`/courses/${id}/learn`} className={styles.continueBtn}>
            {enrollmentStatus === 'COMPLETED' ? 'Review' : 'Continue'}
          </Link>
        </div>
      </div>
    </div>
  );
}
