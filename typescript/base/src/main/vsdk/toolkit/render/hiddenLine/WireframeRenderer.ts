//= References:                                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

// Java classes
import { ArrayList } from "../../../../java/util/ArrayList.js";

// VitralSDK classes
import { VSDK } from "../../common/VSDK.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vector4Dd } from "../../common/linealAlgebra/Vector4Dd.js";
import { Camera } from "../../environment/camera/Camera.js";
import { Surface } from "../../environment/geometry/surface/Surface.js";
import { Solid } from "../../environment/geometry/volume/Solid.js";
import { Geometry } from "../../environment/geometry/Geometry.js";
import { GeometryTriangulator } from "../../environment/geometry/geometricProcessing/GeometryTriangulator.js";
import { Volume } from "../../environment/geometry/volume/Volume.js";
import { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";
import { PolyhedralBoundedSolid } from "../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { TriangleMeshGroup } from "../../environment/geometry/surface/TriangleMeshGroup.js";
import { _PolyhedralBoundedSolidEdge } from "../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { Calligraphic2DBuffer } from "../../media/Calligraphic2DBuffer.js";
import { RenderingElement } from "../RenderingElement.js";

export class WireframeRenderer extends RenderingElement {
    private static calculateCanonicalOutcode(p: Vector3Dd, fpd: number): number {
        let bits = 0x0;

        if (p.z() + p.x() - 1 > 0) bits |= Camera.OPCODE_RIGHT;
        if (p.z() - p.x() - 1 > 0) bits |= Camera.OPCODE_LEFT;
        if (p.z() + p.y() - 1 > 0) bits |= Camera.OPCODE_UP;
        if (p.z() - p.y() - 1 > 0) bits |= Camera.OPCODE_DOWN;
        if (p.z() > 0) bits |= Camera.OPCODE_NEAR;
        if (p.z() < -fpd) bits |= Camera.OPCODE_FAR;

        return bits;
    }

    private static clipLineCanonicVolume(
        point0: Vector3Dd,
        point1: Vector3Dd,
        c: Camera,
    ): [Vector3Dd, Vector3Dd] | null {
        const nt: Matrix4x4d = c.getNormalizingTransformation();
        let clippedPoint0: Vector3Dd = nt.multiply(point0);
        let clippedPoint1: Vector3Dd = nt.multiply(point1);
        const fpd: number = (c.getFarPlaneDistance() - c.getNearPlaneDistance()) / c.getNearPlaneDistance();

        let outcode0: number = WireframeRenderer.calculateCanonicalOutcode(clippedPoint0, fpd);
        let outcode1: number = WireframeRenderer.calculateCanonicalOutcode(clippedPoint1, fpd);

        for (;;) {
            if (outcode0 === 0x0 && outcode1 === 0x0) {
                return [clippedPoint0, clippedPoint1];
            }
            if ((outcode0 & outcode1) !== 0x0) {
                return null;
            }

            const outcodeout: number = outcode0 !== 0 ? outcode0 : outcode1;
            let dir: Vector3Dd = clippedPoint1.subtract(clippedPoint0);
            const l: number = dir.length();
            if (l < VSDK.EPSILON) {
                return null;
            }
            dir = dir.multiply(1.0 / l);

            const de: number = outcodeout === outcode1 ? -VSDK.EPSILON : VSDK.EPSILON;
            let m: Vector3Dd | null = null;

            if ((Camera.OPCODE_UP & outcodeout) !== 0x0) {
                const t =
                    (l * (1 - clippedPoint0.z() - clippedPoint0.y())) /
                    (clippedPoint1.z() - clippedPoint0.z() + clippedPoint1.y() - clippedPoint0.y());
                m = clippedPoint0.add(dir.multiply(t + de));
            } else if ((Camera.OPCODE_DOWN & outcodeout) !== 0x0) {
                const t =
                    (l * (clippedPoint0.y() - clippedPoint0.z() + 1)) /
                    (clippedPoint1.z() - clippedPoint0.z() - clippedPoint1.y() + clippedPoint0.y());
                m = clippedPoint0.add(dir.multiply(t + de));
            } else if ((Camera.OPCODE_LEFT & outcodeout) !== 0x0) {
                const t =
                    (l * (clippedPoint0.x() - clippedPoint0.z() + 1)) /
                    (clippedPoint1.z() - clippedPoint0.z() - clippedPoint1.x() + clippedPoint0.x());
                m = clippedPoint0.add(dir.multiply(t + de));
            } else if ((Camera.OPCODE_RIGHT & outcodeout) !== 0x0) {
                const t =
                    (l * (1 - clippedPoint0.z() - clippedPoint0.x())) /
                    (clippedPoint1.z() - clippedPoint0.z() + clippedPoint1.x() - clippedPoint0.x());
                m = clippedPoint0.add(dir.multiply(t + de));
            } else if ((Camera.OPCODE_NEAR & outcodeout) !== 0x0) {
                const t = (-clippedPoint0.z() * l) / (clippedPoint1.z() - clippedPoint0.z());
                m = clippedPoint0.add(dir.multiply(t + de));
            } else if ((Camera.OPCODE_FAR & outcodeout) !== 0x0) {
                const t = ((-fpd - clippedPoint0.z()) * l) / (clippedPoint1.z() - clippedPoint0.z());
                m = clippedPoint0.add(dir.multiply(t + de));
            }

            if (m === null) {
                return null;
            }

            if (outcodeout === outcode0) {
                clippedPoint0 = m;
                outcode0 = WireframeRenderer.calculateCanonicalOutcode(clippedPoint0, fpd);
            } else {
                clippedPoint1 = m;
                outcode1 = WireframeRenderer.calculateCanonicalOutcode(clippedPoint1, fpd);
            }
        }
    }

    /**
    Given a 3D line (with endpoints `cp0` and `cp1`), previously clipped
    against the current view volume, this method projects the line in to the
    projection plane by applying a projection transformation specified by
    `Proj`, and adds the resulting 2D line to the Calligraphic2DBuffer
    `lineSet`.
    */
    private static addLine(
        lineSet: Calligraphic2DBuffer,
        cp0: Vector3Dd,
        cp1: Vector3Dd,
        Proj: Matrix4x4d,
        _c: Camera,
    ): void {
        //-----------------------------------------------------------------
        let hp0: Vector4Dd, hp1: Vector4Dd; // Clipped points in homogeneous space
        let pp0: Vector4Dd, pp1: Vector4Dd; // Projected points

        let f: number;
        f = 1; // f = (c.getFarPlaneDistance() - c.getNearPlaneDistance())/20;

        hp0 = new Vector4Dd(cp0);
        hp1 = new Vector4Dd(cp1);
        pp0 = Proj.multiply(hp0).dividedByW();
        pp1 = Proj.multiply(hp1).dividedByW();
        lineSet.add2DLine(pp0.x() * f, pp0.y() * f, pp1.x() * f, pp1.y() * f);
    }

    private static processBrep(
        body: SimpleBody,
        P: Matrix4x4d,
        outLineSet: Calligraphic2DBuffer,
        inCamera: Camera,
    ): void {
        //-----------------------------------------------------------------
        let brep: PolyhedralBoundedSolid | null;

        if (!(body.getGeometry() instanceof Volume)) {
            return;
        }
        brep = (body.getGeometry() as Volume).exportToPolyhedralBoundedSolid() as PolyhedralBoundedSolid | null;
        if (brep === null) return;

        //-----------------------------------------------------------------
        let i: number;
        let mp0: Vector3Dd | null, mp1: Vector3Dd | null; // Edge points
        let clippedSegment: [Vector3Dd, Vector3Dd] | null; // Clipped points
        let M: Matrix4x4d;

        M = body.getTransformationMatrix();
        for (i = 0; i < brep.getEdgesList().size(); i++) {
            const e: _PolyhedralBoundedSolidEdge = brep.getEdgesList().get(i) as _PolyhedralBoundedSolidEdge;
            let start: number, end: number;
            start = e.getStartingVertexId();
            end = e.getEndingVertexId();
            if (start >= 0 && end >= 0) {
                mp0 = e.rightHalf === null ? null : e.rightHalf.startingVertex.position;
                mp1 = e.leftHalf === null ? null : e.leftHalf.startingVertex.position;
                if (mp0 !== null && mp1 !== null) {
                    mp0 = M.multiply(mp0);
                    mp1 = M.multiply(mp1);
                    clippedSegment = WireframeRenderer.clipLineCanonicVolume(mp0, mp1, inCamera);
                    if (clippedSegment !== null) {
                        WireframeRenderer.addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                    }
                }
            }
        }
    }

    private static processMesh(
        body: SimpleBody,
        P: Matrix4x4d,
        outLineSet: Calligraphic2DBuffer,
        inCamera: Camera,
    ): void {
        let j: number; // subobject index
        let t: number; // triangle index
        let mg: TriangleMeshGroup | null;
        let mesh: TriangleMesh;
        let nt: number;
        let v: Float64Array;
        let tr: Int32Array;
        let M: Matrix4x4d; // Modelview matrix
        let p0: number, p1: number, p2: number;
        let mp0: Vector3Dd, mp1: Vector3Dd; // Mesh points
        let clippedSegment: [Vector3Dd, Vector3Dd] | null; // Clipped points

        mg = GeometryTriangulator.exportToTriangleMeshGroup(body.getGeometry());
        if (mg === null) return;

        mp0 = new Vector3Dd();
        mp1 = new Vector3Dd();
        M = body.getTransformationMatrix();
        for (j = 0; j < mg.getMeshes().length; j++) {
            mesh = mg.getMeshes()[j] as TriangleMesh;
            nt = mesh.getNumTriangles();
            v = mesh.getVertexPositions() as Float64Array;
            tr = mesh.getTriangleIndexes() as Int32Array;
            for (t = 0; t < nt; t++) {
                p0 = tr[3 * t] as number;
                p1 = tr[3 * t + 1] as number;
                p2 = tr[3 * t + 2] as number;

                mp0 = new Vector3Dd(v[3 * p0] as number, v[3 * p0 + 1] as number, v[3 * p0 + 2] as number);
                mp1 = new Vector3Dd(v[3 * p1] as number, v[3 * p1 + 1] as number, v[3 * p1 + 2] as number);
                mp0 = M.multiply(mp0);
                mp1 = M.multiply(mp1);
                clippedSegment = WireframeRenderer.clipLineCanonicVolume(mp0, mp1, inCamera);
                if (clippedSegment !== null) {
                    WireframeRenderer.addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                }

                mp0 = new Vector3Dd(v[3 * p1] as number, v[3 * p1 + 1] as number, v[3 * p1 + 2] as number);
                mp1 = new Vector3Dd(v[3 * p2] as number, v[3 * p2 + 1] as number, v[3 * p2 + 2] as number);
                mp0 = M.multiply(mp0);
                mp1 = M.multiply(mp1);
                clippedSegment = WireframeRenderer.clipLineCanonicVolume(mp0, mp1, inCamera);
                if (clippedSegment !== null) {
                    WireframeRenderer.addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                }

                mp0 = new Vector3Dd(v[3 * p2] as number, v[3 * p2 + 1] as number, v[3 * p2 + 2] as number);
                mp1 = new Vector3Dd(v[3 * p0] as number, v[3 * p0 + 1] as number, v[3 * p0 + 2] as number);
                mp0 = M.multiply(mp0);
                mp1 = M.multiply(mp1);
                clippedSegment = WireframeRenderer.clipLineCanonicVolume(mp0, mp1, inCamera);
                if (clippedSegment !== null) {
                    WireframeRenderer.addLine(outLineSet, clippedSegment[0], clippedSegment[1], P, inCamera);
                }
            }
        }
    }

    public static execute(
        outLineSet: Calligraphic2DBuffer,
        inSimpleBodyArray: ArrayList<SimpleBody>,
        inCamera: Camera,
    ): void {
        //- Calligraphic rendering of lines in to 2D line buffer ----------
        let i: number; // Index inside objects list
        let P: Matrix4x4d; // Projection matrix
        let g: Geometry | null;

        P = new Matrix4x4d();
        P = P.canonicalPerspectiveProjection();

        for (i = 0; i < inSimpleBodyArray.size(); i++) {
            g = (inSimpleBodyArray.get(i) as SimpleBody).getGeometry();
            if (g instanceof Surface) {
                WireframeRenderer.processMesh(inSimpleBodyArray.get(i) as SimpleBody, P, outLineSet, inCamera);
            } else if (g instanceof Solid) {
                WireframeRenderer.processBrep(inSimpleBodyArray.get(i) as SimpleBody, P, outLineSet, inCamera);
            }
        }
    }
}
