import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Logo from '@/components/Logo';

// Mock the useNavigate hook
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('Logo Component', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('renders correctly with the brand name and icon', () => {
    render(
      <BrowserRouter>
        <Logo />
      </BrowserRouter>
    );

    expect(screen.getByText('PROOF -')).toBeInTheDocument();
    expect(screen.getByText('IT!')).toBeInTheDocument();

    expect(screen.getByTestId('logo')).toBeInTheDocument();
  });

  it('navigates to the home page when clicked', () => {
    render(
      <BrowserRouter>
        <Logo />
      </BrowserRouter>
    );

    const logo = screen.getByTestId('logo');

    fireEvent.click(logo);

    expect(mockNavigate).toHaveBeenCalledWith('/');
    expect(mockNavigate).toHaveBeenCalledTimes(1);
  });

  it('has the correct styling classes', () => {
    render(
      <BrowserRouter>
        <Logo />
      </BrowserRouter>
    );

    const logo = screen.getByTestId('logo');
    expect(logo).toHaveClass('flex');
    expect(logo).toHaveClass('items-center');
    expect(logo).toHaveClass('cursor-pointer');
  });
});