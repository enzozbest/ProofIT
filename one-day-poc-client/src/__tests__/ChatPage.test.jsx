import { render, screen, waitFor,fireEvent } from '@testing-library/react'
import ChatScreen from '../pages/ChatScreen';
import '@testing-library/jest-dom';
import { MemoryRouter, useLocation } from "react-router-dom";
import { vi, test, expect, beforeEach } from "vitest";
import userEvent from '@testing-library/user-event';

globalThis.fetch = vi.fn();
const mockSetPrototype = vi.fn();
const mockSetPrototypeId = vi.fn();
const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

// Ensure mocks are reset before each test
beforeEach(() => {
    vi.resetAllMocks()
});

test("Renders chat page", () => {

    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId} />
        </MemoryRouter>
    );
    const element = screen.getByPlaceholderText(/How can we help you today?/i);
    expect(element).toBeInTheDocument();
});

test("Enter text in chat", async () =>{
    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId}/>
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    expect(userchat).toHaveValue('Hello!')
})

test("Press enter button", async () =>{
    fetch.mockResolvedValueOnce({
        ok: true,
        text: vi.fn().mockResolvedValue("Mock LLM response"),
    });
    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId}/>
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    await userEvent.keyboard('{Enter}')
    await waitFor(() => {
        expect(userchat).toHaveValue('')
    },{timeout: 3000})
})

test("Valid post request", async () =>{
    fetch.mockResolvedValueOnce({
        ok: true,
        text: vi.fn().mockResolvedValue("Mock LLM response"),
    });
    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId}/>
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    await userEvent.keyboard('{Enter}')
    await waitFor(() => {
        expect(userchat).toHaveValue('')
    },{timeout: 3000})


    expect(fetch).toHaveBeenCalledWith("http://localhost:8000/api/chat/json", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body:expect.any(String),
    });
})

test("Invalid post request", async () =>{
    fetch.mockResolvedValueOnce({
        ok: false,
        text: vi.fn().mockResolvedValue("Mock LLM response"),
    });
    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId}/>
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    await userEvent.keyboard('{Enter}')
    await waitFor(() => {
        expect(userchat).toHaveValue('Hello!')
    },{timeout: 3000})

    await waitFor(() => expect(consoleErrorSpy).toHaveBeenCalledTimes(2));
})

test("Clicking send button sends a message", async () =>{
    fetch.mockResolvedValueOnce({
        ok: true,
        text: vi.fn().mockResolvedValue("Mock LLM response"),
    });
    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId}/>
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    const sendButton = screen.getByText("Send");
    fireEvent.click(sendButton);
    await waitFor(() => {
        expect(userchat).toHaveValue('')
    },{timeout: 3000})


    expect(fetch).toHaveBeenCalledWith("http://localhost:8000/api/chat/json", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body:expect.any(String),
    });
})


test("Initial message set when page loads", async ()=>{
    vi.doMock("react-router-dom", async () => {
        const actual = await vi.importActual("react-router-dom");
        return {
            ...actual,
            useLocation: vi.fn().mockReturnValue({
                pathname: "/generate",
                state: { initialMessage: "Test initial message" },
            }),
        };
    });

    const ChatScreen = (await import("../pages/ChatScreen")).default;
    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId} />
        </MemoryRouter>
    );
    
    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await waitFor(() => {
        expect(userchat).toHaveValue('')
    },{timeout: 3000})

})

test("Can't send message until initial message set", async ()=>{
    vi.doMock("react-router-dom", async () => {
        const actual = await vi.importActual("react-router-dom");
        return {
            ...actual,
            useLocation: vi.fn().mockReturnValue({
                pathname: "/generate",
                state: { initialMessage: "" },
            }),
        };
    });

    const ChatScreen = (await import("../pages/ChatScreen")).default;
    render(
        <MemoryRouter>
            <ChatScreen setPrototype={mockSetPrototype} setPrototypeId={mockSetPrototypeId} />
        </MemoryRouter>
    );


    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await waitFor(() => {
        expect(userchat).toHaveValue('')
    },{timeout: 3000})

    await userEvent.type(userchat, 'Test initial message')
    await userEvent.keyboard('{Enter}')

    await waitFor(() => {
        expect(userchat).toHaveValue('Test initial message')
    },{timeout: 3000})

})