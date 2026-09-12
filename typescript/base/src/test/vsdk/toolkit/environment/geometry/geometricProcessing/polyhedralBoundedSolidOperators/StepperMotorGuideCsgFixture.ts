import { ArrayList } from "java/util/ArrayList.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { _PolyhedralBoundedSolidEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";

function mm(value: number): number {
    return value * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
}

/**
Self-contained CSG fixture derived from
`TangibleInterfaceCubeFixture.STEPER_MOTOR_GUIDE`.
 */
export class StepperMotorGuideCsgFixture {
    public static readonly MILLIMETERS_TO_MODEL_UNITS = 0.01;
    public static readonly BASE_TOP_Z = 3.0 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    public static readonly TRANSITION_Z =
        StepperMotorGuideCsgFixture.BASE_TOP_Z + 4.2 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    public static readonly SLEEVE_BASE_Z =
        StepperMotorGuideCsgFixture.BASE_TOP_Z - 1.0 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    public static readonly SLEEVE_TOP_Z =
        StepperMotorGuideCsgFixture.BASE_TOP_Z + 7.06 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;

    private static readonly D_BORE_DIAMETER = 5.05 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly D_FLAT_AXIS = 4.6 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly SLEEVE_INNER_DIAMETER = 9.02 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly LEGACY_COUPLER_OUTER_DIAMETER =
        9.02 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly CORRECTED_COUPLER_OUTER_DIAMETER =
        10.02 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly SLEEVE_WALL = 1.6 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly AXIAL_OVERLAP = 0.1 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly CENTER_X = 20.0 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly CENTER_Y = 20.0 * StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;
    private static readonly CYLINDER_SIDES = 32;
    private static readonly D_PROFILE_SIDES = 64;

    private constructor() {}

    public static createLegacyLowerCoupler(): PolyhedralBoundedSolid {
        const exterior = StepperMotorGuideCsgFixture.createCylinder(
            StepperMotorGuideCsgFixture.LEGACY_COUPLER_OUTER_DIAMETER * 0.5,
            StepperMotorGuideCsgFixture.TRANSITION_Z - StepperMotorGuideCsgFixture.SLEEVE_BASE_Z,
            StepperMotorGuideCsgFixture.SLEEVE_BASE_Z,
        );
        const dCutter = StepperMotorGuideCsgFixture.createDProfile(
            StepperMotorGuideCsgFixture.SLEEVE_BASE_Z - StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
            StepperMotorGuideCsgFixture.TRANSITION_Z -
                StepperMotorGuideCsgFixture.SLEEVE_BASE_Z +
                2.0 * StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
        );
        return StepperMotorGuideCsgFixture.strictSetOp(exterior, dCutter, PolyhedralBoundedSolidModeler.SUBTRACT);
    }

    public static createLegacyBearingSleeve(): PolyhedralBoundedSolid {
        const innerRadius = StepperMotorGuideCsgFixture.SLEEVE_INNER_DIAMETER * 0.5;
        const exterior = StepperMotorGuideCsgFixture.createCylinder(
            innerRadius + StepperMotorGuideCsgFixture.SLEEVE_WALL,
            StepperMotorGuideCsgFixture.SLEEVE_TOP_Z - StepperMotorGuideCsgFixture.SLEEVE_BASE_Z,
            StepperMotorGuideCsgFixture.SLEEVE_BASE_Z,
        );
        const circularCutter = StepperMotorGuideCsgFixture.createCylinder(
            innerRadius,
            StepperMotorGuideCsgFixture.SLEEVE_TOP_Z -
                StepperMotorGuideCsgFixture.SLEEVE_BASE_Z +
                2.0 * StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
            StepperMotorGuideCsgFixture.SLEEVE_BASE_Z - StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
        );
        return StepperMotorGuideCsgFixture.strictSetOp(
            exterior,
            circularCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );
    }

