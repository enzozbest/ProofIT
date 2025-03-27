import { vi, test, expect, beforeEach, describe } from 'vitest';
import { normaliseFiles } from '../../hooks/FileHandler';
import { WebContainer } from '@webcontainer/api';
import { cleanFileSystem } from '../../hooks/FileHandler';

vi.mock('@webcontainer/api', () => ({
  WebContainer: vi.fn(),
}));

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
      'package.json': 'content  ',
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: 'content  ',
      },
    });
  });

  test('should handle package.json as direct object', () => {
    const packageObj = {
      name: 'test-project',
      version: '1.0.0',
      dependencies: {
        react: '^18.0.0',
      },
    };

    const files = {
      'package.json': packageObj,
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: JSON.stringify(packageObj, null, 2),
      },
    });
  });

  test('should handle file with contents property at root level', () => {
    const files = {
      'index.js': {
        contents: 'console.log("Hello");',
      },
    };

    const result = normaliseFiles(files);

    expect(result['index.js']).toEqual({
      file: {
        contents: 'console.log("Hello");',
      },
    });
  });

  test('should handle file with file property at root level', () => {
    const files = {
      'index.js': {
        file: {
          contents: 'console.log("Hello");',
        },
      },
    };

    const result = normaliseFiles(files);

    expect(result['index.js']).toEqual({
      file: {
        contents: 'console.log("Hello");',
      },
    });
  });

  test('should handle empty file with file property', () => {
    const files = {
      'empty.js': {
        file: {},
      },
    };

    const result = normaliseFiles(files);

    expect(result['empty.js']).toEqual({
      file: {
        contents: '',
      },
    });
  });

  test('should handle nested file paths with contents property', () => {
    const files = {
      'src/index.js': {
        contents: 'console.log("Hello");',
      },
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");',
            },
          },
        },
      },
    });
  });

  test('should handle nested file paths with file property', () => {
    const files = {
      'src/components/Button.js': {
        file: {
          contents: 'export default Button = () => {};',
        },
      },
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          components: {
            directory: {
              'Button.js': {
                file: {
                  contents: 'export default Button = () => {};',
                },
              },
            },
          },
        },
      },
    });
  });

  test('should handle directory property', () => {
    const files = {
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");',
            },
          },
        },
      },
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");',
            },
          },
        },
      },
    });
  });

  test('should handle multiple files and directories', () => {
    const files = {
      'package.json': '{ "name": "test" }',
      'src/index.js': {
        contents: 'import App from "./App";',
      },
      'src/App.js': {
        file: {
          contents: 'export default () => <div>App</div>;',
        },
      },
      public: {
        directory: {
          'index.html': {
            file: {
              contents: '<!DOCTYPE html><html></html>',
            },
          },
        },
      },
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      'package.json': {
        file: {
          contents: '{ "name": "test" }',
        },
      },
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'import App from "./App";',
            },
          },
          'App.js': {
            file: {
              contents: 'export default () => <div>App</div>;',
            },
          },
        },
      },
      public: {
        directory: {
          'index.html': {
            file: {
              contents: '<!DOCTYPE html><html></html>',
            },
          },
        },
      },
    });
  });

  test('should handle existing directories when adding nested files', () => {
    const files = {
      'src/index.js': {
        contents: 'console.log("Hello");',
      },
      'src/components/Button.js': {
        contents: 'export default Button = () => {};',
      },
    };

    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello");',
            },
          },
          components: {
            directory: {
              'Button.js': {
                file: {
                  contents: 'export default Button = () => {};',
                },
              },
            },
          },
        },
      },
    });
  });

  test('should handle string content with nested path', () => {
    const files = {
      'src/components/Button.jsx': 'export default function Button() { return <button>Click me</button> }',
    };
  
    const result = normaliseFiles(files);

    expect(result).toEqual({
      src: {
        directory: {
          components: {
            directory: {
              'Button.jsx': {
                file: {
                  contents: 'export default function Button() { return <button>Click me</button> }',
                },
              },
            },
          },
        },
      },
    });
  });

  test('should handle multiple string contents with nested paths', () => {
    const files = {
      'src/index.js': 'console.log("Root file");',
      'src/utils/helpers.js': 'export const add = (a, b) => a + b;',
      'public/index.html': '<!DOCTYPE html><html><body>Hello</body></html>',
    };
  
    const result = normaliseFiles(files);
  
    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Root file");',
            },
          },
          utils: {
            directory: {
              'helpers.js': {
                file: {
                  contents: 'export const add = (a, b) => a + b;',
                },
              },
            },
          },
        },
      },
      public: {
        directory: {
          'index.html': {
            file: {
              contents: '<!DOCTYPE html><html><body>Hello</body></html>',
            },
          },
        },
      },
    });
  });

  test('should handle mixed content types with nested paths', () => {
    const files = {
      'src/index.js': 'console.log("Root file");',
      'src/utils/helpers.js': {
        contents: 'export const add = (a, b) => a + b;',
      },
      'public/index.html': {
        file: {
          contents: '<!DOCTYPE html><html><body>Hello</body></html>',
        },
      },
      'README.md': '# Project Documentation',
    };
  
    const result = normaliseFiles(files);
  
    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Root file");',
            },
          },
          utils: {
            directory: {
              'helpers.js': {
                file: {
                  contents: 'export const add = (a, b) => a + b;',
                },
              },
            },
          },
        },
      },
      public: {
        directory: {
          'index.html': {
            file: {
              contents: '<!DOCTYPE html><html><body>Hello</body></html>',
            },
          },
        },
      },
      'README.md': {
        file: {
          contents: '# Project Documentation',
        },
      },
    });
  });

  test('should treat object with package.json properties as a file even if not at package.json path', () => {
    const packageLikeObj = {
      name: 'component-package',
      version: '0.1.0',
      dependencies: {
        'react': '^18.0.0'
      }
    };
  
    const files = {
      'src/components/package.json': packageLikeObj
    };
  
    const result = normaliseFiles(files);
  
    expect(result).toEqual({
      'src/components/package.json': {
        file: {
          contents: JSON.stringify(packageLikeObj, null, 2)
        }
      }
    });
  });

  test('should handle implicit directory objects', () => {
    const files = {
      'config': {
        'dev': {
          'settings.json': '{ "mode": "development" }'
        },
        'prod': {
          'settings.json': '{ "mode": "production" }'
        }
      }
    };
  
    const result = normaliseFiles(files);
 
    expect(result).toEqual({
      'config': {
        directory: {
          'dev': {
            directory: {
              'settings.json': {
                file: {
                  contents: '{ "mode": "development" }'
                }
              }
            }
          },
          'prod': {
            directory: {
              'settings.json': {
                file: {
                  contents: '{ "mode": "production" }'
                }
              }
            }
          }
        }
      }
    });
  });

  test('should handle mix of implicit directories and package-like objects', () => {
    const files = {
      'libs': {
        'ui-lib': {
          'package.json': {
            name: 'ui-lib',
            version: '1.0.0',
            dependencies: {
              'react': '^18.0.0'
            }
          },
          'src': {
            'index.js': 'export * from "./components";'
          }
        },
        'util-lib': {
          name: 'util-lib',
          version: '0.5.0',
          scripts: {
            'build': 'tsc'
          }
        }
      }
    };
  
    const result = normaliseFiles(files);
  
    expect(result).toEqual({
      'libs': {
        directory: {
          'ui-lib': {
            directory: {
              'package.json': {
                file: {
                  contents: JSON.stringify({
                    name: 'ui-lib',
                    version: '1.0.0',
                    dependencies: {
                      'react': '^18.0.0'
                    }
                  }, null, 2)
                }
              },
              'src': {
                directory: {
                  'index.js': {
                    file: {
                      contents: 'export * from "./components";'
                    }
                  }
                }
              }
            }
          },
          'util-lib': {
            file: {
              contents: JSON.stringify({
                name: 'util-lib',
                version: '0.5.0',
                scripts: {
                  'build': 'tsc'
                }
              }, null, 2)
            }
          }
        }
      }
    });
  });

  test('should convert file to directory when path conflicts exist', () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    
    const files = {
      'src': 'This is a file content', 
      'src/index.js': 'console.log("Hello World");' 
    };
  
    const result = normaliseFiles(files);
  
    expect(result).toEqual({
      src: {
        directory: {
          'index.js': {
            file: {
              contents: 'console.log("Hello World");'
            }
          }
        }
      }
    });
  
    expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('Expected directory but found file at src'));
    
    consoleErrorSpy.mockRestore();
  });
  
  test('should handle undefined contents in file property', () => {
    const files = {
      'src/components/EmptyComponent.js': {
        file: { 
        }
      }
    };
  
    const result = normaliseFiles(files);
  
    expect(result).toEqual({
      src: {
        directory: {
          components: {
            directory: {
              'EmptyComponent.js': {
                file: {
                  contents: ''
                }
              }
            }
          }
        }
      }
    });
  });
  
  test('should handle null contents in file property', () => {
    const files = {
      'src/components/NullComponent.js': {
        file: { 
          contents: null as unknown as string
        }
      }
    };
  
    const result = normaliseFiles(files);
  
    expect(result).toEqual({
      src: {
        directory: {
          components: {
            directory: {
              'NullComponent.js': {
                file: {
                  contents: ''
                }
              }
            }
          }
        }
      }
    });
  });
});

