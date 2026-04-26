import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useExamStore from '../../store/examStore';
import { Plus, Layout, Clock, CheckCircle, FileText, Trash2 } from 'lucide-react';
import Spinner from '../../../../components/ui/Spinner/Spinner';
import { ROUTES } from '../../../../constants/appConstants';

const ExamBuilder = () => {
  const navigate = useNavigate();
  const { exams, fetchExams, isLoading } = useExamStore();
  const [filter, setFilter] = useState({ status: '' });

  useEffect(() => {
    fetchExams(filter);
  }, [filter]);

  return (
    <div className="dashboard dashboard--wide animate-page-enter">
      <header className="dashboard-hero panel">
        <div className="dashboard-hero__content">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h1 className="dashboard-hero__greeting">Exam Builder</h1>
              <p className="dashboard-hero__subtitle">Create and manage your exams, configure rules, and map questions.</p>
            </div>
            <button className="btn btn-primary">
              <Plus size={18} />
              <span>New Exam</span>
            </button>
          </div>
        </div>
      </header>

      <section className="panel" style={{ display: 'flex', gap: 'var(--space-4)', alignItems: 'center', padding: 'var(--space-3)' }}>
        <Layout size={18} style={{ marginLeft: '12px', color: 'var(--color-text-muted)' }} />
        <span style={{ fontWeight: 500, fontSize: 'var(--text-sm)' }}>Filter by Status:</span>
        <select 
          name="status" 
          value={filter.status} 
          onChange={(e) => setFilter({ status: e.target.value })} 
          style={{ width: '160px' }}
        >
          <option value="">All Status</option>
          <option value="DRAFT">Draft</option>
          <option value="PUBLISHED">Published</option>
          <option value="ARCHIVED">Archived</option>
        </select>
      </section>

      {isLoading ? (
        <div style={{ display: 'grid', placeItems: 'center', height: '200px' }}>
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="dashboard-cards" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(340px, 1fr))' }}>
          {exams.length === 0 ? (
            <div className="empty-state">
              <FileText className="empty-state__icon" />
              <h3 className="empty-state__title">No exams created</h3>
              <p className="empty-state__text">Start by creating your first exam draft.</p>
            </div>
          ) : (
            exams.map(exam => (
              <div 
                key={exam.id} 
                className="panel course-card" 
                onClick={() => navigate(ROUTES.EXAM_DETAIL.replace(':examId', exam.id))}
                style={{ cursor: 'pointer' }}
              >
                <div className="course-card__eyebrow">
                  <span className={`course-card__badge ${exam.status.toLowerCase()}`}>{exam.status}</span>
                  <span className="course-card__meta">
                    {exam.hasSections ? 'Sectioned' : 'Flat List'}
                  </span>
                </div>
                <div className="course-card__heading">
                  <h3 className="course-card__title">{exam.title}</h3>
                </div>
                <div className="course-card__footer">
                  <div style={{ display: 'flex', gap: 'var(--space-4)', color: 'var(--color-text-secondary)', fontSize: 'var(--text-xs)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-1)' }}>
                      <Clock size={14} />
                      <span>{exam.timeLimitMins || 'No limit'} mins</span>
                    </div>
                    {exam.passMarks && (
                      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-1)' }}>
                        <CheckCircle size={14} />
                        <span>Pass: {exam.passMarks}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

export default ExamBuilder;
