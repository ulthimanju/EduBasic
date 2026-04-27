import React, { useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCourseOutline } from '../../hooks/useEnrollment';
import useUiStore from '../../stores/uiStore';
import Navbar from '../../components/layout/Navbar/Navbar';
import LessonSidebar from '../../components/player/LessonSidebar/LessonSidebar';
import LessonContent from '../../components/player/LessonContent/LessonContent';
import CompletionBanner from '../../components/player/CompletionBanner/CompletionBanner';
import Spinner from '../../components/common/Spinner/Spinner';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import styles from './LessonPlayerPage.module.css';

export default function LessonPlayerPage() {
  const { courseId, lessonId } = useParams();
  const navigate = useNavigate();
  const { data: outline, isLoading } = useCourseOutline(courseId);
  
  const { activeLessonId, setActiveLessonId, sidebarOpen } = useUiStore();

  const allLessons = useMemo(() => {
    if (!outline) return [];
    return outline.modules.flatMap(m => m.lessons);
  }, [outline]);

  useEffect(() => {
    if (lessonId) {
      setActiveLessonId(lessonId);
    } else if (allLessons.length > 0 && !activeLessonId) {
      setActiveLessonId(allLessons[0].id);
      navigate(`/courses/${courseId}/learn/${allLessons[0].id}`, { replace: true });
    }
  }, [lessonId, allLessons, activeLessonId, setActiveLessonId, navigate, courseId]);

  const activeLesson = useMemo(() => 
    allLessons.find(l => l.id === activeLessonId),
  [allLessons, activeLessonId]);

  const currentIndex = allLessons.findIndex(l => l.id === activeLessonId);
  const prevLesson = allLessons[currentIndex - 1];
  const nextLesson = allLessons[currentIndex + 1];

  const gotoPrev = () => {
    if (prevLesson) navigate(`/courses/${courseId}/learn/${prevLesson.id}`);
  };

  const gotoNext = () => {
    if (nextLesson) navigate(`/courses/${courseId}/learn/${nextLesson.id}`);
  };

  if (isLoading) return <div className={styles.loading}><Spinner size="lg" /></div>;
  if (!outline) return <div>Course not found</div>;

  return (
    <div className={styles.container}>
      <Navbar />
      <div className={styles.layout}>
        {sidebarOpen && <LessonSidebar outline={outline} courseId={courseId} />}
        
        <main className={styles.main}>
          <CompletionBanner courseId={courseId} />
          
          <div className={styles.contentWrapper}>
            {activeLesson ? (
              <>
                <LessonContent lesson={activeLesson} courseId={courseId} />
                <div className={styles.nav}>
                  <button 
                    className={styles.navBtn} 
                    onClick={gotoPrev} 
                    disabled={!prevLesson}
                  >
                    <ChevronLeft size={18} />
                    Previous
                  </button>
                  <button 
                    className={styles.navBtn} 
                    onClick={gotoNext} 
                    disabled={!nextLesson}
                  >
                    Next
                    <ChevronRight size={18} />
                  </button>
                </div>
              </>
            ) : (
              <div className={styles.noLesson}>Select a lesson to start learning</div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
