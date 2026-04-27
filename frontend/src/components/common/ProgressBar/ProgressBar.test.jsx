import { render, screen } from '@testing-library/react';
import ProgressBar from './ProgressBar';
import { expect, test } from 'vitest';

test('renders fill width matching percent prop', () => {
  const { container } = render(<ProgressBar percent={45} />);
  const fill = container.querySelector('[class*="fill"]');
  expect(fill).toHaveStyle({ width: '45%' });
});

test('clamps to 0-100', () => {
  const { container: low } = render(<ProgressBar percent={-10} />);
  expect(low.querySelector('[class*="fill"]')).toHaveStyle({ width: '0%' });

  const { container: high } = render(<ProgressBar percent={150} />);
  expect(high.querySelector('[class*="fill"]')).toHaveStyle({ width: '100%' });
});

test('shows label when showLabel is true', () => {
  render(<ProgressBar percent={75} showLabel />);
  expect(screen.getByText('75%')).toBeInTheDocument();
});
