import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import PrototypeFrame from '../pages/PrototypeFrame';

test("renders the iframe", () => {
    render(<PrototypeFrame />);
    const iframe = screen.getByTestId("prototype-iframe");
    expect(iframe).toBeInTheDocument();
});