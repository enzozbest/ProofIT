/**
 * Normalises file structure to ensure compatibility with WebContainer API
 * Properly handles nested paths like "src/index.js" by creating directory structure
 */
export const normaliseFiles = (
  files: Record<string, any>
): Record<string, any> => {
  console.log('Normalise files:', JSON.stringify(files, null, 2));
  const result: Record<string, any> = {};

  Object.keys(files).forEach((path) => {
    console.log(`\n Processing path: "${path}"`);
    const fileData = files[path];
    console.log('File data:', JSON.stringify(fileData, null, 2));

    if (path === 'package.json') {
      if (typeof fileData === 'string') {
        console.log(
          'Detected package.json as string, converting to proper format'
        );
        result[path] = { file: { contents: fileData.trim() } };
        return;
      }

      if (
        fileData &&
        typeof fileData === 'object' &&
        !fileData.file &&
        !fileData.contents &&
        (fileData.dependencies || fileData.name || fileData.version)
      ) {
        console.log(
          'Detected direct package.json object, converting to proper format'
        );
        result[path] = {
          file: { contents: JSON.stringify(fileData, null, 2) },
        };
        return;
      }
    }

    if (fileData.contents) {
      // fileData directly contains file contents
      if (path.includes('/')) {
        console.log(`Path "${path}" contains directories`);
        const segments = path.split('/');
        const fileName = segments.pop() || '';
        console.log(
          `Segments: ${JSON.stringify(segments)}, Filename: ${fileName}`
        );

        let current = result;
        for (const segment of segments) {
          console.log(`Creating/navigating to directory: ${segment}`);
          if (!current[segment]) {
            console.log(`  Creating new directory: ${segment}`);
            current[segment] = { directory: {} };
          } else {
            console.log(`  Directory ${segment} already exists`);
          }
          current = current[segment].directory;
        }

        console.log(
          `Adding file "${fileName}" to directory with content length: ${fileData.contents.length} chars`
        );
        current[fileName] = { file: { contents: fileData.contents } };
      } else {
        console.log(`Path "${path}" is at root level`);
        console.log(`  Adding file with content: ${path}`);
        result[path] = { file: { contents: fileData.contents } };
      }
    } else if (fileData.file) {
      // correct file structure
      if (path.includes('/')) {
        console.log(`Path "${path}" contains directories`);
        const segments = path.split('/');
        const fileName = segments.pop() || '';
        console.log(
          `Segments: ${JSON.stringify(segments)}, Filename: ${fileName}`
        );

        let current = result;
        for (const segment of segments) {
          console.log(`Creating/navigating to directory: ${segment}`);
          if (!current[segment]) {
            console.log(`  Creating new directory: ${segment}`);
            current[segment] = { directory: {} };
          } else {
            console.log(`  Directory ${segment} already exists`);
          }
          console.log(
            `  Current structure at ${segment}:`,
            JSON.stringify(current[segment], null, 2)
          );
          current = current[segment].directory;
        }

        if (fileData.file) {
          const contents = fileData.file.contents || '';
          console.log(
            `Adding file "${fileName}" to directory with content length: ${contents.length} chars`
          );
          current[fileName] = { file: { contents } };
        }
      } else {
        console.log(`Path "${path}" is at root level`);
        if (fileData.file && Object.keys(fileData.file).length === 0) {
          console.log(`  Adding empty file: ${path}`);
          result[path] = { file: { contents: '' } };
        } else if (fileData.file && fileData.file.contents) {
          console.log(`  Adding file with content: ${path}`);
          result[path] = { file: { contents: fileData.file.contents } };
        }
      }
    } else if (fileData.directory) {
      console.log(`  Processing directory: ${path}`);
      result[path] = { directory: normaliseFiles(fileData.directory) };
    }
  });

  console.log('Normalised result:', JSON.stringify(result, null, 2));
  return result;
};
