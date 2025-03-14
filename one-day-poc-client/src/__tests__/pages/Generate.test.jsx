import { render, screen, waitFor,fireEvent } from '@testing-library/react'
import { vi, test, expect, beforeEach, beforeAll } from "vitest";
import { MemoryRouter } from "react-router-dom";
import Page from '../../pages/Generate.js';
import ChatScreen from "@/pages/ChatScreen.tsx";
import { NavUser } from '@/components/nav-user.tsx';
import { useSidebar } from "@/components/ui/sidebar";
import { SidebarProvider } from "@/components/ui/sidebar";
import userEvent from "@testing-library/user-event";
import React from "react";

vi.mock("@/components/ui/sidebar", async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        useSidebar: vi.fn().mockReturnValue({isMobile: false}),
    };
});


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

    vi.clearAllMocks();
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

test("Chat screen toggles", ()=>{
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

test("Prototype frame displays", async ()=>{
    const setPrototypeMock = vi.fn();
    vi.spyOn(React, 'useState').mockImplementationOnce(() => [true, setPrototypeMock]);
    render(
        <MemoryRouter>
            <Page />
        </MemoryRouter>
    );

    await waitFor(() => {
        const prototypeDiv = document.querySelector(".flex-1.h-full.rounded-xl");
        expect(prototypeDiv).not.toBeNull();
        //expect(prototypeDiv.children.length).toBeGreaterThan(0);
    });
})

test("Mobile sidebar renders correctly", async()=>{
    useSidebar.mockReturnValue({ isMobile: true });

    render(
        <SidebarProvider>
            <NavUser user={{ name: "Test User", email: "test@example.com", avatar: "" }} />
        </SidebarProvider>
    );

    const button = screen.getByRole("button");
    expect(button).toBeInTheDocument();
})

test("Non-mobile sidebar renders correctly", async()=>{
    useSidebar.mockReturnValue({ isMobile: false });
    render(
        <SidebarProvider>
            <NavUser user={{ name: "Test User", email: "test@example.com", avatar: "" }} />
        </SidebarProvider>
    );

    const button = screen.getByRole("button");
    expect(button).toBeInTheDocument();
})