    public static createCorrectedSteppedTube(): PolyhedralBoundedSolid {
        const exterior = StepperMotorGuideCsgFixture.createCombinedExterior();
        const upperCutter = StepperMotorGuideCsgFixture.createCylinder(
            StepperMotorGuideCsgFixture.SLEEVE_INNER_DIAMETER * 0.5,
            StepperMotorGuideCsgFixture.SLEEVE_TOP_Z -
                StepperMotorGuideCsgFixture.TRANSITION_Z +
                StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
            StepperMotorGuideCsgFixture.TRANSITION_Z,
        );
        const withUpperCavity = StepperMotorGuideCsgFixture.strictSetOp(
            exterior,
            upperCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );
        const lowerCutter = StepperMotorGuideCsgFixture.createDProfile(
            StepperMotorGuideCsgFixture.SLEEVE_BASE_Z - StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
            StepperMotorGuideCsgFixture.TRANSITION_Z -
                StepperMotorGuideCsgFixture.SLEEVE_BASE_Z +
                2.0 * StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
        );
        return StepperMotorGuideCsgFixture.strictSetOp(
            withUpperCavity,
            lowerCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );
    }

    public static createCorrectedFinalGuide(): PolyhedralBoundedSolid {
        const base = StepperMotorGuideCsgFixture.createArrowBase();
        const exterior = StepperMotorGuideCsgFixture.createCombinedExterior();
        const baseWithExterior = StepperMotorGuideCsgFixture.strictSetOp(
            base,
            exterior,
            PolyhedralBoundedSolidModeler.UNION,
        );
        const upperCutter = StepperMotorGuideCsgFixture.createCylinder(
            StepperMotorGuideCsgFixture.SLEEVE_INNER_DIAMETER * 0.5,
            StepperMotorGuideCsgFixture.SLEEVE_TOP_Z -
                StepperMotorGuideCsgFixture.TRANSITION_Z +
                StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
            StepperMotorGuideCsgFixture.TRANSITION_Z,
        );
        const withUpperCavity = StepperMotorGuideCsgFixture.strictSetOp(
            baseWithExterior,
            upperCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );
        const lowerCutter = StepperMotorGuideCsgFixture.createDProfile(
            StepperMotorGuideCsgFixture.BASE_TOP_Z,
            StepperMotorGuideCsgFixture.TRANSITION_Z -
                StepperMotorGuideCsgFixture.BASE_TOP_Z +
                StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
        );
        return StepperMotorGuideCsgFixture.strictSetOp(
            withUpperCavity,
            lowerCutter,
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );
    }

    public static containsMaterialAt(solid: PolyhedralBoundedSolid, x: number, y: number, z: number): boolean {
        const probeSide = mm(0.02);
        const probe = new Box(new Vector3Dd(probeSide, probeSide, probeSide)).exportToPolyhedralBoundedSolid();
        let move = new Matrix4x4d();
        move = move.translation(x, y, z);
        PolyhedralBoundedSolidModeler.applyTransformation(probe, move);
        const intersection = PolyhedralBoundedSolidModeler.setOp(
            StepperMotorGuideCsgFixture.deepClone(solid),
            probe,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
            true,
            false,
        );
        return intersection.getPolygonsList().size() > 0;
    }

