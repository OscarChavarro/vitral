import { beforeAll, describe, expect, it } from "vitest";

import { ArrayList } from "java/util/ArrayList.js";
import type { _PolyhedralBoundedSolidEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidSetOperator } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.js";
import { _PolyhedralBoundedSolidSetOperatorNullEdge } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetOperatorNullEdge.js";

/**
Unit-level cobertura for Connect-phase primitives introduced by §6.1.1
and §6.1.2 of plan-csg-boolean-fix-stage2, mapped to
[MANT1988] §15.7 Programs 15.13 (scanjoin) and 15.14 (sgetnextnulledge).

<p>This class complements `SetOpConnectNoLooseInvariantTest`, which
audits the external invariant of Program 15.14 (looseA == looseB == 0).
Here we audit the <em>API surface</em> of the primitives themselves:
their existence, signatures, and structural contract — so a future
refactor that accidentally re-introduces the heuristics deleted in
§6.1-A / §6.1-B (flexibleChains, deferrals, post-loop safety nets) is
caught at compile or test time instead of slowly degrading the suite.</p>

<p>Stage 7 R5d made the connect-phase state per-call: `sonea`,
`soneb` and the cursor `nextNullEdgeIndex` are instance fields and
`scanjoin` / `sgetnextnulledge` are instance methods, so the
operator is re-entrant. The contracts below therefore assert the primitives
are non-static (declared on the prototype) and drive them against a real
connector instance.</p>

<p>Runtime boundary: Java reads the primitives through reflection and asserts
`Modifier.isPrivate` plus the declared return type. TypeScript erases both at
run time (`private` is compile-time only and there is no reflective type
information), so those two assertions cannot be expressed; the remaining
contract — existence, instance (prototype) placement, cursor protocol and the
banned-name guards — is asserted exactly as in Java, and the private members
are reached through a structural cast instead of `setAccessible(true)`.</p>
 */
interface NullEdgePairLike {
    nea: _PolyhedralBoundedSolidSetOperatorNullEdge | null;
    neb: _PolyhedralBoundedSolidSetOperatorNullEdge | null;
    pairIndex: number;
}

interface ConnectorInternals {
    nextNullEdgeIndex: number;
    sonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null;
    soneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null;
    scanjoin: (hea: unknown, heb: unknown) => unknown;
    sgetnextnulledge: (out: NullEdgePairLike) => boolean;
}

