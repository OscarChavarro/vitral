import { IOException, Md2Persistence, RGBImageUncompressed, type Image, type Md2Mesh } from "@vitral/base";
import { WebImagePersistence } from "../image/WebImagePersistence.js";

/**
The URL-bound half of `vsdk.toolkit.io.geometry.Md2Persistence`, the
counterpart of the `RandomAccessFile` reader in `@vitral/base`.

A browser has no local file system, so an MD2 model and its skin are two URLs
fetched over HTTP. Java's `Md2Persistence.read(String, String, Md2Mesh)` opens
the model with `RandomAccessFile` and the skin with
`ImagePersistence.importRGB(File)`; this class fetches the model's bytes and
decodes the skin through {@link WebImagePersistence}, then hands both to the
platform-neutral parser, which is where every byte of the Java control flow
lives.

Java's `loadImagefile` lives here, because only this half knows the resource
name: an unnamed skin answers null and leaves the mesh's skin slot empty, and a
named one that cannot be fetched or decoded prints to `System.err` and answers
the same 64x64 test pattern, rather than throwing. A model resource that cannot
be fetched is the other case, and Java's `RandomAccessFile` constructor throws
there, which is what happens here too.

Since a fetch is asynchronous, both overloads are asynchronous, and the parse
they wrap is not.
*/
export class WebMd2Persistence {
    /**
    Reads an MD2 model and its skin, named by URL.

    @param md2Url the URL of the MD2 resource
    @param textureUrl the URL of the skin image, or null for no skin
    @param outMd2Mesh the mesh to fill
    @param gammaCorrection when given, the exponent Java's four-argument
    `read` applies to the skin
    @return true if loaded successfully, as Java's `read` answers
    */
    public static async read(
        md2Url: string,
        textureUrl: string | null,
        outMd2Mesh: Md2Mesh,
        gammaCorrection?: number,
    ): Promise<boolean> {
        const bytes: Uint8Array = await WebMd2Persistence.fetchBytes(md2Url);
        const skin: Image | null = await WebMd2Persistence.loadSkin(textureUrl);
        const md2Persistence = new Md2Persistence();
        if (gammaCorrection === undefined) {
            return md2Persistence.read(bytes, skin, outMd2Mesh);
        }
        return md2Persistence.read(bytes, skin, outMd2Mesh, gammaCorrection);
    }

    private static async fetchBytes(resourceUrl: string): Promise<Uint8Array> {
        let response: Response;
        try {
            response = await fetch(resourceUrl);
        } catch (error) {
            throw new IOException("Unable to fetch " + resourceUrl + ": " + String(error));
        }
        if (!response.ok) {
            throw new IOException("Unable to fetch " + resourceUrl + ": HTTP " + String(response.status));
        }
        return new Uint8Array(await response.arrayBuffer());
    }

    /**
    Java's `loadImagefile(String)`, over a URL instead of a `File`.
    */
    private static async loadSkin(textureUrl: string | null): Promise<Image | null> {
        let img: RGBImageUncompressed;

        if (textureUrl === null || textureUrl.length < 1) {
            return null;
        }
        try {
            return await WebImagePersistence.importRGB(textureUrl);
        } catch (e) {
            console.error('Error: could not read the image file "' + textureUrl + '".');
            console.error("Check you have access to that resource from the serving origin.");
            console.error(String(e));
            img = new RGBImageUncompressed();
            img.init(64, 64);
            img.createTestPattern();
            return img;
        }
    }
}