describe('Package.json special handling', () => {
  test('should trim whitespace in package.json string content', () => {
    const files = {
      'package.json': '  {  "name": "test-project"  }  ',
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: '  {  "name": "test-project"  }  '
      }
    });
  });

  test('should handle package.json with complex nested properties', () => {
    const packageObj = {
      name: 'test-project',
      version: '1.0.0',
      scripts: {
        start: 'node index.js',
        test: 'jest',
        build: 'webpack'
      },
      dependencies: {
        react: '^18.0.0',
        'react-dom': '^18.0.0'
      },
      devDependencies: {
        typescript: '^4.8.4',
        jest: '^29.0.0'
      }
    };

    const files = {
      'package.json': packageObj,
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: JSON.stringify(packageObj, null, 2)
      }
    });
  });

  test('should handle package.json with minimal properties', () => {
    const minimalPackage = {
      name: 'minimal-package'
    };

    const files = {
      'package.json': minimalPackage
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: JSON.stringify(minimalPackage, null, 2)
      }
    });
  });

  test('should handle package.json with version only', () => {
    const versionOnlyPackage = {
      version: '1.0.0'
    };

    const files = {
      'package.json': versionOnlyPackage
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: JSON.stringify(versionOnlyPackage, null, 2)
      }
    });
  });

  test('should handle package.json with file property structure', () => {
    const files = {
      'package.json': {
        file: {
          contents: '{"name": "already-formatted"}'
        }
      }
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: '{"name": "already-formatted"}'
      }
    });
  });

  test('should handle package.json with contents property', () => {
    const files = {
      'package.json': {
        contents: '{"name": "content-property"}'
      }
    };

    const result = normaliseFiles(files);

    expect(result['package.json']).toEqual({
      file: {
        contents: '{"name": "content-property"}'
      }
    });
  });
});

