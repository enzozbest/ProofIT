import { render, screen, waitFor,fireEvent } from '@testing-library/react'
import { vi, test, expect, beforeEach, beforeAll } from "vitest";
import { MemoryRouter } from "react-router-dom";
import Page from '../../pages/Generate.js';
import userEvent from "@testing-library/user-event";
import React from "react";
import ChatScreen from '../../components/chat/ChatScreen.js';
import { NavUser } from '../../components/sidebar/NavUser.tsx';
import { useSidebar } from "../../components/ui/Sidebar";
import { SidebarProvider } from "../../components/ui/Sidebar";
import Generate from "../../pages/Generate";

import { AuthProvider } from '@/contexts/AuthContext';
import { ConversationProvider } from '@/contexts/ConversationContext';

vi.mock("@/components/ui/Sidebar", async (importOriginal) => {
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

beforeEach(() => {
    vi.clearAllMocks();
    sessionStorage.clear();
});

test("Sets initialMessage when found in sessionStorage", async () => {
    // Mock sessionStorage before rendering
    sessionStorage.setItem("initialMessage", "Hello World");

    render(
        <MemoryRouter>
            <AuthProvider>
                <ConversationProvider>
                    <Page />
                </ConversationProvider>
            </AuthProvider>
        </MemoryRouter>
    );

    // Check if "Hello World" appears in the rendered output
    await expect(screen.findByText("Hello World")).resolves.toBeInTheDocument();
});

test("Does not render PrototypeFrame when showPrototype is false", () => {
    render(
        <MemoryRouter>
            <AuthProvider>
                <ConversationProvider>
                    <Generate showPrototype={false} prototypeFiles={[]} />
                </ConversationProvider>
            </AuthProvider>
        </MemoryRouter>
    );

    expect(screen.queryByTestId("prototype-frame")).not.toBeInTheDocument();
    expect(screen.queryByTestId("prototype-frame")).toBeNull();
});

test("Renders generate page", ()=>{
    render(
        <MemoryRouter>
            <AuthProvider>
                <ConversationProvider>
                    <Page />
                </ConversationProvider>
            </AuthProvider>
        </MemoryRouter>
    );
    const element = screen.getByPlaceholderText(/How can we help you today?/i);
    expect(element).toBeInTheDocument();
})

test("Chat screen toggles", ()=>{
    render(
        <MemoryRouter>
            <AuthProvider>
                <ConversationProvider>
                    <Page />
                </ConversationProvider>
            </AuthProvider>
        </MemoryRouter>
    );
    const toggleButton = screen.getByTestId("toggle-button");
    const chatScreenDiv =  document.querySelector(".w-\\[450px\\]");

    expect(chatScreenDiv).toHaveClass("max-w-[450px]");

    fireEvent.click(toggleButton);
    expect(chatScreenDiv).toHaveClass("max-w-0");
})

test("Prototype frame displays", async ()=>{
    const setPrototypeMock = vi.fn();
    vi.spyOn(React, 'useState').mockImplementationOnce(() => [true, setPrototypeMock]);
    render(
        <MemoryRouter>
            <AuthProvider>
                <ConversationProvider>
                    <Page />
                </ConversationProvider>
            </AuthProvider>
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
        <MemoryRouter>
            <SidebarProvider>
                <NavUser user={{ name: "Test User", email: "test@example.com", avatar: "" }} />
            </SidebarProvider>
        </MemoryRouter>
    );

    const button = screen.getByRole("button");
    expect(button).toBeInTheDocument();
})

test("Non-mobile sidebar renders correctly", async()=>{
    useSidebar.mockReturnValue({ isMobile: false });
    render(
        <MemoryRouter>
            <SidebarProvider>
                <NavUser user={{ name: "Test User", email: "test@example.com", avatar: "" }} />
            </SidebarProvider>
        </MemoryRouter>
    );

    const button = screen.getByRole("button");
    expect(button).toBeInTheDocument();
});

test('Renders background image correctly', () => {
    render(
      <MemoryRouter>
        <AuthProvider>
            <ConversationProvider>
                <Generate />
            </ConversationProvider>
        </AuthProvider>
      </MemoryRouter>
    );

    const containerDiv = screen.getByTestId('container').closest('div');
    expect(containerDiv).toHaveStyle({
        backgroundImage: "url('/background.svg')",
      });
      
});