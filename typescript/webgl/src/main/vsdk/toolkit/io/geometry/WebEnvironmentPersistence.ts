import {
    IOException,
    PersistenceElement,
    ReaderObj,
    SimpleScene,
    StringReader,
    StringTokenizer,
    type ObjResourceProvider,
    type Reader,
} from "@vitral/base";

/**
The URL-bound half of `vsdk.toolkit.io.geometry.EnvironmentPersistence`, the
counterpart of the `java.io.File` importer in `@vitral/fs`.

A browser has no local file system, so the resource a browser program names is
a URL fetched over HTTP. This class is to `@vitral/webgl` what
`EnvironmentPersistence` is to Node: the same extension dispatch, over the same
reader. `ReaderObj` itself is not duplicated — it lives in `@vitral/base` under
its Java package and takes an {@link ObjResourceProvider} for its two file
operations, and what this class supplies is the browser one.

The provider must answer synchronously, because the Java parser it serves is
synchronous from beginning to end. A `fetch` is not, so the whole resource
graph is pulled in first: the `.obj` text, then the text of every `.mtl` its
`mtllib` lines name. That pre-pass is the only thing this port adds to the Java
control flow; the parse that follows sees exactly what Java's `FileReader`
would have handed it.

As in Java, only the `obj` branch is wired: of the eight readers the Java class
reaches (obj, 3ds, gts, ply, ase, wrl/gz, vtk and bin), `ReaderObj` is the only
one ported to TypeScript to date, and an unrecognized extension leaves the
scene untouched, which is what Java does.
*/
export class WebEnvironmentPersistence extends PersistenceElement {
    public static async importEnvironment(sceneUrl: string, inoutScene: SimpleScene): Promise<void> {
        const type = WebEnvironmentPersistence.extractExtensionFromUrl(sceneUrl).toLowerCase();

        if (type === "obj") {
            const provider = await WebFetchedObjResourceProvider.forObjUrl(sceneUrl);
            ReaderObj.importEnvironment(sceneUrl, inoutScene, provider);
        }
    }

    /**
    The counterpart of Java's `extractExtensionFromFile`, which asks a `File`
    for its name. A URL can carry a query string and a fragment, and neither is
    part of the resource name, so both are dropped before the last dot is
    looked for.
    */
    public static extractExtensionFromUrl(resourceUrl: string): string {
        const withoutFragment = resourceUrl.split("#")[0] ?? "";
        const withoutQuery = withoutFragment.split("?")[0] ?? "";
        const name = withoutQuery.substring(withoutQuery.lastIndexOf("/") + 1);
        const dot = name.lastIndexOf(".");
        if (dot < 0) {
            return "";
        }
        return name.substring(dot + 1);
    }
}

/**
The browser half of {@link ObjResourceProvider}: it replays text that has
already been fetched.
*/
class WebFetchedObjResourceProvider implements ObjResourceProvider {
    private constructor(private readonly texts: ReadonlyMap<string, string>) {}

    /**
    Fetches the `.obj` resource and, before any parsing begins, every `.mtl`
    resource its `mtllib` lines name, so that the synchronous parser can be
    served from memory. A material library that cannot be fetched is left out
    of the map, and `openReader` then throws where Java's `FileReader`
    constructor would have thrown `FileNotFoundException` — which `ReaderObj`
    already catches around its material read.
    */
    public static async forObjUrl(objUrl: string): Promise<WebFetchedObjResourceProvider> {
        const texts = new Map<string, string>();
        const objText = await WebFetchedObjResourceProvider.fetchText(objUrl);
        texts.set(objUrl, objText);

        for (const lineOfText of objText.split("\n")) {
            if (!lineOfText.startsWith("mtllib ")) {
                continue;
            }
            const st = new StringTokenizer(lineOfText, " ");
            st.nextToken(); // "mtllib" token
            if (!st.hasMoreTokens()) {
                continue;
            }
            const materialUrl = WebFetchedObjResourceProvider.resolve(objUrl, st.nextToken().toCString().trim());
            if (texts.has(materialUrl)) {
                continue;
            }
            try {
                texts.set(materialUrl, await WebFetchedObjResourceProvider.fetchText(materialUrl));
            } catch {
                // Left absent on purpose: see the note above.
            }
        }

        return new WebFetchedObjResourceProvider(texts);
    }

    public openReader(resourceName: string): Reader {
        const text = this.texts.get(resourceName);
        if (text === undefined) {
            throw new IOException('Resource not fetched: "' + resourceName + '"');
        }
        return new StringReader(text);
    }

    public resolveSibling(resourceName: string, siblingName: string): string {
        return WebFetchedObjResourceProvider.resolve(resourceName, siblingName);
    }

    private static resolve(baseUrl: string, siblingName: string): string {
        return new URL(siblingName, new URL(baseUrl, globalThis.location.href)).href;
    }

    private static async fetchText(resourceUrl: string): Promise<string> {
        const response = await fetch(resourceUrl);
        if (!response.ok) {
            throw new IOException(
                'Could not read "' + resourceUrl + '" (HTTP ' + response.status + " " + response.statusText + ")",
            );
        }
        return response.text();
    }
}
