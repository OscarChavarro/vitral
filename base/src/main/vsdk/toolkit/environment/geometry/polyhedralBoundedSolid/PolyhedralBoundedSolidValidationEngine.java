package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

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
}
