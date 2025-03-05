import { render, screen, waitFor,fireEvent } from '@testing-library/react'
import { vi, test, expect, beforeEach, beforeAll } from "vitest";
import { MemoryRouter } from "react-router-dom";
import Page from '../pages/Generate';

beforeAll(() => {
    globalThis.window.matchMedia = vi.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    }));
});

test("Renders generate page", ()=>{
    render(
        <MemoryRouter>
            <Page />
        </MemoryRouter>
    );
    const element = screen.getByPlaceholderText(/How can we help you today?/i);
    expect(element).toBeInTheDocument();
})

test("Sidebar toggle", ()=>{
    render(
        <MemoryRouter>
            <Page />
        </MemoryRouter>
    );
    const toggleButton = screen.getByTestId("toggle-button");
    const chatScreenDiv =  document.querySelector(".w-\\[450px\\]");
    expect(chatScreenDiv).toHaveClass('opacity-100');

    fireEvent.click(toggleButton);
    expect(chatScreenDiv).toHaveClass('opacity-0');

})