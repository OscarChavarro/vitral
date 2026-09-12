import { platformPrint, platformPrintln } from "@vitral/base";
import type { RGBImageUncompressed } from "@vitral/base";
import { File, ImagePersistence } from "@vitral/fs";

export class ImageExporter {
    public export(outputFileName: string, image: RGBImageUncompressed): boolean {
        const outputFile: File = new File(outputFileName);

        platformPrint('Exporting result image to file "' + outputFileName + '": ');
        const exported: boolean = this.exportToFile(outputFile, image);
        if (exported) {
            platformPrintln(" OK!");
        }
        return exported;
    }

    /**
    Java overloads `export` on `String` / `File`; TypeScript overload
    resolution on two one-argument-different signatures would be ambiguous at
    the call site, so the private half keeps a distinct name.
    */
    private exportToFile(outputFile: File, image: RGBImageUncompressed): boolean {
        // Java uses `toLowerCase(Locale.ROOT)`; JavaScript's argument-less
        // `toLowerCase` is already the locale-independent mapping.
        const lowerName: string = outputFile.getName().toString().toLowerCase();
        if (lowerName.endsWith(".png")) {
            ImagePersistence.exportPNG(outputFile, image);
            return true;
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            // Java delegates to `ImagePersistence.exportJPG`, which is backed
            // by javax.imageio. No JPEG encoder is ported to TypeScript yet,
            // so this branch reports the missing capability rather than
            // silently writing a PPM under a .jpg name.
            throw new Error("JPEG export is not ported to TypeScript yet; use a .png or .ppm output file name.");
        }
        return ImagePersistence.exportPPM(outputFile, image);
    }
}
