// Basic Node classes
import { readFileSync } from "node:fs";

// VSDK classes
import { Double, Integer, Polygon2D } from "@vitral/base";

export class PolygonReader {
    public read(fileName: string): Polygon2D {
        const lines: string[] = readFileSync(fileName, "utf8").split("\n");
        const tokens: string[] = [];

        for (const line of lines) {
            const trimmedLine: string = line.trim();
            if (trimmedLine.length === 0) {
                continue;
            }
            for (const token of trimmedLine.split(/\s+/u)) {
                if (token.length > 0) {
                    tokens.push(token);
                }
            }
        }

        let tokenIndex = 0;
        const contourCount: number = Integer.parseInt(tokens[tokenIndex++]!);
        const polygon: Polygon2D = new Polygon2D();
        polygon.loops.length = 0;

        let contourIndex: number;
        for (contourIndex = 0; contourIndex < contourCount; contourIndex++) {
            polygon.nextLoop();
            const pointCount: number = Integer.parseInt(tokens[tokenIndex++]!);
            let pointIndex: number;
            for (pointIndex = 0; pointIndex < pointCount; pointIndex++) {
                const x: number = Double.parseDouble(tokens[tokenIndex++]!);
                const y: number = Double.parseDouble(tokens[tokenIndex++]!);
                polygon.addVertex(x, y);
            }
        }

        if (polygon.loops.length > 0 && polygon.loops[0]!.vertices.length === 0) {
            polygon.loops.splice(0, 1);
        }

        return polygon;
    }
}
