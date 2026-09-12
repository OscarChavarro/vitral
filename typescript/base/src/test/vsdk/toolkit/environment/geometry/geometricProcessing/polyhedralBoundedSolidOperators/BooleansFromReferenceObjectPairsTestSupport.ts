import { IllegalStateException } from "java/lang/IllegalStateException.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { ArrayList } from "java/util/ArrayList.js";
import { Collections } from "java/util/Collections.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";

/**
`BooleansFromReferenceObjectPairsTest.TopologicalSummary` from the Java suite.

<p>Java declares it as a static nested class of the test; here it lives in a
sibling module because `KurlanderMotif4OperationMatrixTest` reuses it and
importing a `.spec.ts` file would re-run that file's tests.</p>
 */
export class TopologicalSummary {
    private readonly placeholderLabel: string | null;
    public readonly shellCount: number;
    private readonly faceCount: number;
    private readonly edgeCount: number;
    private readonly vertexCount: number;
    private readonly loopCount: number;
    private readonly multiLoopFaceCount: number;
    private readonly eulerCharacteristic: number;
    private readonly shellFaceCountsSorted: number[];
    private readonly loopsPerFaceSorted: number[];
    private readonly verticesPerLoopSorted: number[];
    private readonly minMaxMicrounits: number[];

    private constructor(
        placeholderLabel: string | null,
        shellCount: number,
        faceCount: number,
        edgeCount: number,
        vertexCount: number,
        loopCount: number,
        multiLoopFaceCount: number,
        eulerCharacteristic: number,
        shellFaceCountsSorted: number[],
        loopsPerFaceSorted: number[],
        verticesPerLoopSorted: number[],
        minMaxMicrounits: number[],
    ) {
        this.placeholderLabel = placeholderLabel;
        this.shellCount = shellCount;
        this.faceCount = faceCount;
        this.edgeCount = edgeCount;
        this.vertexCount = vertexCount;
        this.loopCount = loopCount;
        this.multiLoopFaceCount = multiLoopFaceCount;
        this.eulerCharacteristic = eulerCharacteristic;
        this.shellFaceCountsSorted = shellFaceCountsSorted;
        this.loopsPerFaceSorted = loopsPerFaceSorted;
        this.verticesPerLoopSorted = verticesPerLoopSorted;
        this.minMaxMicrounits = minMaxMicrounits;
    }

    public static placeholder(label: string): TopologicalSummary {
        return new TopologicalSummary(label, -1, -1, -1, -1, -1, -1, -1, [], [], [], []);
    }

    public static of(
        shellCount: number,
        faceCount: number,
        edgeCount: number,
        vertexCount: number,
        loopCount: number,
        multiLoopFaceCount: number,
        eulerCharacteristic: number,
        shellFaceCountsSorted: number[],
        loopsPerFaceSorted: number[],
        verticesPerLoopSorted: number[],
        minMaxMicrounits: number[],
    ): TopologicalSummary {
        return new TopologicalSummary(
            null,
            shellCount,
            faceCount,
            edgeCount,
            vertexCount,
            loopCount,
            multiLoopFaceCount,
            eulerCharacteristic,
            shellFaceCountsSorted,
            loopsPerFaceSorted,
            verticesPerLoopSorted,
            minMaxMicrounits,
        );
    }

    public static from(solid: PolyhedralBoundedSolid): TopologicalSummary {
        const faceCount = solid.getPolygonsList().size();
        const edgeCount = solid.getEdgesList().size();
        const vertexCount = solid.getVerticesList().size();
        const eulerCharacteristic = vertexCount - edgeCount + faceCount;
        const loopsPerFace = new ArrayList<number>();
        const verticesPerLoop = new ArrayList<number>();
        let loopCount = 0;
        let multiLoopFaceCount = 0;

        let i: number;
        let j: number;
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            const loopsInFace = face.boundariesList.size();
            loopsPerFace.add(loopsInFace);
            loopCount += loopsInFace;
            if (loopsInFace > 1) {
                multiLoopFaceCount++;
            }
            for (j = 0; j < loopsInFace; j++) {
                verticesPerLoop.add(face.boundariesList.get(j)!.halfEdgesList.size());
            }
        }

        Collections.sort(loopsPerFace);
        Collections.sort(verticesPerLoop);

        const shellFaceCountsSorted = TopologicalSummary.computeShellFaceCounts(solid);
        const shellCount = shellFaceCountsSorted.length;

