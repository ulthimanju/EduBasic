import React, { useEffect } from 'react';
import DOMPurify from 'dompurify';
import { useUpdateProgress } from '../../../hooks/useProgress';
import { ExternalLink, CheckCircle } from 'lucide-react';
import styles from './LessonContent.module.css';

export default function LessonContent({ lesson, courseId }) {
  const { mutate: updateProgress, isPending } = useUpdateProgress(courseId);

  useEffect(() => {
    // Auto-complete logic for TEXT and LINK types after 30s
    if (lesson.contentType === 'TEXT' || lesson.contentType === 'LINK') {
      if (lesson.progress?.status !== 'COMPLETED') {
        const timer = setTimeout(() => {
          updateProgress({ lessonId: lesson.id, percent: 100 });
        }, 30000);
        return () => clearTimeout(timer);
      }
    }
  }, [lesson.id, lesson.contentType, lesson.progress?.status, updateProgress]);

  const handleManualComplete = () => {
    updateProgress({ lessonId: lesson.id, percent: 100 });
  };

  const renderContent = () => {
    switch (lesson.contentType) {
      case 'TEXT':
        return (
          <article
            className={styles.richText}
            dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(lesson.contentBody) }}
          />
        );
      case 'LINK':
        return (
          <div className={styles.linkContent}>
            <iframe src={lesson.contentUrl} className={styles.iframe} title={lesson.title} />
            <div className={styles.linkActions}>
              <a href={lesson.contentUrl} target="_blank" rel="noopener noreferrer" className={styles.externalBtn}>
                <ExternalLink size={16} />
                Open in new tab
              </a>
            </div>
          </div>
        );
      case 'ASSIGNMENT':
        return (
          <div className={styles.assignmentCard}>
            <h3 className={styles.assignmentTitle}>Assignment Instructions</h3>
            <p className={styles.assignmentDesc}>{lesson.contentBody}</p>
            {lesson.contentUrl && (
              <a href={lesson.contentUrl} target="_blank" rel="noopener noreferrer" className={styles.submitBtn}>
                <ExternalLink size={16} />
                Submit Assignment
              </a>
            )}
            <div className={styles.manualComplete}>
              <label className={styles.checkboxRow}>
                <input 
                  type="checkbox" 
                  checked={lesson.progress?.status === 'COMPLETED'}
                  onChange={handleManualComplete}
                  disabled={isPending || lesson.progress?.status === 'COMPLETED'}
                />
                Mark as complete
              </label>
            </div>
          </div>
        );
      default:
        return <div>Unsupported content type</div>;
    }
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h2 className={styles.title}>{lesson.title}</h2>
        {lesson.duration && <span className={styles.duration}>{lesson.duration} min</span>}
      </header>
      
      <div className={styles.body}>
        {renderContent()}
      </div>

      <footer className={styles.footer}>
        <button
          className={styles.completeBtn}
          onClick={handleManualComplete}
          disabled={lesson.progress?.status === 'COMPLETED' || isPending}
        >
          {lesson.progress?.status === 'COMPLETED' ? (
            <><CheckCircle size={18} /> Completed</>
          ) : (
            'Mark as Complete'
          )}
        </button>
      </footer>
    </div>
  );
}
