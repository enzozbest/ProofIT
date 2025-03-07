/**
 * Creates an img element with the given SVG as source
 * @param svg SVG source
 * @returns HTMLImageElement with the SVG as source
 */
export function createSvgImage(svg: string): HTMLImageElement {
    const img = document.createElement('img');
    img.src = svg;
    return img;
}

/**
 * Checks if a string is an SVG
 * @param content String to check
 * @returns True if the string appears to be an SVG
 */
export function isSvgContent(content: string): boolean {
    return content.includes('<svg') && content.includes('</svg>');
}