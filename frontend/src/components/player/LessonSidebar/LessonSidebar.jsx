import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import useUiStore from '../../../stores/uiStore';
import { ClipboardList } from 'lucide-react';
import styles from './LessonSidebar.module.css';

export default function LessonSidebar({ outline, courseId }) {
  const { activeLessonId } = useUiStore();

  return (
    <aside className={styles.sidebar}>
      <div className={styles.header}>
        <h3 className={styles.courseTitle}>{outline.title}</h3>
      </div>
      
      <nav className={styles.nav}>
        {outline.modules.map(module => (
          <div key={module.id} className={styles.module}>
            <h4 className={styles.moduleTitle}>{module.title}</h4>
            <div className={styles.lessons}>
              {module.lessons.map(lesson => (
                <NavLink
                  key={lesson.id}
                  to={`/courses/${courseId}/learn/${lesson.id}`}
                  className={({ isActive }) => 
                    `${styles.lessonLink} ${isActive ? styles.active : ''}`
                  }
                >
                  <span className={`${styles.status} ${styles[lesson.progress?.status || 'NOT_STARTED']}`} />
                  <span className={styles.lessonTitle}>{lesson.title}</span>
                </NavLink>
              ))}
            </div>
            
            {module.exams?.map(exam => (
              <Link 
                key={exam.id} 
                to={`/exam/${exam.id}/start`} 
                className={styles.examLink}
              >
                <ClipboardList size={16} />
                <span className={styles.lessonTitle}>{exam.title}</span>
                {exam.required && <span className={styles.requiredBadge}>Required</span>}
              </Link>
            ))}
          </div>
        ))}
      </nav>
    </aside>
  );
}
