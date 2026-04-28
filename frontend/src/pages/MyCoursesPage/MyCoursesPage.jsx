import React, { useState, useMemo } from 'react';
import { useMyCourses } from '../../hooks/useEnrollment';
import Navbar from '../../components/layout/Navbar/Navbar';
import SkeletonCard from '../../components/common/SkeletonCard/SkeletonCard';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';
import CourseCard from '../../components/common/CourseCard/CourseCard';
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
              <CourseCard key={course.id} course={course} variant="enrolled" />
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
