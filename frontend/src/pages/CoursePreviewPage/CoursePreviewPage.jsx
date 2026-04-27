import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCoursePreview, useEnroll } from '../../hooks/useCatalog';
import { useMyCourses } from '../../hooks/useEnrollment';
import Navbar from '../../components/layout/Navbar/Navbar';
import SkeletonCard from '../../components/common/SkeletonCard/SkeletonCard';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';
import LessonIcon from '../../components/common/LessonIcon/LessonIcon';
import { Lock, ChevronDown, ChevronUp, PlayCircle } from 'lucide-react';
import styles from './CoursePreviewPage.module.css';

export default function CoursePreviewPage() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const { data: course, isLoading, isError, refetch } = useCoursePreview(courseId);
  const { data: myCourses } = useMyCourses();
  const enrollMutation = useEnroll();

  const [expandedModules, setExpandedModules] = useState({});

  const isEnrolled = myCourses?.some(c => c.id === courseId);

  const toggleModule = (moduleId) => {
    setExpandedModules(prev => ({ ...prev, [moduleId]: !prev[moduleId] }));
  };

  const handleEnroll = async () => {
    try {
      await enrollMutation.mutateAsync(courseId);
      navigate(`/courses/${courseId}/learn`);
    } catch (err) {
      console.error('Enrollment failed', err);
    }
  };

  if (isLoading) return <div className={styles.loading}><SkeletonCard /></div>;
  if (isError) return <Navbar /><ErrorBanner onRetry={refetch} />;

  return (
    <div className={styles.container}>
      <Navbar />
      <main className={styles.layout}>
        <section className={styles.content}>
          <h1 className={styles.title}>{course.title}</h1>
          <p className={styles.description}>{course.description}</p>

          <div className={styles.curriculum}>
            <h2 className={styles.sectionTitle}>Course Content</h2>
            <div className={styles.modules}>
              {course.modules?.map(module => (
                <div key={module.id} className={styles.module}>
                  <button className={styles.moduleHeader} onClick={() => toggleModule(module.id)}>
                    {expandedModules[module.id] ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                    <span className={styles.moduleTitle}>{module.title}</span>
                    <span className={styles.moduleMeta}>{module.lessons?.length} lessons</span>
                  </button>
                  
                  {expandedModules[module.id] && (
                    <div className={styles.lessons}>
                      {module.lessons?.map(lesson => (
                        <div key={lesson.id} className={`${styles.lesson} ${lesson.preview ? styles.previewable : styles.locked}`}>
                          <LessonIcon type={lesson.contentType} />
                          <span className={styles.lessonTitle}>{lesson.title}</span>
                          {lesson.preview ? (
                            <span className={styles.previewLabel}>Preview</span>
                          ) : (
                            <Lock size={14} className={styles.lockIcon} />
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>

        <aside className={styles.sidebar}>
          <div className={styles.enrollCard}>
            <div className={styles.previewThumb}>
              {course.thumbnailUrl ? (
                <img src={course.thumbnailUrl} alt={course.title} />
              ) : (
                <div className={styles.placeholder} />
              )}
            </div>
            
            <div className={styles.stats}>
              <div className={styles.stat}>
                <span className={styles.statValue}>{course.totalLessons}</span>
                <span className={styles.statLabel}>Lessons</span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statValue}>{course.totalModules}</span>
                <span className={styles.statLabel}>Modules</span>
              </div>
            </div>

            {isEnrolled ? (
              <button 
                className={styles.enrollBtn} 
                onClick={() => navigate(`/courses/${courseId}/learn`)}
              >
                <PlayCircle size={18} />
                Continue Learning
              </button>
            ) : (
              <button 
                className={styles.enrollBtn} 
                onClick={handleEnroll}
                disabled={enrollMutation.isPending}
              >
                {enrollMutation.isPending ? 'Enrolling...' : 'Enroll Now'}
              </button>
            )}
          </div>
        </aside>
      </main>
    </div>
  );
}
