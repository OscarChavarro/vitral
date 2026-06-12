package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.sameCoordinate;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate;

/**
Structural-shape boolean fallback for an offset vertical-cylinder difference:
detects two vertical cylinders in a subtract and rebuilds the difference from
analytic cylinder descriptions. Extracted verbatim from
{@link PolyhedralBoundedSolidSetOperator} in Stage 7 R2 (one family per class);
pure code motion, no behavior change.
 */
final class _PolyhedralBoundedSolidOffsetCylinderFallback
    extends _PolyhedralBoundedSolidOperator
{
    private static final class VerticalCylinderOperandSpec
    {
        private final double centerX;
        private final double centerY;
        private final double zMin;
        private final double radius;
        private final double height;
        private final int radialDivisions;
        private final int heightDivisions;

        private VerticalCylinderOperandSpec(
            double centerX,
            double centerY,
            double zMin,
            double radius,
            double height,
            int radialDivisions,
            int heightDivisions)
        {
            this.centerX = centerX;
            this.centerY = centerY;
            this.zMin = zMin;
            this.radius = radius;
            this.height = height;
            this.radialDivisions = radialDivisions;
            this.heightDivisions = heightDivisions;
        }
    }

    static final class OffsetCylinderDifferenceFallbackSpec
    {
        private final VerticalCylinderOperandSpec operandA;
        private final VerticalCylinderOperandSpec operandB;

        private OffsetCylinderDifferenceFallbackSpec(
            VerticalCylinderOperandSpec operandA,
            VerticalCylinderOperandSpec operandB)
        {
            this.operandA = operandA;
            this.operandB = operandB;
        }
    }

    private static void addUniqueXy(ArrayList<Vector3Dd> values,
                                    Vector3Dd point)
    {
        int i;

        for ( i = 0; i < values.size(); i++ ) {
            Vector3Dd current = values.get(i);
            if ( sameCoordinate(current.x(), point.x()) &&
                 sameCoordinate(current.y(), point.y()) ) {
                return;
            }
        }
        values.add(point);
    }

    private static VerticalCylinderOperandSpec describeVerticalCylinder(
        PolyhedralBoundedSolid solid)
    {
        double[] bounds;
        double centerX;
        double centerY;
        double radius;
        ArrayList<Double> zs;
        ArrayList<Vector3Dd> xy;
        int i;

        if ( solid == null || solid.getVerticesList().size() < 6 ) {
            return null;
        }

        bounds = solid.getMinMax();
        if ( bounds == null || bounds.length < 6 ||
             bounds[5] <= bounds[2] + numericContext.bigEpsilon() ) {
            return null;
        }

        centerX = (bounds[0] + bounds[3]) * 0.5;
        centerY = (bounds[1] + bounds[4]) * 0.5;
        radius = 0.0;
        zs = new ArrayList<Double>();
        xy = new ArrayList<Vector3Dd>();
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            Vector3Dd p = solid.getVerticesList().get(i).position;
            double radialDistance;

            addUniqueCoordinate(zs, p.z());
            addUniqueXy(xy, p);
            radialDistance = Math.sqrt(
                (p.x() - centerX) * (p.x() - centerX) +
                (p.y() - centerY) * (p.y() - centerY));
            if ( radialDistance > radius ) {
                radius = radialDistance;
            }
        }

        if ( xy.size() < 3 || zs.size() < 2 ||
             radius <= numericContext.bigEpsilon() ) {
            return null;
        }

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            Vector3Dd p = solid.getVerticesList().get(i).position;
            double radialDistance = Math.sqrt(
                (p.x() - centerX) * (p.x() - centerX) +
                (p.y() - centerY) * (p.y() - centerY));

            if ( Math.abs(radialDistance - radius) >
                 Math.max(numericContext.bigEpsilon(),
                     radius * 1.0e-6) ) {
                return null;
            }
        }

        return new VerticalCylinderOperandSpec(
            centerX, centerY, bounds[2], radius, bounds[5] - bounds[2],
            xy.size(), Math.max(1, zs.size() - 1));
    }

    static OffsetCylinderDifferenceFallbackSpec
    prepareOffsetCylinderDifferenceFallbackSpec(
        PolyhedralBoundedSolid inSolidA,
        PolyhedralBoundedSolid inSolidB,
        int op)
    {
        VerticalCylinderOperandSpec operandA;
        VerticalCylinderOperandSpec operandB;
        double centerDistance;

        if ( op != SUBTRACT ) {
            return null;
        }

        operandA = describeVerticalCylinder(inSolidA);
        operandB = describeVerticalCylinder(inSolidB);
        if ( operandA == null || operandB == null ) {
            return null;
        }

        if ( operandA.radialDivisions == operandB.radialDivisions ) {
            return null;
        }
        if ( !sameCoordinate(operandA.radius, operandB.radius) ||
             !sameCoordinate(operandA.height, operandB.height) ) {
            return null;
        }

        centerDistance = Math.sqrt(
            (operandA.centerX - operandB.centerX) *
            (operandA.centerX - operandB.centerX) +
            (operandA.centerY - operandB.centerY) *
            (operandA.centerY - operandB.centerY));
        if ( centerDistance <= numericContext.bigEpsilon() ||
             centerDistance >= operandA.radius + operandB.radius -
                 numericContext.bigEpsilon() ) {
            return null;
        }
        if ( operandA.zMin >= operandB.zMin + operandB.height -
                 numericContext.bigEpsilon() ||
             operandB.zMin >= operandA.zMin + operandA.height -
                 numericContext.bigEpsilon() ) {
            return null;
        }

        return new OffsetCylinderDifferenceFallbackSpec(operandA, operandB);
    }

    private static PolyhedralBoundedSolid createFallbackCylinder(
        VerticalCylinderOperandSpec spec,
        int radialDivisions,
        int heightDivisions)
    {
        PolyhedralBoundedSolid cylinder;
        Matrix4x4d translation;

        cylinder = new Cone(spec.radius, spec.radius, spec.height)
            .exportToPolyhedralBoundedSolid(radialDivisions, heightDivisions);
        translation = new Matrix4x4d();
        translation = translation.translation(
            spec.centerX, spec.centerY, spec.zMin);
        PolyhedralBoundedSolidModeler.applyTransformation(
            cylinder, translation);
        return cylinder;
    }

    static PolyhedralBoundedSolid buildOffsetCylinderDifferenceFallback(
        OffsetCylinderDifferenceFallbackSpec spec)
    {
        PolyhedralBoundedSolid fallbackA;
        PolyhedralBoundedSolid fallbackB;
        PolyhedralBoundedSolid result;
        int radialDivisions;
        int heightDivisions;

        if ( spec == null ) {
            return null;
        }

        radialDivisions = Math.max(spec.operandA.radialDivisions,
            spec.operandB.radialDivisions);
        heightDivisions = Math.max(spec.operandA.heightDivisions,
            spec.operandB.heightDivisions);
        fallbackA = createFallbackCylinder(spec.operandA, radialDivisions,
            heightDivisions);
        fallbackB = createFallbackCylinder(spec.operandB, radialDivisions,
            heightDivisions);

        try {
            result = PolyhedralBoundedSolidSetOperator.setOp(fallbackA, fallbackB, SUBTRACT, false, true);
        }
        catch ( RuntimeException e ) {
            _SetOperationTrace.tracePipelineSummary(
                "offset cylinder fallback failed: " +
                e.getClass().getSimpleName());
            return null;
        }
        if ( !PolyhedralBoundedSolidSetOperator.isStructurallyUsableSetOpResult(result) ) {
            _SetOperationTrace.tracePipelineSummary("offset cylinder fallback rejected");
            return null;
        }
        _SetOperationTrace.tracePipelineSummary(
            "offset cylinder fallback accepted faces=" +
            result.getPolygonsList().size() +
            " edges=" + result.getEdgesList().size() +
            " vertices=" + result.getVerticesList().size());
        return result;
    }
}
