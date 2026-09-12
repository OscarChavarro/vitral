import type { Reader } from "../../../../java/io/Reader.js";

/**
The file-system seam of {@link ReaderObj}.

Java's `vsdk.toolkit.io.geometry.ReaderObj` lives in the base module and opens
its `.obj` and `.mtl` resources through `java.io.FileReader`, because every
Java program VitralSDK serves has a file system under it. The TypeScript
edition serves two runtimes: Node, which has one, and the browser, which has
none and names a resource by URL instead.

The reader's own `\todo` — "should not recieve a filename, but a previously
opened stream, to make it independent of filesystems, and generalize it to URLs
or whatever other connection" — is exactly this seam, so the parser stays one
1:1 port of the Java text and each runtime supplies its own two operations:
`@vitral/fs` opens a `FileReader`, and `@vitral/webgl` replays text it has
already fetched.

Both operations are synchronous, as they are in Java. A browser reaches a
resource over `fetch`, which is not, so the browser provider fetches the `.obj`
and every `.mtl` it names *before* the parse begins and then answers from what
it holds; see `WebEnvironmentPersistence`.
*/
export interface ObjResourceProvider {
    /**
    Answers a character reader over the named resource, the counterpart of
    Java's `new FileReader(name)`.

    @param resourceName name of the resource, in whatever spelling
           {@link resolveSibling} produces for this runtime
    @return an open reader; throws when the resource cannot be read, as Java's
            constructor throws `FileNotFoundException`
    */
    openReader(resourceName: string): Reader;

    /**
    Resolves the name of a resource that sits beside another one, the
    counterpart of Java's `new File(name).getParentFile() + File.separator +
    siblingName`. It is how an `.obj` file names its material library.

    @param resourceName the resource the sibling is named relative to
    @param siblingName the name as it appears inside `resourceName`
    @return the resolved name, to be handed back to {@link openReader}
    */
    resolveSibling(resourceName: string, siblingName: string): string;
}
