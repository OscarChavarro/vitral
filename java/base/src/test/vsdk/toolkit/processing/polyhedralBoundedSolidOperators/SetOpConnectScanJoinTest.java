package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;

/**
Unit-level cobertura for Connect-phase primitives introduced by §6.1.1
and §6.1.2 of plan-csg-boolean-fix-stage2, mapped to
[MANT1988] §15.7 Programs 15.13 (scanjoin) and 15.14 (sgetnextnulledge).

<p>This class complements {@link SetOpConnectNoLooseInvariantTest}, which
audits the external invariant of Program 15.14 (looseA == looseB == 0).
Here we audit the <em>API surface</em> of the primitives themselves:
their existence, signatures, and structural contract — so a future
refactor that accidentally re-introduces the heuristics deleted in
§6.1-A / §6.1-B (flexibleChains, deferrals, post-loop safety nets) is
caught at compile or test time instead of slowly degrading the suite.</p>

<p>Hard contracts asserted:
<ul>
<li>{@code scanjoin(HalfEdge, HalfEdge)} exists and is private static
    on {@link _PolyhedralBoundedSolidSetNullEdgesConnector}, with return
    type {@code _PolyhedralBoundedSolidHalfEdge[]} per Program 15.13.</li>
<li>{@code sgetnextnulledge(NullEdgePair)} exists, returns {@code boolean},
    and uses the cursor field {@code nextNullEdgeIndex} per Program 15.14.</li>
<li>The cursor protocol is correct: starting from index 0 it must
    consume exactly N pairs and then return {@code false}.</li>
<li>The deleted heuristics from §6.1-A and §6.1-B do not resurrect:
    no method named like the removed ones may reappear on the class.</li>
</ul>
</p>
 */
class SetOpConnectScanJoinTest
{
    private static Method scanjoinMethod;
    private static Method sgetnextnulledgeMethod;
    private static Field nextNullEdgeIndexField;
    private static Field soneaField;
    private static Field sonebField;
    private static Class<?> nullEdgePairClass;
    private static Field neaField;
    private static Field nebField;
    private static Field pairIndexField;

    @BeforeAll
    static void resolveReflectionHandles() throws Exception
    {
        Class<?> connector =
            _PolyhedralBoundedSolidSetNullEdgesConnector.class;
        Class<?> halfEdge =
            vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
                .nodes._PolyhedralBoundedSolidHalfEdge.class;

        scanjoinMethod = connector.getDeclaredMethod(
            "scanjoin", halfEdge, halfEdge);
        scanjoinMethod.setAccessible(true);

        nullEdgePairClass = Class.forName(
            connector.getName() + "$NullEdgePair");
        sgetnextnulledgeMethod = connector.getDeclaredMethod(
            "sgetnextnulledge", nullEdgePairClass);
        sgetnextnulledgeMethod.setAccessible(true);

        nextNullEdgeIndexField = connector.getDeclaredField("nextNullEdgeIndex");
        nextNullEdgeIndexField.setAccessible(true);

        soneaField = connector.getDeclaredField("sonea");
        soneaField.setAccessible(true);
        sonebField = connector.getDeclaredField("soneb");
        sonebField.setAccessible(true);

        neaField = nullEdgePairClass.getDeclaredField("nea");
        neaField.setAccessible(true);
        nebField = nullEdgePairClass.getDeclaredField("neb");
        nebField.setAccessible(true);
        pairIndexField = nullEdgePairClass.getDeclaredField("pairIndex");
        pairIndexField.setAccessible(true);
    }

    /**
    Contract: scanjoin must exist as a private static method with the
    exact name (NOT canJoin or any other alias), returning the half-edge
    array per Program 15.13.
     */
    @Test
    void given_connector_when_inspectingApi_then_scanjoinPrimitiveExists()
    {
        assertThat(Modifier.isPrivate(scanjoinMethod.getModifiers()))
            .as("scanjoin must be private")
            .isTrue();
        assertThat(Modifier.isStatic(scanjoinMethod.getModifiers()))
            .as("scanjoin must be static")
            .isTrue();
        assertThat(scanjoinMethod.getReturnType().getSimpleName())
            .isEqualTo("_PolyhedralBoundedSolidHalfEdge[]");
    }

