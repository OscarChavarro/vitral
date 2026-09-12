/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import * as fs from "node:fs";
import { PersistenceElement as PersistenceElementBase } from "@vitral/base/vsdk/toolkit/io/PersistenceElement";
import { StringTokenizer } from "@vitral/base/java/util/StringTokenizer";
import { File } from "../../../java/io/File.js";

/**
File-system-bound half of `vsdk.toolkit.io.PersistenceElement`.

Everything that only moves bytes in and out of streams lives in
`@vitral/base`, so that a browser frontend can use it; this subclass adds the
handful of Java methods that reach the local filesystem, which a browser
sandbox cannot have. That split is the only difference against the Java
original.

Java's `verifyLibrary` is intentionally absent: it answers whether a JNI
native library is resolvable through `java.library.path`, a question that has
no counterpart in a Node runtime.
*/
export abstract class PersistenceElement extends PersistenceElementBase {
    public static checkDirectory(dirName: string): boolean {
        const dirFd = new File(dirName);

        if (dirFd.exists() && !dirFd.isDirectory()) {
            // Java writes this through System.err; `java.lang.System` is one of
            // the classes @vitral/base keeps out of its browser-facing surface.
            console.error(
                "Directory " +
                    dirName +
                    " can not be created, because a file with that name already exists (not overwriten).",
            );
            return false;
        }

        if (!dirFd.exists() && !PersistenceElement.mkdir(dirName)) {
            console.error(
                "Directory " + dirName + " can not be created, check permisions and available free disk space.",
            );
            return false;
        }

        return true;
    }

    /**
    Given a filename, this method extract its extension and return it.
    \todo : This method will fail when directory path or filename contains
    more than one dot.  Needs to be fixed.
    @return file extension
    */
    protected static extractExtensionFromFile(fd: File): string {
        const filename = fd.getName().toCString();
        const st = new StringTokenizer(filename, ".");
        const numTokens = st.countTokens();
        for (let i = 0; i < numTokens - 1; i++) {
            st.nextToken();
        }
        const ext = st.nextToken().toCString();
        return ext;
    }

    private static mkdir(dirName: string): boolean {
        try {
            fs.mkdirSync(dirName);
            return true;
        } catch {
            return false;
        }
    }
}
