import { WebContainer } from '@webcontainer/api';

/**
 * Normalises file structure to ensure compatibility with WebContainer API
 * Properly handles nested paths like "src/index.js" by creating directory structure
 */

// Types
type FileContent = { contents: string };
type FileEntry = { file: FileContent };
type DirectoryEntry = { directory: Record<string, any> };
type FileData = string | FileEntry | DirectoryEntry | Record<string, any>;
type NormalisedFiles = Record<string, FileEntry | DirectoryEntry>;

/**
 * Main function to normalize files for WebContainer
 */
export const normaliseFiles = (input: any): NormalisedFiles => {
  console.log('Original input:', input);
  
  const files = input.files ? input.files : input;
  
  const result: NormalisedFiles = {};
  // console.log('Files to process:', files);

  Object.keys(files).forEach(path => {
    const fileData = files[path];
    console.log(`\nProcessing path: "${path}" with type: ${typeof fileData}`);
    
    // Handle package.json specially
    if (path === "package.json") {
      if (typeof fileData === 'string') {
        // String format
        result[path] = { file: { contents: fileData } };
      } else if (typeof fileData === 'object') {
        if ('file' in fileData && 'contents' in fileData.file) {
          // Already in correct format
          result[path] = fileData;
        } else if ('contents' in fileData) {
          // Has contents property directly
          result[path] = { file: { contents: fileData.contents } };
        } else {
          // Direct package.json object (like with name, version, etc.)
          result[path] = { file: { contents: JSON.stringify(fileData, null, 2) } };
        }
      }
      return;
    }

    // Handle standard files
    if (typeof fileData === 'string') {
      console.log(`Processing string content for: ${path}`);
      if (path.includes('/')) {
        const { directory, fileName } = createDirectoryStructure(path, result);
        directory[fileName] = { file: { contents: fileData } };
      } else {
        result[path] = { file: { contents: fileData } };
      }
      return;
    }

    if (isFileWithContents(fileData)) {
      processFileWithContents(path, fileData, result);
    } else if (isFileEntry(fileData)) {
      processFileEntry(path, fileData, result);
    } else if (isDirectoryEntry(fileData)) {
      processDirectoryEntry(path, fileData, result);
    } else if (typeof fileData === 'object') {
      // handles objects that are not in the required format
      if (Object.keys(fileData).some(key => 
          ['name', 'version', 'scripts', 'dependencies', 'devDependencies'].includes(key))) {
        // package.json content slipped through
        result[path] = { file: { contents: JSON.stringify(fileData, null, 2) } };
      } else {
        // assumes implicit directory object
        console.log(`Processing implicit directory object: ${path}`);
        result[path] = { directory: normaliseFiles(fileData) };
      }
    }
  });

  console.log('Normalized result structure:', result);
  return result;
};

/**
 * Checks if data is a file with direct contents property
 */
const isFileWithContents = (data: FileData): data is { contents: string } => {
  return !!data && typeof data === 'object' && 'contents' in data && typeof data.contents === 'string';
};

/**
 * Checks if data is a properly structured file entry
 */
const isFileEntry = (data: FileData): data is { file: { contents?: string } } => {
  return !!data && typeof data === 'object' && 'file' in data;
};

/**
 * Checks if data is a directory entry
 */
const isDirectoryEntry = (data: FileData): data is { directory: Record<string, any> } => {
  return !!data && typeof data === 'object' && 'directory' in data;
};

/**
 * Special handling for package.json files
 */
const handlePackageJson = (fileData: FileData): FileEntry | null => {
  // Handle string format
  if (typeof fileData === 'string') {
    console.log('Detected package.json as string, converting to proper format');
    return { file: { contents: fileData.trim() } };
  }
  
  // Handle object format with direct properties
  if (fileData && typeof fileData === 'object' && 
      !('file' in fileData) && !('contents' in fileData) && 
      (('dependencies' in fileData) || ('name' in fileData) || ('version' in fileData))) {
    console.log('Detected direct package.json object, converting to proper format');
    return { file: { contents: JSON.stringify(fileData, null, 2) } };
  }
  
  return null;
};

/**
 * Process a file with direct contents property
 */
const processFileWithContents = (path: string, fileData: { contents: string }, result: NormalisedFiles) => {
  if (path.includes('/')) {
    // Handle nested path
    const { directory, fileName } = createDirectoryStructure(path, result);
    directory[fileName] = { file: { contents: fileData.contents } };
    console.log(`Added file "${fileName}" to directory with content length: ${fileData.contents.length} chars`);
  } else {
    // Handle root-level file
    result[path] = { file: { contents: fileData.contents } };
    console.log(`Added root file with content: ${path}`);
  }
};

/**
 * Process a standard file entry
 */
const processFileEntry = (path: string, fileData: { file: { contents?: string } }, result: NormalisedFiles) => {
  if (path.includes('/')) {
    // Handle nested path
    const { directory, fileName } = createDirectoryStructure(path, result);
    const contents = fileData.file.contents || '';
    directory[fileName] = { file: { contents } };
    console.log(`Added file "${fileName}" to directory with content length: ${contents.length} chars`);
  } else {
    // Handle root-level file
    if (Object.keys(fileData.file).length === 0) {
      result[path] = { file: { contents: "" } };
      console.log(`Added empty file: ${path}`);
    } else if (fileData.file.contents) {
      result[path] = { file: { contents: fileData.file.contents } };
      console.log(`Added file with content: ${path}`);
    }
  }
};

/**
 * Process a directory entry
 */
const processDirectoryEntry = (path: string, fileData: { directory: Record<string, any> }, result: NormalisedFiles) => {
  console.log(`Processing directory: ${path}`);
  result[path] = { directory: normaliseFiles(fileData.directory) };
};

/**
 * Creates directory structure for nested paths
 */
const createDirectoryStructure = (path: string, result: NormalisedFiles) => {
  const segments = path.split('/');
  const fileName = segments.pop() || '';
  console.log(`Path segments: ${JSON.stringify(segments)}, Filename: ${fileName}`);

  let current = result;
  for (const segment of segments) {
    if (!current[segment]) {
      console.log(`Creating directory: ${segment}`);
      current[segment] = { directory: {} };
    } else if (!('directory' in current[segment])) {
      console.error(`Expected directory but found file at ${segment}`);
      // Convert to directory if needed
      current[segment] = { directory: {} };
    }
    
    // Type assertion is needed here
    current = (current[segment] as DirectoryEntry).directory;
  }

  return { directory: current, fileName };
};

/**
 * Clean the filesystem for a fresh start
 * @param webcontainerInstance The WebContainer instance
 * @returns Promise that resolves when cleaning is complete
 */
export const cleanFileSystem = async (webcontainerInstance: WebContainer | null): Promise<void> => {
  if (!webcontainerInstance) return;
  
  try {
    const entries = await webcontainerInstance.fs.readdir('/');
    console.log('Current root entries:', entries);
    
    // Remove critical directories
    for (const dir of ['src', 'public', 'node_modules']) {
      if (entries.includes(dir)) {
        await webcontainerInstance.fs.rm(`/${dir}`, { recursive: true, force: true });
      }
    }
    
    // Remove other non-hidden files
    for (const entry of entries) {
      if (!entry.startsWith('.') && !['src', 'public', 'node_modules'].includes(entry)) {
        try {
          await webcontainerInstance.fs.rm(`/${entry}`);
          console.log(`Removed file: ${entry}`);
        } catch (e) {
          console.log(`Error removing ${entry}:`, e);
        }
      }
    }
    
    console.log('Filesystem reset complete');
  } catch (e) {
    console.log('Error during filesystem reset:', e);
  }
};