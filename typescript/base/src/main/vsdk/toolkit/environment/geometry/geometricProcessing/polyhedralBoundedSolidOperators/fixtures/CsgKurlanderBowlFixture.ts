import { Math as JavaMath } from "../../../../../../../java/lang/Math.js";
import { platformPrintln } from "../../../../../../../java/lang/_PlatformConsole.js";
import { Matrix4x4d } from "../../../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { Sphere } from "../../../volume/Sphere.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "../PolyhedralBoundedSolidModeler.js";

export class CsgKurlanderBowlFixture {
    private static readonly CYLINDER_SIDES = 30;
    private static readonly MOTIF_RING_COUNT = 4;
    private static readonly MOTIFS_PER_TYPE_RING = 5;
    private static readonly STAR_COUNT =
        CsgKurlanderBowlFixture.MOTIF_RING_COUNT * CsgKurlanderBowlFixture.MOTIFS_PER_TYPE_RING;
    private static readonly MOON_COUNT =
        CsgKurlanderBowlFixture.MOTIF_RING_COUNT * CsgKurlanderBowlFixture.MOTIFS_PER_TYPE_RING;
    private static readonly STAR_Z_VALUES = [9.0, 6.5, 14.0, 4.0, 11.5];
    private static readonly STAR_AZIMUTH_OFFSETS = [0.0, -22.5, -45.0, -45.0, -67.5];
    private static readonly MOON_Z_VALUES = [4.0, 14.0, 11.5, 9.0, 6.5];
    private static readonly MOON_AZIMUTH_OFFSETS = [0.0, 0.0, -22.5, -45.0, -67.5];
    private static readonly OBJECT_SCALE = 0.1;
    private static readonly MOTIF_RADIAL_DISTANCE = 6.0;
    private static readonly STAR_AXIS_ROLL_DEGREES = -90.0;
    private static readonly MOON_AXIS_ROLL_DEGREES = 90.0;
    private static readonly MOON_BOWL_INSET_FRACTION = 0.1;
    private static readonly MOON_CYLINDER_HEIGHT = 5.5;
    private static readonly MOON_AXIS_PROXIMITY_FRACTION = 0.1;
    private static readonly MOON_AXIS_PROXIMITY_REFERENCE_HEIGHT = 5.0;
    private static readonly MOON_AXIS_PROXIMITY_SHIFT =
        CsgKurlanderBowlFixture.MOON_AXIS_PROXIMITY_FRACTION *
        CsgKurlanderBowlFixture.MOON_AXIS_PROXIMITY_REFERENCE_HEIGHT *
        CsgKurlanderBowlFixture.OBJECT_SCALE;

    private constructor() {}

    private static scale(value: number): number {
        return value * CsgKurlanderBowlFixture.OBJECT_SCALE;
    }

    private static booleanOp(a: PolyhedralBoundedSolid, b: PolyhedralBoundedSolid, op: number): PolyhedralBoundedSolid {
        return PolyhedralBoundedSolidModeler.setOp(a, b, op, false);
    }

    private static booleanOpWithoutFaceMaximization(
        a: PolyhedralBoundedSolid,
        b: PolyhedralBoundedSolid,
        op: number,
    ): PolyhedralBoundedSolid {
        return PolyhedralBoundedSolidModeler.setOp(a, b, op, false, false);
    }

