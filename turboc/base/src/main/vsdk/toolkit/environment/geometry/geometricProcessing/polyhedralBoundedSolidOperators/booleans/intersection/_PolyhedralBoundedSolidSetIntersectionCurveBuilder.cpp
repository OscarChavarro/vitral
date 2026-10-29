//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include <float.h>
#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/TreeMap.h"
#include "java/util/TreeSet.h"
#include "java/lang/String.h"

#include "java/lang/Double.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetIntersectionCurveBuilder.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"

typedef _PolyhedralBoundedSolidSetIntersectionCurveBuilder Builder;

ArrayList<Builder::IndexList> Builder::lastTraversalNeighborPositions;

/**
Orders node indexes by the position of their null edges along a direction
(ties and nodes without position keep the index order).
*/
#ifndef __TURBOC__
class Builder::NodeDirectionOrder : public Comparator<int> {
public:
    const NullEdgeList& sonea;
    const Vector3Dd& direction;

    NodeDirectionOrder(const NullEdgeList& inSonea, const Vector3Dd& inDirection)
        : sonea(inSonea), direction(inDirection) {}

    int compare(const int& a, const int& b) const
    {
        Vector3Dd pa;
        Vector3Dd pb;
        if ( !nodePosition(sonea[a], pa) ||
             !nodePosition(sonea[b], pb) ) {
            return a < b ? -1 : (b < a ? 1 : 0);
        }
        int cmp = Double::compare(direction.dotProduct(pa),
            direction.dotProduct(pb));
        if ( cmp != 0 ) {
            return cmp;
        }
        return a < b ? -1 : (b < a ? 1 : 0);
    }
};

/**
Orders cycles by their minimum node index.
*/
class Builder::CycleMinimumOrder : public Comparator<
#ifdef __TURBOC__
    IndexList
#else
    ArrayList<int>
#endif
    > {
public:
    // C++ port note: ArrayList<int> instead of the IndexList typedef, which
    // gcc 2.7.2 takes as another Comparator instantiation
    int compare(const
#ifdef __TURBOC__
        IndexList
#else
        ArrayList<int>
#endif
        & a, const
#ifdef __TURBOC__
        IndexList
#else
        ArrayList<int>
#endif
        & b) const
    {
        int minA = minOf(a);
        int minB = minOf(b);
        return minA < minB ? -1 : (minB < minA ? 1 : 0);
    }
};
#endif


/** Internal carrier for one face-pair point group. */
struct FacePairGroup {
    _PolyhedralBoundedSolidFace* faceA;
    _PolyhedralBoundedSolidFace* faceB;
    ArrayList<int> nodes;
};

static BOOLEAN containsValue(const ArrayList<int>& values, int value)
{
    return values.contains(value);
}


//= Report ==========================================================

Builder::Report::Report()
    : oddFacePairGroupCount(0), degenerateDirectionGroupCount(0), nodeCount(0)
{
}

BOOLEAN Builder::Report::isCleanlyClosed() const
{
    long coveredByCycles;
    long i;

    if ( !openChains.isEmpty() || !isolatedNodes.isEmpty() ||
         !pinchNodes.isEmpty() || oddFacePairGroupCount > 0 ||
         degenerateDirectionGroupCount > 0 ) {
        return FALSE;
    }
    coveredByCycles = 0;
    for ( i = 0; i < cycles.size(); i++ ) {
        coveredByCycles += cycles[i].size();
    }
    return coveredByCycles == (long)nodeCount;
}

String Builder::Report::summarize() const
{
    String sb;
    long i;

    sb += "curves: nodes=" + Cpp98Support::toString(nodeCount);
    sb += " cycles=" + Cpp98Support::toString(cycles.size()) + "[";
    for ( i = 0; i < cycles.size(); i++ ) {
        if ( i > 0 ) {
            sb += ",";
        }
        sb += Cpp98Support::toString(cycles[i].size());
    }
    sb += "] openChains=" + Cpp98Support::toString(openChains.size()) + "[";
    for ( i = 0; i < openChains.size(); i++ ) {
        if ( i > 0 ) {
            sb += ",";
        }
        sb += Cpp98Support::toString(openChains[i].size());
    }
    sb += "] isolated=" + Cpp98Support::toString(isolatedNodes.size());
    sb += " pinch=" + Cpp98Support::toString(pinchNodes.size());
    sb += " oddGroups=" + Cpp98Support::toString(oddFacePairGroupCount);
    sb += " degenerateGroups=" + Cpp98Support::toString(degenerateDirectionGroupCount);
    sb += String(" cleanlyClosed=") + (isCleanlyClosed() ? "true" : "false");
    return String(sb.c_str());
}

