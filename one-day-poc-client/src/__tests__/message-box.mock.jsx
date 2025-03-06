Object.defineProperty(window.HTMLElement.prototype, 'offsetParent', {
  get() {
    return {}; // Return a non-null value
  },
});
