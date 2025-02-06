import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import Chat from '../pages/Chat';
import { ChatBox } from "../components/chat-box";
import { MessageBox } from "../components/messages-box";
import '@testing-library/jest-dom';
import userEvent from '@testing-library/user-event';

global.fetch = jest.fn();

test("Renders chat page", () => {
    render(<Chat/>);
    const element = screen.getByPlaceholderText(/How can we help you today?/i);
    expect(element).toBeInTheDocument();
});


test("Enter text in chat", async () =>{
    render(<Chat/>);

    const userchat = screen.getByPlaceholderText(/How can we help you today?/i);
    await userEvent.type(userchat, 'Hello!')
    expect(userchat).toHaveValue('Hello!')
})

test("Press enter button", async () =>{
    render(<Chat/>);

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
        text: jest.fn().mockResolvedValue("Mock LLM response"),
    });
    render(<Chat/>);

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
        text: jest.fn().mockResolvedValue("Mock LLM response"),
    });
    render(<Chat/>);

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




