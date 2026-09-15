import type { OutputStream } from "../../../../../java/io/OutputStream.js";
import { Double } from "../../../../../java/lang/Double.js";
import { Float } from "../../../../../java/lang/Float.js";
import { IllegalStateException } from "../../../../../java/lang/IllegalStateException.js";
import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { PersistenceElement } from "../../PersistenceElement.js";

/**
Port of `vsdk.toolkit.io.geometry.stl._StlFacetEmitter`, a package-private
class of the Java module: the binary STL record layout — an eighty-byte
header, a little-endian facet count, and per facet a normal, three vertices
and a zero attribute word — written through the same `PersistenceElement`
little-endian helpers. Java's `getBytes(US_ASCII)` replaces each character
outside ASCII with `?`, which is spelled out here.
*/
export class _StlFacetEmitter {
    private constructor() {}

    public static writeBinaryStl(
        outputStream: OutputStream,
        name: string | null,
        facets: readonly _StlFacetEmitter.Facet[],
        scaleFactor: number,
    ): void {
        const header = new Uint8Array(80);
        const label: string = _StlFacetEmitter.buildHeader(name);
        const labelBytes = new Uint8Array(label.length);
        for (let k = 0; k < label.length; k++) {
            const code: number = label.charCodeAt(k);
            labelBytes[k] = code < 0x80 ? code : 0x3f;
        }
        const copyLength: number = Math.min(header.length, labelBytes.length);
        header.set(labelBytes.subarray(0, copyLength), 0);
        PersistenceElement.writeBytes(outputStream, header);
        PersistenceElement.writeLongLE(outputStream, facets.length);

        let i: number;
        for (i = 0; i < facets.length; i++) {
            const facet: _StlFacetEmitter.Facet = facets[i]!;
            _StlFacetEmitter.writeVector(outputStream, facet.normal, 1.0, "normal", i);
            _StlFacetEmitter.writeVector(outputStream, facet.a, scaleFactor, "vertex a", i);
            _StlFacetEmitter.writeVector(outputStream, facet.b, scaleFactor, "vertex b", i);
            _StlFacetEmitter.writeVector(outputStream, facet.c, scaleFactor, "vertex c", i);
            PersistenceElement.writeSignedShortLE(outputStream, 0);
        }
    }

    private static writeVector(
        outputStream: OutputStream,
        vector: Vector3Dd,
        scaleFactor: number,
        fieldName: string,
        facetIndex: number,
    ): void {
        _StlFacetEmitter.writeFloat(outputStream, vector.x() * scaleFactor, fieldName, facetIndex);
        _StlFacetEmitter.writeFloat(outputStream, vector.y() * scaleFactor, fieldName, facetIndex);
        _StlFacetEmitter.writeFloat(outputStream, vector.z() * scaleFactor, fieldName, facetIndex);
    }

    private static writeFloat(outputStream: OutputStream, value: number, fieldName: string, facetIndex: number): void {
        if (!Double.isFinite(value) || value < -Float.MAX_VALUE || value > Float.MAX_VALUE) {
            throw new IllegalStateException(
                "STL export rejected: facet " +
                    facetIndex +
                    " " +
                    fieldName +
                    " cannot be represented as 32-bit float (" +
                    Double.toString(value) +
                    ")",
            );
        }
        PersistenceElement.writeFloatLE(outputStream, Math.fround(value));
    }

    private static buildHeader(name: string | null): string {
        if (name === null || name.trim().length === 0) {
            return "Vitral STL binary";
        }
        return "Vitral STL binary: " + name.trim();
    }
}

export namespace _StlFacetEmitter {
    export class Facet {
        public constructor(
            public readonly normal: Vector3Dd,
            public readonly a: Vector3Dd,
            public readonly b: Vector3Dd,
            public readonly c: Vector3Dd,
        ) {}
    }
}
