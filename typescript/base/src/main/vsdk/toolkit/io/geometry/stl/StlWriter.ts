import { ByteArrayOutputStream } from "../../../../../java/io/ByteArrayOutputStream.js";
import type { OutputStream } from "../../../../../java/io/OutputStream.js";
import { Double } from "../../../../../java/lang/Double.js";
import { IllegalArgumentException } from "../../../../../java/lang/IllegalArgumentException.js";
import type { PolyhedralBoundedSolid } from "../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PersistenceElement } from "../../PersistenceElement.js";
import { _StlFaceTriangulator } from "./_StlFaceTriangulator.js";
import { _StlFacetEmitter } from "./_StlFacetEmitter.js";
import { _StlSolidValidator } from "./_StlSolidValidator.js";

/**
Exports a `PolyhedralBoundedSolid` to binary STL.

STL carries no unit metadata. Callers targeting slicers that assume
millimetres should use the `scaleFactor` overload to convert from the
model's internal units.

Port of `vsdk.toolkit.io.geometry.stl.StlWriter`. Java's two `exportSolid`
overloads are one method whose scale factor defaults to one.
*/
export class StlWriter extends PersistenceElement {
    private static readonly HEADER_LABEL = "VitralSolid";
    private static readonly STL_BINARY_HEADER_LENGTH = 80;

    private constructor() {
        super();
    }

    public static exportSolid(
        solid: PolyhedralBoundedSolid | null,
        outputStream: OutputStream | null,
        scaleFactor = 1.0,
    ): void {
        if (solid === null) {
            throw new IllegalArgumentException("solid is null");
        }
        if (outputStream === null) {
            throw new IllegalArgumentException("outputStream is null");
        }
        if (!Double.isFinite(scaleFactor) || scaleFactor <= 0.0) {
            throw new IllegalArgumentException("scaleFactor must be finite and > 0");
        }

        _StlSolidValidator.validate(solid);
        const facets: _StlFacetEmitter.Facet[] = _StlFaceTriangulator.triangulateSolid(solid);

        const buffer = new ByteArrayOutputStream(StlWriter.STL_BINARY_HEADER_LENGTH + 4 + facets.length * 50);
        _StlFacetEmitter.writeBinaryStl(buffer, StlWriter.HEADER_LABEL, facets, scaleFactor);
        buffer.writeTo(outputStream);
    }
}
