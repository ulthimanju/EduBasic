import React, { useState } from 'react';
import { X } from 'lucide-react';
import styles from './LinkExamModal.module.css';

export default function LinkExamModal({ onClose, onSave, isPending }) {
  const [data, setData] = useState({
    examId: '',
    title: '',
    minPassPercentage: 70,
    required: true,
    orderIndex: 0
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (data.examId && data.title) {
      onSave(data);
    }
  };

  return (
    <div className={styles.overlay}>
      <div className={styles.modal}>
        <header className={styles.header}>
          <h3>Link Assessment</h3>
          <button onClick={onClose}><X size={20} /></button>
        </header>

        <form onSubmit={handleSubmit} className={styles.form}>
          <div className={styles.formGroup}>
            <label>Exam ID (UUID)</label>
            <input 
              type="text" 
              value={data.examId}
              onChange={e => setData(d => ({ ...d, examId: e.target.value }))}
              placeholder="00000000-0000-0000-0000-000000000000"
              required
            />
          </div>

          <div className={styles.formGroup}>
            <label>Display Title</label>
            <input 
              type="text" 
              value={data.title}
              onChange={e => setData(d => ({ ...d, title: e.target.value }))}
              placeholder="e.g. Final Certification Exam"
              required
            />
          </div>

          <div className={styles.row}>
            <div className={styles.formGroup}>
              <label>Min Pass %</label>
              <input 
                type="number" 
                value={data.minPassPercentage}
                onChange={e => setData(d => ({ ...d, minPassPercentage: parseInt(e.target.value) }))}
                min="0" max="100"
              />
            </div>
            <div className={styles.formGroup}>
              <label>Order Index</label>
              <input 
                type="number" 
                value={data.orderIndex}
                onChange={e => setData(d => ({ ...d, orderIndex: parseInt(e.target.value) }))}
              />
            </div>
          </div>

          <label className={styles.checkboxLabel}>
            <input 
              type="checkbox" 
              checked={data.required}
              onChange={e => setData(d => ({ ...d, required: e.target.checked }))}
            />
            Required to complete course
          </label>

          <div className={styles.actions}>
            <button type="button" onClick={onClose} className={styles.cancelBtn}>Cancel</button>
            <button type="submit" className={styles.saveBtn} disabled={isPending}>
              {isPending ? 'Linking...' : 'Link Exam'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
