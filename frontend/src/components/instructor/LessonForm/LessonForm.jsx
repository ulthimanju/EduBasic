import React, { useState } from 'react';
import { ExternalLink } from 'lucide-react';
import styles from './LessonForm.module.css';

export default function LessonForm({ onSave, onCancel, isPending }) {
  const [data, setData] = useState({
    title: '',
    contentType: 'TEXT',
    contentBody: '',
    contentUrl: '',
    duration: 15,
    preview: false
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (data.title) onSave(data);
  };

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <div className={styles.formGroup}>
        <label>Lesson Title</label>
        <input 
          type="text" 
          value={data.title} 
          onChange={e => setData(d => ({ ...d, title: e.target.value }))}
          placeholder="e.g. Introduction to Streams"
          required
        />
      </div>

      <div className={styles.typeGroup}>
        {['TEXT', 'LINK', 'ASSIGNMENT'].map(type => (
          <button
            key={type}
            type="button"
            className={`${styles.typeBtn} ${data.contentType === type ? styles.active : ''}`}
            onClick={() => setData(d => ({ ...d, contentType: type }))}
          >
            {type}
          </button>
        ))}
      </div>

      {data.contentType === 'TEXT' && (
        <div className={styles.formGroup}>
          <label>Content (HTML supported)</label>
          <textarea 
            value={data.contentBody}
            onChange={e => setData(d => ({ ...d, contentBody: e.target.value }))}
            rows={6}
            placeholder="<p>Welcome to this lesson...</p>"
          />
        </div>
      )}

      {(data.contentType === 'LINK' || data.contentType === 'ASSIGNMENT') && (
        <div className={styles.formGroup}>
          <label>{data.contentType === 'LINK' ? 'Video/External URL' : 'Submission URL'}</label>
          <div className={styles.inputWithLink}>
            <input 
              type="text" 
              value={data.contentUrl}
              onChange={e => setData(d => ({ ...d, contentUrl: e.target.value }))}
              placeholder="https://..."
            />
            {data.contentUrl && (
              <a href={data.contentUrl} target="_blank" rel="noopener noreferrer">
                <ExternalLink size={16} />
              </a>
            )}
          </div>
        </div>
      )}

      {data.contentType === 'ASSIGNMENT' && (
        <div className={styles.formGroup}>
          <label>Instructions</label>
          <textarea 
            value={data.contentBody}
            onChange={e => setData(d => ({ ...d, contentBody: e.target.value }))}
            rows={3}
            placeholder="What should the student do?"
          />
        </div>
      )}

      <div className={styles.footer}>
        <div className={styles.metaFields}>
          <div className={styles.formGroupInline}>
            <label>Duration (min)</label>
            <input 
              type="number" 
              value={data.duration}
              onChange={e => setData(d => ({ ...d, duration: parseInt(e.target.value) }))}
            />
          </div>
          <label className={styles.checkboxLabel}>
            <input 
              type="checkbox" 
              checked={data.preview}
              onChange={e => setData(d => ({ ...d, preview: e.target.checked }))}
            />
            Preview lesson
          </label>
        </div>
        
        <div className={styles.actions}>
          <button type="button" onClick={onCancel} className={styles.cancelBtn}>Cancel</button>
          <button type="submit" className={styles.saveBtn} disabled={isPending}>
            {isPending ? 'Saving...' : 'Save Lesson'}
          </button>
        </div>
      </div>
    </form>
  );
}
