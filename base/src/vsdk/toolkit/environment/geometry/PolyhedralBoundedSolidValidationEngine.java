package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;

public class PolyhedralBoundedSolidValidationEngine
{
    private PolyhedralBoundedSolidValidationEngine()
    {
    }

    public static boolean validateIntermediate(PolyhedralBoundedSolid solid)
    {
        StringBuilder msg = new StringBuilder();
        boolean ok = true;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        ArrayList<PolyhedralBoundedSolidValidationStrategy> strategies =
            new ArrayList<PolyhedralBoundedSolidValidationStrategy>();
        strategies.add(new GeometricPlanarityStrategy());
        strategies.add(new TopologicalIntegrityStrategy());

        int i;
        for ( i = 0; i < strategies.size(); i++ ) {
            if ( !strategies.get(i).validate(solid, numericContext, msg) ) {
                ok = false;
                break;
            }
        }

        solid.setValidationState(ok);
        if ( !ok ) {
            VSDK.reportMessage(solid, VSDK.WARNING, "validateIntermediate",
                "Solid validation test failed!:\n" + msg.toString());
        }
        return ok;
    }

    public static boolean validateStrict(PolyhedralBoundedSolid solid)
    {
        StringBuilder msg = new StringBuilder();
        boolean ok = true;
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext =
            PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        ArrayList<PolyhedralBoundedSolidValidationStrategy> strategies =
            new ArrayList<PolyhedralBoundedSolidValidationStrategy>();
        strategies.add(new GeometricPlanarityStrategy());
        strategies.add(new TopologicalIntegrityStrategy());
        strategies.add(new GeometricStrictLoopsStrategy());
        strategies.add(new GeometricStrictFaceIntersectionsStrategy());

        int i;
        for ( i = 0; i < strategies.size(); i++ ) {
            if ( !strategies.get(i).validate(solid, numericContext, msg) ) {
                ok = false;
                break;
            }
        }

        solid.setValidationState(ok);
        if ( !ok ) {
            VSDK.reportMessage(solid, VSDK.WARNING, "validateStrict",
                "Solid validation test failed!:\n" + msg.toString());
        }
        return ok;
    }

    private static class GeometricPlanarityStrategy
        implements PolyhedralBoundedSolidValidationStrategy
    {
        @Override
        public boolean validate(PolyhedralBoundedSolid solid,
            PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
            StringBuilder msg)
        {
            return PolyhedralBoundedSolidGeometricValidator
                .validateAllFacesPlanarityAndPlanes(solid, numericContext, msg);
        }
    }

    private static class TopologicalIntegrityStrategy
        implements PolyhedralBoundedSolidValidationStrategy
    {
        @Override
        public boolean validate(PolyhedralBoundedSolid solid,
            PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
            StringBuilder msg)
        {
            PolyhedralBoundedSolidTopologicalValidator
                .remakeEmanatingHalfedgesReferences(solid);
            if ( !PolyhedralBoundedSolidTopologicalValidator
                .validateTopologicalIntegrity(solid) ) {
                msg.append("  - Topological integrity test failed.\n");
                return false;
            }
            return true;
        }
    }

    private static class GeometricStrictLoopsStrategy
        implements PolyhedralBoundedSolidValidationStrategy
    {
        @Override
        public boolean validate(PolyhedralBoundedSolid solid,
            PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
            StringBuilder msg)
        {
            return PolyhedralBoundedSolidGeometricValidator
                .validateLoopsStrict(solid, numericContext, msg);
        }
    }

    private static class GeometricStrictFaceIntersectionsStrategy
        implements PolyhedralBoundedSolidValidationStrategy
    {
        @Override
        public boolean validate(PolyhedralBoundedSolid solid,
            PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext,
            StringBuilder msg)
        {
            return PolyhedralBoundedSolidGeometricValidator
                .validateFaceIntersectionsStrict(solid, numericContext, msg);
        }
    }
}