    /**
    Contract: sgetnextnulledge must exist as a private static method
    that takes a NullEdgePair out-param and returns boolean, mirroring
    Program 15.14.
     */
    @Test
    void given_connector_when_inspectingApi_then_sgetnextnulledgePrimitiveExists()
    {
        assertThat(Modifier.isPrivate(sgetnextnulledgeMethod.getModifiers()))
            .as("sgetnextnulledge must be private")
            .isTrue();
        assertThat(Modifier.isStatic(sgetnextnulledgeMethod.getModifiers()))
            .as("sgetnextnulledge must be static")
            .isTrue();
        assertThat(sgetnextnulledgeMethod.getReturnType())
            .isEqualTo(boolean.class);
        assertThat(nullEdgePairClass.getDeclaredFields()).hasSize(3);
    }

    /**
    Cursor protocol: given N entries in sonea/soneb, sgetnextnulledge
    must consume exactly N times (returning true) and then return false
    on the N+1-th call, leaving the cursor parked.
     */
    @Test
    void given_threePairsInSone_when_sgetnextnulledgeIsCalledRepeatedly_then_yieldsExactlyThreeThenStops()
        throws Exception
    {
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedA =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedB =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        seedA.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedA.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedA.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedB.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedB.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedB.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));

        Object savedSonea = soneaField.get(null);
        Object savedSoneb = sonebField.get(null);
        int savedCursor = nextNullEdgeIndexField.getInt(null);
        try {
            soneaField.set(null, seedA);
            sonebField.set(null, seedB);
            nextNullEdgeIndexField.setInt(null, 0);

            java.lang.reflect.Constructor<?> ctor =
                nullEdgePairClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object pair = ctor.newInstance();
            int hits = 0;
            int lastIndex = -1;
            while ( ((Boolean)sgetnextnulledgeMethod.invoke(null, pair))
                    .booleanValue() ) {
                hits++;
                lastIndex = pairIndexField.getInt(pair);
                assertThat(neaField.get(pair)).isSameAs(seedA.get(lastIndex));
                assertThat(nebField.get(pair)).isSameAs(seedB.get(lastIndex));
            }
            assertThat(hits).isEqualTo(3);
            assertThat(lastIndex).isEqualTo(2);
            assertThat(((Boolean)sgetnextnulledgeMethod.invoke(null, pair))
                .booleanValue()).isFalse();
        } finally {
            soneaField.set(null, savedSonea);
            sonebField.set(null, savedSoneb);
            nextNullEdgeIndexField.setInt(null, savedCursor);
        }
    }

    /**
    Cursor protocol: when sonea/soneb have different sizes, the iterator
    must stop at min(sonea.size, soneb.size). Program 15.14 assumes the
    pairing is balanced; if it isn't, no spurious extra iterations.
     */
    @Test
    void given_unbalancedSonea_when_sgetnextnulledge_then_stopsAtShorterList()
        throws Exception
    {
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedA =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedB =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        seedA.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedA.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedA.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        seedB.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));

        Object savedSonea = soneaField.get(null);
        Object savedSoneb = sonebField.get(null);
        int savedCursor = nextNullEdgeIndexField.getInt(null);
        try {
            soneaField.set(null, seedA);
            sonebField.set(null, seedB);
            nextNullEdgeIndexField.setInt(null, 0);

            java.lang.reflect.Constructor<?> ctor =
                nullEdgePairClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object pair = ctor.newInstance();
            int hits = 0;
            while ( ((Boolean)sgetnextnulledgeMethod.invoke(null, pair))
                    .booleanValue() ) {
                hits++;
            }
            assertThat(hits).isEqualTo(1);
        } finally {
            soneaField.set(null, savedSonea);
            sonebField.set(null, savedSoneb);
            nextNullEdgeIndexField.setInt(null, savedCursor);
        }
    }

    /**
    Cursor protocol: cursor reset before each setOpConnect run means
    consecutive boolean operations must be independent (no state leak
    between calls).
     */
    @Test
    void given_twoBooleanOperationsBackToBack_when_running_then_eachStartsWithFreshCursor()
        throws Exception
    {
        PolyhedralBoundedSolid[] firstPair =
            CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.STACKED_BLOCKS);
        PolyhedralBoundedSolidModeler.setOp(firstPair[0], firstPair[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false);
        int cursorAfterFirst = nextNullEdgeIndexField.getInt(null);

        PolyhedralBoundedSolid[] secondPair =
            CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.STACKED_BLOCKS);
        PolyhedralBoundedSolidModeler.setOp(secondPair[0], secondPair[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false);
        int cursorAfterSecond = nextNullEdgeIndexField.getInt(null);

        // The cursor must reflect the size of the second run's null-edge
        // set, not be additive over both runs. (For two overlapping boxes
        // the same operand pair is reused, so the count is reproducible.)
        assertThat(cursorAfterSecond).isEqualTo(cursorAfterFirst);
    }

    /**
    §6.1 regression guard: ensure deleted helpers and flags from the
    flexibleChains / deferral / safety-net families do NOT resurface
    on the connector class. Each name here was eliminated in a specific
    sub-hito; reappearing is a code-review failure caught here.
     */
    @Test
    void given_connector_when_inspectingApi_then_deletedHelpersDoNotResurface()
    {
        String[] forbidden = new String[] {
            // §6.1-A: dual flexible path
            "setOpConnectWithFlexibleChains",
            "processPointWithFlexibleChains",
            "closeFlexibleChainsByCoincidentEndpoints",
            "cutOrDeferFlexibleA",
            "cutOrDeferFlexibleB",
            "flushDeferredFlexibleCuts",
            "keepOnlyPairedFlexibleCutFaces",
            "findEndpointMatch",
            "replaceMatchedEndpoint",
            "removeOpenChain",
            "isFlexibleLooseA",
            "isFlexibleLooseB",
            "isFlexibleEndpointChainsEnabled",
            "isFlexibleSkipCutsEnabled",
            "isFlexibleSamePointSelfClosureEnabled",
            "isFlexibleSkipLegacyPairFinalCutsEnabled",
            "isFlexibleKeepOnlyPairedCutFacesEnabled",
            "isFlexibleAllowCrossChainMergeEnabled",
            "isFlexibleRejectOneSidedMatchesEnabled",
            // §6.1-B: post-loop safety nets
            "closeLegacyCoincidentLooseEnds",
            "resolveClassicAlternatingLooseCycle",
            "resolveClassicLooseNetwork",
            "findTwoDisjointNeighborPairs",
            "hasCoincidentLooseEndpoint",
            "findMinimumLooseMatching",
            "findLooseNetworkCycles",
            "loosePairWeight",
            "cutLiveA",
            "cutLiveB",
            "cutLiveLoosePairs",
            // §6.1-B.4: deferrals
            "cutOrDeferClassicA",
            "cutOrDeferClassicB",
            "flushDeferredClassicCuts",
            "flushDeferredCuts",
            "rememberDeferredCut",
            "shouldDeferClassicCutA",
            "shouldDeferClassicCutB",
            "shouldDeferFlexibleCutA",
            "shouldDeferFlexibleCutB",
            // §6.1-B.5: removeLooseEnds extras
            "removeLooseEndsA",
            "removeLooseEndsB",
            // §6.1.2: scanjoin replaces canJoin
            "canJoin",
            // §6.2.2: forceARingMove flag/getter
            "isForceARingMoveEnabled",
            "isFlexibleDisableBRingMoveForSubtractEnabled",
            // §6.1.2: cross-loose match heuristic
            "isCrossLooseMatchEnabled"
        };
        ArrayList<String> survivors = new ArrayList<String>();
        for ( Method m :
              _PolyhedralBoundedSolidSetNullEdgesConnector.class
                .getDeclaredMethods() ) {
            for ( String banned : forbidden ) {
                if ( m.getName().equals(banned) ) {
                    survivors.add(banned);
                }
            }
        }
        assertThat(survivors)
            .as("Deleted Connect helpers must not resurface")
            .isEmpty();
    }

    /**
    §6.2 regression guard: the retry-with-forceARingMove path was
    deleted from {@link PolyhedralBoundedSolidSetOperator}; no helper
    name from that family may reappear.
     */
    @Test
    void given_setOperator_when_inspectingApi_then_subtractRecoveryRetryDoesNotResurface()
    {
        String[] forbidden = new String[] {
            "trySubtractConnectRecovery",
            "shouldAttemptSubtractConnectRecovery",
            "restoreSystemProperty"
        };
        ArrayList<String> survivors = new ArrayList<String>();
        for ( Method m :
              PolyhedralBoundedSolidSetOperator.class.getDeclaredMethods() ) {
            for ( String banned : forbidden ) {
                if ( m.getName().equals(banned) ) {
                    survivors.add(banned);
                }
            }
        }
        assertThat(survivors)
            .as("§6.2 retry helpers must not resurface")
            .isEmpty();
    }
}
