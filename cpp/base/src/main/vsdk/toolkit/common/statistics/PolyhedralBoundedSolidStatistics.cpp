#include <atomic>
#include <cstdio>

#include "java/lang/Boolean.h"
#include "vsdk/toolkit/common/statistics/PolyhedralBoundedSolidStatistics.h"

namespace {

std::atomic<long long> eulerLmevCalls(0);
std::atomic<long long> eulerLkevCalls(0);
std::atomic<long long> eulerLkefCalls(0);
std::atomic<long long> eulerLmefCalls(0);
std::atomic<long long> eulerLkemrCalls(0);
std::atomic<long long> eulerLmekrCalls(0);
std::atomic<long long> eulerLringmvCalls(0);
std::atomic<long long> setOpCalls(0);
std::atomic<long long> setOpUnionCalls(0);
std::atomic<long long> setOpIntersectionCalls(0);
std::atomic<long long> setOpSubtractCalls(0);
std::atomic<long long> splitCalls(0);
std::atomic<long long> splitNoNullEdgesCases(0);
std::atomic<long long> splitProducedAboveSolids(0);
std::atomic<long long> splitProducedBelowSolids(0);
std::atomic<long long> joinCalls(0);
std::atomic<long long> joinIncompleteCases(0);
std::atomic<long long> he1EqualsHe2Cases(0);
std::atomic<long long> invalidHalfEdgeInputCases(0);
std::atomic<long long> consistencyWarningCases(0);
std::atomic<long long> operationFailureCases(0);

}

bool PolyhedralBoundedSolidStatistics::isEnabled()
{
    static const bool ENABLED = java::Boolean::getBoolean("vsdk.polyhedral.stats");
    return ENABLED;
}

