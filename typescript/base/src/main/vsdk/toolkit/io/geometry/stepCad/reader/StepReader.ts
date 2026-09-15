//= References:                                                             =
//= [ISO10303-21] ISO 10303-21:2016. Industrial automation systems and      =
//=     integration -- Product data representation and exchange -- Part 21: =
//=     Implementation methods: Clear text encoding of the exchange         =
//=     structure.                                                          =
//= [ISO10303-242] ISO 10303-242:2020. Application protocol: Managed        =
//=     model-based 3D engineering (AP242).                                 =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import type { InputStream } from "../../../../../../java/io/InputStream.js";
import { IllegalArgumentException } from "../../../../../../java/lang/IllegalArgumentException.js";
import type { IntegerKeyHashMap } from "../../../../../../java/util/IntegerKeyHashMap.js";
import type { PolyhedralBoundedSolid } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PersistenceElement } from "../../../PersistenceElement.js";
import type { _StepEntity } from "./_StepEntity.js";
import { _StepSolidBuilder } from "./_StepSolidBuilder.js";
import { _StepTokenizer } from "./_StepTokenizer.js";

/**
Reads an ISO 10303-21 ASCII STEP file and reconstructs a
`PolyhedralBoundedSolid` from it.

This class is the inverse of `StepWriter`. It assumes the STEP file
was produced by a CAD kernel that satisfies the same robustness criteria
enforced at write time:

  - The file encodes exactly one MANIFOLD_SOLID_BREP.
  - The solid is closed: every EDGE_CURVE is referenced by exactly two
    ORIENTED_EDGEs with opposite orientations (.T. and .F.).
  - Every FACE_OUTER_BOUND loop has three or more vertices (no
    degenerate faces).
  - Coordinates are in metres.
  - Vertex positions can be resolved transitively via VERTEX_POINT and
    CARTESIAN_POINT.
  - Edge geometry (EDGE_CURVE params[3]) may be LINE, SURFACE_CURVE,
    or B_SPLINE_CURVE_WITH_KNOTS.  Spline edges are accepted and treated
    as straight-line segments; this is valid when the solid has been
    planarised (tangent directions coincide with the chord between the
    two endpoint VERTEX_POINTs).

The reader orchestrates three collaborators:

  - `_StepTokenizer`  — parses the DATA section into a flat entity map.
  - `_StepSolidBuilder` — traverses the entity map and builds the PBS
                          half-edge structure in four passes.
  - `PolyhedralBoundedSolidValidationEngine.validateIntermediate` — post-
                          construction integrity check.

If validation fails after construction the solid is still returned, but
a warning is printed to stderr so that callers can decide whether to
proceed or abort.

Port of `vsdk.toolkit.io.geometry.stepCad.reader.StepReader`. Java offers two
overloads, `readSolid(File)` and `readSolid(InputStream)`, and the first only
opens the file and hands its stream to the second. This package reaches no
file system — see `PersistenceElement` — so it carries the stream overload,
and a caller that has the bytes, from `fetch` in a browser or from a file in
Node, wraps them in a `ByteArrayInputStream`.
*/
export class StepReader extends PersistenceElement {
    private constructor() {
        super();
    }

    /**
    Reads a STEP file from an input stream and returns a reconstructed
    `PolyhedralBoundedSolid`. The stream is not closed by this method.

    @param inStream input stream for the STEP file.
    @return the reconstructed solid.
    @throws Exception on I/O errors or malformed STEP content.
    */
    public static readSolid(inStream: InputStream | null): PolyhedralBoundedSolid {
        if (inStream === null) {
            throw new IllegalArgumentException("input stream is null");
        }

        const entities: IntegerKeyHashMap<_StepEntity> = _StepTokenizer.parse(inStream);

        const solid: PolyhedralBoundedSolid = _StepSolidBuilder.build(entities);

        if (!PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)) {
            console.error(
                "StepReader: reconstructed solid failed validateIntermediate. " +
                    "The source STEP file may not satisfy manifold assumptions.",
            );
        }

        return solid;
    }
}
