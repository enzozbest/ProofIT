import { vi } from "vitest";

export const mockAuth = async ({ isAuthenticated, isAdmin = false }) => {
    await vi.doMock('@/contexts/AuthContext.tsx', async () => {
        const actual = await vi.importActual('@/contexts/AuthContext.tsx');
        return {
            ...actual,
            useAuth: () => ({
                isAuthenticated,
                isAdmin,
                checkAuth: vi.fn(),
                login: vi.fn(),
                logout: vi.fn(),
            }),
        };
    });
};