import React, { useEffect, useState } from 'react';
import useExamStore from '../../store/examStore';
import { Plus, Filter, Search, MoreVertical, Edit2, Trash2, Tag as TagIcon } from 'lucide-react';
import Spinner from '../../../../components/ui/Spinner/Spinner';

const QuestionBank = () => {
  const { questions, fetchQuestions, isLoading, tags, fetchTags } = useExamStore();
  const [filter, setFilter] = useState({ type: '', difficulty: '', tag: '', search: '' });

  // Load tags only once on mount
  useEffect(() => {
    fetchTags();
  }, [fetchTags]);

  // Debounce API calls for questions
  useEffect(() => {
    const handler = setTimeout(() => {
      fetchQuestions(filter);
    }, 300);

    return () => clearTimeout(handler);
  }, [filter, fetchQuestions]);

  const handleFilterChange = (e) => {
    setFilter({ ...filter, [e.target.name]: e.target.value });
  };

  return (
    <div className="dashboard dashboard--wide animate-page-enter">
      <header className="dashboard-hero panel">
        <div className="dashboard-hero__content">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h1 className="dashboard-hero__greeting">Question Bank</h1>
              <p className="dashboard-hero__subtitle">Manage your repository of questions across different types and difficulties.</p>
            </div>
            <button className="btn btn-primary">
              <Plus size={18} />
              <span>Create Question</span>
            </button>
          </div>
        </div>
      </header>

      <section className="panel" style={{ display: 'flex', gap: 'var(--space-4)', alignItems: 'center', padding: 'var(--space-3)' }}>
        <div style={{ position: 'relative', flex: 1 }}>
          <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)' }} />
          <input 
            name="search"
            placeholder="Search titles..." 
            style={{ width: '100%', paddingLeft: '36px' }} 
            value={filter.search}
            onChange={handleFilterChange}
          />
        </div>
        <select name="type" value={filter.type} onChange={handleFilterChange} style={{ width: '160px' }}>
          <option value="">All Types</option>
          <option value="MCQ_SINGLE">MCQ Single</option>
          <option value="MCQ_MULTI">MCQ Multi</option>
          <option value="TRUE_FALSE">True/False</option>
          <option value="FILL_BLANK">Fill Blanks</option>
          <option value="MATCH">Match</option>
          <option value="SEQUENCE">Sequence</option>
          <option value="CODING">Coding</option>
          <option value="SUBJECTIVE">Subjective</option>
        </select>
        <select name="difficulty" value={filter.difficulty} onChange={handleFilterChange} style={{ width: '140px' }}>
          <option value="">All Difficulty</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
        <select name="tag" value={filter.tag} onChange={handleFilterChange} style={{ width: '140px' }}>
          <option value="">All Tags</option>
          {tags.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
      </section>

      {isLoading ? (
        <div style={{ display: 'grid', placeItems: 'center', height: '200px' }}>
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="dashboard-cards">
          {questions.length === 0 ? (
            <div className="empty-state">
              <Search className="empty-state__icon" />
              <h3 className="empty-state__title">No questions found</h3>
              <p className="empty-state__text">Try adjusting your filters or create a new question to get started.</p>
            </div>
          ) : (
            questions.map(q => (
              <div key={q.id} className="panel course-card" style={{ cursor: 'default' }}>
                <div className="course-card__eyebrow">
                  <span className="course-card__badge">{q.type.replace('_', ' ')}</span>
                  <span className={`course-card__meta ${q.difficulty.toLowerCase()}`}>{q.difficulty}</span>
                </div>
                <div className="course-card__heading">
                  <h3 className="course-card__title" style={{ fontSize: 'var(--text-lg)' }}>{q.title}</h3>
                </div>
                <div className="course-card__topics">
                  {q.tags && q.tags.map(tag => (
                    <span key={tag} className="course-card__topic">#{tag}</span>
                  ))}
                </div>
                <div className="course-card__footer">
                  <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
                    <button className="btn btn-secondary btn-sm" style={{ padding: '4px 8px' }}>
                      <Edit2 size={14} />
                    </button>
                    <button className="btn btn-secondary btn-sm" style={{ padding: '4px 8px', color: 'var(--color-accent)' }}>
                      <Trash2 size={14} />
                    </button>
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

export default QuestionBank;
