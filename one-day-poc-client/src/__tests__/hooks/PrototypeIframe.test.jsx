import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import PrototypeFrame from '../../hooks/PrototypeFrame.js';

test("renders the iframe", () => {
    render(<PrototypeFrame />);
    const iframe = screen.getByTestId("prototype-iframe");
    expect(iframe).toBeInTheDocument();
});