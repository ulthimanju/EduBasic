import React from 'react';
import { Save, Send } from 'lucide-react';
import FormField from '../../../components/common/FormField/FormField';
import styles from '../CourseBuilderPage.module.css';

export default function CourseMetaForm({ 
  metadata, 
  setMetadata, 
  onSave, 
  onPublish, 
  isNew, 
  isSaving, 
  isPublishing,
  courseStatus,
  publishError
}) {
  return (
    <aside className={`${styles.panel} ${styles.leftPanel}`}>
      <h2 className={styles.panelTitle}>Course Details</h2>
      
      <FormField label="Title" required>
        <input 
          type="text" 
          value={metadata.title} 
          onChange={e => setMetadata(m => ({ ...m, title: e.target.value }))}
          placeholder="e.g. Advanced Java Patterns"
        />
      </FormField>

      <FormField label="Description">
        <textarea 
          value={metadata.description} 
          onChange={e => setMetadata(m => ({ ...m, description: e.target.value }))}
          placeholder="What will students learn?"
          rows={4}
        />
      </FormField>

      <FormField label="Thumbnail URL">
        <input 
          type="text" 
          value={metadata.thumbnailUrl} 
          onChange={e => setMetadata(m => ({ ...m, thumbnailUrl: e.target.value }))}
          placeholder="https://..."
        />
        {metadata.thumbnailUrl && (
          <img src={metadata.thumbnailUrl} alt="Preview" className={styles.thumbPreview} />
        )}
      </FormField>

      <div className={styles.rulesSection}>
        <h3 className={styles.subTitle}>Completion Rules</h3>
        <label className={styles.checkboxLabel}>
          <input 
            type="checkbox" 
            checked={metadata.requireAllLessons}
            onChange={e => setMetadata(m => ({ ...m, requireAllLessons: e.target.checked }))}
          />
          Require all lessons
        </label>
        <label className={styles.checkboxLabel}>
          <input 
            type="checkbox" 
            checked={metadata.requireAllExams}
            onChange={e => setMetadata(m => ({ ...m, requireAllExams: e.target.checked }))}
          />
          Require all exams
        </label>
        
        <FormField label="Min Pass %" className={styles.formGroupInline}>
          <input 
            type="number" 
            value={metadata.minPassPercentage}
            onChange={e => setMetadata(m => ({ ...m, minPassPercentage: parseInt(e.target.value) }))}
            min="0" max="100"
          />
        </FormField>
      </div>

      <div className={styles.metadataActions}>
        <button 
          className={styles.saveBtn} 
          onClick={onSave}
          disabled={isSaving}
        >
          <Save size={18} />
          {isNew ? 'Create Course' : 'Save Changes'}
        </button>
        {!isNew && (
          <button 
            className={styles.publishBtn} 
            onClick={onPublish}
            disabled={isPublishing || courseStatus === 'PUBLISHED'}
          >
            <Send size={18} />
            Publish
          </button>
        )}
        {publishError && (
          <div className={styles.inlineError}>
            ⚠ {publishError?.response?.data?.message || 'Publish failed'}
          </div>
        )}
      </div>
    </aside>
  );
}