describe('cleanFileSystem function', () => {
  let mockWebContainer: {
    fs: {
      readdir: vi.Mock;
      rm: vi.Mock;
    }
  };

  beforeEach(() => {
    mockWebContainer = {
      fs: {
        readdir: vi.fn(),
        rm: vi.fn().mockResolvedValue(undefined),
      }
    };
    vi.clearAllMocks();
  });

  test('should do nothing if webcontainerInstance is null', async () => {
    await cleanFileSystem(null);
  });

  test('should remove critical directories if they exist', async () => {
    mockWebContainer.fs.readdir.mockResolvedValue(['src', 'public', 'node_modules', 'other-file.txt']);

    await cleanFileSystem(mockWebContainer as unknown as WebContainer);

    expect(mockWebContainer.fs.readdir).toHaveBeenCalledWith('/');

    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/src', { recursive: true, force: true });
    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/public', { recursive: true, force: true });
    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/node_modules', { recursive: true, force: true });

    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/other-file.txt');

    expect(mockWebContainer.fs.rm).toHaveBeenCalledTimes(4);
  });

  test('should handle case where critical directories do not exist', async () => {
    mockWebContainer.fs.readdir.mockResolvedValue(['package.json', 'README.md']);

    await cleanFileSystem(mockWebContainer as unknown as WebContainer);

    expect(mockWebContainer.fs.readdir).toHaveBeenCalledWith('/');

    expect(mockWebContainer.fs.rm).not.toHaveBeenCalledWith('/src', expect.anything());
    expect(mockWebContainer.fs.rm).not.toHaveBeenCalledWith('/public', expect.anything());
    expect(mockWebContainer.fs.rm).not.toHaveBeenCalledWith('/node_modules', expect.anything());

    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/package.json');
    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/README.md');

    expect(mockWebContainer.fs.rm).toHaveBeenCalledTimes(2);
  });

  test('should skip hidden files', async () => {
    mockWebContainer.fs.readdir.mockResolvedValue(['.git', '.env', 'package.json']);

    await cleanFileSystem(mockWebContainer as unknown as WebContainer);

    expect(mockWebContainer.fs.readdir).toHaveBeenCalledWith('/');

    expect(mockWebContainer.fs.rm).not.toHaveBeenCalledWith('/.git');
    expect(mockWebContainer.fs.rm).not.toHaveBeenCalledWith('/.env');

    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/package.json');

    expect(mockWebContainer.fs.rm).toHaveBeenCalledTimes(1);
  });

  test('should handle error when removing a file', async () => {
    const consoleSpy = vi.spyOn(console, 'log');

    mockWebContainer.fs.readdir.mockResolvedValue(['file1.txt', 'file2.txt']);

    mockWebContainer.fs.rm.mockImplementation((path) => {
      if (path === '/file1.txt') {
        return Promise.reject(new Error('Failed to remove file'));
      }
      return Promise.resolve();
    });

    await cleanFileSystem(mockWebContainer as unknown as WebContainer);

    expect(mockWebContainer.fs.readdir).toHaveBeenCalledWith('/');

    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/file1.txt');
    expect(mockWebContainer.fs.rm).toHaveBeenCalledWith('/file2.txt');

    expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('Error removing file1.txt'), expect.anything());
    expect(mockWebContainer.fs.rm).toHaveBeenCalledTimes(2);
  });

  test('should handle error in readdir', async () => {
    const consoleSpy = vi.spyOn(console, 'log');

    mockWebContainer.fs.readdir.mockRejectedValue(new Error('Failed to read directory'));

    await cleanFileSystem(mockWebContainer as unknown as WebContainer);

    expect(mockWebContainer.fs.readdir).toHaveBeenCalledWith('/');

    expect(mockWebContainer.fs.rm).not.toHaveBeenCalled();

    expect(consoleSpy).toHaveBeenCalledWith('Error during filesystem reset:', expect.anything());
  });
});