//= Helpers =========================================================

_PolyhedralBoundedSolidSetIntersectionCurveBuilder::_PolyhedralBoundedSolidSetIntersectionCurveBuilder()
{
}

_PolyhedralBoundedSolidFace* Builder::halfEdgeFace(
    _PolyhedralBoundedSolidHalfEdge* he)
{
    if ( he == 0 || he->parentLoop == 0 ) {
        return 0;
    }
    return he->parentLoop->parentFace;
}

void Builder::collectFaces(const _PolyhedralBoundedSolidSetOperatorNullEdge& ne,
    ArrayList<_PolyhedralBoundedSolidFace*>& outFaces)
{
    _PolyhedralBoundedSolidFace* rightFace;
    _PolyhedralBoundedSolidFace* leftFace;

    outFaces.clear();
    if ( ne.e == 0 ) {
        return;
    }
    rightFace = halfEdgeFace(ne.e->rightHalf);
    leftFace = halfEdgeFace(ne.e->leftHalf);
    if ( rightFace != 0 ) {
        outFaces.add(rightFace);
    }
    if ( leftFace != 0 && leftFace != rightFace ) {
        outFaces.add(leftFace);
    }
}

BOOLEAN Builder::nodePosition(const _PolyhedralBoundedSolidSetOperatorNullEdge& ne,
    Vector3Dd& outPosition)
{
    if ( ne.e == 0 || ne.e->rightHalf == 0 ||
         ne.e->rightHalf->startingVertex == 0 ) {
        return FALSE;
    }
    outPosition = ne.e->rightHalf->startingVertex->position;
    return TRUE;
}

BOOLEAN Builder::faceNormal(_PolyhedralBoundedSolidFace* face, Vector3Dd& outNormal)
{
    InfinitePlane* plane;

    if ( face == 0 ) {
        return FALSE;
    }
    plane = face->getContainingPlane();
    if ( plane == 0 ) {
        return FALSE;
    }
    outNormal = plane->getNormal();
    delete plane;
    return TRUE;
}

void Builder::link(ArrayList<IndexList>& neighbors, int i, int j)
{
    if ( i == j ) {
        return;
    }
    // As Java `LinkedHashSet`: insertion ordered, without repetitions
    if ( !containsValue(neighbors[i], j) ) {
        neighbors[i].add(j);
    }
    if ( !containsValue(neighbors[j], i) ) {
        neighbors[j].add(i);
    }
}

int Builder::minOf(const IndexList& values)
{
    int best;
    long i;

    best = values[0];
    for ( i = 1; i < values.size(); i++ ) {
        if ( values[i] < best ) {
            best = values[i];
        }
    }
    return best;
}

//= Curve report ====================================================

