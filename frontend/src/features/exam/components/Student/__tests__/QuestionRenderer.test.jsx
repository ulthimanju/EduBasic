import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import QuestionRenderer from '../QuestionRenderer';

describe('QuestionRenderer', () => {
  const onChange = vi.fn();

  it('renders MCQ_SINGLE and handles change', () => {
    const question = {
      type: 'MCQ_SINGLE',
      payload: {
        options: [
          { id: '1', text: 'Option 1' },
          { id: '2', text: 'Option 2' }
        ]
      }
    };
    render(<QuestionRenderer question={question} answer="1" onChange={onChange} />);
    
    const opt1 = screen.getByText('Option 1');
    const opt2 = screen.getByText('Option 2');
    
    // Check selection (in this UI, selected item has class is-selected on parent button)
    expect(opt1.closest('button')).toHaveClass('is-selected');
    expect(opt2.closest('button')).not.toHaveClass('is-selected');
    
    fireEvent.click(opt2);
    expect(onChange).toHaveBeenCalledWith('2');
  });

  it('renders MCQ_MULTI and handles change', () => {
    const question = {
      type: 'MCQ_MULTI',
      payload: {
        options: [
          { id: '1', text: 'Option 1' },
          { id: '2', text: 'Option 2' }
        ]
      }
    };
    render(<QuestionRenderer question={question} answer={['1']} onChange={onChange} />);
    
    const opt1 = screen.getByText('Option 1');
    const opt2 = screen.getByText('Option 2');
    
    expect(opt1.closest('button')).toHaveClass('is-selected');
    expect(opt2.closest('button')).not.toHaveClass('is-selected');
    
    fireEvent.click(opt2);
    expect(onChange).toHaveBeenCalledWith(['1', '2']);
  });

  it('renders TRUE_FALSE and handles change', () => {
    const question = {
      type: 'TRUE_FALSE',
      payload: {}
    };
    render(<QuestionRenderer question={question} answer="true" onChange={onChange} />);
    
    const trueBtn = screen.getByText('TRUE');
    const falseBtn = screen.getByText('FALSE');
    
    expect(trueBtn).toHaveClass('btn-primary');
    fireEvent.click(falseBtn);
    expect(onChange).toHaveBeenCalledWith('false');
  });

  it('renders FILL_BLANK and handles change', () => {
    const question = {
      type: 'FILL_BLANK',
      payload: {
        text: 'The capital of France is {blank}.',
        blanks: [{ id: 'b1' }]
      }
    };
    render(<QuestionRenderer question={question} answer={{ b1: 'Paris' }} onChange={onChange} />);
    
    const input = screen.getByRole('textbox');
    expect(input.value).toBe('Paris');
    
    fireEvent.change(input, { target: { value: 'Lyon' } });
    expect(onChange).toHaveBeenCalledWith({ b1: 'Lyon' });
  });

  it('renders CODING and handles change', () => {
    const question = {
      type: 'CODING',
      payload: {
        languagesAllowed: ['JAVA'],
        starterCode: 'public class Main {}'
      }
    };
    render(<QuestionRenderer question={question} answer="" onChange={onChange} />);
    
    const textarea = screen.getByRole('textbox');
    expect(textarea.value).toBe('public class Main {}');
    
    fireEvent.change(textarea, { target: { value: 'updated' } });
    expect(onChange).toHaveBeenCalledWith('updated');
  });

  it('renders SUBJECTIVE and handles change', () => {
    const question = {
      type: 'SUBJECTIVE',
      payload: {
        maxWords: 500
      }
    };
    render(<QuestionRenderer question={question} answer="Initial response" onChange={onChange} />);
    
    const textarea = screen.getByRole('textbox');
    expect(textarea.value).toBe('Initial response');
    expect(screen.getByText(/Max words: 500/i)).toBeInTheDocument();
    
    fireEvent.change(textarea, { target: { value: 'new detailed response' } });
    expect(onChange).toHaveBeenCalledWith('new detailed response');
  });
});
