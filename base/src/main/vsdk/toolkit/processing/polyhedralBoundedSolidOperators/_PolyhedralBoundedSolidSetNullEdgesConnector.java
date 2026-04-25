//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import vsdk.toolkit.common.PolyhedralBoundedSolidStatistics;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Connect stage (big phase 3) for set operations: null-edges pairing and joins,
following section [MANT1988].15.7 and programs [MANT1988].15.13 and
[MANT1988].15.14.
*/
final class _PolyhedralBoundedSolidSetNullEdgesConnector
    extends _PolyhedralBoundedSolidOperator
{
    private static final String TRACE_PIPELINE_SUMMARY_PROPERTY =
        "vsdk.setop.tracePipelineSummary";
    private static final String FORCE_A_RING_MOVE_PROPERTY =
        "vsdk.setop.connect.forceARingMove";
    private static final String ALLOW_CROSS_LOOSE_MATCH_PROPERTY =
        "vsdk.setop.connect.allowCrossLooseMatch";
    private static final String KEEP_INSERTION_ORDER_PROPERTY =
        "vsdk.setop.connect.keepInsertionOrder";
    private static final String FLEXIBLE_ENDPOINT_CHAINS_PROPERTY =
        "vsdk.setop.connect.flexibleEndpointChains";
    private static final String FLEXIBLE_SKIP_CUTS_PROPERTY =
        "vsdk.setop.connect.flexibleSkipCuts";
    private static final String
        FLEXIBLE_ALLOW_SAME_POINT_SELF_CLOSURE_PROPERTY =
        "vsdk.setop.connect.flexibleAllowSamePointSelfClosure";
    private static final String
        FLEXIBLE_SKIP_LEGACY_PAIR_FINAL_CUTS_PROPERTY =
        "vsdk.setop.connect.flexibleSkipLegacyPairFinalCuts";
    private static final String
        FLEXIBLE_KEEP_ONLY_PAIRED_CUT_FACES_PROPERTY =
        "vsdk.setop.connect.flexibleKeepOnlyPairedCutFaces";
    private static final String
        FLEXIBLE_DISABLE_B_RING_MOVE_FOR_SUBTRACT_PROPERTY =
        "vsdk.setop.connect.flexibleDisableBRingMoveForSubtract";
    private static final String
        FLEXIBLE_ALLOW_CROSS_CHAIN_MERGE_PROPERTY =
        "vsdk.setop.connect.flexibleAllowCrossChainMerge";
    private static final String
        FLEXIBLE_REJECT_ONE_SIDED_MATCHES_PROPERTY =
        "vsdk.setop.connect.flexibleRejectOneSidedMatches";
    private static final int DEBUG_01_STRUCTURE = 0x01;
    private static final int DEBUG_05_CONNECT = 0x10;
    private static final int DEBUG_99_SHOWOPERATIONS = 0x40;
    private static final int ENDPOINT_SOLID_A = 0;
    private static final int ENDPOINT_SOLID_B = 1;

    static final class ConnectResult
    {
        private final ArrayList<_PolyhedralBoundedSolidFace> sonfa;
        private final ArrayList<_PolyhedralBoundedSolidFace> sonfb;

        private ConnectResult(ArrayList<_PolyhedralBoundedSolidFace> sonfa,
                              ArrayList<_PolyhedralBoundedSolidFace> sonfb)
        {
            this.sonfa = sonfa;
            this.sonfb = sonfb;
        }

        ArrayList<_PolyhedralBoundedSolidFace> sonfa()
        {
            return sonfa;
        }

        ArrayList<_PolyhedralBoundedSolidFace> sonfb()
        {
            return sonfb;
        }
    }

    private static final class ChainEndpoint
    {
        private _PolyhedralBoundedSolidHalfEdge halfEdge;
        private final int solid;

        private ChainEndpoint(_PolyhedralBoundedSolidHalfEdge halfEdge, int solid)
        {
            this.halfEdge = halfEdge;
            this.solid = solid;
        }
    }

    private static final class OpenChain
    {
        private ChainEndpoint first;
        private ChainEndpoint second;

        private OpenChain(ChainEndpoint first, ChainEndpoint second)
        {
            this.first = first;
            this.second = second;
        }
    }

    private static final class EndpointMatch
    {
        private final int chainIndex;
        private final boolean firstEndpoint;
        private final ChainEndpoint endpoint;
        private final ChainEndpoint otherEndpoint;

        private EndpointMatch(int chainIndex,
                              boolean firstEndpoint,
                              ChainEndpoint endpoint,
                              ChainEndpoint otherEndpoint)
        {
            this.chainIndex = chainIndex;
            this.firstEndpoint = firstEndpoint;
            this.endpoint = endpoint;
            this.otherEndpoint = otherEndpoint;
        }
    }

    private static final class PointJoinResult
    {
        private _PolyhedralBoundedSolidHalfEdge matchedA;
        private _PolyhedralBoundedSolidHalfEdge matchedB;
    }

    private static final class DeferredCut
    {
        private final _PolyhedralBoundedSolidHalfEdge halfEdge;
        private final int pairIndex;

        private DeferredCut(_PolyhedralBoundedSolidHalfEdge halfEdge,
                            int pairIndex)
        {
            this.halfEdge = halfEdge;
            this.pairIndex = pairIndex;
        }
    }

    private static int debugFlags;
    private static int operation;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsa;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsb;
    private static ArrayList<DeferredCut> deferredCutsA;
    private static ArrayList<DeferredCut> deferredCutsB;
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfa;
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfb;
    private static ArrayList<OpenChain> openChains;
    private static Map<Integer, Integer> sonfaPairIndexByFaceId;
    private static Map<Integer, Integer> sonfbPairIndexByFaceId;
    private static int lastLooseACount;
    private static int lastLooseBCount;
    private static int lastSonfaCount;
    private static int lastSonfbCount;
    private static int lastPairCount;
    private static int currentConnectPairIndex;
    private static int nextSyntheticPairIndex;

    private static boolean isPipelineSummaryTraceEnabled()
    {
        return Boolean.getBoolean(TRACE_PIPELINE_SUMMARY_PROPERTY);
    }

    private static boolean isForceARingMoveEnabled()
    {
        return Boolean.getBoolean(FORCE_A_RING_MOVE_PROPERTY);
    }

    private static boolean isCrossLooseMatchEnabled()
    {
        return Boolean.getBoolean(ALLOW_CROSS_LOOSE_MATCH_PROPERTY);
    }

    private static boolean isKeepInsertionOrderEnabled()
    {
        String propertyValue;

        propertyValue = System.getProperty(KEEP_INSERTION_ORDER_PROPERTY);
        if ( propertyValue == null ) {
            return true;
        }
        return Boolean.parseBoolean(propertyValue);
    }

    private static boolean isFlexibleEndpointChainsEnabled()
    {
        return Boolean.getBoolean(FLEXIBLE_ENDPOINT_CHAINS_PROPERTY);
    }

    private static boolean isFlexibleSkipCutsEnabled()
    {
        return Boolean.getBoolean(FLEXIBLE_SKIP_CUTS_PROPERTY);
    }

    private static boolean isFlexibleSamePointSelfClosureEnabled()
    {
        return Boolean.getBoolean(
            FLEXIBLE_ALLOW_SAME_POINT_SELF_CLOSURE_PROPERTY);
    }

    private static boolean isFlexibleSkipLegacyPairFinalCutsEnabled()
    {
        return Boolean.getBoolean(
            FLEXIBLE_SKIP_LEGACY_PAIR_FINAL_CUTS_PROPERTY);
    }

    private static boolean isFlexibleKeepOnlyPairedCutFacesEnabled()
    {
        return Boolean.getBoolean(
            FLEXIBLE_KEEP_ONLY_PAIRED_CUT_FACES_PROPERTY);
    }

    private static boolean isFlexibleDisableBRingMoveForSubtractEnabled()
    {
        return Boolean.getBoolean(
            FLEXIBLE_DISABLE_B_RING_MOVE_FOR_SUBTRACT_PROPERTY);
    }

    private static boolean isFlexibleAllowCrossChainMergeEnabled()
    {
        return Boolean.getBoolean(
            FLEXIBLE_ALLOW_CROSS_CHAIN_MERGE_PROPERTY);
    }

    private static boolean isFlexibleRejectOneSidedMatchesEnabled()
    {
        return Boolean.getBoolean(
            FLEXIBLE_REJECT_ONE_SIDED_MATCHES_PROPERTY);
    }

    private static void tracePipelineSummary(String message)
    {
        if ( !isPipelineSummaryTraceEnabled() ) {
            return;
        }
        System.out.println("[SetOpPipelineTrace] " + message);
    }

    private static String summarizeHalfEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        if ( he == null ) {
            return "null";
        }

        String from = "?";
        String to = "?";
        String face = "?";

        if ( he.startingVertex != null ) {
            from = Integer.toString(he.startingVertex.id);
        }
        if ( he.parentLoop != null ) {
            _PolyhedralBoundedSolidHalfEdge next = he.next();
            if ( next != null && next.startingVertex != null ) {
                to = Integer.toString(next.startingVertex.id);
            }
            if ( he.parentLoop.parentFace != null ) {
                face = Integer.toString(he.parentLoop.parentFace.id);
            }
        }
        String fromPoint = "?";
        String toPoint = "?";

        if ( he.startingVertex != null ) {
            fromPoint = he.startingVertex.position.toString();
        }
        if ( he.parentLoop != null ) {
            _PolyhedralBoundedSolidHalfEdge next = he.next();
            if ( next != null && next.startingVertex != null ) {
                toPoint = next.startingVertex.position.toString();
            }
        }
        return "he(v=" + from + "->" + to + ",f=" + face +
            ",p=" + fromPoint + "->" + toPoint + ")";
    }

    private static String summarizeNullEdge(
        _PolyhedralBoundedSolidSetOperatorNullEdge edge)
    {
        if ( edge == null || edge.e == null ) {
            return "null";
        }
        return summarizeHalfEdge(edge.e.rightHalf) + " | " +
            summarizeHalfEdge(edge.e.leftHalf);
    }

    private static void setCurrentConnectContext(int pairIndex)
    {
        currentConnectPairIndex = pairIndex;
    }

    private static int allocateSyntheticPairIndex()
    {
        int pairIndex;

        pairIndex = nextSyntheticPairIndex;
        nextSyntheticPairIndex++;
        return pairIndex;
    }

    static int getSonfaPairIndex(_PolyhedralBoundedSolidFace face)
    {
        Integer pairIndex;

        if ( face == null || sonfaPairIndexByFaceId == null ) {
            return -1;
        }
        pairIndex = sonfaPairIndexByFaceId.get(Integer.valueOf(face.id));
        return pairIndex != null ? pairIndex.intValue() : -1;
    }

    static int getSonfbPairIndex(_PolyhedralBoundedSolidFace face)
    {
        Integer pairIndex;

        if ( face == null || sonfbPairIndexByFaceId == null ) {
            return -1;
        }
        pairIndex = sonfbPairIndexByFaceId.get(Integer.valueOf(face.id));
        return pairIndex != null ? pairIndex.intValue() : -1;
    }

    private static String summarizeLooseEnds(
        ArrayList<_PolyhedralBoundedSolidHalfEdge> endsA,
        ArrayList<_PolyhedralBoundedSolidHalfEdge> endsB)
    {
        StringBuilder out = new StringBuilder();
        int i;

        out.append("pairs=").append(endsA.size()).append(" [");
        for ( i = 0; i < endsA.size() && i < endsB.size(); i++ ) {
            if ( i > 0 ) {
                out.append(" | ");
            }
            out.append(i)
               .append(":A=")
               .append(summarizeHalfEdge(endsA.get(i)))
               .append(",B=")
               .append(summarizeHalfEdge(endsB.get(i)));
        }
        out.append("]");
        return out.toString();
    }

    private static String summarizeChainEndpoint(ChainEndpoint endpoint)
    {
        if ( endpoint == null ) {
            return "null";
        }
        return (endpoint.solid == ENDPOINT_SOLID_A ? "A=" : "B=") +
            summarizeHalfEdge(endpoint.halfEdge);
    }

    private static String summarizeOpenChains()
    {
        StringBuilder out = new StringBuilder();
        int i;

        if ( openChains == null ) {
            return "chains=0 []";
        }
        out.append("chains=").append(openChains.size()).append(" [");
        for ( i = 0; i < openChains.size(); i++ ) {
            if ( i > 0 ) {
                out.append(" | ");
            }
            out.append(i)
               .append(":")
               .append(summarizeChainEndpoint(openChains.get(i).first))
               .append(",")
               .append(summarizeChainEndpoint(openChains.get(i).second));
        }
        out.append("]");
        return out.toString();
    }

    private static ChainEndpoint endpointForSolid(OpenChain chain, int solid)
    {
        if ( chain == null ) {
            return null;
        }
        if ( chain.first != null && chain.first.solid == solid ) {
            return chain.first;
        }
        if ( chain.second != null && chain.second.solid == solid ) {
            return chain.second;
        }
        return null;
    }

    private static int countOpenEndpoints(int solid)
    {
        int count = 0;
        int i;

        if ( openChains == null ) {
            return 0;
        }
        for ( i = 0; i < openChains.size(); i++ ) {
            if ( openChains.get(i).first.solid == solid ) {
                count++;
            }
            if ( openChains.get(i).second.solid == solid ) {
                count++;
            }
        }
        return count;
    }

    private static void updateLastSnapshotFromOpenChains()
    {
        lastLooseACount = countOpenEndpoints(ENDPOINT_SOLID_A);
        lastLooseBCount = countOpenEndpoints(ENDPOINT_SOLID_B);
        lastSonfaCount = (sonfa != null) ? sonfa.size() : 0;
        lastSonfbCount = (sonfb != null) ? sonfb.size() : 0;
    }

    private static EndpointMatch findEndpointMatch(
        _PolyhedralBoundedSolidHalfEdge current,
        int solid)
    {
        int i;
        EndpointMatch found = null;
        ChainEndpoint endpoint;
        ChainEndpoint other;

        if ( openChains == null ) {
            return null;
        }
        for ( i = 0; i < openChains.size(); i++ ) {
            endpoint = openChains.get(i).first;
            other = openChains.get(i).second;
            if ( endpoint.solid == solid &&
                 neighbor(current, endpoint.halfEdge) &&
                 (isFlexibleSamePointSelfClosureEnabled() ||
                  !isSamePoint(current, endpoint.halfEdge)) ) {
                if ( found == null ) {
                    found = new EndpointMatch(i, true, endpoint, other);
                }
                else {
                }
            }
            endpoint = openChains.get(i).second;
            other = openChains.get(i).first;
            if ( endpoint.solid == solid &&
                 neighbor(current, endpoint.halfEdge) &&
                 (isFlexibleSamePointSelfClosureEnabled() ||
                  !isSamePoint(current, endpoint.halfEdge)) ) {
                if ( found == null ) {
                    found = new EndpointMatch(i, false, endpoint, other);
                }
                else {
                }
            }
        }
        return found;
    }

    private static void replaceMatchedEndpoint(EndpointMatch match,
                                               ChainEndpoint replacement)
    {
        if ( match.firstEndpoint ) {
            openChains.get(match.chainIndex).first = replacement;
        }
        else {
            openChains.get(match.chainIndex).second = replacement;
        }
    }

    private static void removeOpenChain(int chainIndex)
    {
        openChains.remove(chainIndex);
    }

    private static boolean isFlexibleLooseA(_PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        if ( he == null || openChains == null ) {
            return false;
        }
        for ( i = 0; i < openChains.size(); i++ ) {
            if ( openChains.get(i).first.solid == ENDPOINT_SOLID_A &&
                 openChains.get(i).first.halfEdge == he ) {
                return true;
            }
            if ( openChains.get(i).second.solid == ENDPOINT_SOLID_A &&
                 openChains.get(i).second.halfEdge == he ) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFlexibleLooseB(_PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        if ( he == null || openChains == null ) {
            return false;
        }
        for ( i = 0; i < openChains.size(); i++ ) {
            if ( openChains.get(i).first.solid == ENDPOINT_SOLID_B &&
                 openChains.get(i).first.halfEdge == he ) {
                return true;
            }
            if ( openChains.get(i).second.solid == ENDPOINT_SOLID_B &&
                 openChains.get(i).second.halfEdge == he ) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSamePoint(_PolyhedralBoundedSolidHalfEdge first,
                                       _PolyhedralBoundedSolidHalfEdge second)
    {
        if ( first == null || second == null ||
             first.startingVertex == null || second.startingVertex == null ) {
            return false;
        }
        return PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
            first.startingVertex.position,
            second.startingVertex.position,
            numericContext);
    }

    private static boolean isSamePoint(ChainEndpoint first, ChainEndpoint second)
    {
        if ( first == null || second == null ) {
            return false;
        }
        return isSamePoint(first.halfEdge, second.halfEdge);
    }

    private static boolean canCutCoincidentHalfEdge(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidEdge edge;
        _PolyhedralBoundedSolidLoop loop;

        if ( he == null ) {
            return false;
        }
        edge = he.parentEdge;
        loop = he.parentLoop;
        if ( edge == null || loop == null ) {
            return false;
        }
        if ( edge.rightHalf == null || edge.leftHalf == null ) {
            return false;
        }
        if ( edge.rightHalf.parentLoop != edge.leftHalf.parentLoop ) {
            return true;
        }
        return loop.halfEdgesList.size() > 2;
    }

    private static boolean canCutCoincidentEndpoint(ChainEndpoint endpoint)
    {
        if ( endpoint == null ) {
            return false;
        }
        return canCutCoincidentHalfEdge(endpoint.halfEdge);
    }

    private static boolean hasReusableCoincidentCutFace(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidFace face;

        if ( he == null || he.parentLoop == null ) {
            return false;
        }
        face = he.parentLoop.parentFace;
        return face != null && face.boundariesList.size() > 1;
    }

    private static boolean canCutCoincidentFinishFace(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidEdge edge;
        _PolyhedralBoundedSolidLoop loop;

        if ( he == null ) {
            return false;
        }
        edge = he.parentEdge;
        loop = he.parentLoop;
        if ( edge == null || loop == null ||
             edge.rightHalf == null || edge.leftHalf == null ) {
            return false;
        }
        if ( edge.rightHalf.parentLoop != edge.leftHalf.parentLoop ) {
            return false;
        }
        return loop.halfEdgesList.size() > 2;
    }

    private static _PolyhedralBoundedSolidFace registerCoincidentCutFace(
        _PolyhedralBoundedSolidHalfEdge he,
        ArrayList<_PolyhedralBoundedSolidFace> target,
        String label)
    {
        _PolyhedralBoundedSolidFace face;

        if ( he == null || he.parentLoop == null ) {
            return null;
        }
        face = he.parentLoop.parentFace;
        if ( face == null || face.boundariesList.size() <= 1 ) {
            return null;
        }
        target.add(face);
        return face;
    }

    private static _PolyhedralBoundedSolidFace registerCoincidentCutFace(
        ChainEndpoint endpoint,
        ArrayList<_PolyhedralBoundedSolidFace> target,
        String label)
    {
        if ( endpoint == null ) {
            return null;
        }
        return registerCoincidentCutFace(endpoint.halfEdge, target, label);
    }

    private static _PolyhedralBoundedSolidFace
    finalizeCoincidentChainEndpointA(ChainEndpoint endpoint)
    {
        if ( canCutCoincidentEndpoint(endpoint) ) {
            return cutA(endpoint.halfEdge);
        }
        return registerCoincidentCutFace(endpoint, sonfa, "reuse-sonfa");
    }

    private static _PolyhedralBoundedSolidFace
    finalizeCoincidentChainEndpointB(ChainEndpoint endpoint)
    {
        if ( canCutCoincidentEndpoint(endpoint) ) {
            return cutB(endpoint.halfEdge);
        }
        return registerCoincidentCutFace(endpoint, sonfb, "reuse-sonfb");
    }

    private static boolean canFinalizeCoincidentLooseA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return canCutCoincidentFinishFace(he) ||
            hasReusableCoincidentCutFace(he);
    }

    private static boolean canFinalizeCoincidentLooseB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return canCutCoincidentFinishFace(he) ||
            hasReusableCoincidentCutFace(he);
    }

    private static void finalizeCoincidentLooseA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( canCutCoincidentFinishFace(he) ) {
            cutA(he);
        }
        else {
            registerCoincidentCutFace(he, sonfa, "reuse-sonfa");
        }
    }

    private static void finalizeCoincidentLooseB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( canCutCoincidentFinishFace(he) ) {
            cutB(he);
        }
        else {
            registerCoincidentCutFace(he, sonfb, "reuse-sonfb");
        }
    }

    private static void removeLoosePair(int index)
    {
        endsa.remove(index);
        endsb.remove(index);
    }

    private static boolean closeLegacyCoincidentLooseEnds()
    {
        int i;
        int j;

        if ( endsa == null || endsb == null ) {
            return false;
        }

        for ( i = 0; i < endsa.size() && i < endsb.size(); i++ ) {
            for ( j = i + 1; j < endsa.size() && j < endsb.size(); j++ ) {
                if ( !isSamePoint(endsa.get(i), endsa.get(j)) ||
                     !isSamePoint(endsb.get(i), endsb.get(j)) ) {
                    continue;
                }
                if ( !canFinalizeCoincidentLooseA(endsa.get(i)) ||
                     !canFinalizeCoincidentLooseB(endsb.get(i)) ) {
                    tracePipelineSummary(
                        "connect coincident-loose skip i=" + i + " j=" + j +
                        " A0=" + summarizeHalfEdge(endsa.get(i)) +
                        " A1=" + summarizeHalfEdge(endsa.get(j)) +
                        " B0=" + summarizeHalfEdge(endsb.get(i)) +
                        " B1=" + summarizeHalfEdge(endsb.get(j)));
                    continue;
                }
                tracePipelineSummary(
                    "connect coincident-loose close i=" + i + " j=" + j +
                    " A0=" + summarizeHalfEdge(endsa.get(i)) +
                    " A1=" + summarizeHalfEdge(endsa.get(j)) +
                    " B0=" + summarizeHalfEdge(endsb.get(i)) +
                    " B1=" + summarizeHalfEdge(endsb.get(j)));
                setCurrentConnectContext(allocateSyntheticPairIndex());
                finalizeCoincidentLooseA(endsa.get(i));
                finalizeCoincidentLooseB(endsb.get(i));
                removeLoosePair(j);
                removeLoosePair(i);
                return true;
            }
        }
        return false;
    }

    private static boolean closeFlexibleChainsByCoincidentEndpoints()
    {
        int i;
        int j;

        if ( openChains == null ) {
            return false;
        }

        for ( i = 0; i < openChains.size(); i++ ) {
            ChainEndpoint a0 = endpointForSolid(openChains.get(i),
                ENDPOINT_SOLID_A);
            ChainEndpoint b0 = endpointForSolid(openChains.get(i),
                ENDPOINT_SOLID_B);
            if ( a0 == null || b0 == null ) {
                continue;
            }

            for ( j = i + 1; j < openChains.size(); j++ ) {
                ChainEndpoint a1 = endpointForSolid(openChains.get(j),
                    ENDPOINT_SOLID_A);
                ChainEndpoint b1 = endpointForSolid(openChains.get(j),
                    ENDPOINT_SOLID_B);
                _PolyhedralBoundedSolidFace addedA;
                _PolyhedralBoundedSolidFace addedB;

                if ( a1 == null || b1 == null ) {
                    continue;
                }
                if ( !isSamePoint(a0, a1) || !isSamePoint(b0, b1) ) {
                    continue;
                }

                tracePipelineSummary(
                    "connect coincident-chain closure i=" + i + " j=" + j +
                    " A0=" + summarizeChainEndpoint(a0) +
                    " A1=" + summarizeChainEndpoint(a1) +
                    " B0=" + summarizeChainEndpoint(b0) +
                    " B1=" + summarizeChainEndpoint(b1));

                setCurrentConnectContext(allocateSyntheticPairIndex());
                addedA = finalizeCoincidentChainEndpointA(a0);
                addedB = finalizeCoincidentChainEndpointB(b0);
                if ( addedA == null || addedB == null ) {
                    keepOnlyPairedFlexibleCutFaces(addedA, addedB,
                        "coincident-chain");
                    continue;
                }

                openChains.remove(j);
                openChains.remove(i);
                return true;
            }
        }
        return false;
    }

    static int getLastLooseACount()
    {
        return lastLooseACount;
    }

    static int getLastLooseBCount()
    {
        return lastLooseBCount;
    }

    static int getLastSonfaCount()
    {
        return lastSonfaCount;
    }

    static int getLastSonfbCount()
    {
        return lastSonfbCount;
    }

    static int getLastPairCount()
    {
        return lastPairCount;
    }

    private static void updateLastSnapshot()
    {
        lastLooseACount = (endsa != null) ? endsa.size() : 0;
        lastLooseBCount = (endsb != null) ? endsb.size() : 0;
        lastSonfaCount = (sonfa != null) ? sonfa.size() : 0;
        lastSonfbCount = (sonfb != null) ? sonfb.size() : 0;
    }

    private static boolean isLiveHalfEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        return he != null &&
            he.parentEdge != null &&
            he.parentLoop != null &&
            he.parentLoop.parentFace != null;
    }

    private static boolean sharesParentFace(
        _PolyhedralBoundedSolidHalfEdge first,
        _PolyhedralBoundedSolidHalfEdge second)
    {
        return isLiveHalfEdge(first) &&
            isLiveHalfEdge(second) &&
            first.parentLoop.parentFace == second.parentLoop.parentFace;
    }

    private static boolean hasPendingNullEdgeOnSameFace(
        _PolyhedralBoundedSolidHalfEdge he,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> pendingNullEdges)
    {
        int i;

        if ( !isLiveHalfEdge(he) ||
             pendingNullEdges == null ||
             currentConnectPairIndex < 0 ) {
            return false;
        }

        for ( i = currentConnectPairIndex + 1; i < pendingNullEdges.size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge;

            edge = pendingNullEdges.get(i).e;
            if ( edge == null ) {
                continue;
            }
            if ( sharesParentFace(he, edge.rightHalf) ||
                 sharesParentFace(he, edge.leftHalf) ) {
                return true;
            }
        }
        return false;
    }

    private static void rememberDeferredCut(
        ArrayList<DeferredCut> deferredCuts,
        _PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        if ( !isLiveHalfEdge(he) || deferredCuts == null ) {
            return;
        }

        for ( i = 0; i < deferredCuts.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge existing;

            existing = deferredCuts.get(i).halfEdge;
            if ( existing == he ) {
                return;
            }
            if ( isLiveHalfEdge(existing) &&
                 existing.parentEdge == he.parentEdge ) {
                return;
            }
        }
        deferredCuts.add(new DeferredCut(he, currentConnectPairIndex));
    }

    private static boolean shouldDeferClassicCutA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return hasPendingNullEdgeOnSameFace(he, sonea);
    }

    private static boolean shouldDeferClassicCutB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return hasPendingNullEdgeOnSameFace(he, soneb);
    }

    private static boolean shouldDeferFlexibleCutA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return false;
    }

    private static boolean shouldDeferFlexibleCutB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        return false;
    }

    private static _PolyhedralBoundedSolidFace cutOrDeferClassicA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( shouldDeferClassicCutA(he) ) {
            rememberDeferredCut(deferredCutsA, he);
            tracePipelineSummary(
                "connect defer cutA " + summarizeHalfEdge(he));
            return null;
        }
        return cutA(he);
    }

    private static _PolyhedralBoundedSolidFace cutOrDeferClassicB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( shouldDeferClassicCutB(he) ) {
            rememberDeferredCut(deferredCutsB, he);
            tracePipelineSummary(
                "connect defer cutB " + summarizeHalfEdge(he));
            return null;
        }
        return cutB(he);
    }

    private static _PolyhedralBoundedSolidFace cutOrDeferFlexibleA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( shouldDeferFlexibleCutA(he) ) {
            rememberDeferredCut(deferredCutsA, he);
            tracePipelineSummary(
                "connect defer cutA " + summarizeHalfEdge(he));
            return null;
        }
        return cutA(he);
    }

    private static _PolyhedralBoundedSolidFace cutOrDeferFlexibleB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        if ( shouldDeferFlexibleCutB(he) ) {
            rememberDeferredCut(deferredCutsB, he);
            tracePipelineSummary(
                "connect defer cutB " + summarizeHalfEdge(he));
            return null;
        }
        return cutB(he);
    }

    private static boolean flushDeferredCuts(
        ArrayList<DeferredCut> deferredCuts,
        boolean onSolidA,
        boolean flexibleMode)
    {
        boolean progress;
        int i;

        progress = false;
        if ( deferredCuts == null ) {
            return false;
        }

        for ( i = deferredCuts.size() - 1; i >= 0; i-- ) {
            DeferredCut deferredCut;
            _PolyhedralBoundedSolidHalfEdge he;
            boolean stillBlocked;
            int previousPairIndex;

            deferredCut = deferredCuts.get(i);
            he = deferredCut.halfEdge;
            if ( !isLiveHalfEdge(he) ) {
                deferredCuts.remove(i);
                continue;
            }

            stillBlocked = onSolidA ?
                (flexibleMode ? shouldDeferFlexibleCutA(he) :
                    shouldDeferClassicCutA(he)) :
                (flexibleMode ? shouldDeferFlexibleCutB(he) :
                    shouldDeferClassicCutB(he));
            if ( stillBlocked ) {
                continue;
            }

            previousPairIndex = currentConnectPairIndex;
            setCurrentConnectContext(deferredCut.pairIndex);
            if ( onSolidA ) {
                cutA(he);
            }
            else {
                cutB(he);
            }
            setCurrentConnectContext(previousPairIndex);
            deferredCuts.remove(i);
            progress = true;
        }
        return progress;
    }

    private static void flushDeferredClassicCuts()
    {
        boolean progress;

        setCurrentConnectContext(-1);
        do {
            progress = false;
            progress |= flushDeferredCuts(deferredCutsA, true, false);
            progress |= flushDeferredCuts(deferredCutsB, false, false);
        } while ( progress );
    }

    private static void flushDeferredFlexibleCuts()
    {
        boolean progress;

        setCurrentConnectContext(-1);
        do {
            progress = false;
            progress |= flushDeferredCuts(deferredCutsA, true, true);
            progress |= flushDeferredCuts(deferredCutsB, false, true);
        } while ( progress );
    }

    static ConnectResult connect(
        int op,
        int flags,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSonea,
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> inSoneb)
    {
        operation = op;
        debugFlags = flags;
        sonea = inSonea;
        soneb = inSoneb;
        lastPairCount = Math.min(sonea.size(), soneb.size());
        lastLooseACount = 0;
        lastLooseBCount = 0;
        lastSonfaCount = 0;
        lastSonfbCount = 0;
        openChains = null;
        nextSyntheticPairIndex = lastPairCount;
        deferredCutsA = new ArrayList<DeferredCut>();
        deferredCutsB = new ArrayList<DeferredCut>();
        if ( isFlexibleEndpointChainsEnabled() ) {
            setOpConnectWithFlexibleChains();
        }
        else {
            setOpConnect();
        }
        return new ConnectResult(sonfa, sonfb);
    }

    private static void sortNullEdges()
    {
        if ( isKeepInsertionOrderEnabled() ) {
            tracePipelineSummary("connect sort skipped; using insertion order");
            return;
        }
        Collections.sort(sonea);
        Collections.sort(soneb);
    }

    /**
    Following section [MANT1988].15.7. and program [MANT1988].15.13.
    */
    private static _PolyhedralBoundedSolidHalfEdge[]
    canJoin(_PolyhedralBoundedSolidHalfEdge hea,
             _PolyhedralBoundedSolidHalfEdge heb)
    {
        int i;
        _PolyhedralBoundedSolidHalfEdge ret[];
        boolean condition1;
        boolean condition2;
        int matchAIndex = -1;
        int matchBIndex = -1;
        _PolyhedralBoundedSolidHalfEdge matchA = null;
        _PolyhedralBoundedSolidHalfEdge matchB = null;

        ret = new _PolyhedralBoundedSolidHalfEdge[2];

        for ( i = 0; i < endsa.size(); i++ ) {

            condition1 = neighbor(hea, endsa.get(i));
            condition2 = neighbor(heb, endsb.get(i));
            if ( condition1 && matchA == null ) {
                matchAIndex = i;
                matchA = endsa.get(i);
            }
            if ( condition2 && matchB == null ) {
                matchBIndex = i;
                matchB = endsb.get(i);
            }

            if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
                System.out.println("    . Testing for neighborhood A[" +
                   hea.startingVertex.id +
                   "/" +
                   hea.next().startingVertex.id +
                   "] vs. A[" +
                   endsa.get(i).startingVertex.id +
                   "/" +
                   endsa.get(i).next().startingVertex.id +
                   "]: " +
                   (condition1?"true":"false") +
                   " ParentFaces: " +
                   hea.parentLoop.parentFace.id +
                   " / " +
                   endsa.get(i).parentLoop.parentFace.id);

                System.out.println("    . Testing for neighborhood B[" +
                   heb.startingVertex.id +
                   "/" +
                   heb.next().startingVertex.id +
                   "] vs. B[" +
                   endsb.get(i).startingVertex.id +
                   "/" +
                   endsb.get(i).next().startingVertex.id +
                   "]: " +
                   (condition2?"true":"false") +
                   " ParentFaces: " +
                   heb.parentLoop.parentFace.id +
                   " / " +
                   endsb.get(i).parentLoop.parentFace.id);
            }

            if ( condition1 && condition2 ) {
                ret[0] = endsa.get(i);
                ret[1] = endsb.get(i);
                endsa.remove(i);
                endsb.remove(i);
                return ret;
            }
        }
        if ( isCrossLooseMatchEnabled() &&
             matchAIndex >= 0 &&
             matchBIndex >= 0 &&
             matchAIndex != matchBIndex ) {
            int minIndex;
            int maxIndex;
            _PolyhedralBoundedSolidHalfEdge residualA;
            _PolyhedralBoundedSolidHalfEdge residualB;

            ret[0] = matchA;
            ret[1] = matchB;
            residualA = endsa.get(matchBIndex);
            residualB = endsb.get(matchAIndex);
            minIndex = Math.min(matchAIndex, matchBIndex);
            maxIndex = Math.max(matchAIndex, matchBIndex);
            endsa.remove(maxIndex);
            endsb.remove(maxIndex);
            endsa.remove(minIndex);
            endsb.remove(minIndex);
            endsa.add(residualA);
            endsb.add(residualB);
            return ret;
        }
        endsa.add(hea);
        endsb.add(heb);
        return null;
    }

    private static boolean isLooseA(_PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        for ( i = 0; i < endsa.size(); i++ ) {
            if ( he == endsa.get(i) ) return true;
        }

        return false;
    }

    private static boolean isLooseB(_PolyhedralBoundedSolidHalfEdge he)
    {
        int i;

        for ( i = 0; i < endsb.size(); i++ ) {
            if ( he == endsb.get(i) ) return true;
        }

        return false;
    }

    private static _PolyhedralBoundedSolidFace cutA(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;
        _PolyhedralBoundedSolidFace addedFace = null;
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        if ( withDebug ) {
            System.out.println("       -> CUTA:");
            System.out.println("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;
        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            addedFace = he.parentLoop.parentFace;
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
            if ( addedFace.boundariesList.size() >= 2 ) {
                sonfa.add(addedFace);
                if ( sonfaPairIndexByFaceId != null ) {
                    sonfaPairIndexByFaceId.put(Integer.valueOf(addedFace.id),
                        Integer.valueOf(currentConnectPairIndex));
                }
            }
            else {
                addedFace = null;
            }
        }
        else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        return addedFace;
    }

    private static _PolyhedralBoundedSolidFace cutB(
        _PolyhedralBoundedSolidHalfEdge he)
    {
        PolyhedralBoundedSolid s;
        _PolyhedralBoundedSolidFace addedFace = null;
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        if ( withDebug ) {
            System.out.println("       -> CUTB:");
            System.out.println("          . He: " + he);
        }

        s = he.parentLoop.parentFace.parentSolid;
        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            addedFace = he.parentLoop.parentFace;
            PolyhedralBoundedSolidEulerOperators.lkemr(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
            if ( addedFace.boundariesList.size() >= 2 ) {
                sonfb.add(addedFace);
                if ( sonfbPairIndexByFaceId != null ) {
                    sonfbPairIndexByFaceId.put(Integer.valueOf(addedFace.id),
                        Integer.valueOf(currentConnectPairIndex));
                }
            }
            else {
                addedFace = null;
            }
        }
        else {
            PolyhedralBoundedSolidEulerOperators.lkef(s, he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        return addedFace;
    }

    private static void removeLastCutFaceIfSame(
        ArrayList<_PolyhedralBoundedSolidFace> faces,
        _PolyhedralBoundedSolidFace face)
    {
        int last;

        if ( faces == null || face == null || faces.isEmpty() ) {
            return;
        }
        last = faces.size() - 1;
        if ( faces.get(last) == face ) {
            faces.remove(last);
        }
    }

    private static void keepOnlyPairedFlexibleCutFaces(
        _PolyhedralBoundedSolidFace addedA,
        _PolyhedralBoundedSolidFace addedB,
        String context)
    {
        if ( !isFlexibleKeepOnlyPairedCutFacesEnabled() ||
             ((addedA == null) == (addedB == null)) ) {
            return;
        }

        if ( addedA != null ) {
            removeLastCutFaceIfSame(sonfa, addedA);
        }
        if ( addedB != null ) {
            removeLastCutFaceIfSame(sonfb, addedB);
        }
    }

    private static void removeLooseEndsA(_PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidHalfEdge heStart;
        int i;

        heStart = he;
        do {
            for ( i = 0; i < endsa.size(); i++ ) {
                if ( endsa.get(i) == he ) {
                    endsa.remove(i);
                    break;
                }
            }
            he = he.next();
        } while ( he != heStart );
    }

    private static void removeLooseEndsB(_PolyhedralBoundedSolidHalfEdge he)
    {
        _PolyhedralBoundedSolidHalfEdge heStart;
        int i;

        heStart = he;
        do {
            for ( i = 0; i < endsb.size(); i++ ) {
                if ( endsb.get(i) == he ) {
                    endsb.remove(i);
                    break;
                }
            }
            he = he.next();
        } while ( he != heStart );
    }

    private static PointJoinResult processPointWithFlexibleChains(
        _PolyhedralBoundedSolidHalfEdge currentA,
        _PolyhedralBoundedSolidHalfEdge currentB,
        boolean withDebug,
        boolean allowRingMoveOnAJoin,
        boolean allowRingMoveOnBJoin)
    {
        EndpointMatch matchA;
        EndpointMatch matchB;
        PointJoinResult result;
        int firstRemove;
        int secondRemove;

        result = new PointJoinResult();
        matchA = findEndpointMatch(currentA, ENDPOINT_SOLID_A);
        matchB = findEndpointMatch(currentB, ENDPOINT_SOLID_B);
        if ( matchA != null && matchB == null &&
             isSamePoint(currentA, matchA.endpoint.halfEdge) ) {
            matchA = null;
        }
        if ( matchB != null && matchA == null &&
             isSamePoint(currentB, matchB.endpoint.halfEdge) ) {
            matchB = null;
        }
        if ( matchA != null && matchB != null &&
             matchA.chainIndex != matchB.chainIndex &&
             !isFlexibleAllowCrossChainMergeEnabled() ) {
            matchA = null;
            matchB = null;
        }
        if ( (matchA == null) != (matchB == null) &&
             isFlexibleRejectOneSidedMatchesEnabled() ) {
            matchA = null;
            matchB = null;
        }

        if ( matchA != null ) {
            result.matchedA = matchA.endpoint.halfEdge;
            join(matchA.endpoint.halfEdge, currentA, withDebug,
                allowRingMoveOnAJoin);
        }
        if ( matchB != null ) {
            result.matchedB = matchB.endpoint.halfEdge;
            join(matchB.endpoint.halfEdge, currentB, withDebug,
                allowRingMoveOnBJoin);
        }

        if ( matchA == null && matchB == null ) {
            openChains.add(new OpenChain(
                new ChainEndpoint(currentA, ENDPOINT_SOLID_A),
                new ChainEndpoint(currentB, ENDPOINT_SOLID_B)));
        }
        else if ( matchA != null && matchB == null ) {
            replaceMatchedEndpoint(matchA,
                new ChainEndpoint(currentB, ENDPOINT_SOLID_B));
        }
        else if ( matchA == null ) {
            replaceMatchedEndpoint(matchB,
                new ChainEndpoint(currentA, ENDPOINT_SOLID_A));
        }
        else if ( matchA.chainIndex == matchB.chainIndex ) {
            removeOpenChain(matchA.chainIndex);
        }
        else {
            ChainEndpoint remainingA;
            ChainEndpoint remainingB;

            remainingA = matchA.otherEndpoint;
            remainingB = matchB.otherEndpoint;
            firstRemove = Math.max(matchA.chainIndex, matchB.chainIndex);
            secondRemove = Math.min(matchA.chainIndex, matchB.chainIndex);
            removeOpenChain(firstRemove);
            removeOpenChain(secondRemove);
            openChains.add(new OpenChain(remainingA, remainingB));
        }

        if ( matchA != null &&
             !isFlexibleSkipCutsEnabled() &&
             !isFlexibleLooseA(matchA.endpoint.halfEdge.mirrorHalfEdge()) ) {
            _PolyhedralBoundedSolidFace addedA = null;
            _PolyhedralBoundedSolidFace addedB = null;
            addedA = cutOrDeferFlexibleA(matchA.endpoint.halfEdge);
            if ( matchB != null &&
                 !isFlexibleLooseB(matchB.endpoint.halfEdge.mirrorHalfEdge()) ) {
                addedB = cutOrDeferFlexibleB(matchB.endpoint.halfEdge);
            }
            keepOnlyPairedFlexibleCutFaces(addedA, addedB, "point");
        }
        else if ( matchB != null &&
             !isFlexibleSkipCutsEnabled() &&
             !isFlexibleLooseB(matchB.endpoint.halfEdge.mirrorHalfEdge()) ) {
            _PolyhedralBoundedSolidFace addedB;
            addedB = cutOrDeferFlexibleB(matchB.endpoint.halfEdge);
            keepOnlyPairedFlexibleCutFaces(null, addedB, "point");
        }
        if ( isFlexibleSkipCutsEnabled() && (matchA != null || matchB != null) ) {
        }
        return result;
    }

    private static void setOpConnectWithFlexibleChains()
    {
        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 3. ------------------------------------------------------------------------------------------------------------------------------------------------------");
        }

        sortNullEdges();

        int i;

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            System.out.println("SORTED SET OF " + sonea.size() + " NULL EDGES PAIRS TO BE CONNECTED");
        }
        tracePipelineSummary(
            "connect start pairsA=" + sonea.size() +
            " pairsB=" + soneb.size() + " mode=flexibleEndpointChains");
        for ( i = 0; i < sonea.size() && i < soneb.size(); i++ ) {
        }

        _PolyhedralBoundedSolidEdge nextedgea;
        _PolyhedralBoundedSolidEdge nextedgeb;
        _PolyhedralBoundedSolidHalfEdge h1a = null;
        _PolyhedralBoundedSolidHalfEdge h2a = null;
        _PolyhedralBoundedSolidHalfEdge h1b = null;
        _PolyhedralBoundedSolidHalfEdge h2b = null;
        PointJoinResult pointResult;
        boolean allowRingMoveOnAJoin = (operation == INTERSECTION) ||
            isForceARingMoveEnabled();
        boolean allowRingMoveOnBJoin = (operation != SUBTRACT) ||
            !isFlexibleDisableBRingMoveForSubtractEnabled();
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        openChains = new ArrayList<OpenChain>();

        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfaPairIndexByFaceId = new HashMap<Integer, Integer>();
        sonfbPairIndexByFaceId = new HashMap<Integer, Integer>();
        setCurrentConnectContext(-1);

        if ( sonea.size() != soneb.size() ) {
            PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
            System.out.println("**** Not paired null edges!");
        }

        for ( i = 0; i < sonea.size() && i < soneb.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge ha;
            _PolyhedralBoundedSolidHalfEdge ham;
            _PolyhedralBoundedSolidHalfEdge hb;
            _PolyhedralBoundedSolidHalfEdge hbm;
            _PolyhedralBoundedSolidHalfEdge tmp;

            ha = sonea.get(i).e.rightHalf;
            ham = sonea.get(i).e.leftHalf;
            hb = soneb.get(i).e.rightHalf;
            hbm = soneb.get(i).e.leftHalf;
            setCurrentConnectContext(i);
            tracePipelineSummary(
                "connect pair[" + i + "] A{" + summarizeNullEdge(sonea.get(i)) +
                "} B{" + summarizeNullEdge(soneb.get(i)) + "}");

            nextedgea = sonea.get(i).e;
            nextedgeb = soneb.get(i).e;
            h1a = null;
            h2a = null;
            h1b = null;
            h2b = null;
            if ( ha.startingVertex.id > ham.startingVertex.id ) {
                tmp = nextedgea.rightHalf;
                nextedgea.rightHalf = nextedgea.leftHalf;
                nextedgea.leftHalf = tmp;
            }
            if ( hb.startingVertex.id > hbm.startingVertex.id ) {
                tmp = nextedgeb.rightHalf;
                nextedgeb.rightHalf = nextedgeb.leftHalf;
                nextedgeb.leftHalf = tmp;
            }

            pointResult = processPointWithFlexibleChains(
                nextedgea.rightHalf, nextedgeb.leftHalf, withDebug,
                allowRingMoveOnAJoin, allowRingMoveOnBJoin);
            h1a = pointResult.matchedA;
            h2b = pointResult.matchedB;

            pointResult = processPointWithFlexibleChains(
                nextedgea.leftHalf, nextedgeb.rightHalf, withDebug,
                allowRingMoveOnAJoin, allowRingMoveOnBJoin);
            h2a = pointResult.matchedA;
            h1b = pointResult.matchedB;

            if ( h1a != null && h1b != null && h2a != null && h2b != null ) {
                if ( isFlexibleSkipLegacyPairFinalCutsEnabled() ) {
                }
                else {
                    _PolyhedralBoundedSolidFace addedA;
                    _PolyhedralBoundedSolidFace addedB;
                    addedA = cutOrDeferFlexibleA(nextedgea.rightHalf);
                    addedB = cutOrDeferFlexibleB(nextedgeb.rightHalf);
                    keepOnlyPairedFlexibleCutFaces(addedA, addedB,
                        "pair[" + i + "]");
                }
                tracePipelineSummary(
                    "connect pair[" + i + "] produced cuts h1a=" +
                    summarizeHalfEdge(h1a) + " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) + " h2b=" +
                    summarizeHalfEdge(h2b));
            }
            else {
                PolyhedralBoundedSolidStatistics.recordJoinIncompleteCase();
                PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
                tracePipelineSummary(
                    "connect pair[" + i + "] incomplete h1a=" +
                    summarizeHalfEdge(h1a) + " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) + " h2b=" +
                    summarizeHalfEdge(h2b) + " looseA=" +
                    countOpenEndpoints(ENDPOINT_SOLID_A) + " looseB=" +
                    countOpenEndpoints(ENDPOINT_SOLID_B));
            }
        }

        tracePipelineSummary(
            "connect end sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size() +
            " looseA=" + countOpenEndpoints(ENDPOINT_SOLID_A) +
            " looseB=" + countOpenEndpoints(ENDPOINT_SOLID_B));
        while ( closeFlexibleChainsByCoincidentEndpoints() ) {
            tracePipelineSummary(
                "connect coincident-chain pass sonfa=" + sonfa.size() +
                " sonfb=" + sonfb.size() +
                " looseA=" + countOpenEndpoints(ENDPOINT_SOLID_A) +
                " looseB=" + countOpenEndpoints(ENDPOINT_SOLID_B));
        }
        flushDeferredFlexibleCuts();
        tracePipelineSummary(
            "connect post-pass sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size() +
            " looseA=" + countOpenEndpoints(ENDPOINT_SOLID_A) +
            " looseB=" + countOpenEndpoints(ENDPOINT_SOLID_B));
        updateLastSnapshotFromOpenChains();

        for ( i = 0; i < openChains.size(); i++ ) {
            tracePipelineSummary("connect chain[" + i + "] " +
                summarizeChainEndpoint(openChains.get(i).first) + " " +
                summarizeChainEndpoint(openChains.get(i).second));
        }
    }

    /**
    Neighbor null edges connector for the set operations algorithm
    (big phase 3).
    Following section [MANT1988].15.7. and program [MANT1988].15.14.
    */
    private static void setOpConnect()
    {
        if ( (debugFlags & DEBUG_01_STRUCTURE) != 0x00 ) {
            System.out.println("- 3. ------------------------------------------------------------------------------------------------------------------------------------------------------");
        }

        sortNullEdges();

        int i;

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            System.out.println("SORTED SET OF " + sonea.size() + " NULL EDGES PAIRS TO BE CONNECTED");
        }
        tracePipelineSummary(
            "connect start pairsA=" + sonea.size() +
            " pairsB=" + soneb.size());
        for ( i = 0; i < sonea.size() && i < soneb.size(); i++ ) {
        }

        _PolyhedralBoundedSolidEdge nextedgea;
        _PolyhedralBoundedSolidEdge nextedgeb;
        _PolyhedralBoundedSolidHalfEdge h1a = null;
        _PolyhedralBoundedSolidHalfEdge h2a = null;
        _PolyhedralBoundedSolidHalfEdge h1b = null;
        _PolyhedralBoundedSolidHalfEdge h2b = null;
        _PolyhedralBoundedSolidHalfEdge r[];
        boolean allowRingMoveOnAJoin = (operation == INTERSECTION) ||
            isForceARingMoveEnabled();
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfaPairIndexByFaceId = new HashMap<Integer, Integer>();
        sonfbPairIndexByFaceId = new HashMap<Integer, Integer>();
        setCurrentConnectContext(-1);
        int j;

        if ( sonea.size() != soneb.size() ) {
            PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
            System.out.println("**** Not paired null edges!");
        }

        for ( i = 0; i < sonea.size() && i < soneb.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge ha;
            _PolyhedralBoundedSolidHalfEdge ham;
            _PolyhedralBoundedSolidHalfEdge hb;
            _PolyhedralBoundedSolidHalfEdge hbm;
            _PolyhedralBoundedSolidHalfEdge tmp;

            ha = sonea.get(i).e.rightHalf;
            ham = sonea.get(i).e.leftHalf;
            hb = soneb.get(i).e.rightHalf;
            hbm = soneb.get(i).e.leftHalf;
            tracePipelineSummary(
                "connect pair[" + i + "] A{" + summarizeNullEdge(sonea.get(i)) +
                "} B{" + summarizeNullEdge(soneb.get(i)) + "}");

            if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
                System.out.println("  - " + (endsa.size()+endsb.size()) +
                    " = " + endsa.size() + "+" + endsb.size() +
                    " loose ends before processing pair [" + i + "]:");

                for ( j = 0; j < endsa.size(); j++ ) {
                    _PolyhedralBoundedSolidHalfEdge hat;
                    _PolyhedralBoundedSolidHalfEdge hbt;
                    hat = endsa.get(j);
                    hbt = endsb.get(j);
                    System.out.println("    . [" + j + "]: He(A): " +
                                       hat.startingVertex.id +
                                       "/" + hat.next().startingVertex.id +
                                       " | He(B): " + hbt.startingVertex.id +
                                       "/" + hbt.next().startingVertex.id);
                }

                if ( ha.startingVertex.id > ham.startingVertex.id ) {
                    System.out.println("********* FORCING ORDER!");
                }

                System.out.println("  - Processing pair [" + i + "]: "+
                    "He(A1): " + ha.startingVertex.id + "/" + ha.next().startingVertex.id +
                    " He(A2): " + ham.startingVertex.id + "/" + ham.next().startingVertex.id +
                    " He(B1): " + hb.startingVertex.id + "/" + hb.next().startingVertex.id +
                    " He(B2): " + hbm.startingVertex.id + "/" + hbm.next().startingVertex.id);
            }

            nextedgea = sonea.get(i).e;
            nextedgeb = soneb.get(i).e;
            h1a = null;
            h2a = null;
            h1b = null;
            h2b = null;
            if ( ha.startingVertex.id > ham.startingVertex.id ) {
                tmp = nextedgea.rightHalf;
                nextedgea.rightHalf = nextedgea.leftHalf;
                nextedgea.leftHalf = tmp;
            }
            if ( hb.startingVertex.id > hbm.startingVertex.id ) {
                tmp = nextedgeb.rightHalf;
                nextedgeb.rightHalf = nextedgeb.leftHalf;
                nextedgeb.leftHalf = tmp;
            }

            setCurrentConnectContext(i);
            r = canJoin(nextedgea.rightHalf, nextedgeb.leftHalf);
            if ( r != null ) {
                h1a = r[0];
                h2b = r[1];
                join(h1a, nextedgea.rightHalf, withDebug,
                    allowRingMoveOnAJoin);
                removeLooseEndsA(h1a);
                if ( !isLooseA(h1a.mirrorHalfEdge()) ) {
                    cutOrDeferClassicA(h1a);
                }
                join(h2b, nextedgeb.leftHalf, withDebug);
                removeLooseEndsB(h2b);
                if ( !isLooseB(h2b.mirrorHalfEdge()) ) {
                    cutOrDeferClassicB(h2b);
                }
            }

            setCurrentConnectContext(i);
            r = canJoin(nextedgea.leftHalf, nextedgeb.rightHalf);
            if ( r != null ) {
                h2a = r[0];
                h1b = r[1];
                join(h2a, nextedgea.leftHalf, withDebug,
                    allowRingMoveOnAJoin);
                removeLooseEndsA(h2a);
                if ( !isLooseA(h2a.mirrorHalfEdge()) ) {
                    cutOrDeferClassicA(h2a);
                }
                join(h1b, nextedgeb.rightHalf, withDebug);
                removeLooseEndsB(h1b);
                if ( !isLooseB(h1b.mirrorHalfEdge()) ) {
                    cutOrDeferClassicB(h1b);
                }
            }

            if ( h1a != null && h1b != null && h2a != null && h2b != null ) {
                cutOrDeferClassicA(nextedgea.rightHalf);
                cutOrDeferClassicB(nextedgeb.rightHalf);
                tracePipelineSummary(
                    "connect pair[" + i + "] produced cuts h1a=" +
                    summarizeHalfEdge(h1a) + " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) + " h2b=" +
                    summarizeHalfEdge(h2b));
            }
            else {
                PolyhedralBoundedSolidStatistics.recordJoinIncompleteCase();
                PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
                tracePipelineSummary(
                    "connect pair[" + i + "] incomplete h1a=" +
                    summarizeHalfEdge(h1a) + " h2a=" + summarizeHalfEdge(h2a) +
                    " h1b=" + summarizeHalfEdge(h1b) + " h2b=" +
                    summarizeHalfEdge(h2b) + " looseA=" + endsa.size() +
                    " looseB=" + endsb.size());
            }
        }

        if ( (debugFlags & DEBUG_05_CONNECT) != 0x00 ) {
            System.out.println("  . Pending null edges to connect:");
            for ( i = 0; i < endsa.size(); i++ ) {
                System.out.println("    . A[" + (i+1) + "]: " + endsa.get(i));
            }
            for ( i = 0; i < endsb.size(); i++ ) {
                System.out.println("    . B[" + (i+1) + "]: " + endsb.get(i));
            }
        }
        tracePipelineSummary(
            "connect end sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size() +
            " looseA=" + endsa.size() +
            " looseB=" + endsb.size());
        while ( closeLegacyCoincidentLooseEnds() ) {
            tracePipelineSummary(
                "connect coincident-loose pass sonfa=" + sonfa.size() +
                " sonfb=" + sonfb.size() +
                " looseA=" + endsa.size() +
                " looseB=" + endsb.size());
        }
        flushDeferredClassicCuts();
        tracePipelineSummary(
            "connect post-pass sonfa=" + sonfa.size() +
            " sonfb=" + sonfb.size() +
            " looseA=" + endsa.size() +
            " looseB=" + endsb.size());
        updateLastSnapshot();

        for ( i = 0; i < endsa.size() && i < endsb.size(); i++ ) {
            tracePipelineSummary(
                "connect loose[" + i + "] A=" + summarizeHalfEdge(endsa.get(i)) +
                " B=" + summarizeHalfEdge(endsb.get(i)));
        }
    }
}
