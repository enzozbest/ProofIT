import { render, screen, waitFor } from '@testing-library/react'
import ChatScreen from '../pages/ChatScreen';
import '@testing-library/jest-dom';
import { MemoryRouter } from "react-router-dom";
import { vi, test, expect } from "vitest";

import userEvent from '@testing-library/user-event';

globalThis.fetch = vi.fn();

test("Renders chat page", () => {
    render(
        <MemoryRouter>
            <ChatScreen />
        </MemoryRouter>
    );
    const element = screen.getByPlaceholderText(/How can we help you today?/i);
    expect(element).toBeInTheDocument();
});


test("Enter text in chat", async () =>{
    render(
        <MemoryRouter>
            <ChatScreen />
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    expect(userchat).toHaveValue('Hello!')
})

test("Press enter button", async () =>{
    render(
        <MemoryRouter>
            <ChatScreen />
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
            <ChatScreen />
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    await userEvent.keyboard('{Enter}')
    await waitFor(() => {
        expect(userchat).toHaveValue('')
    },{timeout: 3000})

    expect(fetch).toHaveBeenCalledWith("http://localhost:8000/api/chat/send", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify("Hello!"),
    });
})

test("Invalid post request", async () =>{
    fetch.mockResolvedValueOnce({
        ok: false,
        text: vi.fn().mockResolvedValue("Mock LLM response"),
    });
    render(
        <MemoryRouter>
            <ChatScreen />
        </MemoryRouter>
    );

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    await userEvent.keyboard('{Enter}')
    await waitFor(() => {
        expect(userchat).toHaveValue('')
    },{timeout: 3000})

    expect(fetch).toHaveBeenCalledWith("http://localhost:8000/api/chat/send", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify("Hello!"),
    });
})