Builder::Report Builder::build(const NullEdgeList& sonea,
    const NullEdgeList& soneb,
    double unitVectorTolerance)
{
    Report report;
    int n;
    int k;
    int i;

    n = (int)Math::min(sonea.size(), soneb.size());
    if ( n == 0 ) {
        return report;
    }

    //-----------------------------------------------------------------
    // 1. Group nodes by (faceA, faceB) pair (insertion ordered, as Java
    //    `LinkedHashMap`).
    //-----------------------------------------------------------------
    ArrayList<FacePairGroup> groups;
    TreeMap<long long, long> groupIndex;
    ArrayList<_PolyhedralBoundedSolidFace*> facesA;
    ArrayList<_PolyhedralBoundedSolidFace*> facesB;

    for ( k = 0; k < n; k++ ) {
        collectFaces(sonea[k], facesA);
        collectFaces(soneb[k], facesB);
        for ( long a = 0; a < facesA.size(); a++ ) {
            _PolyhedralBoundedSolidFace* fa = facesA[a];
            for ( long b = 0; b < facesB.size(); b++ ) {
                _PolyhedralBoundedSolidFace* fb = facesB[b];
                long long key = (((long long)fa->id) << 32) ^
                    ((long long)fb->id & VSDK_LL(0xffffffff));
                long* found = groupIndex.get(key);
                long index;
                if ( found == NULL ) {
                    FacePairGroup group;
                    group.faceA = fa;
                    group.faceB = fb;
                    index = groups.size();
                    groups.add(group);
                    groupIndex.put(key, index);
                }
                else {
                    index = (*found);
                }
                groups[index].nodes.add(k);
            }
        }
    }

    //-----------------------------------------------------------------
    // 2. Derive curve adjacency from each group's chord structure.
    //-----------------------------------------------------------------
    ArrayList<IndexList> neighbors(
        Collections<IndexList>::nCopies(n, IndexList()));

    for ( long g = 0; g < groups.size(); g++ ) {
        FacePairGroup& group = groups[g];
        int m = (int)group.nodes.size();
        if ( m < 2 ) {
            continue;
        }
        if ( m == 2 ) {
            link(neighbors, group.nodes[0], group.nodes[1]);
            continue;
        }

        Vector3Dd normalA;
        Vector3Dd normalB;
        Vector3Dd direction;
        BOOLEAN hasDirection = FALSE;
        if ( faceNormal(group.faceA, normalA) &&
             faceNormal(group.faceB, normalB) ) {
            direction = normalA.crossProduct(normalB);
            hasDirection = direction.length() > unitVectorTolerance;
        }
        if ( !hasDirection ) {
            // Parallel or degenerate planes: chord order along the
            // intersection line is undefined; report instead of guessing.
            report.degenerateDirectionGroupCount++;
            continue;
        }

        IndexList sorted = group.nodes;
#ifdef __TURBOC__
        long sortI;
        long sortJ;
        long sortMin;
        for ( sortI = 0; sortI + 1 < sorted.size(); sortI++ ) {
            sortMin = sortI;
            for ( sortJ = sortI + 1; sortJ < sorted.size(); sortJ++ ) {
                Vector3Dd aPosition;
                Vector3Dd bPosition;
                int a = sorted[sortJ];
                int b = sorted[sortMin];
                int cmp;
                if ( !nodePosition(sonea[a], aPosition) ||
                     !nodePosition(sonea[b], bPosition) ) {
                    cmp = a < b ? -1 : (b < a ? 1 : 0);
                }
                else {
                    cmp = Double::compare(direction.dotProduct(aPosition),
                        direction.dotProduct(bPosition));
                    if ( cmp == 0 ) cmp = a < b ? -1 : (b < a ? 1 : 0);
                }
                if ( cmp < 0 ) sortMin = sortJ;
            }
            if ( sortMin != sortI ) {
                int tmp = sorted[sortI];
                sorted[sortI] = sorted[sortMin];
                sorted[sortMin] = tmp;
            }
        }
#else
        sorted.sort(NodeDirectionOrder(sonea, direction));
#endif

        if ( (m % 2) != 0 ) {
            report.oddFacePairGroupCount++;
        }
        // Entry/exit parity along the intersection line: chord
        // endpoints pair as (0,1), (2,3), ... - linking consecutive
        // sorted points across chords would bridge separate curve
        // passes over the same face pair.
        for ( i = 0; i + 1 < m; i += 2 ) {
            link(neighbors, sorted[i], sorted[i + 1]);
        }
    }

    //-----------------------------------------------------------------
    // 3. Classify nodes and extract chains (from terminals) and cycles.
    //-----------------------------------------------------------------
    ArrayList<BOOLEAN> visited(Collections<BOOLEAN>::nCopies(n, FALSE));

    for ( k = 0; k < n; k++ ) {
        long degree = neighbors[k].size();
        if ( degree == 0 ) {
            report.isolatedNodes.add(k);
            visited[k] = TRUE;
        }
        else if ( degree > 2 ) {
            report.pinchNodes.add(k);
        }
    }

    // Chains: corridors of degree-2 nodes hanging off terminal nodes
    // (degree 1 or degree > 2). Terminal-terminal direct links are
    // deduplicated with an edge-visited set.
    TreeSet<long long> walkedTerminalLinks;
    for ( k = 0; k < n; k++ ) {
        long degree = neighbors[k].size();
        if ( degree == 2 || degree == 0 ) {
            continue;
        }
        for ( long t = 0; t < neighbors[k].size(); t++ ) {
            int nb = neighbors[k][t];
            long nbDegree = neighbors[nb].size();
            if ( nbDegree != 2 ) {
                long long a = Math::min(k, nb);
                long long b = Math::max(k, nb);
                long long linkKey = (a << 32) | b;
                if ( walkedTerminalLinks.add(linkKey) ) {
                    IndexList chain;
                    chain.add(k);
                    chain.add(nb);
                    report.openChains.add(chain);
                }
                continue;
            }
            if ( visited[nb] ) {
                continue;
            }
            IndexList path;
            path.add(k);
            int prev = k;
            int cur = nb;
            while ( neighbors[cur].size() == 2 && !visited[cur] ) {
                visited[cur] = TRUE;
                path.add(cur);
                int next = -1;
                for ( long c = 0; c < neighbors[cur].size(); c++ ) {
                    if ( neighbors[cur][c] != prev ) {
                        next = neighbors[cur][c];
                        break;
                    }
                }
                if ( next < 0 ) {
                    break;
                }
                prev = cur;
                cur = next;
            }
            if ( cur != prev && !containsValue(path, cur) ) {
                path.add(cur);
            }
            report.openChains.add(path);
        }
    }

    // Cycles: remaining unvisited degree-2 components are pure cycles
    // (every corridor touching a terminal was consumed above).
    for ( k = 0; k < n; k++ ) {
        if ( visited[k] || neighbors[k].size() != 2 ) {
            continue;
        }
        IndexList path;
        path.add(k);
        visited[k] = TRUE;
        int prev = k;
        int cur = neighbors[k][0];
        BOOLEAN closed = TRUE;
        while ( cur != k ) {
            if ( visited[cur] || neighbors[cur].size() != 2 ) {
                closed = FALSE;
                break;
            }
            visited[cur] = TRUE;
            path.add(cur);
            int next = -1;
            for ( long c = 0; c < neighbors[cur].size(); c++ ) {
                if ( neighbors[cur][c] != prev ) {
                    next = neighbors[cur][c];
                    break;
                }
            }
            if ( next < 0 ) {
                closed = FALSE;
                break;
            }
            prev = cur;
            cur = next;
        }
        if ( closed ) {
            report.cycles.add(path);
        }
        else {
            report.openChains.add(path);
        }
    }

    report.nodeCount = n;
    return report;
}

