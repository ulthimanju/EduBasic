import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useExamStore from '../../store/examStore';
import examApi from '../../../../api/exam';
import { Trophy, Home, AlertCircle, FileCheck } from 'lucide-react';
import Spinner from '../../../../components/common/Spinner/Spinner';
import { ROUTES } from '../../../../constants/appConstants';

const ResultPage = () => {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchResult = async () => {
      try {
        const response = await examApi.getResult(attemptId);
        setResult(response.data);
      } catch (err) {
        console.error('Failed to fetch result', err);
      } finally {
        setLoading(false);
      }
    };
    fetchResult();
  }, [attemptId]);

  if (loading) return <Spinner />;

  if (!result) {
    return (
      <div className="exam-state-screen">
        <div className="exam-state-card panel">
          <AlertCircle size={48} color="var(--color-accent)" />
          <h1 className="exam-state-card__title">Evaluation in Progress</h1>
          <p className="exam-state-card__description">
            Your exam has been submitted successfully. Some questions might require manual grading. 
            Check back later for your final score.
          </p>
          <button className="btn btn-primary" onClick={() => navigate(ROUTES.DASHBOARD)}>
            <Home size={18} />
            Return Home
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard animate-page-enter">
      <header className="dashboard-hero panel" style={{ textAlign: 'center' }}>
        <div style={{ display: 'grid', justifyItems: 'center', gap: 'var(--space-4)' }}>
          <div className="exam-state-card__icon" style={{ width: '80px', height: '80px' }}>
            <Trophy size={40} />
          </div>
          <h1 className="dashboard-hero__greeting">Exam Completed!</h1>
          <div style={{ display: 'flex', gap: 'var(--space-8)', marginTop: 'var(--space-2)' }}>
            <div style={{ textAlign: 'center' }}>
              <span className="exam-meta-label">Total Score</span>
              <div className="course-select__stat-value">{result.totalScore}</div>
            </div>
            <div style={{ textAlign: 'center' }}>
              <span className="exam-meta-label">Status</span>
              <div className={`course-card__badge ${result.status.toLowerCase()}`} style={{ fontSize: 'var(--text-lg)' }}>
                {result.status.replace('_', ' ')}
              </div>
            </div>
          </div>
        </div>
      </header>

      <div className="panel" style={{ marginTop: 'var(--space-4)' }}>
        <h2 className="exam-side-card__title" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <FileCheck size={20} />
          <span>Evaluation Summary</span>
        </h2>
        <div className="divider" style={{ margin: 'var(--space-3) 0' }} />
        <p className="course-card__description">
          Your attempt is currently being processed. If manual grading was required, your score will be updated 
          once an instructor completes the review.
        </p>
        <div style={{ marginTop: 'var(--space-6)', display: 'flex', justifyContent: 'center' }}>
          <button className="btn btn-primary" onClick={() => navigate(ROUTES.DASHBOARD)}>
            <Home size={18} />
            Return to Dashboard
          </button>
        </div>
      </div>
    </div>
  );
};

export default ResultPage;
