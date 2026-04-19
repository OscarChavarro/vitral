//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

/**
Applies topological consistency checks for the half-edge data structure of
[MANT1988].10.2.1 and [MANT1988].10.2.2.
*/
public class TopologicalIntegrityStrategy
    implements PolyhedralBoundedSolidValidationStrategy
{
    /**
    Validates the basic incidence and cycle properties assumed by the half-edge
    representation in [MANT1988].10.2.1 and [MANT1988].10.2.2.
    */
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
