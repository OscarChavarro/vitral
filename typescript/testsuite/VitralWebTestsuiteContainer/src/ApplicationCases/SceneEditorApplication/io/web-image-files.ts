import {
  ImagePersistence,
  RGBAImageUncompressed,
  type Image,
  type IndexedColorImageUncompressed,
  type RGBImageUncompressed,
} from '@vitral/base';
import { HtmlImageRenderer, WebImagePersistence, downloadFile } from '@vitral/webgl';
import type { ImageFileAccess } from './image-files';

/**
 * The `ImageFileAccess` of a page: images are fetched from the server with
 * `WebImagePersistence`, and written files are handed to the user as
 * downloads. A PNG is encoded by `ImagePersistence` of `@vitral/base` (as Java
 * encodes it with its own writer); a JPG by the browser's encoder, with the
 * quality of Java's `ImageIO` default (0.75).
 */
export class WebImageFiles implements ImageFileAccess {
  private static readonly JPEG_QUALITY = 0.75;

  async importRGBA(path: string): Promise<RGBAImageUncompressed> {
    const image: Image = await WebImagePersistence.importRGBA(path);
    if (!(image instanceof RGBAImageUncompressed)) {
      throw new Error('"' + path + '" is not an uncompressed RGBA image');
    }
    return image;
  }

  importRGB(path: string): Promise<Image> {
    return WebImagePersistence.importRGB(path);
  }

  importIndexedColor(path: string): Promise<IndexedColorImageUncompressed> {
    return WebImagePersistence.importIndexedColor(path);
  }

  async exportJPG(path: string, image: RGBImageUncompressed): Promise<boolean> {
    const canvas: HTMLCanvasElement = HtmlImageRenderer.exportToCanvas(image);
    const blob: Blob | null = await new Promise<Blob | null>((resolve): void =>
      canvas.toBlob(resolve, 'image/jpeg', WebImageFiles.JPEG_QUALITY),
    );
    if (blob === null) {
      return false;
    }
    downloadFile(path, new Uint8Array(await blob.arrayBuffer()), 'image/jpeg');
    return true;
  }

  async exportPNG(path: string, image: RGBImageUncompressed): Promise<boolean> {
    downloadFile(path, ImagePersistence.exportPNGToByteArray(image), 'image/png');
    return true;
  }

  async writeBytes(path: string, contents: Uint8Array): Promise<void> {
    downloadFile(path, contents);
  }
}
