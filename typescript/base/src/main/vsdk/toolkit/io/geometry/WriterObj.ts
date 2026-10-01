import { Math as JavaMath } from "../../../../java/lang/Math.js";
import { Double } from "../../../../java/lang/Double.js";
import type { OutputStream } from "../../../../java/io/OutputStream.js";
import { Matrix4x4d } from "../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Geometry } from "../../environment/geometry/Geometry.js";
import { FunctionalExplicitSurface } from "../../environment/geometry/surface/FunctionalExplicitSurface.js";
import { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";
import type { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { PersistenceElement } from "../PersistenceElement.js";

/**
Port of `vsdk.toolkit.io.geometry.WriterObj`: writes the triangle meshes of a
scene (and the internal meshes of its functional explicit surfaces) as an
Alias/Wavefront OBJ text, rotated -90 degrees around X (OBJ is Y up).
*/
export class WriterObj extends PersistenceElement {
    private static exportMesh(inOutputStream: OutputStream, mesh: TriangleMesh, offset: number): number {
        const nv: number = mesh.getNumVertices();
        const nt: number = mesh.getNumTriangles();
        const vp: Float64Array = mesh.getVertexPositions()!;
        const vn: Float64Array | null = mesh.getVertexNormals();
        const vuv: Float64Array | null = mesh.getVertexUvs();
        let R: Matrix4x4d = new Matrix4x4d();

        R = R.axisRotation(JavaMath.toRadians(-90), new Vector3Dd(1, 0, 0));

        //-----------------------------------------------------------------
        PersistenceElement.writeAsciiLine(inOutputStream, "# " + nv + " vertex positions");
        for (let i: number = 0; i < nv; i++) {
            const vpi: Vector3Dd = new Vector3Dd(vp[3 * i]!, vp[3 * i + 1]!, vp[3 * i + 2]!);
            const p: Vector3Dd = R.multiply(vpi);
            PersistenceElement.writeAsciiLine(inOutputStream, "v " + Double.toString(p.x()) + " " +
                Double.toString(p.y()) + " " + Double.toString(p.z()));
        }

        //-----------------------------------------------------------------
        PersistenceElement.writeAsciiLine(inOutputStream, "# " + nv + " vertex texture coordinates");
        for (let i: number = 0; i < nv && vuv !== null; i++) {
            PersistenceElement.writeAsciiLine(inOutputStream, "vt " + Double.toString(vuv[2 * i]!) + " " +
                Double.toString(vuv[2 * i + 1]!));
        }

        //-----------------------------------------------------------------
        PersistenceElement.writeAsciiLine(inOutputStream, "# " + nv + " vertex normals");
        for (let i: number = 0; i < nv && vn !== null; i++) {
            const vni: Vector3Dd = new Vector3Dd(vn[3 * i]!, vn[3 * i + 1]!, vn[3 * i + 2]!);
            const n: Vector3Dd = R.multiply(vni);
            PersistenceElement.writeAsciiLine(inOutputStream, "vn " + Double.toString(n.x()) + " " +
                Double.toString(n.y()) + " " + Double.toString(n.z()));
        }

        //-----------------------------------------------------------------
        const t: Int32Array = mesh.getTriangleIndexes()!;

        PersistenceElement.writeAsciiLine(inOutputStream, "# " + nt + " triangles");
        PersistenceElement.writeAsciiLine(inOutputStream, "o NewObject");
        for (let i: number = 0; i < nt; i++) {
            const n0: number = t[3 * i]! + offset + 1;
            const n1: number = t[3 * i + 1]! + offset + 1;
            const n2: number = t[3 * i + 2]! + offset + 1;
            if (vuv !== null && vn !== null) {
                PersistenceElement.writeAsciiLine(inOutputStream, "f " +
                    n0 + "/" + n0 + "/" + n0 + " " +
                    n1 + "/" + n1 + "/" + n1 + " " +
                    n2 + "/" + n2 + "/" + n2);
            }
            else {
                PersistenceElement.writeAsciiLine(inOutputStream, "f " +
                    n0 + " " +
                    n1 + " " +
                    n2);
            }
        }

        return offset + nv;
    }

    public static exportEnvironment(inOutputStream: OutputStream, inScene: SimpleScene): void {
        //-----------------------------------------------------------------
        PersistenceElement.writeAsciiLine(inOutputStream, "# OBJ File generated with VitralSDK.");
        PersistenceElement.writeAsciiLine(inOutputStream, "# http://sophia.javeriana.edu.co/~ochavarr");
        //-----------------------------------------------------------------
        const objs = inScene.getSimpleBodies();
        let baseVertexStart: number = 0;

        for (let i: number = 0; i < objs.size(); i++) {

            //-----------------------------------------------------------------
            const g: Geometry | null = objs.get(i).getGeometry();
            let mesh: TriangleMesh | null = null;
            if (g instanceof FunctionalExplicitSurface) {
                mesh = g.getInternalTriangleMesh();
            }
            else if (g instanceof TriangleMesh) {
                mesh = g;
            }

            //-----------------------------------------------------------------
            if (mesh !== null) {
                baseVertexStart += WriterObj.exportMesh(inOutputStream, mesh, baseVertexStart);
            }
        }
    }
}
