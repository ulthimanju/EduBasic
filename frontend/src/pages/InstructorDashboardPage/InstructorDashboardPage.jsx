import React from 'react';
import { Link } from 'react-router-dom';
import { useMyCoursesInstructor, usePublishCourse, useDeleteCourse } from '../../hooks/useInstructor';
import useUiStore from '../../stores/uiStore';
import Navbar from '../../components/layout/Navbar/Navbar';
import StatusBadge from '../../components/common/StatusBadge/StatusBadge';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';
import { Plus, Edit3, Trash2, Send, Archive } from 'lucide-react';
import styles from './InstructorDashboardPage.module.css';

export default function InstructorDashboardPage() {
  const { data: courses, isLoading, isError, refetch } = useMyCoursesInstructor();
  const publishMutation = usePublishCourse();
  const deleteMutation = useDeleteCourse();
  const openConfirmModal = useUiStore(s => s.openConfirmModal);
  const closeConfirmModal = useUiStore(s => s.closeConfirmModal);

  const handlePublish = (courseId) => {
    publishMutation.mutate(courseId);
  };

  const handleDelete = (courseId, title) => {
    openConfirmModal({
      title: 'Delete Course',
      message: `Are you sure you want to delete "${title}"? This action cannot be undone.`,
      confirmLabel: 'Delete',
      isDangerous: true,
      onConfirm: async () => {
        await deleteMutation.mutateAsync(courseId);
        closeConfirmModal();
      },
      onCancel: closeConfirmModal
    });
  };

  return (
    <div className={styles.container}>
      <Navbar />
      <main className={styles.main}>
        <header className={styles.header}>
          <h1 className={styles.title}>Instructor Dashboard</h1>
          <Link to="/instructor/courses/new" className={styles.createBtn}>
            <Plus size={18} />
            Create Course
          </Link>
        </header>

        {isError && <ErrorBanner onRetry={refetch} />}

        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Title</th>
                <th>Status</th>
                <th>Modules</th>
                <th>Lessons</th>
                <th>Created</th>
                <th className={styles.actionsHeader}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i} className={styles.skeletonRow}>
                    <td colSpan={6}><div className={styles.skeletonBar} /></td>
                  </tr>
                ))
              ) : (
                courses?.map(course => (
                  <React.Fragment key={course.id}>
                    <tr>
                      <td className={styles.courseTitle}>{course.title}</td>
                      <td><StatusBadge status={course.status} /></td>
                      <td>{course.totalModules}</td>
                      <td>{course.totalLessons}</td>
                      <td className={styles.date}>{new Date(course.createdAt).toLocaleDateString()}</td>
                      <td className={styles.actions}>
                        <Link to={`/instructor/courses/${course.id}`} className={styles.actionBtn} title="Edit">
                          <Edit3 size={16} />
                        </Link>
                        
                        {course.status === 'DRAFT' && (
                          <button 
                            className={`${styles.actionBtn} ${styles.publishBtn}`} 
                            onClick={() => handlePublish(course.id)}
                            disabled={publishMutation.isPending}
                            title="Publish"
                          >
                            <Send size={16} />
                          </button>
                        )}
                        
                        {course.status === 'PUBLISHED' && (
                          <button className={styles.actionBtn} title="Archive">
                            <Archive size={16} />
                          </button>
                        )}

                        <button 
                          className={`${styles.actionBtn} ${styles.deleteBtn}`} 
                          onClick={() => handleDelete(course.id, course.title)}
                          title="Delete"
                        >
                          <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                    {publishMutation.error?.response?.data?.courseId === course.id && (
                      <tr className={styles.errorRow}>
                        <td colSpan={6}>
                          <div className={styles.publishError}>
                            ⚠ {publishMutation.error.response.data.message}
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))
              )}
            </tbody>
          </table>
          {!isLoading && courses?.length === 0 && (
            <div className={styles.empty}>
              You haven't created any courses yet.
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
