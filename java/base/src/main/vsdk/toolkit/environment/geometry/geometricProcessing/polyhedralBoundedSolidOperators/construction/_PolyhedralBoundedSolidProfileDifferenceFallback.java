package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologyEditing;

import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.boundsMatch;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.extractProfileAtX;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators._PolyhedralBoundedSolidFallbackGeometry.clipProfileAboveZ;

/**
Structural-shape boolean fallback for extruded YZ profiles: detects the
profile-subtraction case where the general pipeline emits only degenerate
vertex/face rings and rebuilds the difference directly from the clipped
profile. Extracted verbatim from {@link _PolyhedralBoundedSolidSetOperator} in
Stage 7 R2 (one family per class); pure code motion, no behavior change.
 */
final class _PolyhedralBoundedSolidProfileDifferenceFallback
    extends _PolyhedralBoundedSolidOperator
{
    private static boolean isBetween(double value, double min, double max)
    {
        return value > min + numericContext.bigEpsilon() &&
               value < max - numericContext.bigEpsilon();
    }

    static _PolyhedralBoundedSolidProfileDifferenceFallbackSpec
    prepareProfileDifferenceFallbackSpec(
        PolyhedralBoundedSolid minuend,
        PolyhedralBoundedSolid subtrahend,
        int op)
    {
        double[] minuendBounds;
        double[] subtrahendBounds;
        ArrayList<Double> minuendX;
        ArrayList<Double> subtrahendX;
        ArrayList<Double> subtrahendZ;
        ArrayList<Vector3Dd> profile;
        ArrayList<Vector3Dd> clippedProfile;
        double xCut;
        double zCut;

        // Covers profile-subtraction cases where connect emits only
        // degenerate vertex/face rings and finish returns the full minuend.
        if ( op != SUBTRACT ||
             minuend.getVerticesList().size() <= 0 ||
             subtrahend.getVerticesList().size() <= 0 ) {
            return null;
        }

        minuendBounds = minuend.getMinMax();
        subtrahendBounds = subtrahend.getMinMax();
        if ( !boundsMatch(minuendBounds, subtrahendBounds) ) {
            return null;
        }

        minuendX = uniqueVertexCoordinates(minuend, 0);
        subtrahendX = uniqueVertexCoordinates(subtrahend, 0);
        subtrahendZ = uniqueVertexCoordinates(subtrahend, 2);
        if ( minuendX.size() != 2 ||
             subtrahendX.size() != 3 ||
             subtrahendZ.size() != 3 ) {
            return null;
        }

        xCut = subtrahendX.get(1);
        zCut = subtrahendZ.get(1);
        if ( !isBetween(xCut, minuendBounds[0], minuendBounds[3]) ||
             !isBetween(zCut, minuendBounds[2], minuendBounds[5]) ) {
            return null;
        }

        profile = extractProfileAtX(minuend, minuendBounds[0]);
        clippedProfile = clipProfileAboveZ(profile, xCut, zCut);
        if ( clippedProfile.size() < 3 ) {
            return null;
        }
        Collections.reverse(clippedProfile);

        return new _PolyhedralBoundedSolidProfileDifferenceFallbackSpec(
            clippedProfile, xCut, minuendBounds[3], minuendBounds);
    }

    private static PolyhedralBoundedSolid buildProfileDifferenceFallback(
        _PolyhedralBoundedSolidProfileDifferenceFallbackSpec spec)
    {
        PolyhedralBoundedSolid solid;
        Matrix4x4d translation;
        int i;

        if ( spec == null ||
             spec.clippedProfileAtCut == null ||
             spec.clippedProfileAtCut.size() < 3 ||
             spec.xMax <= spec.xCut + numericContext.bigEpsilon() ) {
            return null;
        }

        solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, spec.clippedProfileAtCut.get(0), 1, 1);
        for ( i = 1; i < spec.clippedProfileAtCut.size(); i++ ) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, spec.clippedProfileAtCut.get(i));
        }
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, spec.clippedProfileAtCut.size(),
            spec.clippedProfileAtCut.size() - 1, 1, 2, 2);

        translation = new Matrix4x4d();
        translation = translation.translation(spec.xMax - spec.xCut, 0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), translation);
        PolyhedralBoundedSolidTopologyEditing.compactIds(solid);
        return solid;
    }

    static PolyhedralBoundedSolid
    applyProfileDifferenceFallbackIfNeeded(
        _PolyhedralBoundedSolidProfileDifferenceFallbackSpec spec,
        PolyhedralBoundedSolid result)
    {
        PolyhedralBoundedSolid fallback;

        if ( spec == null || result == null ||
             !boundsMatch(result.getMinMax(), spec.minuendBounds) ) {
            return result;
        }

        fallback = buildProfileDifferenceFallback(spec);
        if ( fallback == null ) {
            return result;
        }
        return fallback;
    }
}
