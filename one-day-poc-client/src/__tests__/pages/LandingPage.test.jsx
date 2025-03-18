import { render, screen, waitFor,fireEvent } from '@testing-library/react'
import { vi, test, expect, beforeEach, beforeAll } from "vitest";
import { MemoryRouter } from "react-router-dom";
import LandingPage from '../../pages/LandingPage.js';
import userEvent from "@testing-library/user-event";
import { AuthProvider, useAuth } from "@/contexts/AuthContext.tsx";
import { act } from 'react-dom/test-utils';
import React, { useEffect } from 'react';


beforeEach(() => {
    vi.resetAllMocks()
});

test("Renders landing page",()=>{
    render(
        <MemoryRouter>
            <AuthProvider>
                <LandingPage />
            </AuthProvider>
        </MemoryRouter>
    );

    const element = screen.getByText("Enabling you from");
    expect(element).toBeInTheDocument();
})


test("Authenticated users see new prompts",async()=>{
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ userId: 1, isAdmin: false }),
    }));

    const TestComponent = () => {
        const { checkAuth, logout } = useAuth();

        // Call checkAuth to simulate login
        useEffect(() => {
            checkAuth();
        }, [checkAuth]);

        return (
            <div>
                <button onClick={logout}>Log Out</button>
            </div>
        );
    };

    const { container } = render(
        <MemoryRouter>
            <AuthProvider>
                <LandingPage />
                <TestComponent />
            </AuthProvider>
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

    fetch.mockResolvedValueOnce({
        ok: false,
        json: () => Promise.resolve("Mock LLM response"),
    });
    render(
        <MemoryRouter>
            <AuthProvider>
                <LandingPage />
            </AuthProvider>
        </MemoryRouter>
    );

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/auth/check', expect.any(Object)));

    const promptElement = screen.queryByText(/Generating Code For An Application/i);
    expect(promptElement).not.toBeInTheDocument();
})

test("Prompts are sent via the enter key",async()=>{
    vi.doMock("react-router-dom", async () => {
        const actual = await vi.importActual("react-router-dom");
        return {
            ...actual,
            useNavigate: vi.fn(),
        };
    });

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ userId: null, isAdmin: false }),
    }));

    fetch.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve("Mock LLM response"),
    });

    const { useNavigate } = await import("react-router-dom");
    const mockNavigate = vi.fn();
    useNavigate.mockReturnValue(mockNavigate)

    render(
        <MemoryRouter>
            <AuthProvider>
                <LandingPage />
            </AuthProvider>
        </MemoryRouter>
    );

    const userinput = screen.getByPlaceholderText(/Tell us what we can do for you?/i);
    await userEvent.type(userinput, 'Hello!')
    await userEvent.keyboard('{Enter}')

    await waitFor(() => {
        expect(userinput).toHaveValue('Hello!')
    },{timeout: 3000})
})

test("Sign in button activates authentication",async()=>{
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ userId: 1, isAdmin: false }),
    }));

    render(
        <MemoryRouter>
            <AuthProvider>
                <LandingPage />
            </AuthProvider>
        </MemoryRouter>
    );

    const signinButton = screen.getByRole("button", { name: "Sign In" });
    fireEvent.click(signinButton);
    await waitFor(() => expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/auth/check', expect.any(Object)));
})

test("Sign out button triggers logging out",async()=>{
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
        json: () => Promise.resolve({ userId: 1, isAdmin: false }),
    }));

    const TestComponent = () => {
        const { checkAuth, logout } = useAuth();

        // Call checkAuth to simulate login
        useEffect(() => {
            checkAuth();
        }, [checkAuth]);

        return (
            <div>
                <button onClick={logout}>Log Out</button>
            </div>
        );
    };

    const { container } = render(
        <MemoryRouter>
            <AuthProvider>
                <LandingPage />
                <TestComponent />
            </AuthProvider>
        </MemoryRouter>
    );


    const signInButton = screen.getByRole("button", { name: "Sign In" });
    expect(signInButton).toBeInTheDocument();
    await act(async () => {
        fireEvent.click(signInButton);
    });

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/auth/check', expect.any(Object)));

    const signOutButton = await screen.findByRole("button", { name: "Log Out" });
    expect(signOutButton).toBeInTheDocument();

    await act(async () => {
        fireEvent.click(signOutButton);
    });

    await waitFor(() => expect(fetch).toHaveBeenCalledWith('http://localhost:8000/api/auth/logout', expect.any(Object)));


})