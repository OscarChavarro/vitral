/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import * as fs from "node:fs";
import { PersistenceElement as PersistenceElementBase } from "@vitral/base/vsdk/toolkit/io/PersistenceElement";
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
    /**
    Follows the C++ `PersistenceElement::checkDirectory` merged from rpkBase:
    an empty name is rejected, a missing directory is created with its
    parents (`File.mkdirs`), and an existing one must be readable and
    writable. Java writes these messages through `System.err`;
    `java.lang.System` is one of the classes @vitral/base keeps out of its
    browser-facing surface, so they go to `console.error`.
    */
    public static checkDirectory(dirName: string | null): boolean {
        if (dirName === null || dirName.length === 0) {
            console.error("Directory name is empty.");
            return false;
        }

        const directory = new File(dirName);
        if (directory.exists() && !directory.isDirectory()) {
            console.error(
                "Directory " +
                    dirName +
                    " can not be created, because a file with that name already exists (not overwriten).",
            );
            return false;
        }

        if (!directory.exists()) {
            if (!PersistenceElement.mkdirs(dirName)) {
                console.error(
                    "Directory " + dirName + " can not be created, check permisions and available free disk space.",
                );
                return false;
            }
        }

        if (!directory.canRead() || !directory.canWrite()) {
            console.error("Directory " + dirName + " is not accessible for read/write operations.");
            return false;
        }

        return true;
    }

    /**
    Given a filename, this method extract its extension and return it: the
    text after its last dot, the whole name when it has no dot, and the
    empty string for an empty name.
    @return file extension
    */
    protected static extractExtensionFromFile(fd: File): string {
        const javaFilename = fd.getName().toCString();
        if (javaFilename.length === 0) {
            return "";
        }

        let extension = javaFilename;
        let cursor = 0;
        while (true) {
            const dot = javaFilename.indexOf(".", cursor);
            if (dot < 0) {
                break;
            }
            extension = javaFilename.substring(dot + 1);
            cursor = dot + 1;
        }

        return extension;
    }

    private static mkdirs(dirName: string): boolean {
        try {
            fs.mkdirSync(dirName, { recursive: true });
            return true;
        } catch {
            return false;
        }
    }
}
