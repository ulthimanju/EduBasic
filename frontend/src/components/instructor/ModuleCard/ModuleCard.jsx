import React, { useState } from 'react';
import { useDeleteModule, useAddLesson, useDeleteLesson } from '../../../hooks/useInstructor';
import useUiStore from '../../../stores/uiStore';
import LessonForm from '../LessonForm/LessonForm';
import LessonIcon from '../../common/LessonIcon/LessonIcon';
import { ChevronDown, ChevronUp, Edit3, Trash2, Plus, GripVertical } from 'lucide-react';
import styles from './ModuleCard.module.css';

export default function ModuleCard({ module, courseId }) {
  const [isExpanded, setIsExpanded] = useState(true);
  const [showLessonForm, setShowLessonForm] = useState(false);
  const deleteModuleMutation = useDeleteModule(courseId);
  const addLessonMutation = useAddLesson(courseId);
  const deleteLessonMutation = useDeleteLesson(courseId);
  const openConfirmModal = useUiStore(s => s.openConfirmModal);
  const closeConfirmModal = useUiStore(s => s.closeConfirmModal);

  const handleDeleteModule = (e) => {
    e.stopPropagation();
    openConfirmModal({
      title: 'Delete Module',
      message: `Are you sure you want to delete "${module.title}"? All lessons inside will also be deleted.`,
      confirmLabel: 'Delete',
      isDangerous: true,
      onConfirm: async () => {
        await deleteModuleMutation.mutateAsync(module.id);
        closeConfirmModal();
      },
      onCancel: closeConfirmModal
    });
  };

  const handleAddLesson = (lessonData) => {
    addLessonMutation.mutate({ 
      moduleId: module.id, 
      data: { ...lessonData, orderIndex: (module.lessons?.length || 0) } 
    }, {
      onSuccess: () => setShowLessonForm(false)
    });
  };

  const handleDeleteLesson = (lessonId, title) => {
    openConfirmModal({
      title: 'Delete Lesson',
      message: `Are you sure you want to delete "${title}"?`,
      confirmLabel: 'Delete',
      isDangerous: true,
      onConfirm: async () => {
        await deleteLessonMutation.mutateAsync(lessonId);
        closeConfirmModal();
      },
      onCancel: closeConfirmModal
    });
  };

  return (
    <div className={styles.card}>
      <header className={styles.header} onClick={() => setIsExpanded(!isExpanded)}>
        <GripVertical size={16} className={styles.dragHandle} />
        <span className={styles.title}>{module.title}</span>
        <div className={styles.actions}>
          <button onClick={(e) => { e.stopPropagation(); /* edit module title */ }} className={styles.iconBtn}>
            <Edit3 size={14} />
          </button>
          <button onClick={handleDeleteModule} className={styles.iconBtn}>
            <Trash2 size={14} />
          </button>
          {isExpanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
        </div>
      </header>

      {isExpanded && (
        <div className={styles.body}>
          <div className={styles.lessons}>
            {module.lessons?.map(lesson => (
              <div key={lesson.id} className={styles.lessonItem}>
                <GripVertical size={14} className={styles.dragHandle} />
                <LessonIcon type={lesson.contentType} />
                <span className={styles.lessonTitle}>{lesson.title}</span>
                {lesson.preview && <span className={styles.previewBadge}>Preview</span>}
                <div className={styles.lessonActions}>
                  <button onClick={() => handleDeleteLesson(lesson.id, lesson.title)} className={styles.iconBtn}>
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>

          {showLessonForm ? (
            <div className={styles.formWrapper}>
              <LessonForm 
                onSave={handleAddLesson} 
                onCancel={() => setShowLessonForm(false)} 
                isPending={addLessonMutation.isPending}
              />
            </div>
          ) : (
            <button className={styles.addLessonBtn} onClick={() => setShowLessonForm(true)}>
              <Plus size={16} /> Add Lesson
            </button>
          )}
        </div>
      )}
    </div>
  );
}
