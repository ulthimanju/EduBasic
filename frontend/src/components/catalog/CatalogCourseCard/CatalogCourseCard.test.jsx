import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import CatalogCourseCard from './CatalogCourseCard';
import { expect, test } from 'vitest';

const mockCourse = {
  id: '123',
  title: 'Java Patterns',
  description: 'Learn Java patterns',
  thumbnailUrl: null,
  totalModules: 2,
  totalLessons: 10
};

test('renders course info correctly', () => {
  render(
    <BrowserRouter>
      <CatalogCourseCard course={mockCourse} />
    </BrowserRouter>
  );

  expect(screen.getByText('Java Patterns')).toBeInTheDocument();
  expect(screen.getByText('Learn Java patterns')).toBeInTheDocument();
  expect(screen.getByText('2 modules · 10 lessons')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: /view course/i })).toHaveAttribute('href', '/courses/123');
});
