import React from 'react';
import { Plus, ClipboardList, Trash2 } from 'lucide-react';
import StatusBadge from '../../../components/common/StatusBadge/StatusBadge';
import styles from '../CourseBuilderPage.module.css';

export default function ExamLinker({ exams, onLink, onUnlink, isNew }) {
  return (
    <aside className={`${styles.panel} ${styles.rightPanel}`}>
      <div className={styles.panelHeader}>
        <h2 className={styles.panelTitle}>Linked Exams</h2>
        <button 
          className={styles.iconBtn} 
          onClick={onLink}
          disabled={isNew}
          title="Link Exam"
        >
          <Plus size={18} />
        </button>
      </div>

      <div className={styles.examList}>
        {exams?.map(exam => (
          <div key={exam.id} className={styles.examItem}>
            <div className={styles.examInfo}>
              <ClipboardList size={16} />
              <span className={styles.examTitle}>{exam.title}</span>
            </div>
            <div className={styles.examBadges}>
              {exam.required && <StatusBadge status="Required" />}
              <span className={styles.badge}>{exam.minPassPercentage}% pass</span>
            </div>
            <div className={styles.examActions}>
              <button onClick={() => onUnlink(exam.id, exam.title)} className={styles.unlinkBtn}>
                <Trash2 size={14} /> Unlink
              </button>
            </div>
          </div>
        ))}
        {!isNew && exams?.length === 0 && (
          <div className={styles.emptyStateSmall}>No exams linked.</div>
        )}
      </div>
    </aside>
  );
}
