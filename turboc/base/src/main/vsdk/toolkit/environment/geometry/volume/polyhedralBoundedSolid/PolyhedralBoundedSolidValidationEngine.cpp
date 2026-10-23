#include <math.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricPlanarityStrategy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricStrictFaceIntersectionsStrategy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_GeometricStrictLoopsStrategy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/_TopologicalIntegrityStrategy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "java/lang/IllegalArgumentException.h"

AtomicLong PolyhedralBoundedSolidValidationEngine::strictValidationInvocationCount(0L);


static const char* const CLASS_NAME = "PolyhedralBoundedSolidValidationEngine";

/**
Runs the strategies in order, stopping at the first failure.
@return true when all strategies succeed
*/
static BOOLEAN runStrategies(
    ArrayList<_PolyhedralBoundedSolidValidationStrategy*>& strategies,
    PolyhedralBoundedSolid* solid,
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext,
    String* msg)
{
    BOOLEAN ok = TRUE;
    long i;

    for ( i = 0; i < strategies.size(); i++ ) {
        if ( !strategies[i]->validate(solid, numericContext, msg) ) {
            ok = FALSE;
            break;
        }
    }
    for ( i = 0; i < strategies.size(); i++ ) {
        delete strategies[i];
    }
    return ok;
}

static void append(String* msg, const String& text)
{
    if ( msg != 0 ) {
        *msg += text;
    }
}


BOOLEAN PolyhedralBoundedSolidValidationEngine::validateIntermediate(PolyhedralBoundedSolid* solid)
{
    String msg;
    BOOLEAN ok;
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    ArrayList<_PolyhedralBoundedSolidValidationStrategy*> strategies;

    strategies.add(new _GeometricPlanarityStrategy());
    strategies.add(new _TopologicalIntegrityStrategy());
    ok = runStrategies(strategies, solid, numericContext, &msg);

    solid->setValidationState(ok);
    if ( !ok ) {
        Logger::reportMessage(CLASS_NAME, Logger::WARNING,
            "validateIntermediate",
            String("Solid validation test failed!:\n") + msg);
    }
    return ok;
}

BOOLEAN PolyhedralBoundedSolidValidationEngine::validateBooleanInputs(
    PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB,
    String* msg)
{
    BOOLEAN ok;
    String sub;
    int welded;

    ok = TRUE;

    if ( !validateIntermediate(solidA) ) {
        append(msg, "solidA failed validateIntermediate\n");
        ok = FALSE;
    }
    if ( !validateIntermediate(solidB) ) {
        append(msg, "solidB failed validateIntermediate\n");
        ok = FALSE;
    }
    if ( !ok ) {
        return FALSE;
    }

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext ctxA =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solidA);
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext ctxB =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solidB);

    sub = "";
    if ( !PolyhedralBoundedSolidGeometricValidator::validateNoCoincidentVertices(
            solidA, ctxA, &sub) ) {
        welded = PolyhedralBoundedSolidTopologyEditing::weldCoincidentVertices(
            solidA, ctxA);
        append(msg, String("solidA had coincident vertices; welded ") +
            Cpp98Support::toString(welded).c_str() + " pair(s)\n");
        if ( !validateIntermediate(solidA) ) {
            append(msg, "solidA failed validateIntermediate after weld\n");
            throw IllegalArgumentException(String(
                "solidA is topologically invalid after coincident-vertex weld:\n") +
                (msg != 0 ? msg->c_str() : ""));
        }
    }

    sub = "";
    if ( !PolyhedralBoundedSolidGeometricValidator::validateNoCoincidentVertices(
            solidB, ctxB, &sub) ) {
        welded = PolyhedralBoundedSolidTopologyEditing::weldCoincidentVertices(
            solidB, ctxB);
        append(msg, String("solidB had coincident vertices; welded ") +
            Cpp98Support::toString(welded).c_str() + " pair(s)\n");
        if ( !validateIntermediate(solidB) ) {
            append(msg, "solidB failed validateIntermediate after weld\n");
            throw IllegalArgumentException(String(
                "solidB is topologically invalid after coincident-vertex weld:\n") +
                (msg != 0 ? msg->c_str() : ""));
        }
    }

    sub = "";
    if ( !PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(
            solidA, &sub) ) {
        append(msg, String("solidA has ID violations:\n") + sub);
        ok = FALSE;
    }

    sub = "";
    if ( !PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(
            solidB, &sub) ) {
        append(msg, String("solidB has ID violations:\n") + sub);
        ok = FALSE;
    }

    return ok;
}