        return new TopologicalSummary(
            null,
            shellCount,
            faceCount,
            edgeCount,
            vertexCount,
            loopCount,
            multiLoopFaceCount,
            eulerCharacteristic,
            shellFaceCountsSorted,
            TopologicalSummary.toIntArray(loopsPerFace),
            TopologicalSummary.toIntArray(verticesPerLoop),
            TopologicalSummary.toMinMaxMicrounits(solid.getMinMax()),
        );
    }

    public toLiteral(): string {
        return (
            "TopologicalSummary.of(" +
            this.shellCount +
            ", " +
            this.faceCount +
            ", " +
            this.edgeCount +
            ", " +
            this.vertexCount +
            ", " +
            this.loopCount +
            ", " +
            this.multiLoopFaceCount +
            ", " +
            this.eulerCharacteristic +
            ", " +
            TopologicalSummary.intArrayLiteral(this.shellFaceCountsSorted) +
            ", " +
            TopologicalSummary.intArrayLiteral(this.loopsPerFaceSorted) +
            ", " +
            TopologicalSummary.intArrayLiteral(this.verticesPerLoopSorted) +
            ", " +
            TopologicalSummary.longArrayLiteral(this.minMaxMicrounits) +
            ")"
        );
    }

    private static intArrayLiteral(values: number[]): string {
        return "new int[] {" + TopologicalSummary.joinInts(values) + "}";
    }

    private static longArrayLiteral(values: number[]): string {
        return "new long[] {" + TopologicalSummary.joinLongs(values) + "}";
    }

    private static joinInts(values: number[]): string {
        let out = "";
        let i: number;
        for (i = 0; i < values.length; i++) {
            if (i > 0) {
                out += ", ";
            }
            out += values[i];
        }
        return out;
    }

    private static joinLongs(values: number[]): string {
        let out = "";
        let i: number;
        for (i = 0; i < values.length; i++) {
            if (i > 0) {
                out += ", ";
            }
            out += values[i] + "L";
        }
        return out;
    }

    private static toIntArray(values: ArrayList<number>): number[] {
        const out = new Array<number>(values.size());
        let i: number;
        for (i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static toMinMaxMicrounits(minMax: Float64Array | number[]): number[] {
        const out = new Array<number>(minMax.length);
        let i: number;
        for (i = 0; i < minMax.length; i++) {
            out[i] = JavaMath.round(minMax[i]! * 1000000.0);
        }
        return out;
    }

    private static computeShellFaceCounts(solid: PolyhedralBoundedSolid): number[] {
        const sharedSummary = PolyhedralBoundedSolidTopologySummary.from(solid);
        const componentSizes = new ArrayList<number>();
        for (let i = 0; i < sharedSummary.getShells().length; i++) {
            componentSizes.add(sharedSummary.getShells()[i]!.getFaceCount());
        }
        Collections.sort(componentSizes);
        return TopologicalSummary.toIntArray(componentSizes);
    }

    public equals(other: TopologicalSummary): boolean {
        if (this === other) {
            return true;
        }

        if (this.placeholderLabel !== null || other.placeholderLabel !== null) {
            throw new IllegalStateException(
                "Missing hardcoded reference summary: " +
                    (this.placeholderLabel !== null ? this.placeholderLabel : other.placeholderLabel),
            );
        }

        return (
            this.shellCount === other.shellCount &&
            this.faceCount === other.faceCount &&
            this.edgeCount === other.edgeCount &&
            this.vertexCount === other.vertexCount &&
            this.loopCount === other.loopCount &&
            this.multiLoopFaceCount === other.multiLoopFaceCount &&
            this.eulerCharacteristic === other.eulerCharacteristic &&
            TopologicalSummary.arraysEqual(this.shellFaceCountsSorted, other.shellFaceCountsSorted) &&
            TopologicalSummary.arraysEqual(this.loopsPerFaceSorted, other.loopsPerFaceSorted) &&
            TopologicalSummary.arraysEqual(this.verticesPerLoopSorted, other.verticesPerLoopSorted) &&
            TopologicalSummary.arraysEqual(this.minMaxMicrounits, other.minMaxMicrounits)
        );
    }

    private static arraysEqual(first: number[], second: number[]): boolean {
        if (first.length !== second.length) {
            return false;
        }
        for (let i = 0; i < first.length; i++) {
            if (first[i] !== second[i]) {
                return false;
            }
        }
        return true;
    }

    public toString(): string {
        if (this.placeholderLabel !== null) {
            return "TopologicalSummary{placeholder=" + this.placeholderLabel + "}";
        }

        return (
            "TopologicalSummary{" +
            "shellCount=" +
            this.shellCount +
            ", faceCount=" +
            this.faceCount +
            ", edgeCount=" +
            this.edgeCount +
            ", vertexCount=" +
            this.vertexCount +
            ", loopCount=" +
            this.loopCount +
            ", multiLoopFaceCount=" +
            this.multiLoopFaceCount +
            ", eulerCharacteristic=" +
            this.eulerCharacteristic +
            ", shellFaceCountsSorted=[" +
            this.shellFaceCountsSorted.join(", ") +
            "]" +
            ", loopsPerFaceSorted=[" +
            this.loopsPerFaceSorted.join(", ") +
            "]" +
            ", verticesPerLoopSorted=[" +
            this.verticesPerLoopSorted.join(", ") +
            "]" +
            ", minMaxMicrounits=[" +
            this.minMaxMicrounits.join(", ") +
            "]" +
            "}"
        );
    }
}
