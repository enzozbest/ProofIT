import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import OldPrompts from '../../components/landing/OldPrompts';

describe('OldPrompts', () => {

    it('renders the correct number of prompt buttons', () => {
        render(<OldPrompts />);
        
        const buttons = screen.getAllByRole('button');
        expect(buttons).toHaveLength(3);
    });

    it('includes MessageCircle icons in each prompt', () => {
        render(<OldPrompts />);
        
        const svgElements = document.querySelectorAll('svg');
        expect(svgElements).toHaveLength(3);
    });

    it('applies proper styling to prompt buttons', () => {
        render(<OldPrompts />);
        
        const buttons = screen.getAllByRole('button');
        
        buttons.forEach(button => {
          expect(button).toHaveClass('border-2');
          expect(button).toHaveClass('border-white');
          expect(button).toHaveClass('rounded-lg');
          expect(button).toHaveClass('bg-transparent');
          expect(button).toHaveClass('text-left');
          expect(button).toHaveClass('w-[220px]');
        });
    });

    it('truncates long text with line-clamp-3', () => {
        render(<OldPrompts />);

        const longTextElement = screen.getByText('Creating A Web Page From Scratch That Is Fully Responsive and Optimized for SEO');
        
        const spanElement = longTextElement.closest('span');
        expect(spanElement).toHaveClass('line-clamp-3');
        expect(spanElement).toHaveClass('overflow-hidden');
        expect(spanElement).toHaveClass('text-ellipsis');
    });

    it('renders within a parent container', () => {
        render(
          <div data-testid="parent-container">
            <OldPrompts />
          </div>
        );
        
        const parentContainer = screen.getByTestId('parent-container');
        expect(parentContainer).toContainElement(screen.getByText('Generating Code For An Application'));
    });
});