    public static hasLoopWithFewerThanThreeDistinctEdges(solid: PolyhedralBoundedSolid): boolean {
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            for (let j = 0; j < solid.getPolygonsList().get(i)!.boundariesList.size(); j++) {
                if (solid.getPolygonsList().get(i)!.boundariesList.get(j)!.halfEdgesList.size() < 3) {
                    return true;
                }
            }
        }
        return false;
    }

    public static centerX(): number {
        return StepperMotorGuideCsgFixture.CENTER_X;
    }

    public static centerY(): number {
        return StepperMotorGuideCsgFixture.CENTER_Y;
    }

    private static createCombinedExterior(): PolyhedralBoundedSolid {
        const lowerExterior = StepperMotorGuideCsgFixture.createCylinder(
            StepperMotorGuideCsgFixture.CORRECTED_COUPLER_OUTER_DIAMETER * 0.5,
            StepperMotorGuideCsgFixture.TRANSITION_Z -
                (StepperMotorGuideCsgFixture.SLEEVE_BASE_Z + StepperMotorGuideCsgFixture.AXIAL_OVERLAP),
            StepperMotorGuideCsgFixture.SLEEVE_BASE_Z + StepperMotorGuideCsgFixture.AXIAL_OVERLAP,
        );
        const sleeveExterior = StepperMotorGuideCsgFixture.createCylinder(
            StepperMotorGuideCsgFixture.SLEEVE_INNER_DIAMETER * 0.5 + StepperMotorGuideCsgFixture.SLEEVE_WALL,
            StepperMotorGuideCsgFixture.SLEEVE_TOP_Z - StepperMotorGuideCsgFixture.SLEEVE_BASE_Z,
            StepperMotorGuideCsgFixture.SLEEVE_BASE_Z,
        );
        return StepperMotorGuideCsgFixture.strictSetOp(
            lowerExterior,
            sleeveExterior,
            PolyhedralBoundedSolidModeler.UNION,
        );
    }

    private static createArrowBase(): PolyhedralBoundedSolid {
        const polygon = new ArrayList<Vector3Dd>();
        polygon.add(StepperMotorGuideCsgFixture.point(0.0, 0.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(40.0, 0.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(40.0, 15.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(45.0, 15.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(55.0, 20.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(45.0, 25.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(40.0, 25.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(40.0, 40.0, 0.0));
        polygon.add(StepperMotorGuideCsgFixture.point(0.0, 40.0, 0.0));
        return StepperMotorGuideCsgFixture.extrudePolygon(polygon, StepperMotorGuideCsgFixture.BASE_TOP_Z);
    }

    private static createDProfile(baseZ: number, height: number): PolyhedralBoundedSolid {
        const radius = StepperMotorGuideCsgFixture.D_BORE_DIAMETER * 0.5;
        const flatX = StepperMotorGuideCsgFixture.D_FLAT_AXIS - radius;
        const polygon = new ArrayList<Vector3Dd>();
        for (let i = 0; i < StepperMotorGuideCsgFixture.D_PROFILE_SIDES; i++) {
            const angleA = (2.0 * Math.PI * i) / StepperMotorGuideCsgFixture.D_PROFILE_SIDES;
            const angleB = (2.0 * Math.PI * (i + 1)) / StepperMotorGuideCsgFixture.D_PROFILE_SIDES;
            const a = new Vector3Dd(radius * Math.cos(angleA), radius * Math.sin(angleA), baseZ);
            const b = new Vector3Dd(radius * Math.cos(angleB), radius * Math.sin(angleB), baseZ);
            const aInside = a.x() <= flatX + 1.0e-9;
            const bInside = b.x() <= flatX + 1.0e-9;
            if (aInside) {
                StepperMotorGuideCsgFixture.appendUnique(
                    polygon,
                    new Vector3Dd(
                        StepperMotorGuideCsgFixture.CENTER_X + a.x(),
                        StepperMotorGuideCsgFixture.CENTER_Y + a.y(),
                        baseZ,
                    ),
                );
            }
            if (aInside !== bInside) {
                const factor = (flatX - a.x()) / (b.x() - a.x());
                const y = a.y() + factor * (b.y() - a.y());
                StepperMotorGuideCsgFixture.appendUnique(
                    polygon,
                    new Vector3Dd(
                        StepperMotorGuideCsgFixture.CENTER_X + flatX,
                        StepperMotorGuideCsgFixture.CENTER_Y + y,
                        baseZ,
                    ),
                );
            }
        }
        return StepperMotorGuideCsgFixture.extrudePolygon(polygon, height);
    }

    private static createCylinder(radius: number, height: number, baseZ: number): PolyhedralBoundedSolid {
        const solid = new Cone(radius, radius, height).exportToPolyhedralBoundedSolid(
            StepperMotorGuideCsgFixture.CYLINDER_SIDES,
            1,
        );
        let move = new Matrix4x4d();
        move = move.translation(StepperMotorGuideCsgFixture.CENTER_X, StepperMotorGuideCsgFixture.CENTER_Y, baseZ);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
        return solid;
    }

    private static extrudePolygon(polygon: ArrayList<Vector3Dd>, height: number): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, polygon.get(0), 1, 1);
        for (let i = 1; i < polygon.size(); i++) {
            PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, polygon.get(i));
        }
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, polygon.size(), 1, 2);
        let sweep = new Matrix4x4d();
        sweep = sweep.translation(0.0, 0.0, height);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, sweep);
        return solid;
    }

    private static strictSetOp(
        a: PolyhedralBoundedSolid,
        b: PolyhedralBoundedSolid,
        op: number,
    ): PolyhedralBoundedSolid {
        return PolyhedralBoundedSolidModeler.setOp(a, b, op, false, true, true);
    }

    private static appendUnique(polygon: ArrayList<Vector3Dd>, candidate: Vector3Dd): void {
        if (
            polygon.isEmpty() ||
            polygon
                .get(polygon.size() - 1)
                .subtract(candidate)
                .length() > 1.0e-9
        ) {
            polygon.add(candidate);
        }
    }

    private static point(xMm: number, yMm: number, zMm: number): Vector3Dd {
        return new Vector3Dd(mm(xMm), mm(yMm), mm(zMm));
    }

    /**
    Runtime boundary: Java clones the solid through object serialization;
    there is no serialization runtime here, so the half-edge graph is copied
    node by node through identity maps, preserving ids, positions and the
    exact ordering of every list (the same adapter the production
    `_PolyhedralBoundedSolidSetOperator.deepCloneSolid` uses).
    */
    private static deepClone(solid: PolyhedralBoundedSolid): PolyhedralBoundedSolid {
        const clone = new PolyhedralBoundedSolid();
        const vertices = new Map<_PolyhedralBoundedSolidVertex, _PolyhedralBoundedSolidVertex>();
        const halfEdges = new Map<_PolyhedralBoundedSolidHalfEdge, _PolyhedralBoundedSolidHalfEdge>();
        let i: number;
        let j: number;
        let k: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const source = solid.getVerticesList().get(i)!;
            vertices.set(source, new _PolyhedralBoundedSolidVertex(clone, new Vector3Dd(source.position), source.id));
        }

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const sourceFace = solid.getPolygonsList().get(i)!;
            const cloneFace = new _PolyhedralBoundedSolidFace(clone, sourceFace.id);
            for (j = 0; j < sourceFace.boundariesList.size(); j++) {
                const sourceLoop = sourceFace.boundariesList.get(j)!;
                const cloneLoop = new _PolyhedralBoundedSolidLoop(cloneFace);
                for (k = 0; k < sourceLoop.halfEdgesList.size(); k++) {
                    const sourceHalfEdge = sourceLoop.halfEdgesList.get(k)!;
                    const cloneHalfEdge = new _PolyhedralBoundedSolidHalfEdge(
                        vertices.get(sourceHalfEdge.startingVertex)!,
                        cloneLoop,
                        clone,
                    );
                    cloneLoop.halfEdgesList.add(cloneHalfEdge);
                    halfEdges.set(sourceHalfEdge, cloneHalfEdge);
                }
                if (sourceLoop.boundaryStartHalfEdge !== null) {
                    cloneLoop.boundaryStartHalfEdge = halfEdges.get(sourceLoop.boundaryStartHalfEdge)!;
                }
            }
        }

        for (i = 0; i < solid.getEdgesList().size(); i++) {
            const sourceEdge = solid.getEdgesList().get(i)!;
            const cloneEdge = new _PolyhedralBoundedSolidEdge(clone);
            cloneEdge.id = sourceEdge.id;
            if (sourceEdge.rightHalf !== null) {
                cloneEdge.rightHalf = halfEdges.get(sourceEdge.rightHalf)!;
                cloneEdge.rightHalf.parentEdge = cloneEdge;
            }
            if (sourceEdge.leftHalf !== null) {
                cloneEdge.leftHalf = halfEdges.get(sourceEdge.leftHalf)!;
                cloneEdge.leftHalf.parentEdge = cloneEdge;
            }
        }

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const source = solid.getVerticesList().get(i)!;
            if (source.emanatingHalfEdge !== null) {
                vertices.get(source)!.emanatingHalfEdge = halfEdges.get(source.emanatingHalfEdge)!;
            }
        }

        clone.setMaxVertexId(solid.getMaxVertexId());
        clone.setMaxFaceId(solid.getMaxFaceId());

        return clone;
    }
}
