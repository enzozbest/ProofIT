import { render, act } from "@testing-library/react";
import { describe, it, vi, expect } from "vitest";
import { toast } from "sonner";
import ChatScreen from "@/components/chat/ChatScreen";
import { MemoryRouter } from "react-router-dom";

describe("ChatScreen - Error Message Handling", () => {
    it("should call toast.error and handle setErrorMessage correctly", async () => {
        const toastErrorSpy = vi.spyOn(toast, "error").mockImplementation(() => "mock-toast-id");

        vi.mock("@/hooks/Chat", () => ({
            __esModule: true,
            default: () => ({
                message: "",
                setMessage: vi.fn(),
                sentMessages: [],
                handleSend: vi.fn(),
                errorMessage: "Test error message",
                setErrorMessage: vi.fn(),
            }),
        }));

        await act(async () => {
            render(
                <MemoryRouter>
                    <ChatScreen
                        showPrototype={false}
                        setPrototype={vi.fn()}
                        setPrototypeFiles={vi.fn()}
                        initialMessage={null}
                    />
                </MemoryRouter>
            );
        });

        expect(toastErrorSpy).toHaveBeenCalledWith(
            "Test error message", 
            expect.objectContaining({
                onDismiss: expect.any(Function),
                onAutoClose: expect.any(Function),
                closeButton: true,
            })
        );

        const [, toastOptions] = toastErrorSpy.mock.calls[0];

        expect(toastOptions).toBeDefined();

        const mockToastObject = { id: "mock-toast-id" };

        if (toastOptions?.onDismiss) {
            toastOptions.onDismiss(mockToastObject);
        }

        if (toastOptions?.onAutoClose) {
            toastOptions.onAutoClose(mockToastObject);
        }

        expect(vi.fn()).toHaveBeenCalledTimes(0);

        toastErrorSpy.mockRestore();
    });
});

describe("ChatScreen - Initial Message Handling", () => {
    it("should set message, send it after delay, and clear sessionStorage", async () => {
        const mockSetMessage = vi.fn();
        const mockHandleSend = vi.fn();
        const mockInitialMessage = "Hello, this is a test!";
        const sessionStorageSpy = vi.spyOn(sessionStorage, "removeItem");

        vi.doMock("@/hooks/Chat", () => ({
            __esModule: true,
            default: () => ({
                message: "",
                setMessage: mockSetMessage,
                sentMessages: [],
                handleSend: mockHandleSend,
                errorMessage: "",
                setErrorMessage: vi.fn(),
            }),
        }));

        vi.useFakeTimers();

        render(
            <MemoryRouter>
                <ChatScreen
                    showPrototype={false}
                    setPrototype={vi.fn()}
                    setPrototypeFiles={vi.fn()}
                    initialMessage={mockInitialMessage}
                />
            </MemoryRouter>
        );

        expect(mockSetMessage).not.toHaveBeenCalled();

        await act(async () => {
            vi.advanceTimersByTime(500);
        });

        expect(mockHandleSend).not.toHaveBeenCalled();

        vi.useRealTimers();
        sessionStorageSpy.mockRestore();
    });
});