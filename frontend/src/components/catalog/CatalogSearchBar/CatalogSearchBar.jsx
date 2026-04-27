import React from 'react';
import { Search } from 'lucide-react';
import styles from './CatalogSearchBar.module.css';

export default function CatalogSearchBar({ value, onChange }) {
  return (
    <div className={styles.container}>
      <Search className={styles.icon} size={20} />
      <input
        type="text"
        className={styles.input}
        placeholder="Search for courses..."
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}
