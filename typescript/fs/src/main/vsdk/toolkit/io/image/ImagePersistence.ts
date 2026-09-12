import { deflateSync } from "node:zlib";
import {
    ImagePersistence as ImagePersistenceBase,
    ImagePersistencePng,
    Image,
    Logger,
    VSDK,
    OutputStream,
    BufferedOutputStream,
} from "@vitral/base";
import { File } from "../../../../java/io/File.js";
import { FileOutputStream } from "../../../../java/io/FileOutputStream.js";

// Node owns a real DEFLATE implementation, so the platform-neutral PNG writer
// is upgraded from its dependency-free stored-blocks default to true zlib
// compression. The bytes stay a normal PNG either way; only their size changes.
ImagePersistencePng.setDeflater((rawBytes: Uint8Array): Uint8Array => {
    const compressed = deflateSync(rawBytes);
    return new Uint8Array(compressed.buffer, compressed.byteOffset, compressed.byteLength);
});

/**
File-system-bound half of `vsdk.toolkit.io.image.ImagePersistence`.

The encoding itself lives in `@vitral/base`, where a browser frontend can use
it to build image bytes in memory; this subclass only adds the local-file
flavors, which a browser sandbox cannot have. That split is why this class is
not a 1:1 port of the Java original: Java reaches PNG through `javax.imageio`
and the C++ port through `libpng`, whereas here PNG is encoded by Vitral's own
platform-neutral writer and merely compressed by `node:zlib`. Any additional
format that warrants a third-party codec (JPEG, for instance) is to be added as
an NPM dependency of this package, never of `@vitral/base`.

PPM stays hand-written and dependency-free, as in the Java and C++ ports.
*/
export class ImagePersistence extends ImagePersistenceBase {
    public static override exportPNG(os: OutputStream, img: Image): void;
    public static override exportPNG(fd: File, img: Image): void;
    public static override exportPNG(target: OutputStream | File, img: Image): void {
        if (!(target instanceof File)) {
            ImagePersistenceBase.exportPNG(target, img);
            return;
        }
        try {
            const fos = new FileOutputStream(target);
            const writer = new BufferedOutputStream(fos);
            ImagePersistenceBase.exportPNG(writer, img);
            writer.flush();
            writer.close();
            fos.close();
        } catch {
            Logger.reportMessage(null, VSDK.WARNING, "ImagePersistence", "Error saving PNG image");
        }
    }

    /**
    This method writes the contents of the specified image to a file in
    binary RGB PPM format (i.e. P6 PPM sub-format). Returns true if everything
    works fine, false if something fails, like a permission access denied
    or if storage device runs out of space.
    @param fd
    @param img
    @return
    */
    public static override exportPPM(os: OutputStream, img: Image): boolean;
    public static override exportPPM(fd: File, img: Image): boolean;
    public static override exportPPM(target: OutputStream | File, img: Image): boolean {
        if (!(target instanceof File)) {
            return ImagePersistenceBase.exportPPM(target, img);
        }
        try {
            const fos = new FileOutputStream(target);
            const writer = new BufferedOutputStream(fos);
            const result = ImagePersistenceBase.exportPPM(writer, img);
            writer.flush();
            writer.close();
            fos.close();
            return result;
        } catch {
            return false;
        }
    }
}
