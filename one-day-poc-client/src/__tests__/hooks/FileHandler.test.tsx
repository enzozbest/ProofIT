import { vi, test, expect, beforeEach, describe } from 'vitest';
import { normaliseFiles } from '../../hooks/FileHandler';

describe('normaliseFiles function', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  test('should handle empty files object', () => {
    const result = normaliseFiles({});
    expect(result).toEqual({});
  });

  test('should handle package.json as string', () => {
    const files = {
      'package.json': 'content  '
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: 'content'
      }
    });
  });

  test('should handle package.json as direct object', () => {
    const packageObj = {
      name: 'test-project',
      version: '1.0.0',
      dependencies: {
        'react': '^18.0.0'
      }
    };

    const files = {
      'package.json': packageObj
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: JSON.stringify(packageObj, null, 2)
      }
    });
  });

  test('should handle file with contents property at root level', () => {
    const files = {
      'index.js': {
        contents: 'console.log("Hello");'
      }
    };

    const result = normaliseFiles(files);

    expect(result['index.js']).toEqual({
      file: {
        contents: 'console.log("Hello");'
      }
    });
  });

  test('should handle file with file property at root level', () => {
    const files = {
      'index.js': {
        file: {
          contents: 'console.log("Hello");'
        }
      }
    };

    const result = normaliseFiles(files);

    expect(result['index.js']).toEqual({
      file: {
        contents: 'console.log("Hello");'
      }
    });
  });

  test('should handle empty file with file property', () => {
    const files = {
      'empty.js': {
        file: {}
      }
    };

    const result = normaliseFiles(files);

    expect(result['empty.js']).toEqual({
      file: {
        contents: ""
      }
    });
  });

  test('should handle nested file paths with contents property', () => {
    const files = {
      'src/index.js': {
        contents: 'console.log("Hello");'
      }
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");'
            }
          }
        }
      }
    });
  });

  test('should handle nested file paths with file property', () => {
    const files = {
      'src/components/Button.js': {
        file: {
          contents: 'export default Button = () => {};'
        }
      }
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          components: {
            directory: {
              'Button.js': {
                file: {
                  contents: 'export default Button = () => {};'
                }
              }
            }
          }
        }
      }
    });
  });

  test('should handle directory property', () => {
    const files = {
      'src': {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");'
            }
          }
        }
      }
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");'
            }
          }
        }
      }
    });
  });

  test('should handle multiple files and directories', () => {
    const files = {
      'package.json': '{ "name": "test" }',
      'src/index.js': {
        contents: 'import App from "./App";'
      },
      'src/App.js': {
        file: {
          contents: 'export default () => <div>App</div>;'
        }
      },
      'public': {
        directory: {
          'index.html': {
            file: {
              contents: '<!DOCTYPE html><html></html>'
            }
          }
        }
      }
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      'package.json': {
        file: {
          contents: '{ "name": "test" }'
        }
      },
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'import App from "./App";'
            }
          },
          'App.js': {
            file: {
              contents: 'export default () => <div>App</div>;'
            }
          }
        }
      },
      public: {
        directory: {
          'index.html': {
            file: {
              contents: '<!DOCTYPE html><html></html>'
            }
          }
        }
      }
    });
  });

  test('should handle existing directories when adding nested files', () => {
    const files = {
      'src/index.js': {
        contents: 'console.log("Hello");'
      },
      'src/components/Button.js': {
        contents: 'export default Button = () => {};'
      }
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");'
            }
          },
          components: {
            directory: {
              'Button.js': {
                file: {
                  contents: 'export default Button = () => {};'
                }
              }
            }
          }
        }
      }
    });
  });
});