//= Traversal order =================================================

BOOLEAN Builder::computeTraversalOrder(const ArrayList<IndexList>& cycles,
    int nodeCount,
    IndexList& outPermutation)
{
    int covered;
    long i;
    long j;

    if ( nodeCount <= 0 ) {
        return FALSE;
    }
    ArrayList<BOOLEAN> seen(Collections<BOOLEAN>::nCopies(nodeCount, FALSE));
    covered = 0;
    for ( i = 0; i < cycles.size(); i++ ) {
        const IndexList& cycle = cycles[i];
        for ( j = 0; j < cycle.size(); j++ ) {
            if ( cycle[j] < 0 || cycle[j] >= nodeCount || seen[cycle[j]] ) {
                return FALSE;
            }
            seen[cycle[j]] = TRUE;
            covered++;
        }
    }
    if ( covered != nodeCount ) {
        return FALSE;
    }

    ArrayList<IndexList> ordered = cycles;
    long sortI;
    long sortJ;
    long sortMin;
    for ( sortI = 0; sortI + 1 < ordered.size(); sortI++ ) {
        sortMin = sortI;
        for ( sortJ = sortI + 1; sortJ < ordered.size(); sortJ++ ) {
            if ( minOf(ordered[sortJ]) < minOf(ordered[sortMin]) ) sortMin = sortJ;
        }
        if ( sortMin != sortI ) {
            IndexList tmp = ordered[sortI];
            ordered[sortI] = ordered[sortMin];
            ordered[sortMin] = tmp;
        }
    }

    outPermutation = Collections<int>::nCopies(nodeCount, 0);
    int position = 0;
    for ( i = 0; i < ordered.size(); i++ ) {
        const IndexList& cycle = ordered[i];
        long len = cycle.size();
        long startPos = 0;
        for ( j = 1; j < len; j++ ) {
            if ( cycle[j] < cycle[startPos] ) {
                startPos = j;
            }
        }
        for ( j = 0; j < len; j++ ) {
            outPermutation[position] = cycle[(startPos + j) % len];
            position++;
        }
    }
    return TRUE;
}

