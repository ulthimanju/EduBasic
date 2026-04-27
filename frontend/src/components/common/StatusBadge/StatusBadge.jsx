import React from 'react';
import styles from './StatusBadge.module.css';

const STATUS_MAP = {
  PUBLISHED:   { label: 'Published',   cls: 'success' },
  COMPLETED:   { label: 'Completed',   cls: 'success' },
  ACTIVE:      { label: 'Active',      cls: 'info'    },
  DRAFT:       { label: 'Draft',       cls: 'neutral' },
  NOT_STARTED: { label: 'Not Started', cls: 'neutral' },
  IN_PROGRESS: { label: 'In Progress', cls: 'info'    },
  ARCHIVED:    { label: 'Archived',    cls: 'warning' },
  DROPPED:     { label: 'Dropped',     cls: 'warning' },
};

export default function StatusBadge({ status }) {
  const { label, cls } = STATUS_MAP[status] ?? { label: status, cls: 'neutral' };
  return <span className={`${styles.badge} ${styles[cls]}`}>{label}</span>;
}
