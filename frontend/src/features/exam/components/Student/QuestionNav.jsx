import React from 'react';
import { List } from 'lucide-react';

export default function QuestionNav({ 
  questions, 
  currentQuestionIdx, 
  setCurrentQuestionIdx, 
  answers 
}) {
  return (
    <div className="exam-side-card panel">
      <h2 className="exam-side-card__title" style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
        <List size={18} />
        <span>Questions</span>
      </h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 'var(--space-2)', marginTop: 'var(--space-3)' }}>
        {questions.map((m, idx) => (
          <button
            key={m.id}
            className={`btn ${currentQuestionIdx === idx ? 'btn-primary' : answers[m.question.id] ? 'btn-secondary' : 'btn-ghost'}`}
            style={{ padding: '8px', minWidth: '0' }}
            onClick={() => setCurrentQuestionIdx(idx)}
          >
            {idx + 1}
          </button>
        ))}
      </div>
    </div>
  );
}
