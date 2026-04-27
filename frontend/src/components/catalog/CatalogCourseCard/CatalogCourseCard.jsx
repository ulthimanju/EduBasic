import React from 'react';
import { Link } from 'react-router-dom';
import styles from './CatalogCourseCard.module.css';

export default function CatalogCourseCard({ course }) {
  const { id, title, description, thumbnailUrl, totalModules, totalLessons } = course;

  return (
    <div className={styles.card}>
      <div className={styles.thumbnail}>
        {thumbnailUrl ? (
          <img src={thumbnailUrl} alt={title} />
        ) : (
          <div className={styles.placeholder} />
        )}
      </div>
      <div className={styles.body}>
        <h3 className={styles.title}>{title}</h3>
        <p className={styles.desc}>{description}</p>
        <div className={styles.footer}>
          <span>{totalModules} modules · {totalLessons} lessons</span>
          <Link to={`/courses/${id}`} className={styles.cta}>
            View Course
          </Link>
        </div>
      </div>
    </div>
  );
}
