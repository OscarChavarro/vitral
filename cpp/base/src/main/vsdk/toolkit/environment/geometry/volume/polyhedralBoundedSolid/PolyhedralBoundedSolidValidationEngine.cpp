#include <cmath>
#include <stdexcept>
#include <string>
#include <vector>

#include "java/util/ArrayList.txx"
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

std::atomic<long> PolyhedralBoundedSolidValidationEngine::strictValidationInvocationCount(0L);

namespace {

const char* const CLASS_NAME = "PolyhedralBoundedSolidValidationEngine";

/**
Runs the strategies in order, stopping at the first failure.
@return true when all strategies succeed
*/
bool runStrategies(
    std::vector<_PolyhedralBoundedSolidValidationStrategy*>& strategies,
    PolyhedralBoundedSolid* solid,
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext,
    java::String* msg)
{
    bool ok = true;
    size_t i;

    for ( i = 0; i < strategies.size(); i++ ) {
        if ( !strategies[i]->validate(solid, numericContext, msg) ) {
            ok = false;
            break;
        }
    }
    for ( i = 0; i < strategies.size(); i++ ) {
        delete strategies[i];
    }
    return ok;
}

void append(java::String* msg, const java::String& text)
{
    if ( msg != 0 ) {
        *msg += text;
    }
}

} // namespace

bool PolyhedralBoundedSolidValidationEngine::validateIntermediate(PolyhedralBoundedSolid* solid)
{
    java::String msg;
    bool ok;
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    std::vector<_PolyhedralBoundedSolidValidationStrategy*> strategies;

    strategies.push_back(new _GeometricPlanarityStrategy());
    strategies.push_back(new _TopologicalIntegrityStrategy());
    ok = runStrategies(strategies, solid, numericContext, &msg);

    solid->setValidationState(ok);
    if ( !ok ) {
        Logger::reportMessage(CLASS_NAME, Logger::WARNING,
            "validateIntermediate",
            java::String("Solid validation test failed!:\n") + msg);
    }
    return ok;
}

bool PolyhedralBoundedSolidValidationEngine::validateBooleanInputs(
    PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB,
    java::String* msg)
{
    bool ok;
    java::String sub;
    int welded;

    ok = true;

    if ( !validateIntermediate(solidA) ) {
        append(msg, "solidA failed validateIntermediate\n");
        ok = false;
    }
    if ( !validateIntermediate(solidB) ) {
        append(msg, "solidB failed validateIntermediate\n");
        ok = false;
    }
    if ( !ok ) {
        return false;
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
        append(msg, java::String("solidA had coincident vertices; welded ") +
            std::to_string(welded).c_str() + " pair(s)\n");
        if ( !validateIntermediate(solidA) ) {
            append(msg, "solidA failed validateIntermediate after weld\n");
            throw std::invalid_argument(std::string(
                "solidA is topologically invalid after coincident-vertex weld:\n") +
                (msg != 0 ? msg->c_str() : ""));
        }
    }

    sub = "";
    if ( !PolyhedralBoundedSolidGeometricValidator::validateNoCoincidentVertices(
            solidB, ctxB, &sub) ) {
        welded = PolyhedralBoundedSolidTopologyEditing::weldCoincidentVertices(
            solidB, ctxB);
        append(msg, java::String("solidB had coincident vertices; welded ") +
            std::to_string(welded).c_str() + " pair(s)\n");
        if ( !validateIntermediate(solidB) ) {
            append(msg, "solidB failed validateIntermediate after weld\n");
            throw std::invalid_argument(std::string(
                "solidB is topologically invalid after coincident-vertex weld:\n") +
                (msg != 0 ? msg->c_str() : ""));
        }
    }

    sub = "";
    if ( !PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(
            solidA, &sub) ) {
        append(msg, java::String("solidA has ID violations:\n") + sub);
        ok = false;
    }

    sub = "";
    if ( !PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(
            solidB, &sub) ) {
        append(msg, java::String("solidB has ID violations:\n") + sub);
        ok = false;
    }

    return ok;
}

bool PolyhedralBoundedSolidValidationEngine::validateStrict(PolyhedralBoundedSolid* solid)
{
    java::String msg;
    return validateStrict(solid, &msg);
}

bool PolyhedralBoundedSolidValidationEngine::validateStrict(
    PolyhedralBoundedSolid* solid,
    java::String* msg)
{
    java::String localMsg;
    bool ok;

    if ( msg == 0 ) {
        msg = &localMsg;
    }
    strictValidationInvocationCount++;

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    std::vector<_PolyhedralBoundedSolidValidationStrategy*> strategies;

    strategies.push_back(new _GeometricPlanarityStrategy());
    strategies.push_back(new _TopologicalIntegrityStrategy());
    strategies.push_back(new _GeometricStrictLoopsStrategy());
    strategies.push_back(new _GeometricStrictFaceIntersectionsStrategy());
    ok = runStrategies(strategies, solid, numericContext, msg);

    if ( ok ) {
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary::from(solid);
        if ( topology.hasUniversalContradiction() ) {
            *msg += java::String("  - Global topology contradiction: ") +
                topology.toString() + "\n";
            ok = false;
        }
    }

    solid->setValidationState(ok);
    if ( !ok ) {
        Logger::reportMessage(CLASS_NAME, Logger::WARNING, "validateStrict",
            java::String("Solid validation test failed!:\n") + *msg);
    }
    return ok;
}

long PolyhedralBoundedSolidValidationEngine::getStrictValidationInvocationCount()
{
    return strictValidationInvocationCount.load();
}

void PolyhedralBoundedSolidValidationEngine::resetStrictValidationInvocationCount()
{
    strictValidationInvocationCount.store(0L);
}

bool PolyhedralBoundedSolidValidationEngine::areGeometricallyIdentical(
    PolyhedralBoundedSolid* a,
    PolyhedralBoundedSolid* b,
    double tolerance)
{
    if ( a == 0 || b == 0 ) {
        return false;
    }
    if ( a->getVerticesList().size() != b->getVerticesList().size() ) {
        return false;
    }
    if ( a->getEdgesList().size() != b->getEdgesList().size() ) {
        return false;
    }
    if ( a->getPolygonsList().size() != b->getPolygonsList().size() ) {
        return false;
    }
    double* ma = a->getMinMax();
    double* mb = b->getMinMax();
    bool sameBox = true;
    for ( int i = 0; i < 6; i++ ) {
        if ( std::fabs(ma[i] - mb[i]) > tolerance ) {
            sameBox = false;
            break;
        }
    }
    delete[] ma;
    delete[] mb;
    if ( !sameBox ) {
        return false;
    }

    // Pairwise vertex coincidence: every vertex in A has at least
    // one matching counterpart in B (and by cardinality this implies
    // a bijection). O(n^2) on vertex count.
    long int n = a->getVerticesList().size();
    std::vector<bool> matched((size_t)n, false);
    for ( long int i = 0; i < n; i++ ) {
        Vector3Dd pa = a->getVerticesList().get(i)->position;
        bool found = false;
        for ( long int j = 0; j < n; j++ ) {
            if ( matched[j] ) continue;
            Vector3Dd pb = b->getVerticesList().get(j)->position;
            if ( std::fabs(pa.x() - pb.x()) <= tolerance
                 && std::fabs(pa.y() - pb.y()) <= tolerance
                 && std::fabs(pa.z() - pb.z()) <= tolerance ) {
                matched[j] = true;
                found = true;
                break;
            }
        }
        if ( !found ) return false;
    }
    return true;
}
