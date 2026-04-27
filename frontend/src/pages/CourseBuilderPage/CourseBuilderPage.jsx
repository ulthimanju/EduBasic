import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  useCourseDetail, 
  useCreateCourse, 
  useUpdateCourse, 
  useAddModule, 
  useDeleteModule,
  useAddLesson,
  useDeleteLesson,
  useLinkExam,
  useUnlinkExam,
  usePublishCourse
} from '../../hooks/useInstructor';
import useUiStore from '../../stores/uiStore';
import Navbar from '../../components/layout/Navbar/Navbar';
import ModuleCard from '../../components/instructor/ModuleCard/ModuleCard';
import LinkExamModal from '../../components/instructor/LinkExamModal/LinkExamModal';
import Spinner from '../../components/common/Spinner/Spinner';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';
import { Save, Send, Plus, ClipboardList, Trash2, ExternalLink } from 'lucide-react';
import styles from './CourseBuilderPage.module.css';

export default function CourseBuilderPage() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const isNew = !courseId;

  const { data: course, isLoading, isError } = useCourseDetail(courseId);
  const createMutation = useCreateCourse();
  const updateMutation = useUpdateCourse(courseId);
  const addModuleMutation = useAddModule(courseId);
  const linkExamMutation = useLinkExam(courseId);
  const unlinkExamMutation = useUnlinkExam(courseId);
  const publishMutation = usePublishCourse();

  const openConfirmModal = useUiStore(s => s.openConfirmModal);
  const closeConfirmModal = useUiStore(s => s.closeConfirmModal);

  const [metadata, setMetadata] = useState({
    title: '',
    description: '',
    thumbnailUrl: '',
    requireAllLessons: true,
    requireAllExams: true,
    minPassPercentage: 70
  });

  const [showExamModal, setShowExamModal] = useState(false);

  useEffect(() => {
    if (course) {
      setMetadata({
        title: course.title || '',
        description: course.description || '',
        thumbnailUrl: course.thumbnailUrl || '',
        requireAllLessons: course.completionRules?.requireAllLessons ?? true,
        requireAllExams: course.completionRules?.requireAllExams ?? true,
        minPassPercentage: course.completionRules?.minPassPercentage ?? 70
      });
    }
  }, [course]);

  const handleSaveMetadata = async () => {
    try {
      if (isNew) {
        const newCourse = await createMutation.mutateAsync(metadata);
        navigate(`/instructor/courses/${newCourse.id}`, { replace: true });
      } else {
        await updateMutation.mutateAsync(metadata);
      }
    } catch (err) {
      console.error('Failed to save metadata', err);
    }
  };

  const handleAddModule = () => {
    const title = prompt('Enter module title:');
    if (title) {
      addModuleMutation.mutate({ title, orderIndex: (course?.modules?.length || 0) });
    }
  };

  const handleLinkExam = (data) => {
    linkExamMutation.mutate(data, {
      onSuccess: () => setShowExamModal(false)
    });
  };

  const handleUnlinkExam = (examId, title) => {
    openConfirmModal({
      title: 'Unlink Exam',
      message: `Are you sure you want to remove "${title}" from this course?`,
      confirmLabel: 'Remove',
      isDangerous: true,
      onConfirm: async () => {
        await unlinkExamMutation.mutateAsync(examId);
        closeConfirmModal();
      },
      onCancel: closeConfirmModal
    });
  };

  if (isLoading && !isNew) return <div className={styles.loading}><Spinner size="lg" /></div>;
  if (isError) return <ErrorBanner message="Failed to load course details" />;

  return (
    <div className={styles.container}>
      <Navbar />
      <div className={styles.layout}>
        {/* Left Panel: Metadata */}
        <aside className={`${styles.panel} ${styles.leftPanel}`}>
          <h2 className={styles.panelTitle}>Course Details</h2>
          <div className={styles.formGroup}>
            <label>Title</label>
            <input 
              type="text" 
              value={metadata.title} 
              onChange={e => setMetadata(m => ({ ...m, title: e.target.value }))}
              placeholder="e.g. Advanced Java Patterns"
            />
          </div>
          <div className={styles.formGroup}>
            <label>Description</label>
            <textarea 
              value={metadata.description} 
              onChange={e => setMetadata(m => ({ ...m, description: e.target.value }))}
              placeholder="What will students learn?"
              rows={4}
            />
          </div>
          <div className={styles.formGroup}>
            <label>Thumbnail URL</label>
            <input 
              type="text" 
              value={metadata.thumbnailUrl} 
              onChange={e => setMetadata(m => ({ ...m, thumbnailUrl: e.target.value }))}
              placeholder="https://..."
            />
            {metadata.thumbnailUrl && (
              <img src={metadata.thumbnailUrl} alt="Preview" className={styles.thumbPreview} />
            )}
          </div>

          <div className={styles.rulesSection}>
            <h3 className={styles.subTitle}>Completion Rules</h3>
            <label className={styles.checkboxLabel}>
              <input 
                type="checkbox" 
                checked={metadata.requireAllLessons}
                onChange={e => setMetadata(m => ({ ...m, requireAllLessons: e.target.checked }))}
              />
              Require all lessons
            </label>
            <label className={styles.checkboxLabel}>
              <input 
                type="checkbox" 
                checked={metadata.requireAllExams}
                onChange={e => setMetadata(m => ({ ...m, requireAllExams: e.target.checked }))}
              />
              Require all exams
            </label>
            <div className={styles.formGroupInline}>
              <label>Min Pass %</label>
              <input 
                type="number" 
                value={metadata.minPassPercentage}
                onChange={e => setMetadata(m => ({ ...m, minPassPercentage: parseInt(e.target.value) }))}
                min="0" max="100"
              />
            </div>
          </div>

          <div className={styles.metadataActions}>
            <button 
              className={styles.saveBtn} 
              onClick={handleSaveMetadata}
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              <Save size={18} />
              {isNew ? 'Create Course' : 'Save Changes'}
            </button>
            {!isNew && (
              <button 
                className={styles.publishBtn} 
                onClick={() => publishMutation.mutate(courseId)}
                disabled={publishMutation.isPending || course.status === 'PUBLISHED'}
              >
                <Send size={18} />
                Publish
              </button>
            )}
            {publishMutation.isError && (
              <div className={styles.inlineError}>
                ⚠ {publishMutation.error?.response?.data?.message || 'Publish failed'}
              </div>
            )}
          </div>
        </aside>

        {/* Center Panel: Module Builder */}
        <main className={`${styles.panel} ${styles.centerPanel}`}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>Curriculum</h2>
            <button className={styles.addBtn} onClick={handleAddModule} disabled={isNew}>
              <Plus size={18} /> Add Module
            </button>
          </div>

          {isNew ? (
            <div className={styles.emptyCurriculum}>
              Save the course metadata first to start building the curriculum.
            </div>
          ) : (
            <div className={styles.moduleList}>
              {course.modules?.map((module, idx) => (
                <ModuleCard 
                  key={module.id} 
                  module={module} 
                  courseId={courseId} 
                  isFirst={idx === 0}
                  isLast={idx === (course.modules.length - 1)}
                />
              ))}
              {course.modules?.length === 0 && (
                <div className={styles.emptyState}>No modules added yet.</div>
              )}
            </div>
          )}
        </main>

        {/* Right Panel: Exam Linker */}
        <aside className={`${styles.panel} ${styles.rightPanel}`}>
          <div className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>Linked Exams</h2>
            <button 
              className={styles.iconBtn} 
              onClick={() => setShowExamModal(true)}
              disabled={isNew}
              title="Link Exam"
            >
              <Plus size={18} />
            </button>
          </div>

          <div className={styles.examList}>
            {course?.exams?.map(exam => (
              <div key={exam.id} className={styles.examItem}>
                <div className={styles.examInfo}>
                  <ClipboardList size={16} />
                  <span className={styles.examTitle}>{exam.title}</span>
                </div>
                <div className={styles.examBadges}>
                  {exam.required && <span className={styles.badge}>Required</span>}
                  <span className={styles.badge}>{exam.minPassPercentage}% pass</span>
                </div>
                <div className={styles.examActions}>
                  <button onClick={() => handleUnlinkExam(exam.id, exam.title)} className={styles.unlinkBtn}>
                    <Trash2 size={14} /> Unlink
                  </button>
                </div>
              </div>
            ))}
            {!isNew && course?.exams?.length === 0 && (
              <div className={styles.emptyStateSmall}>No exams linked.</div>
            )}
          </div>
        </aside>
      </div>

      {showExamModal && (
        <LinkExamModal 
          onClose={() => setShowExamModal(false)} 
          onSave={handleLinkExam}
          isPending={linkExamMutation.isPending}
        />
      )}
    </div>
  );
}
