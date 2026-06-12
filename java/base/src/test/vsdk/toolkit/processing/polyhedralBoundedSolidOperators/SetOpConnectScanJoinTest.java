package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

<p>Stage 7 R5d made the connect-phase state per-call: {@code sonea},
{@code soneb} and the cursor {@code nextNullEdgeIndex} are instance fields and
{@code scanjoin} / {@code sgetnextnulledge} are instance methods, so the
operator is re-entrant. The contracts below therefore assert the primitives
are private and <em>non-static</em>, and drive them against a real connector
instance.</p>

<p>Hard contracts asserted:
<ul>
<li>{@code scanjoin(HalfEdge, HalfEdge)} exists and is private (instance)
    on {@link _PolyhedralBoundedSolidSetNullEdgesConnector}, with return
    type {@code _PolyhedralBoundedSolidHalfEdge[]} per Program 15.13.</li>
<li>{@code sgetnextnulledge(NullEdgePair)} exists, returns {@code boolean},
    and uses the cursor field {@code nextNullEdgeIndex} per Program 15.14.</li>
<li>The cursor protocol is correct: starting from index 0 it must
    consume exactly N pairs and then return {@code false}.</li>
<li>The per-call cursor is independent across connector instances: a fresh
    instance starts parked at 0, so consecutive boolean operations cannot
    leak cursor state.</li>
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

    private static _PolyhedralBoundedSolidSetNullEdgesConnector newConnector()
    {
        return new _PolyhedralBoundedSolidSetNullEdgesConnector();
    }

    private static Object newNullEdgePair() throws Exception
    {
        java.lang.reflect.Constructor<?> ctor =
            nullEdgePairClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private static ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedList(
        int count)
    {
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seed =
            new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        for ( int i = 0; i < count; i++ ) {
            seed.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null));
        }
        return seed;
    }

    /**
    Contract: scanjoin must exist as a private (non-static) method with the
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
            .as("scanjoin must be a per-call instance method (Stage 7 R5d)")
            .isFalse();
        assertThat(scanjoinMethod.getReturnType().getSimpleName())
            .isEqualTo("_PolyhedralBoundedSolidHalfEdge[]");
    }

    /**
    Contract: sgetnextnulledge must exist as a private (non-static) method
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
            .as("sgetnextnulledge must be a per-call instance method (Stage 7 R5d)")
            .isFalse();
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
        _PolyhedralBoundedSolidSetNullEdgesConnector connector = newConnector();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedA = seedList(3);
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedB = seedList(3);

        soneaField.set(connector, seedA);
        sonebField.set(connector, seedB);
        nextNullEdgeIndexField.setInt(connector, 0);

        Object pair = newNullEdgePair();
        int hits = 0;
        int lastIndex = -1;
        while ( ((Boolean)sgetnextnulledgeMethod.invoke(connector, pair))
                .booleanValue() ) {
            hits++;
            lastIndex = pairIndexField.getInt(pair);
            assertThat(neaField.get(pair)).isSameAs(seedA.get(lastIndex));
            assertThat(nebField.get(pair)).isSameAs(seedB.get(lastIndex));
        }
        assertThat(hits).isEqualTo(3);
        assertThat(lastIndex).isEqualTo(2);
        assertThat(((Boolean)sgetnextnulledgeMethod.invoke(connector, pair))
            .booleanValue()).isFalse();
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
        _PolyhedralBoundedSolidSetNullEdgesConnector connector = newConnector();
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedA = seedList(3);
        ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> seedB = seedList(1);

        soneaField.set(connector, seedA);
        sonebField.set(connector, seedB);
        nextNullEdgeIndexField.setInt(connector, 0);

        Object pair = newNullEdgePair();
        int hits = 0;
        while ( ((Boolean)sgetnextnulledgeMethod.invoke(connector, pair))
                .booleanValue() ) {
            hits++;
        }
        assertThat(hits).isEqualTo(1);
    }

    /**
    Cursor independence (per-call state, Stage 7 R5d): each connector instance
    owns its cursor, so a fresh instance starts parked at 0 regardless of any
    other instance's progress. This is the re-entrant replacement for the old
    "reset before each setOpConnect" contract: consecutive boolean operations
    can no longer leak cursor state because they no longer share one.
     */
    @Test
    void given_twoConnectorInstances_when_eachRunsCursor_then_cursorsAreIndependent()
        throws Exception
    {
        _PolyhedralBoundedSolidSetNullEdgesConnector first = newConnector();
        soneaField.set(first, seedList(3));
        sonebField.set(first, seedList(3));
        nextNullEdgeIndexField.setInt(first, 0);

        Object pairFirst = newNullEdgePair();
        int hitsFirst = 0;
        while ( ((Boolean)sgetnextnulledgeMethod.invoke(first, pairFirst))
                .booleanValue() ) {
            hitsFirst++;
        }
        assertThat(hitsFirst).isEqualTo(3);
        assertThat(nextNullEdgeIndexField.getInt(first))
            .as("first connector's cursor is exhausted")
            .isEqualTo(3);

        // A second, independent connector must start with a fresh cursor and
        // run its own pairing without any leakage from the first.
        _PolyhedralBoundedSolidSetNullEdgesConnector second = newConnector();
        assertThat(nextNullEdgeIndexField.getInt(second))
            .as("a fresh connector instance starts with a zero cursor")
            .isEqualTo(0);
        soneaField.set(second, seedList(2));
        sonebField.set(second, seedList(2));

        Object pairSecond = newNullEdgePair();
        int hitsSecond = 0;
        while ( ((Boolean)sgetnextnulledgeMethod.invoke(second, pairSecond))
                .booleanValue() ) {
            hitsSecond++;
        }
        assertThat(hitsSecond)
            .as("second connector yields exactly its own pair count")
            .isEqualTo(2);
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