void PolyhedralBoundedSolidStatistics::recordLmevCall()
{
    if ( isEnabled() ) {
        eulerLmevCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordLkevCall()
{
    if ( isEnabled() ) {
        eulerLkevCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordLkefCall()
{
    if ( isEnabled() ) {
        eulerLkefCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordLmefCall()
{
    if ( isEnabled() ) {
        eulerLmefCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordLkemrCall()
{
    if ( isEnabled() ) {
        eulerLkemrCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordLmekrCall()
{
    if ( isEnabled() ) {
        eulerLmekrCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordLringmvCall()
{
    if ( isEnabled() ) {
        eulerLringmvCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordSplitCall()
{
    if ( isEnabled() ) {
        splitCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordSplitNoNullEdgesCase()
{
    if ( isEnabled() ) {
        splitNoNullEdgesCases++;
    }
}

void PolyhedralBoundedSolidStatistics::recordJoinCall()
{
    if ( isEnabled() ) {
        joinCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordJoinIncompleteCase()
{
    if ( isEnabled() ) {
        joinIncompleteCases++;
    }
}

void PolyhedralBoundedSolidStatistics::recordHe1EqualsHe2Case()
{
    if ( isEnabled() ) {
        he1EqualsHe2Cases++;
    }
}

void PolyhedralBoundedSolidStatistics::recordInvalidHalfEdgeInputCase()
{
    if ( isEnabled() ) {
        invalidHalfEdgeInputCases++;
    }
}

void PolyhedralBoundedSolidStatistics::recordConsistencyWarningCase()
{
    if ( isEnabled() ) {
        consistencyWarningCases++;
    }
}

void PolyhedralBoundedSolidStatistics::recordOperationFailureCase()
{
    if ( isEnabled() ) {
        operationFailureCases++;
    }
}

void PolyhedralBoundedSolidStatistics::recordSetOpCall(int op)
{
    if ( !isEnabled() ) {
        return;
    }
    setOpCalls++;
    if ( op == 1 ) {
        setOpUnionCalls++;
    }
    else if ( op == 2 ) {
        setOpIntersectionCalls++;
    }
    else if ( op == 3 ) {
        setOpSubtractCalls++;
    }
}

void PolyhedralBoundedSolidStatistics::recordSplitProducedSolids(int above,
                                                                 int below)
{
    if ( !isEnabled() ) {
        return;
    }
    if ( above > 0 ) {
        splitProducedAboveSolids += above;
    }
    if ( below > 0 ) {
        splitProducedBelowSolids += below;
    }
}

void PolyhedralBoundedSolidStatistics::reset()
{
    if ( !isEnabled() ) {
        return;
    }
    eulerLmevCalls = 0;
    eulerLkevCalls = 0;
    eulerLkefCalls = 0;
    eulerLmefCalls = 0;
    eulerLkemrCalls = 0;
    eulerLmekrCalls = 0;
    eulerLringmvCalls = 0;
    setOpCalls = 0;
    setOpUnionCalls = 0;
    setOpIntersectionCalls = 0;
    setOpSubtractCalls = 0;
    splitCalls = 0;
    splitNoNullEdgesCases = 0;
    splitProducedAboveSolids = 0;
    splitProducedBelowSolids = 0;
    joinCalls = 0;
    joinIncompleteCases = 0;
    he1EqualsHe2Cases = 0;
    invalidHalfEdgeInputCases = 0;
    consistencyWarningCases = 0;
    operationFailureCases = 0;
}

long long PolyhedralBoundedSolidStatistics::getOperationFailureCases()
{
    return operationFailureCases;
}

long long PolyhedralBoundedSolidStatistics::getConsistencyWarningCases()
{
    return consistencyWarningCases;
}

long long PolyhedralBoundedSolidStatistics::getHe1EqualsHe2Cases()
{
    return he1EqualsHe2Cases;
}

long long PolyhedralBoundedSolidStatistics::getInvalidHalfEdgeInputCases()
{
    return invalidHalfEdgeInputCases;
}

long long PolyhedralBoundedSolidStatistics::getJoinIncompleteCases()
{
    return joinIncompleteCases;
}

long long PolyhedralBoundedSolidStatistics::getSetOpCalls()
{
    return setOpCalls;
}

void PolyhedralBoundedSolidStatistics::printSummary()
{
    long long lmev = eulerLmevCalls;
    long long lkev = eulerLkevCalls;
    long long lkef = eulerLkefCalls;
    long long lmef = eulerLmefCalls;
    long long lkemr = eulerLkemrCalls;
    long long lmekr = eulerLmekrCalls;
    long long lringmv = eulerLringmvCalls;
    long long eulerTotal = lmev + lkev + lkef + lmef + lkemr + lmekr + lringmv;

    printf("PolyhedralBoundedSolid statistics (enabled=%s):\n",
           isEnabled() ? "true" : "false");
    printf("  Euler ops total: %lld\n", eulerTotal);
    printf("    lmev: %lld\n", lmev);
    printf("    lkev: %lld\n", lkev);
    printf("    lkef: %lld\n", lkef);
    printf("    lmef: %lld\n", lmef);
    printf("    lkemr: %lld\n", lkemr);
    printf("    lmekr: %lld\n", lmekr);
    printf("    lringmv: %lld\n", lringmv);

    printf("  Boolean setOp calls: %lld\n", setOpCalls.load());
    printf("    union: %lld\n", setOpUnionCalls.load());
    printf("    intersection: %lld\n", setOpIntersectionCalls.load());
    printf("    subtract: %lld\n", setOpSubtractCalls.load());

    printf("  Slicing split calls: %lld\n", splitCalls.load());
    printf("    no-null-edges fast-path: %lld\n", splitNoNullEdgesCases.load());
    printf("    produced above solids: %lld\n", splitProducedAboveSolids.load());
    printf("    produced below solids: %lld\n", splitProducedBelowSolids.load());

    printf("  Join calls: %lld\n", joinCalls.load());
    printf("  Join incomplete cases: %lld\n", joinIncompleteCases.load());

    printf("  Borderline he1 == he2 cases: %lld\n", he1EqualsHe2Cases.load());
    printf("  Invalid half-edge inputs: %lld\n", invalidHalfEdgeInputCases.load());
    printf("  Consistency warnings: %lld\n", consistencyWarningCases.load());
    printf("  Operation failure cases: %lld\n", operationFailureCases.load());
}
