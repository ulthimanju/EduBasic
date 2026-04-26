import React from 'react';

const QuestionRenderer = ({ question, answer, onChange, disabled }) => {
  const { type, payload } = question;

  const handleMcqChange = (optionId) => {
    if (disabled) return;
    if (type === 'MCQ_SINGLE') {
      onChange(optionId);
    } else if (type === 'MCQ_MULTI') {
      const currentAnswers = Array.isArray(answer) ? answer : [];
      if (currentAnswers.includes(optionId)) {
        onChange(currentAnswers.filter(id => id !== optionId));
      } else {
        onChange([...currentAnswers, optionId]);
      }
    }
  };

  switch (type) {
    case 'MCQ_SINGLE':
    case 'MCQ_MULTI':
      return (
        <div className="options-container">
          {payload.options.map((opt, idx) => {
            const isSelected = type === 'MCQ_SINGLE' ? answer === opt.id : (Array.isArray(answer) && answer.includes(opt.id));
            return (
              <button
                key={opt.id}
                disabled={disabled}
                onClick={() => handleMcqChange(opt.id)}
                className={`option-item ${isSelected ? 'is-selected' : ''}`}
              >
                <span className="option-item__label">{String.fromCharCode(65 + idx)}</span>
                <span className="option-item__text">{opt.text}</span>
                <div className={type === 'MCQ_SINGLE' ? 'option-radio' : 'option-checkbox'}>
                  {isSelected && <div className="option-radio__dot" />}
                </div>
              </button>
            );
          })}
        </div>
      );

    case 'TRUE_FALSE':
      return (
        <div style={{ display: 'flex', gap: 'var(--space-4)' }}>
          {['true', 'false'].map(val => (
            <button
              key={val}
              disabled={disabled}
              onClick={() => onChange(val)}
              className={`btn ${answer === val ? 'btn-primary' : 'btn-secondary'}`}
              style={{ flex: 1, padding: 'var(--space-4)' }}
            >
              {val.toUpperCase()}
            </button>
          ))}
        </div>
      );

    case 'FILL_BLANK':
      return (
        <div style={{ display: 'grid', gap: 'var(--space-4)' }}>
          {payload.blanks.map(blank => (
            <div key={blank.id} style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
              <span style={{ minWidth: '80px', fontSize: 'var(--text-sm)' }}>Blank {blank.id}:</span>
              <input
                disabled={disabled}
                value={(answer && answer[blank.id]) || ''}
                onChange={(e) => {
                  const newAns = { ...(answer || {}) };
                  newAns[blank.id] = e.target.value;
                  onChange(newAns);
                }}
                placeholder="Type your answer here..."
                style={{ flex: 1 }}
              />
            </div>
          ))}
        </div>
      );

    case 'SUBJECTIVE':
      return (
        <div style={{ display: 'grid', gap: 'var(--space-2)' }}>
          <textarea
            disabled={disabled}
            value={answer || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder="Type your detailed response here..."
            rows={10}
            style={{ width: '100%', resize: 'vertical' }}
          />
          <div style={{ display: 'flex', justifyContent: 'flex-end', fontSize: 'var(--text-xs)', color: 'var(--color-text-muted)' }}>
            Max words: {payload.maxWords}
          </div>
        </div>
      );

    case 'CODING':
      return (
        <div style={{ display: 'grid', gap: 'var(--space-4)' }}>
          <div style={{ display: 'flex', gap: 'var(--space-2)' }}>
            {payload.languagesAllowed.map(lang => (
              <span key={lang} className="course-card__topic">{lang}</span>
            ))}
          </div>
          <textarea
            disabled={disabled}
            value={answer || payload.starterCode || ''}
            onChange={(e) => onChange(e.target.value)}
            style={{ 
              fontFamily: 'monospace', 
              backgroundColor: '#1e1e1e', 
              color: '#d4d4d4', 
              padding: 'var(--space-4)',
              minHeight: '300px'
            }}
          />
        </div>
      );

    default:
      return <div>Unsupported question type: {type}</div>;
  }
};

export default QuestionRenderer;
