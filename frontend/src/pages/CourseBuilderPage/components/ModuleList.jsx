import React from 'react';
import { Plus } from 'lucide-react';
import ModuleCard from '../../../components/instructor/ModuleCard/ModuleCard';
import styles from '../CourseBuilderPage.module.css';

export default function ModuleList({ modules, onAddModule, isNew, courseId }) {
  return (
    <main className={`${styles.panel} ${styles.centerPanel}`}>
      <div className={styles.panelHeader}>
        <h2 className={styles.panelTitle}>Curriculum</h2>
        <button className={styles.addBtn} onClick={onAddModule} disabled={isNew}>
          <Plus size={18} /> Add Module
        </button>
      </div>

      {isNew ? (
        <div className={styles.emptyCurriculum}>
          Save the course metadata first to start building the curriculum.
        </div>
      ) : (
        <div className={styles.moduleList}>
          {modules?.map((module, idx) => (
            <ModuleCard 
              key={module.id} 
              module={module} 
              courseId={courseId} 
              isFirst={idx === 0}
              isLast={idx === (modules.length - 1)}
            />
          ))}
          {modules?.length === 0 && (
            <div className={styles.emptyState}>No modules added yet.</div>
          )}
        </div>
      )}
    </main>
  );
}