describe("SetOpConnectScanJoinTest", () => {
    let connectorPrototype: Record<string, unknown>;

    beforeAll(() => {
        connectorPrototype = _PolyhedralBoundedSolidSetNullEdgesConnector.prototype as unknown as Record<
            string,
            unknown
        >;
    });

    function newConnector(): ConnectorInternals {
        return new _PolyhedralBoundedSolidSetNullEdgesConnector() as unknown as ConnectorInternals;
    }

    function newNullEdgePair(): NullEdgePairLike {
        return { nea: null, neb: null, pairIndex: 0 };
    }

    function seedList(count: number): ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> {
        const seed = new ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge>();
        for (let i = 0; i < count; i++) {
            seed.add(new _PolyhedralBoundedSolidSetOperatorNullEdge(null as unknown as _PolyhedralBoundedSolidEdge));
        }
        return seed;
    }

    /**
    Contract: scanjoin must exist as an instance method with the exact name
    (NOT canJoin or any other alias), per Program 15.13.
     */
    it("given_connector_when_inspectingApi_then_scanjoinPrimitiveExists", () => {
        expect(
            Object.prototype.hasOwnProperty.call(connectorPrototype, "scanjoin"),
            "scanjoin must be a per-call instance method (Stage 7 R5d)",
        ).toBe(true);
        expect(typeof connectorPrototype["scanjoin"]).toBe("function");
        expect(
            Object.prototype.hasOwnProperty.call(_PolyhedralBoundedSolidSetNullEdgesConnector, "scanjoin"),
            "scanjoin must not be static",
        ).toBe(false);
        expect((connectorPrototype["scanjoin"] as (...args: unknown[]) => unknown).length).toBe(2);
    });

    /**
    Contract: sgetnextnulledge must exist as an instance method that takes a
    NullEdgePair out-param and returns boolean, mirroring Program 15.14.
     */
    it("given_connector_when_inspectingApi_then_sgetnextnulledgePrimitiveExists", () => {
        expect(
            Object.prototype.hasOwnProperty.call(connectorPrototype, "sgetnextnulledge"),
            "sgetnextnulledge must be a per-call instance method (Stage 7 R5d)",
        ).toBe(true);
        expect(typeof connectorPrototype["sgetnextnulledge"]).toBe("function");
        expect(
            Object.prototype.hasOwnProperty.call(_PolyhedralBoundedSolidSetNullEdgesConnector, "sgetnextnulledge"),
            "sgetnextnulledge must not be static",
        ).toBe(false);

        const connector = newConnector();
        connector.sonea = seedList(1);
        connector.soneb = seedList(1);
        connector.nextNullEdgeIndex = 0;
        const pair = newNullEdgePair();
        expect(typeof connector.sgetnextnulledge(pair)).toBe("boolean");
        // NullEdgePair carries exactly the three Program 15.14 out-params.
        expect(Object.keys(pair)).toHaveLength(3);
    });

    /**
    Cursor protocol: given N entries in sonea/soneb, sgetnextnulledge
    must consume exactly N times (returning true) and then return false
    on the N+1-th call, leaving the cursor parked.
     */
    it("given_threePairsInSone_when_sgetnextnulledgeIsCalledRepeatedly_then_yieldsExactlyThreeThenStops", () => {
        const connector = newConnector();
        const seedA = seedList(3);
        const seedB = seedList(3);

        connector.sonea = seedA;
        connector.soneb = seedB;
        connector.nextNullEdgeIndex = 0;

        const pair = newNullEdgePair();
        let hits = 0;
        let lastIndex = -1;
        while (connector.sgetnextnulledge(pair)) {
            hits++;
            lastIndex = pair.pairIndex;
            expect(pair.nea).toBe(seedA.get(lastIndex));
            expect(pair.neb).toBe(seedB.get(lastIndex));
        }
        expect(hits).toBe(3);
        expect(lastIndex).toBe(2);
        expect(connector.sgetnextnulledge(pair)).toBe(false);
    });

    /**
    Cursor protocol: when sonea/soneb have different sizes, the iterator
    must stop at min(sonea.size, soneb.size). Program 15.14 assumes the
    pairing is balanced; if it isn't, no spurious extra iterations.
     */
    it("given_unbalancedSonea_when_sgetnextnulledge_then_stopsAtShorterList", () => {
        const connector = newConnector();
        const seedA = seedList(3);
        const seedB = seedList(1);

        connector.sonea = seedA;
        connector.soneb = seedB;
        connector.nextNullEdgeIndex = 0;

        const pair = newNullEdgePair();
        let hits = 0;
        while (connector.sgetnextnulledge(pair)) {
            hits++;
        }
        expect(hits).toBe(1);
    });

    /**
    Cursor independence (per-call state, Stage 7 R5d): each connector instance
    owns its cursor, so a fresh instance starts parked at 0 regardless of any
    other instance's progress. This is the re-entrant replacement for the old
    "reset before each setOpConnect" contract: consecutive boolean operations
    can no longer leak cursor state because they no longer share one.
     */
    it("given_twoConnectorInstances_when_eachRunsCursor_then_cursorsAreIndependent", () => {
        const first = newConnector();
        first.sonea = seedList(3);
        first.soneb = seedList(3);
        first.nextNullEdgeIndex = 0;

        const pairFirst = newNullEdgePair();
        let hitsFirst = 0;
        while (first.sgetnextnulledge(pairFirst)) {
            hitsFirst++;
        }
        expect(hitsFirst).toBe(3);
        expect(first.nextNullEdgeIndex, "first connector's cursor is exhausted").toBe(3);

        // A second, independent connector must start with a fresh cursor and
        // run its own pairing without any leakage from the first.
        const second = newConnector();
        expect(second.nextNullEdgeIndex, "a fresh connector instance starts with a zero cursor").toBe(0);
        second.sonea = seedList(2);
        second.soneb = seedList(2);

        const pairSecond = newNullEdgePair();
        let hitsSecond = 0;
        while (second.sgetnextnulledge(pairSecond)) {
            hitsSecond++;
        }
        expect(hitsSecond, "second connector yields exactly its own pair count").toBe(2);
    });

    /**
    §6.1 regression guard: ensure deleted helpers and flags from the
    flexibleChains / deferral / safety-net families do NOT resurface
    on the connector class. Each name here was eliminated in a specific
    sub-hito; reappearing is a code-review failure caught here.
     */
    it("given_connector_when_inspectingApi_then_deletedHelpersDoNotResurface", () => {
        const forbidden = [
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
            "isCrossLooseMatchEnabled",
        ];
        const declared = Object.getOwnPropertyNames(_PolyhedralBoundedSolidSetNullEdgesConnector.prototype).concat(
            Object.getOwnPropertyNames(_PolyhedralBoundedSolidSetNullEdgesConnector),
        );
        const survivors: string[] = [];
        for (const name of declared) {
            for (const banned of forbidden) {
                if (name === banned) {
                    survivors.push(banned);
                }
            }
        }
        expect(survivors, "Deleted Connect helpers must not resurface").toEqual([]);
    });

    /**
    §6.2 regression guard: the retry-with-forceARingMove path was
    deleted from `_PolyhedralBoundedSolidSetOperator`; no helper
    name from that family may reappear.
     */
    it("given_setOperator_when_inspectingApi_then_subtractRecoveryRetryDoesNotResurface", () => {
        const forbidden = [
            "trySubtractConnectRecovery",
            "shouldAttemptSubtractConnectRecovery",
            "restoreSystemProperty",
        ];
        const declared = Object.getOwnPropertyNames(_PolyhedralBoundedSolidSetOperator.prototype).concat(
            Object.getOwnPropertyNames(_PolyhedralBoundedSolidSetOperator),
        );
        const survivors: string[] = [];
        for (const name of declared) {
            for (const banned of forbidden) {
                if (name === banned) {
                    survivors.push(banned);
                }
            }
        }
        expect(survivors, "§6.2 retry helpers must not resurface").toEqual([]);
    });
});