BOOLEAN Builder::orderAndOrientAlongCurves(const ArrayList<IndexList>& cycles,
    int nodeCount,
    NullEdgeList& sonea,
    NullEdgeList& soneb,
    IndexList& outPermutation)
{
    long c;
    long j;
    IndexList validation;

    lastTraversalNeighborPositions.clear();
    if ( !computeTraversalOrder(cycles, nodeCount, validation) ) {
        return FALSE;
    }

    ArrayList<IndexList> ordered = cycles;
    long sortI;
    long sortJ;
    long sortMin;
    for ( sortI = 0; sortI + 1 < ordered.size(); sortI++ ) {
        sortMin = sortI;
        for ( sortJ = sortI + 1; sortJ < ordered.size(); sortJ++ ) {
            if ( minOf(ordered[sortJ]) < minOf(ordered[sortMin]) ) sortMin = sortJ;
        }
        if ( sortMin != sortI ) {
            IndexList tmp = ordered[sortI];
            ordered[sortI] = ordered[sortMin];
            ordered[sortMin] = tmp;
        }
    }

    ArrayList<IndexList> directedCycles;
    for ( c = 0; c < ordered.size(); c++ ) {
        const IndexList& storedCycle = ordered[c];
        long len = storedCycle.size();

        // Direction vote: count two-face struts whose current vertex-id
        // orientation already satisfies the scanjoin role rule in the
        // stored direction. Reversing the cycle inverts every
        // unambiguous vote, so a single count decides the direction.
        int agree = 0;
        int disagree = 0;
        for ( j = 0; j < len; j++ ) {
            int current = storedCycle[j];
            int successor = storedCycle[(j + 1) % len];
            int sharedIsRightA = successorSharedHalfIsRight(
                sonea[current], sonea[successor]);
            if ( sharedIsRightA >= 0 ) {
                // A-side rule: half toward successor must be LEFT.
                if ( sharedIsRightA == 0 ) {
                    agree++;
                }
                else {
                    disagree++;
                }
            }
            int sharedIsRightB = successorSharedHalfIsRight(
                soneb[current], soneb[successor]);
            if ( sharedIsRightB >= 0 ) {
                // B-side rule: half toward successor must be RIGHT.
                if ( sharedIsRightB == 1 ) {
                    agree++;
                }
                else {
                    disagree++;
                }
            }
        }
        IndexList directedCycle;
        if ( disagree > agree ) {
            directedCycle = Collections<int>::nCopies(len, 0);
            for ( j = 0; j < len; j++ ) {
                directedCycle[j] = storedCycle[(len - j) % len];
            }
        }
        else {
            directedCycle = storedCycle;
        }

        // Orient the disagreeing minority along the chosen direction.
        for ( j = 0; j < len; j++ ) {
            int current = directedCycle[j];
            int successor = directedCycle[(j + 1) % len];
            orientTowardSuccessor(sonea[current], sonea[successor], FALSE);
            orientTowardSuccessor(soneb[current], soneb[successor], TRUE);
        }

        // Rotation: the first cycle starts at its minimum member index;
        // later cycles start at the node geometrically closest to the
        // first cycle's start, so the interleaving below begins in phase.
        long startPos = 0;
        if ( directedCycles.isEmpty() ) {
            for ( j = 1; j < len; j++ ) {
                if ( directedCycle[j] < directedCycle[startPos] ) {
                    startPos = j;
                }
            }
        }
        else {
            Vector3Dd anchor;
            BOOLEAN hasAnchor = nodePosition(sonea[directedCycles[0][0]], anchor);
            double bestDistance = DBL_MAX;
            for ( j = 0; j < len; j++ ) {
                Vector3Dd p;
                if ( !hasAnchor || !nodePosition(sonea[directedCycle[j]], p) ) {
                    continue;
                }
                double d = p.subtract(anchor).length();
                if ( d < bestDistance ) {
                    bestDistance = d;
                    startPos = j;
                }
            }
        }
        // ArrayList(len) reserves capacity but leaves size() at zero.
        // Populate the list before indexed assignment, otherwise the
        // round-robin traversal below never advances position.
        IndexList rotated(Collections<int>::nCopies(len, 0));
        for ( j = 0; j < len; j++ ) {
            rotated[j] = directedCycle[(startPos + j) % len];
        }
        directedCycles.add(rotated);
    }

    // Interleave the cycles round-robin so parallel curves advance
    // together. A geometric-proximity merge was tried here and regressed
    // four star motifs: round-robin with phase-aligned starts is the
    // deterministic pacing that preserves every case the legacy emission
    // order handled.
    outPermutation = Collections<int>::nCopies(nodeCount, 0);
    int position = 0;
    long round = 0;
    while ( position < nodeCount ) {
        for ( c = 0; c < directedCycles.size(); c++ ) {
            const IndexList& cycle = directedCycles[c];
            if ( round < cycle.size() ) {
                outPermutation[position] = cycle[round];
                position++;
            }
        }
        round++;
    }

    // Export curve-junction adjacency in processing-position space.
    IndexList positionOf(Collections<int>::nCopies(nodeCount, 0));
    for ( j = 0; j < (long)nodeCount; j++ ) {
        positionOf[outPermutation[j]] = (int)j;
    }
    ArrayList<IndexList> neighborPositions(
        Collections<IndexList>::nCopies(nodeCount, IndexList()));
    for ( c = 0; c < directedCycles.size(); c++ ) {
        const IndexList& cycle = directedCycles[c];
        long len = cycle.size();
        for ( j = 0; j < len; j++ ) {
            int original = cycle[j];
            int previousOriginal = cycle[(j + len - 1) % len];
            int nextOriginal = cycle[(j + 1) % len];
            IndexList partners;
            partners.add(positionOf[previousOriginal]);
            partners.add(positionOf[nextOriginal]);
            neighborPositions[positionOf[original]] = partners;
        }
    }
    lastTraversalNeighborPositions = neighborPositions;
    return TRUE;
}

