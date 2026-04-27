import { render, screen } from '@testing-library/react';
import StatusBadge from './StatusBadge';
import { expect, test } from 'vitest';

test('renders correct label for status', () => {
  render(<StatusBadge status="PUBLISHED" />);
  expect(screen.getByText('Published')).toBeInTheDocument();

  render(<StatusBadge status="COMPLETED" />);
  expect(screen.getByText('Completed')).toBeInTheDocument();
});

test('applies correct class per status', () => {
  const { container } = render(<StatusBadge status="PUBLISHED" />);
  expect(container.firstChild).toHaveClass(/success/);
});
