import { Double } from "../../../../java/lang/Double.js";
import type { OutputStream } from "../../../../java/io/OutputStream.js";
import type { Geometry } from "../../environment/geometry/Geometry.js";
import { FunctionalExplicitSurface } from "../../environment/geometry/surface/FunctionalExplicitSurface.js";
import { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";
import type { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { PersistenceElement } from "../PersistenceElement.js";

class _WriterGtsEdge extends PersistenceElement {
    public from: number = 0;
    public to: number = 0;
}

class _WriterGtsTriangle extends PersistenceElement {
    public point0: number = 0;
    public point1: number = 0;
    public point2: number = 0;
}

/**
Port of `vsdk.toolkit.io.geometry.WriterGts`: writes the triangle meshes of a
scene (and the internal meshes of its functional explicit surfaces) as GNU
Triangulated Surface (GTS) text: vertices, edges and faces made of edges.
*/
export class WriterGts extends PersistenceElement {
    private static addEdge(edges: _WriterGtsEdge[], from: number, to: number): number {
        for (let i: number = 0; i < edges.length; i++) {
            const e: _WriterGtsEdge = edges[i]!;
            if ((e.from === from && e.to === to) ||
                (e.from === to && e.to === from)) {
                return i;
            }
        }

        const i: number = edges.length;
        const e: _WriterGtsEdge = new _WriterGtsEdge();
        e.from = from;
        e.to = to;
        edges.push(e);
        return i;
    }

    private static exportMesh(inOutputStream: OutputStream, mesh: TriangleMesh, offset: number): number {
        const nv: number = mesh.getNumVertices();
        const nt: number = mesh.getNumTriangles();
        const edges: _WriterGtsEdge[] = [];
        const triangles: _WriterGtsTriangle[] = [];

        //- Compute edges -------------------------------------------------
        const t: Int32Array = mesh.getTriangleIndexes()!;
        for (let i: number = 0; i < nt; i++) {
            const tt: _WriterGtsTriangle = new _WriterGtsTriangle();
            tt.point0 = WriterGts.addEdge(edges, t[3 * i]!, t[3 * i + 1]!);
            tt.point1 = WriterGts.addEdge(edges, t[3 * i + 1]!, t[3 * i + 2]!);
            tt.point2 = WriterGts.addEdge(edges, t[3 * i + 2]!, t[3 * i]!);
            triangles.push(tt);
        }

        //- Write GTS header ----------------------------------------------
        PersistenceElement.writeAsciiLine(inOutputStream, "" + nv + " " + edges.length + " " + triangles.length +
            " GtsSurface GtsFace GtsEdge GtsVertex");

        //- Write vertices ------------------------------------------------
        const v: Float64Array = mesh.getVertexPositions()!;
        for (let i: number = 0; i < nv; i++) {
            PersistenceElement.writeAsciiLine(inOutputStream, Double.toString(v[3 * i]!) + " " +
                Double.toString(v[3 * i + 1]!) + " " + Double.toString(v[3 * i + 2]!));
        }

        //- Write edges ---------------------------------------------------
        for (const e of edges) {
            PersistenceElement.writeAsciiLine(inOutputStream, "" + (e.from + 1) + " " + (e.to + 1));
        }

        //- Write triangles -----------------------------------------------
        for (const tt of triangles) {
            PersistenceElement.writeAsciiLine(inOutputStream, "" + (tt.point0 + 1) + " " + (tt.point1 + 1) + " " +
                (tt.point2 + 1));
        }

        return offset + nv;
    }

    public static exportEnvironment(inOutputStream: OutputStream, inScene: SimpleScene): void {
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
                baseVertexStart += WriterGts.exportMesh(inOutputStream, mesh, baseVertexStart);
            }
        }
    }
}
