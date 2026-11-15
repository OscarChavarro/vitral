/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import { ReaderAltimetry as ReaderAltimetryParser } from "@vitral/base/vsdk/toolkit/io/geometry/ReaderAltimetry";
import { VSDKFatalException } from "@vitral/base/vsdk/toolkit/common/VSDKFatalException";
import type { HeightFieldSolid } from "@vitral/base/vsdk/toolkit/environment/geometry/volume/HeightFieldSolid";
import { FileInputStream } from "../../../../java/io/FileInputStream.js";

/**
The file name entry point of cpp `ReaderAltimetry::importHeightField`. The
parser itself is `@vitral/base`'s `ReaderAltimetry`, which reads a stream.
*/
export class ReaderAltimetry {
    // Throws VSDKFatalException for unreadable or malformed files.
    public static importHeightField(fileName: string): HeightFieldSolid {
        let stream: FileInputStream;
        try {
            stream = new FileInputStream(fileName);
        } catch {
            throw new VSDKFatalException("ReaderAltimetry: could not open file");
        }
        try {
            return ReaderAltimetryParser.importHeightField(stream);
        } finally {
            stream.close();
        }
    }
}