BOOLEAN PolyhedralBoundedSolidValidationEngine::validateStrict(PolyhedralBoundedSolid* solid)
{
    String msg;
    return validateStrict(solid, &msg);
}

BOOLEAN PolyhedralBoundedSolidValidationEngine::validateStrict(
    PolyhedralBoundedSolid* solid,
    String* msg)
{
    String localMsg;
    BOOLEAN ok;

    if ( msg == 0 ) {
        msg = &localMsg;
    }
    strictValidationInvocationCount.incrementAndGet();

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    ArrayList<_PolyhedralBoundedSolidValidationStrategy*> strategies;

    strategies.add(new _GeometricPlanarityStrategy());
    strategies.add(new _TopologicalIntegrityStrategy());
    strategies.add(new _GeometricStrictLoopsStrategy());
    strategies.add(new _GeometricStrictFaceIntersectionsStrategy());
    ok = runStrategies(strategies, solid, numericContext, msg);

    if ( ok ) {
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary::from(solid);
        if ( topology.hasUniversalContradiction() ) {
            *msg += String("  - Global topology contradiction: ") +
                topology.toString() + "\n";
            ok = FALSE;
        }
    }

    solid->setValidationState(ok);
    if ( !ok ) {
        Logger::reportMessage(CLASS_NAME, Logger::WARNING, "validateStrict",
            String("Solid validation test failed!:\n") + *msg);
    }
    return ok;
}

long PolyhedralBoundedSolidValidationEngine::getStrictValidationInvocationCount()
{
    return strictValidationInvocationCount.get();
}

void PolyhedralBoundedSolidValidationEngine::resetStrictValidationInvocationCount()
{
    strictValidationInvocationCount.set(0L);
}

BOOLEAN PolyhedralBoundedSolidValidationEngine::areGeometricallyIdentical(
    PolyhedralBoundedSolid* a,
    PolyhedralBoundedSolid* b,
    double tolerance)
{
    if ( a == 0 || b == 0 ) {
        return FALSE;
    }
    if ( a->getVerticesList().size() != b->getVerticesList().size() ) {
        return FALSE;
    }
    if ( a->getEdgesList().size() != b->getEdgesList().size() ) {
        return FALSE;
    }
    if ( a->getPolygonsList().size() != b->getPolygonsList().size() ) {
        return FALSE;
    }
    double* ma = a->getMinMax();
    double* mb = b->getMinMax();
    BOOLEAN sameBox = TRUE;
    long i;
    for ( i = 0; i < 6; i++ ) {
        if ( fabs(ma[i] - mb[i]) > tolerance ) {
            sameBox = FALSE;
            break;
        }
    }
    delete[] ma;
    delete[] mb;
    if ( !sameBox ) {
        return FALSE;
    }

    // Pairwise vertex coincidence: every vertex in A has at least
    // one matching counterpart in B (and by cardinality this implies
    // a bijection). O(n^2) on vertex count.
    long int n = a->getVerticesList().size();
    ArrayList<BOOLEAN> matched(Collections<BOOLEAN>::nCopies(n, FALSE));
    for ( i = 0; i < n; i++ ) {
        Vector3Dd pa = a->getVerticesList().get(i)->position;
        BOOLEAN found = FALSE;
        for ( long int j = 0; j < n; j++ ) {
            if ( matched[j] ) continue;
            Vector3Dd pb = b->getVerticesList().get(j)->position;
            if ( fabs(pa.x() - pb.x()) <= tolerance
                 && fabs(pa.y() - pb.y()) <= tolerance
                 && fabs(pa.z() - pb.z()) <= tolerance ) {
                matched[j] = TRUE;
                found = TRUE;
                break;
            }
        }
        if ( !found ) return FALSE;
    }
    return TRUE;
}
