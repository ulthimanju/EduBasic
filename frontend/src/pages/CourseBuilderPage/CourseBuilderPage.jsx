import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  useCourseDetail, 
  useCreateCourse, 
  useUpdateCourse, 
  useAddModule, 
  useLinkExam,
  useUnlinkExam,
  usePublishCourse
} from '../../hooks/useInstructor';
import useUiStore from '../../stores/uiStore';
import Navbar from '../../components/layout/Navbar/Navbar';
import LinkExamModal from '../../components/instructor/LinkExamModal/LinkExamModal';
import Spinner from '../../components/common/Spinner/Spinner';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';

import CourseMetaForm from './components/CourseMetaForm';
import ModuleList from './components/ModuleList';
import ExamLinker from './components/ExamLinker';

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
        <CourseMetaForm 
          metadata={metadata}
          setMetadata={setMetadata}
          onSave={handleSaveMetadata}
          onPublish={() => publishMutation.mutate(courseId)}
          isNew={isNew}
          isSaving={createMutation.isPending || updateMutation.isPending}
          isPublishing={publishMutation.isPending}
          courseStatus={course?.status}
          publishError={publishMutation.error}
        />

        <ModuleList 
          modules={course?.modules}
          onAddModule={handleAddModule}
          isNew={isNew}
          courseId={courseId}
        />

        <ExamLinker 
          exams={course?.exams}
          onLink={() => setShowExamModal(true)}
          onUnlink={handleUnlinkExam}
          isNew={isNew}
        />
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
