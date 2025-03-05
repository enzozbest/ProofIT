import { render, screen, waitFor,fireEvent } from '@testing-library/react'
import { vi, test, expect, beforeEach, beforeAll } from "vitest";
import { MemoryRouter } from "react-router-dom";
import LandingPage from '../pages/LandingPage';

beforeEach(() => {
    vi.resetAllMocks()
});

test("Renders landing page",()=>{
    render(
        <MemoryRouter>
            <LandingPage />
        </MemoryRouter>
    );

    const element = screen.getByText("Enabling you from");
    expect(element).toBeInTheDocument();
})

test("Authenticated users see new prompts",async()=>{
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ userId: 1, isAdmin: false }),
    }));

    render(
        <MemoryRouter>
            <LandingPage />
        </MemoryRouter>
    );

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/auth/check', expect.any(Object)));

    const promptElement = await screen.findByText(/Generating Code For An Application/i);
    expect(promptElement).toBeVisible();
})

test("Unauthenticated users can't see new prompts",async()=>{
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ userId: null, isAdmin: false }),
    }));

    render(
        <MemoryRouter>
            <LandingPage />
        </MemoryRouter>
    );

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/auth/check', expect.any(Object)));

    const promptElement = screen.queryByText(/Generating Code For An Application/i);
    expect(promptElement).not.toBeInTheDocument();
})