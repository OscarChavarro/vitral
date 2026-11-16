//= References:                                                             =
//= [ISO10303-21] ISO 10303-21:2016. Industrial automation systems and      =
//=     integration -- Product data representation and exchange -- Part 21: =
//=     Implementation methods: Clear text encoding of the exchange         =
//=     structure.                                                          =
//= [ISO10303-242] ISO 10303-242:2020. Application protocol: Managed        =
//=     model-based 3D engineering (AP242).                                 =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef __STEP_READER__
#define __STEP_READER__

#include "java/io/File.h"
#include "java/io/InputStream.h"
#include "vsdk/toolkit/io/PersistenceElement.h"

class PolyhedralBoundedSolid;

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
  - `PolyhedralBoundedSolidValidationEngine::validateIntermediate` — post-
                          construction integrity check.

If validation fails after construction the solid is still returned, but
a warning is printed to stderr so that callers can decide whether to
proceed or abort.

C++ counterpart of Java's `vsdk.toolkit.io.geometry.stepCad.reader.StepReader`.
*/
class StepReader : public PersistenceElement {
public:
    /**
    Reads a STEP file from the file system and returns a reconstructed
    `PolyhedralBoundedSolid`.

    @param stepFile the `.step` file to read.
    @return the reconstructed solid, owned by the caller.
    @throws java::Exception on I/O errors or malformed STEP content (C++
    port: a file that cannot be opened raises a `java::Exception` with the
    message of Java's `FileNotFoundException`).
    */
    static PolyhedralBoundedSolid* readSolid(const java::File* stepFile);

    /**
    Reads a STEP file from an input stream and returns a reconstructed
    `PolyhedralBoundedSolid`. The stream is not closed by this method.

    @param in input stream for the STEP file.
    @return the reconstructed solid, owned by the caller.
    @throws java::Exception on I/O errors or malformed STEP content.
    */
    static PolyhedralBoundedSolid* readSolid(java::InputStream* in);

private:
    StepReader() {}
};

#endif
