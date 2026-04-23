//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.Collections;

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

    private static int debugFlags;
    private static int operation;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;
    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsa;
    private static ArrayList<_PolyhedralBoundedSolidHalfEdge> endsb;
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfa;
    private static ArrayList<_PolyhedralBoundedSolidFace> sonfb;
    private static ArrayList<OpenChain> openChains;
    private static int lastLooseACount;
    private static int lastLooseBCount;
    private static int lastSonfaCount;
    private static int lastSonfbCount;
    private static int lastPairCount;

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
        return Boolean.getBoolean(KEEP_INSERTION_ORDER_PROPERTY);
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
        return "he(v=" + from + "->" + to + ",f=" + face + ")";
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

    private static String summarizeLoop(_PolyhedralBoundedSolidLoop loop)
    {
        if ( loop == null ) {
            return "null";
        }

        StringBuilder out = new StringBuilder();
        int i;

        out.append("size=").append(loop.halfEdgesList.size()).append(" path=");
        for ( i = 0; i < loop.halfEdgesList.size(); i++ ) {
            if ( i > 0 ) {
                out.append(" -> ");
            }
            out.append(loop.halfEdgesList.get(i).startingVertex.position);
        }
        return out.toString();
    }

    private static String summarizeFace(_PolyhedralBoundedSolidFace face)
    {
        if ( face == null ) {
            return "null";
        }

        StringBuilder out = new StringBuilder();
        int i;

        out.append("face=").append(face.id)
           .append(" loops=").append(face.boundariesList.size());
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            out.append(" [").append(i).append("] ")
               .append(summarizeLoop(face.boundariesList.get(i)));
        }
        return out.toString();
    }

    private static void traceFaceState(String prefix,
        _PolyhedralBoundedSolidFace face)
    {
        tracePipelineSummary(prefix + " " + summarizeFace(face));
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
        tracePipelineSummary("cutA start " +
            summarizeHalfEdge(he) + " mirror=" +
            summarizeHalfEdge(he.mirrorHalfEdge()));

        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            addedFace = he.parentLoop.parentFace;
            sonfa.add(addedFace);
            traceFaceState("cutA add-sonfa", addedFace);
            s.lkemr(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        else {
            s.lkef(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        tracePipelineSummary("cutA end solidFaces=" + s.polygonsList.size() +
            " solidEdges=" + s.edgesList.size() +
            " solidVertices=" + s.verticesList.size());
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
        tracePipelineSummary("cutB start " +
            summarizeHalfEdge(he) + " mirror=" +
            summarizeHalfEdge(he.mirrorHalfEdge()));

        if ( he.parentEdge.rightHalf.parentLoop ==
             he.parentEdge.leftHalf.parentLoop ) {
            addedFace = he.parentLoop.parentFace;
            sonfb.add(addedFace);
            traceFaceState("cutB add-sonfb", addedFace);
            s.lkemr(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        else {
            s.lkef(he.parentEdge.rightHalf, he.parentEdge.leftHalf);
        }
        tracePipelineSummary("cutB end solidFaces=" + s.polygonsList.size() +
            " solidEdges=" + s.edgesList.size() +
            " solidVertices=" + s.verticesList.size());
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
            addedA = cutA(matchA.endpoint.halfEdge);
            if ( matchB != null &&
                 !isFlexibleLooseB(matchB.endpoint.halfEdge.mirrorHalfEdge()) ) {
                addedB = cutB(matchB.endpoint.halfEdge);
            }
            keepOnlyPairedFlexibleCutFaces(addedA, addedB, "point");
        }
        else if ( matchB != null &&
             !isFlexibleSkipCutsEnabled() &&
             !isFlexibleLooseB(matchB.endpoint.halfEdge.mirrorHalfEdge()) ) {
            _PolyhedralBoundedSolidFace addedB;
            addedB = cutB(matchB.endpoint.halfEdge);
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

        _PolyhedralBoundedSolidEdge nextedgea, nextedgeb;
        _PolyhedralBoundedSolidHalfEdge h1a = null, h2a = null, h1b = null, h2b = null;
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

        if ( sonea.size() != soneb.size() ) {
            PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
            System.out.println("**** Not paired null edges!");
        }

        for ( i = 0; i < sonea.size() && i < soneb.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge ha, ham;
            _PolyhedralBoundedSolidHalfEdge hb, hbm;
            _PolyhedralBoundedSolidHalfEdge tmp;

            ha = sonea.get(i).e.rightHalf;
            ham = sonea.get(i).e.leftHalf;
            hb = soneb.get(i).e.rightHalf;
            hbm = soneb.get(i).e.leftHalf;
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
                if ( hb.startingVertex.id > hbm.startingVertex.id ) {
                    tmp = nextedgeb.rightHalf;
                    nextedgeb.rightHalf = nextedgeb.leftHalf;
                    nextedgeb.leftHalf = tmp;
                }
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
                    addedA = cutA(nextedgea.rightHalf);
                    addedB = cutB(nextedgeb.rightHalf);
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
        updateLastSnapshotFromOpenChains();

        for ( i = 0; i < sonfa.size(); i++ ) {
            traceFaceState("connect sonfa[" + i + "]", sonfa.get(i));
        }
        for ( i = 0; i < sonfb.size(); i++ ) {
            traceFaceState("connect sonfb[" + i + "]", sonfb.get(i));
        }
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

        _PolyhedralBoundedSolidEdge nextedgea, nextedgeb;
        _PolyhedralBoundedSolidHalfEdge h1a = null, h2a = null, h1b = null, h2b = null;
        _PolyhedralBoundedSolidHalfEdge r[];
        boolean allowRingMoveOnAJoin = (operation == INTERSECTION) ||
            isForceARingMoveEnabled();
        boolean withDebug = ((debugFlags & DEBUG_99_SHOWOPERATIONS) != 0x0) &&
                            ((debugFlags & DEBUG_05_CONNECT) != 0x00);

        endsa = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        endsb = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        sonfa = new ArrayList<_PolyhedralBoundedSolidFace>();
        sonfb = new ArrayList<_PolyhedralBoundedSolidFace>();
        int j;

        if ( sonea.size() != soneb.size() ) {
            PolyhedralBoundedSolidStatistics.recordOperationFailureCase();
            System.out.println("**** Not paired null edges!");
        }

        for ( i = 0; i < sonea.size() && i < soneb.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge ha, ham;
            _PolyhedralBoundedSolidHalfEdge hb, hbm;
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
                    _PolyhedralBoundedSolidHalfEdge hat, hbt;
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
                if ( hb.startingVertex.id > hbm.startingVertex.id ) {
                    tmp = nextedgeb.rightHalf;
                    nextedgeb.rightHalf = nextedgeb.leftHalf;
                    nextedgeb.leftHalf = tmp;
                }
            }

            r = canJoin(nextedgea.rightHalf, nextedgeb.leftHalf);
            if ( r != null ) {
                h1a = r[0];
                h2b = r[1];
                join(h1a, nextedgea.rightHalf, withDebug,
                    allowRingMoveOnAJoin);
                removeLooseEndsA(h1a);
                if ( !isLooseA(h1a.mirrorHalfEdge()) ) {
                    cutA(h1a);
                }
                join(h2b, nextedgeb.leftHalf, withDebug);
                removeLooseEndsB(h2b);
                if ( !isLooseB(h2b.mirrorHalfEdge()) ) {
                    cutB(h2b);
                }
            }

            r = canJoin(nextedgea.leftHalf, nextedgeb.rightHalf);
            if ( r != null ) {
                h2a = r[0];
                h1b = r[1];
                join(h2a, nextedgea.leftHalf, withDebug,
                    allowRingMoveOnAJoin);
                removeLooseEndsA(h2a);
                if ( !isLooseA(h2a.mirrorHalfEdge()) ) {
                    cutA(h2a);
                }
                join(h1b, nextedgeb.rightHalf, withDebug);
                removeLooseEndsB(h1b);
                if ( !isLooseB(h1b.mirrorHalfEdge()) ) {
                    cutB(h1b);
                }
            }

            if ( h1a != null && h1b != null && h2a != null && h2b != null ) {
                cutA(nextedgea.rightHalf);
                cutB(nextedgeb.rightHalf);
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
        updateLastSnapshot();

        for ( i = 0; i < sonfa.size(); i++ ) {
            traceFaceState("connect sonfa[" + i + "]", sonfa.get(i));
        }
        for ( i = 0; i < sonfb.size(); i++ ) {
            traceFaceState("connect sonfb[" + i + "]", sonfb.get(i));
        }
        for ( i = 0; i < endsa.size() && i < endsb.size(); i++ ) {
            tracePipelineSummary(
                "connect loose[" + i + "] A=" + summarizeHalfEdge(endsa.get(i)) +
                " B=" + summarizeHalfEdge(endsb.get(i)));
        }
    }
}
