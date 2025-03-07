import { describe, test, expect } from 'vitest';
import testIcon from '@/__tests__/__mocks__/test.svg';
import { createSvgImage, isSvgContent } from './__utils__/svg-utils';

/**
 * This file will test global.d.ts, but it won't give any line coverage because:
 * this is a declaration file, and they are completely removed during compilation to JavaScript
 * coverage tools measure runtime execution - they track which lines of code are executed
 * declaration files don't exist at runtime and have no executable code
 */


describe('SVG Type Declaration', () => {
    test('SVG import type is correctly defined as string', () => {
        expect(typeof testIcon).toBe('string');
    });

    test('SVG utilities work with imported SVGs', () => {
        const img = createSvgImage(testIcon);
        expect(img.src).toContain(testIcon);

        expect(isSvgContent('<svg></svg>')).toBe(true);
        expect(isSvgContent('not an svg')).toBe(false);
    });

    test('SVG content can be used in DOM elements', () => {
        document.body.innerHTML = `<div id="test-container"></div>`;
        const container = document.getElementById('test-container');
        if (container) {
            const svgImg = createSvgImage(testIcon);
            container.appendChild(svgImg);
            expect(container.innerHTML).toContain('img');
            expect(svgImg.src).toContain(testIcon);
        }
    });
});