    private static createSphere(radius: number, center: Vector3Dd): PolyhedralBoundedSolid {
        const solid = new Sphere(radius).exportToPolyhedralBoundedSolid();
        let t = new Matrix4x4d();
        t = t.translation(center);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, t);
        return solid;
    }

    private static createCylinder(radius: number, height: number, translation: Vector3Dd): PolyhedralBoundedSolid {
        const solid = PolyhedralBoundedSolidModeler.createCircularLamina(
            0.0,
            0.0,
            radius,
            0.0,
            CsgKurlanderBowlFixture.CYLINDER_SIDES,
        );
        let sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, sweep);

        let move = new Matrix4x4d();
        move = move.translation(translation);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static createExtrudedPolygon(points: Vector3Dd[], thickness: number): PolyhedralBoundedSolid {
        let i: number;
        const solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, points[0]!, 1, 1);

        for (i = 1; i < points.length; i++) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, points[i]!);
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, points.length, 1, 2);

        let t = new Matrix4x4d();
        t = t.translation(0.0, 0.0, thickness);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, t);
        return solid;
    }

    private static createStar(): PolyhedralBoundedSolid {
        let i: number;
        const n = 10;
        const outerR = CsgKurlanderBowlFixture.scale(2.0);
        const innerR = CsgKurlanderBowlFixture.scale(0.77);
        const start = JavaMath.toRadians(-90.0);
        const points = new Array<Vector3Dd>(n);

        for (i = 0; i < n; i++) {
            const a = start + (i * Math.PI) / 5.0;
            const r = i % 2 === 0 ? outerR : innerR;
            points[i] = new Vector3Dd(r * Math.cos(a), r * Math.sin(a), 0.0);
        }

        return CsgKurlanderBowlFixture.createExtrudedPolygon(points, CsgKurlanderBowlFixture.scale(5.5));
    }

    private static createMoon(): PolyhedralBoundedSolid {
        const a = CsgKurlanderBowlFixture.createCylinder(
            CsgKurlanderBowlFixture.scale(1.5),
            CsgKurlanderBowlFixture.scale(CsgKurlanderBowlFixture.MOON_CYLINDER_HEIGHT),
            new Vector3Dd(0, 0, 0),
        );
        const b = CsgKurlanderBowlFixture.createCylinder(
            CsgKurlanderBowlFixture.scale(1.5),
            CsgKurlanderBowlFixture.scale(CsgKurlanderBowlFixture.MOON_CYLINDER_HEIGHT),
            new Vector3Dd(CsgKurlanderBowlFixture.scale(1.1), 0, CsgKurlanderBowlFixture.scale(0.6)),
        );
        return CsgKurlanderBowlFixture.booleanOp(a, b, PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    private static placeStar(star: PolyhedralBoundedSolid, z: number, azimuthDeg: number): PolyhedralBoundedSolid {
        return CsgKurlanderBowlFixture.placeMotif(
            star,
            z,
            azimuthDeg,
            1.0,
            0.0,
            CsgKurlanderBowlFixture.STAR_AXIS_ROLL_DEGREES,
        );
    }

    public static createStarPlacementTransformation(z: number, azimuthDeg: number): Matrix4x4d {
        return CsgKurlanderBowlFixture.createMotifPlacementTransformation(
            z,
            azimuthDeg,
            1.0,
            0.0,
            CsgKurlanderBowlFixture.STAR_AXIS_ROLL_DEGREES,
        );
    }

    private static placeMoon(moon: PolyhedralBoundedSolid, z: number, azimuthDeg: number): PolyhedralBoundedSolid {
        return CsgKurlanderBowlFixture.placeMotif(
            moon,
            z,
            azimuthDeg,
            1.0 - CsgKurlanderBowlFixture.MOON_BOWL_INSET_FRACTION,
            -CsgKurlanderBowlFixture.MOON_AXIS_PROXIMITY_SHIFT,
            CsgKurlanderBowlFixture.MOON_AXIS_ROLL_DEGREES,
        );
    }

    public static createMoonPlacementTransformation(z: number, azimuthDeg: number): Matrix4x4d {
        return CsgKurlanderBowlFixture.createMotifPlacementTransformation(
            z,
            azimuthDeg,
            1.0 - CsgKurlanderBowlFixture.MOON_BOWL_INSET_FRACTION,
            -CsgKurlanderBowlFixture.MOON_AXIS_PROXIMITY_SHIFT,
            CsgKurlanderBowlFixture.MOON_AXIS_ROLL_DEGREES,
        );
    }

    private static placeMotif(
        motif: PolyhedralBoundedSolid,
        z: number,
        azimuthDeg: number,
        radialDistanceFactor: number,
        radialOffset: number,
        axisRollDeg: number,
    ): PolyhedralBoundedSolid {
        const m = CsgKurlanderBowlFixture.createMotifPlacementTransformation(
            z,
            azimuthDeg,
            radialDistanceFactor,
            radialOffset,
            axisRollDeg,
        );

        PolyhedralBoundedSolidModeler.applyTransformation(motif, m);
        return motif;
    }

    /**
    Builds the rigid transformation that places a motif on the bowl's
    inner surface.

    @param z motif height along the bowl axis, in unscaled model units.
    @param azimuthDeg motif azimuth angle around the bowl axis, in degrees.
    @param radialDistanceFactor fraction of `MOTIF_RADIAL_DISTANCE`
        used as the motif's nominal radial distance from the bowl axis.
    @param radialOffset additional radial displacement, in scaled world
        units, applied along the same azimuth direction; negative values
        move the motif closer to the bowl axis. Used to nudge moon motifs
        toward the axis by `MOON_AXIS_PROXIMITY_SHIFT`.
    @param axisRollDeg roll angle, in degrees, applied to the motif around
        its own placement axis.
    @return the combined translation/rotation matrix for the motif.
    */
    private static createMotifPlacementTransformation(
        z: number,
        azimuthDeg: number,
        radialDistanceFactor: number,
        radialOffset: number,
        axisRollDeg: number,
    ): Matrix4x4d {
        let ry = new Matrix4x4d();
        let rz = new Matrix4x4d();
        let roll = new Matrix4x4d();
        let t = new Matrix4x4d();
        let m: Matrix4x4d;
        const azimuthRad = JavaMath.toRadians(azimuthDeg);
        const radialDistance = CsgKurlanderBowlFixture.MOTIF_RADIAL_DISTANCE * radialDistanceFactor;
        const x =
            CsgKurlanderBowlFixture.scale(radialDistance * Math.cos(azimuthRad)) + radialOffset * Math.cos(azimuthRad);
        const y =
            CsgKurlanderBowlFixture.scale(radialDistance * Math.sin(azimuthRad)) + radialOffset * Math.sin(azimuthRad);

        ry = ry.axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
        rz = rz.axisRotation(JavaMath.toRadians(azimuthDeg), 0, 0, 1);
        roll = roll.axisRotation(JavaMath.toRadians(axisRollDeg), 0, 0, 1);
        t = t.translation(x, y, CsgKurlanderBowlFixture.scale(z));
        m = t.multiply(rz.multiply(ry.multiply(roll)));
        return m;
    }

    public static getSingleMotifStarCount(): number {
        return CsgKurlanderBowlFixture.STAR_COUNT;
    }

    public static getSingleMotifMoonCount(): number {
        return CsgKurlanderBowlFixture.MOON_COUNT;
    }

    public static getSingleMotifCount(): number {
        return CsgKurlanderBowlFixture.STAR_COUNT + CsgKurlanderBowlFixture.MOON_COUNT;
    }

    public static normalizeSingleMotifIndex(motifIndex: number): number {
        return JavaMath.floorMod(motifIndex, CsgKurlanderBowlFixture.getSingleMotifCount());
    }

    public static describeSingleMotif(motifIndex: number): string {
        const normalizedIndex = CsgKurlanderBowlFixture.normalizeSingleMotifIndex(motifIndex);
        const lastIndex = CsgKurlanderBowlFixture.getSingleMotifCount() - 1;
        let typeIndex: number;

        if (normalizedIndex < CsgKurlanderBowlFixture.STAR_COUNT) {
            typeIndex = normalizedIndex + 1;
            return (
                "STAR " +
                typeIndex +
                "/" +
                CsgKurlanderBowlFixture.STAR_COUNT +
                " index " +
                normalizedIndex +
                "/" +
                lastIndex
            );
        }

        typeIndex = normalizedIndex - CsgKurlanderBowlFixture.STAR_COUNT + 1;
        return (
            "MOON " +
            typeIndex +
            "/" +
            CsgKurlanderBowlFixture.MOON_COUNT +
            " index " +
            normalizedIndex +
            "/" +
            lastIndex
        );
    }

    /**
    Returns the bowl base: outer sphere minus inner sphere, clipped by the
    guide cylinder.  This is operand A for all boolean subtraction tests.
    */
    public static createBowl(): PolyhedralBoundedSolid {
        const outer = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(10.0),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );
        const inner = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(9.5),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );
        const shell = CsgKurlanderBowlFixture.booleanOp(outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
        return CsgKurlanderBowlFixture.booleanOp(
            shell,
            CsgKurlanderBowlFixture.createCylinder(
                CsgKurlanderBowlFixture.scale(10.5),
                CsgKurlanderBowlFixture.scale(16.5),
                new Vector3Dd(0, 0, 0),
            ),
            PolyhedralBoundedSolidModeler.INTERSECTION,
        );
    }

    /**
    Returns the union of all 40 motifs (20 moons + 20 stars) as a single
    multi-shell solid.  The motifs are spatially disjoint so the union
    produces one shell per motif.
    */
    public static createAllMotifsUnion(): PolyhedralBoundedSolid {
        let i: number;
        const moonCount = CsgKurlanderBowlFixture.getSingleMotifMoonCount();
        const starCount = CsgKurlanderBowlFixture.getSingleMotifStarCount();
        const motifCount = moonCount + starCount;

        CsgKurlanderBowlFixture.printProgressMessage("createAllMotifsUnion: building " + motifCount + " motifs");

        let result = CsgKurlanderBowlFixture.placeMoon(
            CsgKurlanderBowlFixture.createMoon(),
            CsgKurlanderBowlFixture.getMoonZ(0),
            CsgKurlanderBowlFixture.getMoonAzimuthDeg(0),
        );
        CsgKurlanderBowlFixture.printMotifProgress("moon", 1, moonCount, 1, motifCount);

        for (i = 1; i < moonCount; i++) {
            CsgKurlanderBowlFixture.printMotifProgress("moon", i + 1, moonCount, i + 1, motifCount);
            result = CsgKurlanderBowlFixture.booleanOpWithoutFaceMaximization(
                result,
                CsgKurlanderBowlFixture.placeMoon(
                    CsgKurlanderBowlFixture.createMoon(),
                    CsgKurlanderBowlFixture.getMoonZ(i),
                    CsgKurlanderBowlFixture.getMoonAzimuthDeg(i),
                ),
                PolyhedralBoundedSolidModeler.UNION,
            );
        }

        for (i = 0; i < starCount; i++) {
            CsgKurlanderBowlFixture.printMotifProgress("star", i + 1, starCount, moonCount + i + 1, motifCount);
            result = CsgKurlanderBowlFixture.booleanOpWithoutFaceMaximization(
                result,
                CsgKurlanderBowlFixture.placeStar(
                    CsgKurlanderBowlFixture.createStar(),
                    CsgKurlanderBowlFixture.getStarZ(i),
                    CsgKurlanderBowlFixture.getStarAzimuthDeg(i),
                ),
                PolyhedralBoundedSolidModeler.UNION,
            );
        }

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        CsgKurlanderBowlFixture.printProgressMessage("createAllMotifsUnion: finished");
        return result;
    }

    public static createBowlAndFirstStarOperands(motifIndex = 0): PolyhedralBoundedSolid[] {
        const operands = new Array<PolyhedralBoundedSolid>(2);
        const outer = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(10.0),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );
        const inner = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(9.5),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );
        const shell = CsgKurlanderBowlFixture.booleanOp(outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
        const bowl = CsgKurlanderBowlFixture.booleanOp(
            shell,
            CsgKurlanderBowlFixture.createCylinder(
                CsgKurlanderBowlFixture.scale(10.5),
                CsgKurlanderBowlFixture.scale(16.5),
                new Vector3Dd(0, 0, 0),
            ),
            PolyhedralBoundedSolidModeler.INTERSECTION,
        );

        operands[0] = bowl;
        operands[1] = CsgKurlanderBowlFixture.createSingleMotif(motifIndex);
        return operands;
    }

    public static createSingleMotif(motifIndex: number): PolyhedralBoundedSolid {
        const normalizedIndex = CsgKurlanderBowlFixture.normalizeSingleMotifIndex(motifIndex);
        let motifTypeIndex: number;

        if (normalizedIndex < CsgKurlanderBowlFixture.STAR_COUNT) {
            motifTypeIndex = normalizedIndex;
            return CsgKurlanderBowlFixture.placeStar(
                CsgKurlanderBowlFixture.createStar(),
                CsgKurlanderBowlFixture.getStarZ(motifTypeIndex),
                CsgKurlanderBowlFixture.getStarAzimuthDeg(motifTypeIndex),
            );
        }

        motifTypeIndex = normalizedIndex - CsgKurlanderBowlFixture.STAR_COUNT;
        return CsgKurlanderBowlFixture.placeMoon(
            CsgKurlanderBowlFixture.createMoon(),
            CsgKurlanderBowlFixture.getMoonZ(motifTypeIndex),
            CsgKurlanderBowlFixture.getMoonAzimuthDeg(motifTypeIndex),
        );
    }

    private static getStarZ(motifTypeIndex: number): number {
        return CsgKurlanderBowlFixture.getMotifValue(motifTypeIndex, CsgKurlanderBowlFixture.STAR_Z_VALUES);
    }

    private static getMoonZ(motifTypeIndex: number): number {
        return CsgKurlanderBowlFixture.getMotifValue(motifTypeIndex, CsgKurlanderBowlFixture.MOON_Z_VALUES);
    }

    private static getStarAzimuthDeg(motifTypeIndex: number): number {
        return CsgKurlanderBowlFixture.getMotifAzimuthDeg(motifTypeIndex, CsgKurlanderBowlFixture.STAR_AZIMUTH_OFFSETS);
    }

    private static getMoonAzimuthDeg(motifTypeIndex: number): number {
        return CsgKurlanderBowlFixture.getMotifAzimuthDeg(motifTypeIndex, CsgKurlanderBowlFixture.MOON_AZIMUTH_OFFSETS);
    }

    private static getMotifValue(motifTypeIndex: number, values: readonly number[]): number {
        const positionIndex = motifTypeIndex % CsgKurlanderBowlFixture.MOTIFS_PER_TYPE_RING;
        return values[positionIndex]!;
    }

    private static getMotifAzimuthDeg(motifTypeIndex: number, offsets: readonly number[]): number {
        const positionIndex = motifTypeIndex % CsgKurlanderBowlFixture.MOTIFS_PER_TYPE_RING;
        const ringIndex = Math.trunc(motifTypeIndex / CsgKurlanderBowlFixture.MOTIFS_PER_TYPE_RING) + 1;
        const base = -90.0 * ringIndex;

        return base + offsets[positionIndex]!;
    }

    public static createShellAndFirstMoonOperands(): PolyhedralBoundedSolid[] {
        const operands = new Array<PolyhedralBoundedSolid>(2);
        const outer = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(10.0),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );
        const inner = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(9.5),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );

        operands[0] = CsgKurlanderBowlFixture.booleanOp(outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
        operands[1] = CsgKurlanderBowlFixture.placeMoon(CsgKurlanderBowlFixture.createMoon(), 4.0, -90.0);
        return operands;
    }

    public static create(): PolyhedralBoundedSolid {
        let i: number;
        let moonIndex = 0;
        let starIndex = 0;
        let motifIndex = 0;
        const moonCount = CsgKurlanderBowlFixture.getSingleMotifMoonCount();
        const starCount = CsgKurlanderBowlFixture.getSingleMotifStarCount();
        const motifCount = moonCount + starCount;
        CsgKurlanderBowlFixture.printProgressMessage("Processing Kurlander bowl all motifs: starting base shell");
        const outer = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(10.0),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );
        const inner = CsgKurlanderBowlFixture.createSphere(
            CsgKurlanderBowlFixture.scale(9.5),
            new Vector3Dd(0, 0, CsgKurlanderBowlFixture.scale(10.0)),
        );
        let shell = CsgKurlanderBowlFixture.booleanOp(outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);
        CsgKurlanderBowlFixture.printProgressMessage("Processing Kurlander bowl all motifs: base shell ready");

        for (i = 0; i < moonCount; i++) {
            moonIndex++;
            motifIndex++;
            CsgKurlanderBowlFixture.printMotifProgress("moon", moonIndex, moonCount, motifIndex, motifCount);
            shell = CsgKurlanderBowlFixture.booleanOpWithoutFaceMaximization(
                shell,
                CsgKurlanderBowlFixture.placeMoon(
                    CsgKurlanderBowlFixture.createMoon(),
                    CsgKurlanderBowlFixture.getMoonZ(i),
                    CsgKurlanderBowlFixture.getMoonAzimuthDeg(i),
                ),
                PolyhedralBoundedSolidModeler.SUBTRACT,
            );
        }

        for (i = 0; i < starCount; i++) {
            starIndex++;
            motifIndex++;
            CsgKurlanderBowlFixture.printMotifProgress("star", starIndex, starCount, motifIndex, motifCount);
            shell = CsgKurlanderBowlFixture.booleanOpWithoutFaceMaximization(
                shell,
                CsgKurlanderBowlFixture.placeStar(
                    CsgKurlanderBowlFixture.createStar(),
                    CsgKurlanderBowlFixture.getStarZ(i),
                    CsgKurlanderBowlFixture.getStarAzimuthDeg(i),
                ),
                PolyhedralBoundedSolidModeler.SUBTRACT,
            );
        }

        const guide = CsgKurlanderBowlFixture.createCylinder(
            CsgKurlanderBowlFixture.scale(10.5),
            CsgKurlanderBowlFixture.scale(16.5),
            new Vector3Dd(0, 0, 0),
        );
        CsgKurlanderBowlFixture.printProgressMessage("Processing Kurlander bowl all motifs: clipping final bowl");
        const result = CsgKurlanderBowlFixture.booleanOpWithoutFaceMaximization(
            shell,
            guide,
            PolyhedralBoundedSolidModeler.INTERSECTION,
        );

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        CsgKurlanderBowlFixture.printProgressMessage("Processing Kurlander bowl all motifs: finished");
        return result;
    }

    private static printMotifProgress(
        motifType: string,
        typeIndex: number,
        typeCount: number,
        motifIndex: number,
        motifCount: number,
    ): void {
        CsgKurlanderBowlFixture.printProgressMessage(
            "Processing " + motifType + " " + typeIndex + "/" + typeCount + ", motif " + motifIndex + "/" + motifCount,
        );
    }

    private static printProgressMessage(message: string): void {
        platformPrintln(message);
        // Runtime boundary: the Java version mirrors the message on
        // `System.err` as well; the base package only exposes one console
        // adapter, so the message is emitted once.
    }
}
