package vsdk.toolkit.environment.geometry.surface;

/**
How a SubdividedBezierPatch approximates its Bezier surface by flat
triangles, with the numbering of PovRay bicubic_patch types.
*/
public enum BezierPatchSubdivision {
    // uSteps x vSteps grid of cells evaluated for every ray.
    UniformGrid(0),
    // The same grid, evaluated once with its triangle planes.
    PrecomputedGrid(1),
    // Recursive halving for every ray, up to uSteps and vSteps levels or
    // until a piece is flatter than the flatness value.
    Adaptive(2),
    // The adaptive pieces, built once as a bounding sphere tree.
    AdaptiveTree(3),
    // The precomputed grid with normals interpolated from the surface
    // normals at the grid points.
    SmoothGrid(4);

    private final int value;
    BezierPatchSubdivision(int value) { this.value = value; }
    /** cpp enumerator value (`static_cast<int>`). */
    public int value() { return value; }
}
