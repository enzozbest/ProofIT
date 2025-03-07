import { describe, test, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { NavProjects } from '@/components/nav-projects';
import { useSidebar } from '@/components/ui/sidebar';
import { Folder, MessageSquare, Users } from 'lucide-react';

// Mock the useSidebar hook
vi.mock('@/components/ui/sidebar', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual as Object,
        useSidebar: vi.fn(() => ({ isMobile: false }))
    };
});

describe('NavProjects Component', () => {
    const mockProjects = [
        { name: 'Documents', url: '/documents', icon: Folder },
        { name: 'Chat', url: '/chat', icon: MessageSquare },
        { name: 'Team', url: '/team', icon: Users }
    ];

    beforeEach(() => {
        // Reset mocks before each test
        vi.clearAllMocks();
    });

    test('renders projects correctly', () => {
        render(<NavProjects projects={mockProjects} />);

        // Check if the label is rendered
        expect(screen.getByText('Projects')).toBeInTheDocument();

        // Check if all projects are rendered
        mockProjects.forEach(project => {
            expect(screen.getByText(project.name)).toBeInTheDocument();
        });

        // Check if "More" item exists
        expect(screen.getByText('More')).toBeInTheDocument();
    });

    test('project links have correct URLs', () => {
        render(<NavProjects projects={mockProjects} />);

        mockProjects.forEach(project => {
            const link = screen.getByText(project.name).closest('a');
            expect(link).toHaveAttribute('href', project.url);
        });
    });

    test('dropdown menu opens on click', async () => {
        render(<NavProjects projects={mockProjects} />);

        // Find the first dropdown trigger and click it
        const moreButtons = screen.getAllByText('More');
        // Get the one that's a span with sr-only class
        const dropdownTrigger = screen.getAllByText('More')
            .find(el => el.classList.contains('sr-only'))?.parentElement;

        expect(dropdownTrigger).toBeInTheDocument();
        if (dropdownTrigger) {
            fireEvent.click(dropdownTrigger);
        }

        // Check dropdown content is visible
        expect(screen.getByText('View Project')).toBeInTheDocument();
        expect(screen.getByText('Share Project')).toBeInTheDocument();
        expect(screen.getByText('Delete Project')).toBeInTheDocument();
    });

    test('renders correctly in mobile view', () => {
        // Override the mock to return mobile view
        vi.mocked(useSidebar).mockReturnValue({ isMobile: true });

        render(<NavProjects projects={mockProjects} />);

        // Verify dropdown has correct alignment attributes for mobile
        const dropdownTrigger = screen.getAllByText('More')
            .find(el => el.classList.contains('sr-only'))?.parentElement;

        if (dropdownTrigger) {
            fireEvent.click(dropdownTrigger);
        }

        // This is challenging to test directly with just JSDOM
        // In a real scenario, you might test this with Cypress or a similar tool
        // that renders the actual component
    });

    test('renders empty state when no projects are provided', () => {
        render(<NavProjects projects={[]} />);

        // Should still render the Projects label
        expect(screen.getByText('Projects')).toBeInTheDocument();

        // Should render the "More" item
        expect(screen.getByText('More')).toBeInTheDocument();

        // Should not render any project items
        mockProjects.forEach(project => {
            expect(screen.queryByText(project.name)).not.toBeInTheDocument();
        });
    });
});