int Builder::successorSharedHalfIsRight(
    const _PolyhedralBoundedSolidSetOperatorNullEdge& current,
    const _PolyhedralBoundedSolidSetOperatorNullEdge& successor)
{
    _PolyhedralBoundedSolidFace* currentRightFace;
    _PolyhedralBoundedSolidFace* currentLeftFace;

    if ( current.e == 0 || successor.e == 0 ) {
        return -1;
    }
    currentRightFace = halfEdgeFace(current.e->rightHalf);
    currentLeftFace = halfEdgeFace(current.e->leftHalf);
    if ( currentRightFace == 0 || currentLeftFace == 0 ||
         currentRightFace == currentLeftFace ) {
        return -1;
    }
    _PolyhedralBoundedSolidFace* successorRightFace =
        halfEdgeFace(successor.e->rightHalf);
    _PolyhedralBoundedSolidFace* successorLeftFace =
        halfEdgeFace(successor.e->leftHalf);
    BOOLEAN rightShared = currentRightFace == successorRightFace ||
        currentRightFace == successorLeftFace;
    BOOLEAN leftShared = currentLeftFace == successorRightFace ||
        currentLeftFace == successorLeftFace;
    if ( rightShared == leftShared ) {
        return -1;
    }
    return rightShared ? 1 : 0;
}

void Builder::orientTowardSuccessor(
    _PolyhedralBoundedSolidSetOperatorNullEdge& current,
    const _PolyhedralBoundedSolidSetOperatorNullEdge& successor,
    BOOLEAN successorSideIsRight)
{
    int sharedIsRight = successorSharedHalfIsRight(current, successor);

    if ( sharedIsRight < 0 ) {
        return;
    }
    if ( (sharedIsRight == 1) != successorSideIsRight ) {
        _PolyhedralBoundedSolidHalfEdge* tmp = current.e->rightHalf;
        current.e->rightHalf = current.e->leftHalf;
        current.e->leftHalf = tmp;
    }
}
