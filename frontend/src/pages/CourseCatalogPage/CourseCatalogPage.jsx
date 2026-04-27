import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useCatalog } from '../../hooks/useCatalog';
import CatalogCourseCard from '../../components/catalog/CatalogCourseCard/CatalogCourseCard';
import CatalogSearchBar from '../../components/catalog/CatalogSearchBar/CatalogSearchBar';
import SkeletonCard from '../../components/common/SkeletonCard/SkeletonCard';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';
import Navbar from '../../components/layout/Navbar/Navbar';
import styles from './CourseCatalogPage.module.css';

export default function CourseCatalogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const page = parseInt(searchParams.get('page') || '0', 10);
  const size = 12;

  const { data, isLoading, isError, refetch } = useCatalog({ keyword, page, size });

  // Handle keyword change with debounce would be better, but keeping it simple for now
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchParams(prev => {
        if (keyword) prev.set('keyword', keyword);
        else prev.delete('keyword');
        prev.set('page', '0');
        return prev;
      });
    }, 500);
    return () => clearTimeout(timer);
  }, [keyword, setSearchParams]);

  const setPage = (newPage) => {
    setSearchParams(prev => {
      prev.set('page', newPage.toString());
      return prev;
    });
  };

  return (
    <div className={styles.container}>
      <Navbar />
      <main className={styles.main}>
        <header className={styles.header}>
          <h1 className={styles.title}>Course Catalog</h1>
          <CatalogSearchBar value={keyword} onChange={setKeyword} />
        </header>

        {isError && (
          <div className={styles.errorWrapper}>
            <ErrorBanner message="Failed to load courses. Please try again." onRetry={refetch} />
          </div>
        )}

        <div className={styles.grid}>
          {isLoading ? (
            Array.from({ length: 8 }).map((_, i) => <SkeletonCard key={i} />)
          ) : (
            data?.content?.map(course => (
              <CatalogCourseCard key={course.id} course={course} />
            ))
          )}
        </div>

        {data && data.totalPages > 1 && (
          <div className={styles.pagination}>
            <button 
              onClick={() => setPage(page - 1)} 
              disabled={page === 0}
              className={styles.pageBtn}
            >
              Previous
            </button>
            <span className={styles.pageInfo}>
              Page {page + 1} of {data.totalPages}
            </span>
            <button 
              onClick={() => setPage(page + 1)} 
              disabled={page >= data.totalPages - 1}
              className={styles.pageBtn}
            >
              Next
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
