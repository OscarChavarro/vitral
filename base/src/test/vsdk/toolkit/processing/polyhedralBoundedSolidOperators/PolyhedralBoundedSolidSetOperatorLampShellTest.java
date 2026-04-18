package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.processing.GeometricModeler;

class PolyhedralBoundedSolidSetOperatorLampShellTest
{
    @Test
    void reproduce_csgLampShell_defaultExampleConfiguration_nr3_nh1()
    {
        int subdivisionCircumference = 3;
        int subdivisionHeight = 1;

        double outerRadius = 0.5;
        double innerRadius = 0.45;

        PolyhedralBoundedSolid outerSphere = createSphere(
            outerRadius, subdivisionCircumference, subdivisionHeight);
        PolyhedralBoundedSolid innerSphere = createSphere(
            innerRadius, subdivisionCircumference, subdivisionHeight);

        PolyhedralBoundedSolid sphericalShell = GeometricModeler.setOp(
            outerSphere, innerSphere, GeometricModeler.SUBTRACT, false);

        Box clipCubeGeometry = new Box(new Vector3D(1.4, 1.4, 1.05));
        PolyhedralBoundedSolid clipCube = clipCubeGeometry
            .exportToPolyhedralBoundedSolid();
        Matrix4x4 cubeMove = new Matrix4x4();
        cubeMove.translation(0.55, 0.55, 0.325);
        clipCube.applyTransformation(cubeMove);

        GeometricModeler.setOp(
            sphericalShell, clipCube, GeometricModeler.INTERSECTION, false);
    }

    private static PolyhedralBoundedSolid createSphere(
        double radius, int subdivisionCircumference, int subdivisionHeight)
    {
        Matrix4x4 move = new Matrix4x4();
        move.translation(0.55, 0.55, 0.55);

        Sphere sphere = new Sphere(radius);
        PolyhedralBoundedSolid solid = sphere.exportToPolyhedralBoundedSolid(
            subdivisionCircumference, subdivisionHeight);
        solid.applyTransformation(move);
        return solid;
    }
}
