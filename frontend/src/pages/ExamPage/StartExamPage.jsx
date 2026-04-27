import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useExamStore from '../../features/exam/store/examStore';
import Navbar from '../../components/layout/Navbar/Navbar';
import Spinner from '../../components/common/Spinner/Spinner';
import ErrorBanner from '../../components/common/ErrorBanner/ErrorBanner';
import { PlayCircle, Clock, FileText, AlertTriangle } from 'lucide-react';
import { ROUTES } from '../../constants/appConstants';
import styles from './ExamPage.module.css';

export default function StartExamPage() {
  const { examId } = useParams();
  const navigate = useNavigate();
  const { fetchExam, startAttempt, currentExam, isLoading, error } = useExamStore();
  const [isStarting, setIsStarting] = useState(false);

  useEffect(() => {
    if (examId) {
      fetchExam(examId);
    }
  }, [examId, fetchExam]);

  const handleStart = async () => {
    setIsStarting(true);
    try {
      const attempt = await startAttempt(examId);
      navigate(ROUTES.EXAM.replace(':attemptId', attempt.id));
    } catch (err) {
      console.error('Failed to start exam', err);
      setIsStarting(false);
    }
  };

  if (isLoading) return <div className={styles.loading}><Spinner size="lg" /></div>;
  if (error) return <Navbar /><ErrorBanner message={error} />;
  if (!currentExam) return <Navbar /><div>Exam not found</div>;

  return (
    <div className={styles.container}>
      <Navbar />
      <main className={styles.main}>
        <div className={styles.startCard}>
          <header className={styles.startHeader}>
            <h1 className={styles.title}>{currentExam.title}</h1>
            <p className={styles.subtitle}>{currentExam.description}</p>
          </header>

          <div className={styles.detailsGrid}>
            <div className={styles.detailItem}>
              <Clock size={20} />
              <div>
                <strong>Duration</strong>
                <span>{currentExam.timeLimitMins || 'No limit'} minutes</span>
              </div>
            </div>
            <div className={styles.detailItem}>
              <FileText size={20} />
              <div>
                <strong>Questions</strong>
                <span>{currentExam.totalQuestions} total questions</span>
              </div>
            </div>
          </div>

          <div className={styles.warningBox}>
            <AlertTriangle size={20} />
            <div>
              <strong>Proctoring Enabled</strong>
              <p>This exam is monitored. Switching tabs or leaving fullscreen may result in automatic submission.</p>
            </div>
          </div>

          <button 
            className={styles.startBtn} 
            onClick={handleStart}
            disabled={isStarting}
          >
            {isStarting ? (
              <Spinner size="sm" />
            ) : (
              <>
                <PlayCircle size={20} />
                Begin Exam
              </>
            )}
          </button>
        </div>
      </main>
    </div>
  );
}
