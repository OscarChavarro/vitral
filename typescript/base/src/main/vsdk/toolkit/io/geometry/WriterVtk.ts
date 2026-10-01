import { BufferedOutputStream } from "../../../../java/io/BufferedOutputStream.js";
import type { OutputStream } from "../../../../java/io/OutputStream.js";
import { Logger } from "../../common/logging/Logger.js";
import { VSDK } from "../../common/VSDK.js";
import type { Geometry } from "../../environment/geometry/Geometry.js";
import { FunctionalExplicitSurface } from "../../environment/geometry/surface/FunctionalExplicitSurface.js";
import { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";
import type { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { PersistenceElement } from "../PersistenceElement.js";

/**
Port of `vsdk.toolkit.io.geometry.WriterVtk`: writes the first triangle mesh
of a scene (or the internal mesh of a functional explicit surface) as a
Kitware VTK legacy binary file (positions scaled by 1000, big endian).
*/
export class WriterVtk extends PersistenceElement {
    private static exportMesh(inOutputStream: OutputStream, mesh: TriangleMesh): void {
        const v: Float64Array = mesh.getVertexPositions()!;
        const n: Float64Array | null = mesh.getVertexNormals();
        const t: Int32Array = mesh.getTriangleIndexes()!;

        let line: string;
        PersistenceElement.writeAsciiLine(inOutputStream, "DATASET POLYDATA");

        //-----------------------------------------------------------------
        line = "POINTS " + Math.trunc(v.length / 3) + " float";
        PersistenceElement.writeAsciiLine(inOutputStream, line);
        let val: number;
        for (let i: number = 0; i < v.length; i++) {
            val = Math.fround(v[i]! * 1000.0);
            PersistenceElement.writeFloatBE(inOutputStream, val);
        }
        PersistenceElement.writeAsciiLine(inOutputStream, "");

        //-----------------------------------------------------------------
        const nt: number = Math.trunc(t.length / 3);
        line = "POLYGONS " + nt + " " + nt * 4;
        PersistenceElement.writeAsciiLine(inOutputStream, line);
        const p: number = 3;
        for (let i: number = 0; i < nt; i++) {
            PersistenceElement.writeLongBE(inOutputStream, p);
            PersistenceElement.writeLongBE(inOutputStream, t[3 * i + 0]!);
            PersistenceElement.writeLongBE(inOutputStream, t[3 * i + 1]!);
            PersistenceElement.writeLongBE(inOutputStream, t[3 * i + 2]!);
        }
        PersistenceElement.writeAsciiLine(inOutputStream, "");

        //-----------------------------------------------------------------
        if (n !== null) {
            line = "CELL_DATA " + nt;
            PersistenceElement.writeAsciiLine(inOutputStream, line);
            line = "POINT_DATA " + Math.trunc(v.length / 3);
            PersistenceElement.writeAsciiLine(inOutputStream, line);
            line = "NORMALS Normals float";
            PersistenceElement.writeAsciiLine(inOutputStream, line);
            for (let i: number = 0; i < Math.trunc(v.length / 3); i++) {
                val = Math.fround(n[3 * i + 0]!);
                PersistenceElement.writeFloatBE(inOutputStream, val);
                val = Math.fround(n[3 * i + 1]!);
                PersistenceElement.writeFloatBE(inOutputStream, val);
                val = Math.fround(n[3 * i + 2]!);
                PersistenceElement.writeFloatBE(inOutputStream, val);
            }
            PersistenceElement.writeAsciiLine(inOutputStream, "");
        }
        inOutputStream.close();
    }

    public static exportEnvironment(inOutputStream: OutputStream, inScene: SimpleScene): void {
        let bos: OutputStream;
        if (inOutputStream instanceof BufferedOutputStream) {
            bos = inOutputStream;
        }
        else {
            bos = new BufferedOutputStream(inOutputStream);
        }

        //-----------------------------------------------------------------
        PersistenceElement.writeAsciiLine(bos, "# vtk DataFile Version 3.0");
        PersistenceElement.writeAsciiLine(bos, "vtk output");
        PersistenceElement.writeAsciiLine(bos, "BINARY");

        //-----------------------------------------------------------------
        let exported: boolean = false;
        const objs = inScene.getSimpleBodies();

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
            else {
                Logger.reportMessage(null, VSDK.WARNING, "WriterVtk.exportEnvironment",
                    "Current writer implementation only supports writing of triangle meshes. Object skipped.");
            }

            //-----------------------------------------------------------------
            if (mesh !== null) {
                if (!exported) {
                    WriterVtk.exportMesh(bos, mesh);
                    exported = true;
                }
                else {
                    Logger.reportMessage(null, VSDK.WARNING, "WriterVtk.exportEnvironment",
                        "Current writer implementation only supports writing ONE triangle meshes. Only first mesh exported, remaining meshes skipped.");
                }
            }
        }
    }
}
