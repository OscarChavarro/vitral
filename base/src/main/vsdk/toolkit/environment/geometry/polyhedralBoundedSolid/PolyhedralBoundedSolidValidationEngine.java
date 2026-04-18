//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;

/**
Orchestrates validation passes that preserve the half-edge representation of
[MANT1988].10 and the geometric consistency conditions used by chapters
[MANT1988].13 and [MANT1988].15.
*/
public class PolyhedralBoundedSolidValidationEngine
{
    private PolyhedralBoundedSolidValidationEngine()
    {
    }

    /**
    Runs a lightweight validation pass aimed at intermediate models that still
    need to respect the face/loop/half-edge structure of [MANT1988].10.2.1 and
    the planar-face assumptions of [MANT1988].13.1.
    */
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

    /**
    Runs a stricter validation pass that additionally enforces the non-self-
    intersection expectations stated for valid boundary models in
    [MANT1988].15.2.
    */
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
