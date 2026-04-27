import React from 'react';
import { FileText, Link as LinkIcon, FileCheck } from 'lucide-react';

export default function LessonIcon({ type, size = 16 }) {
  switch (type) {
    case 'TEXT':
      return <FileText size={size} />;
    case 'LINK':
      return <LinkIcon size={size} />;
    case 'ASSIGNMENT':
      return <FileCheck size={size} />;
    default:
      return <FileText size={size} />;
  }
}
