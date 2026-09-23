#ifndef __POLYHEDRAL_BOUNDED_SOLID_MODELER__
#define __POLYHEDRAL_BOUNDED_SOLID_MODELER__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/processing/ProcessingElement.h"

class ParametricCurve;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;

/**
Utility class with static modeling and boolean operations specific to
`PolyhedralBoundedSolid`.

This class contains creation/sweep/split/set-op helpers and centralizes the
polyhedral B-Rep operations previously exposed through `GeometricModeler`.

C++ port note: the `split` and `setOp` wrappers are not ported yet, as the
boolean pipeline of [MANT1988] chapters 14 and 15 (splitter, set operator)
is not available in the C++ port.
*/
class PolyhedralBoundedSolidModeler : public ProcessingElement {
public:
    static const int UNION = 1;
    static const int INTERSECTION = 2;
    static const int SUBTRACT = 3;

    /**
    Applies a transformation matrix to all vertices in the solid.
    @param solid target solid instance.
    @param transformation transformation matrix to apply.
    */
    static void applyTransformation(PolyhedralBoundedSolid* solid,
                                    const Matrix4x4d& transformation);

    /**
    Implements the construction style from [MANT1988] section 12.2 / program
    12.1.

    Builds an arc on plane `z = height`, centered at (`cx`, `cy`), radius
    `radius`. The start vertex already exists in `faceId` and is identified
    by `vertexId`; the method appends `n` new edge steps from `phi1` to
    `phi2` (degrees, counterclockwise, 0 degrees on +X).
    */
    static void addArcToExistingFace(PolyhedralBoundedSolid* solid,
        int faceId, int vertexId, double cx, double cy, double radius,
        double height, double phi1, double phi2, int n);

    /**
    Implements [MANT1988] section 12.2 / program 12.2.

    Creates a planar circular lamina as a single face by:
    1) creating an initial vertex (`mvfs`)
    2) adding arc vertices (`addArc`)
    3) closing the loop (`smef`)
    @return a new solid, owned by the caller
    */
    static PolyhedralBoundedSolid* createCircularLamina(
        double cx, double cy, double rad, double h, int n);

    /**
    Generalized translational sweep of a face, inspired by [MANT1988] 12.3.1.

    Instead of pure translation, transform `T` can include translation,
    rotation, and scale in the face-local context.

    PRE: `face` is closed and planar.
    */
    static void translationalSweepExtrudeFace(PolyhedralBoundedSolid* solid,
        _PolyhedralBoundedSolidFace* face,
        const Matrix4x4d& transformationMatrix);

    /**
    Variant of `translationalSweepExtrudeFace` with an extra planar check
    pass.

    After sweep generation, each created side face is tested for planarity.
    If a face is non-planar, it is split once (`lmef`) to triangulate it.
    */
    static void translationalSweepExtrudeFacePlanar(
        PolyhedralBoundedSolid* solid,
        _PolyhedralBoundedSolidFace* face,
        const Matrix4x4d& transformationMatrix);

    /**
    Rotational sweep of an open wire profile around the X axis, following the
    construction style of [MANT1988] 12.2 and 12.5.

    PRE:
    - `solid` is a wire-like profile (single face, open loop)
    - profile lies on `z = 0`
    - `numberOfFaces >= 3`
    */
    static void rotationalSweepExtrudeWireAroundXAxis(
        PolyhedralBoundedSolid* solid, int numberOfFaces);

    /**
    Builds a planar B-Rep face (with holes) from the contours of a curve.
    @param curve contours, separated by `BREAK` markers
    @return a new solid, owned by the caller
    */
    static PolyhedralBoundedSolid* createBrepFromParametricCurve(
        ParametricCurve* curve);
};

#endif
