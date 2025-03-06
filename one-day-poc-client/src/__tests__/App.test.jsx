import { render, screen, waitFor,fireEvent } from '@testing-library/react'
import App from '../App';
import '@testing-library/jest-dom';
import { MemoryRouter } from "react-router-dom";
import { vi, test, expect } from "vitest";

test("Renders base page", () => {

    render(
        <MemoryRouter>
            <App />
        </MemoryRouter>
    );
    expect(screen.getByText(/Enabling you from/i)).toBeInTheDocument();
});