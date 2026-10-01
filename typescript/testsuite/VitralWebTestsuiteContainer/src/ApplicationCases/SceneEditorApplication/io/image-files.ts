import type { Image, IndexedColorImageUncompressed, RGBAImageUncompressed, RGBImageUncompressed } from '@vitral/base';

/**
 * How the application reads and writes files (mostly images). Java calls the
 * static `ImagePersistence` (and opens `FileOutputStream`s) with `java.io.File`
 * paths; a page fetches files from its server and hands written files to the
 * user as downloads, which only the browser side of the application can do
 * (see `io/web-image-files.ts`).
 */
export interface ImageFileAccess {
  /** Java's `ImagePersistence.importRGBA(new File(path))` */
  importRGBA(path: string): Promise<RGBAImageUncompressed>;
  /** Java's `ImagePersistence.importRGB(new File(path))` */
  importRGB(path: string): Promise<Image>;
  /** Java's `ImagePersistence.importIndexedColor(new File(path))` */
  importIndexedColor(path: string): Promise<IndexedColorImageUncompressed>;
  /** Java's `ImagePersistence.exportJPG(new File(path), image)` */
  exportJPG(path: string, image: RGBImageUncompressed): Promise<boolean>;
  /** Java's `ImagePersistence.exportPNG(new File(path), image)` */
  exportPNG(path: string, image: RGBImageUncompressed): Promise<boolean>;
  /**
   * Java's `new FileOutputStream(file)` written with some bytes: the file is
   * handed to the user
   * @param path name of the file
   * @param contents bytes of the file
   */
  writeBytes(path: string, contents: Uint8Array): Promise<void>;
}

/**
 * The `ImageFileAccess` installed by the application: the counterpart of
 * Java's static `ImagePersistence` entry points.
 */
export class ImageFiles {
  private static access: ImageFileAccess | null = null;

  private constructor() {}

  /**
   * @param access how image files are read and written
   */
  static install(access: ImageFileAccess | null): void {
    ImageFiles.access = access;
  }

  /**
   * @return the installed access
   * @throws Error if the application did not install one
   */
  static get(): ImageFileAccess {
    if (ImageFiles.access === null) {
      throw new Error('Image file access is not installed (see ImageFiles.install)');
    }
    return ImageFiles.access;
  }
}
