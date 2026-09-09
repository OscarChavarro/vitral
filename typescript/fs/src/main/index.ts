/**
 * Server-side filesystem adapters. This package imports Node's filesystem API
 * and must not be installed or bundled into a browser frontend.
 */
export { File } from "./java/io/File.js";
export { FileInputStream } from "./java/io/FileInputStream.js";
export { FileOutputStream } from "./java/io/FileOutputStream.js";
export { FileReader } from "./java/io/FileReader.js";
export { RandomAccessFile } from "./java/io/RandomAccessFile.js";
