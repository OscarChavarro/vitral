import {
  Polygon2D,
  Vector3Dd,
  WeilerAthertonPolygonClipper,
  type _DoubleLinkedListNode,
  type _Polygon2DWA,
  type _VertexNode2D,
} from '@vitral/base';
import type { PolygonClippingDebuggerModel } from './polygon-clipping-debugger-model';
import type { PolygonClippingTestCase } from './polygon-clipping-test-case';

/**
 * Bounding rectangle of `PolygonClippingModelingTools.Bounds2D`, a private
 * static nested class of the Java file.
 */
class Bounds2D {
  initialized = false;
  minX = 0.0;
  maxX = 0.0;
  minY = 0.0;
  maxY = 0.0;

  include(x: number, y: number): void {
    if (!this.initialized) {
      this.initialized = true;
      this.minX = this.maxX = x;
      this.minY = this.maxY = y;
      return;
    }
    if (x < this.minX) {
      this.minX = x;
    }
    if (x > this.maxX) {
      this.maxX = x;
    }
    if (y < this.minY) {
      this.minY = y;
    }
    if (y > this.maxY) {
      this.maxY = y;
    }
  }
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/model/PolygonClippingModelingTools.java`.
 *
 * `rebuildScene` runs the Weiler--Atherton clipper over the two contour files
 * of the current fixture and stores its four results plus the two annotated
 * polygons the clipper kept; `calculateSceneCenter` frames the three panels the
 * renderer lays out; and `countPairedVertices` counts the intersection pairs
 * the HUD reports. All three are unchanged, the switch included: the union and
 * the two differences each leave the secondary result empty, and `A_MINUS_B`
 * and `B_MINUS_A` differ only in which polygon is handed in as the clip.
 *
 * One boundary: `buildPolygon` reads its file. The read moved to
 * `io.PolygonClippingReader`, so the text arrives already fetched and this
 * class parses it exactly as Java does — whitespace-separated tokens, the
 * contour count first, then a vertex count and that many coordinate pairs per
 * contour, with `yOffset` added to every ordinate.
 */
export class PolygonClippingModelingTools {
  private static readonly CLIP_Y_OFFSET = -1.0;

  private constructor() {}

  static rebuildScene(
    model: PolygonClippingDebuggerModel,
    polygonTexts: ReadonlyMap<string, string>,
  ): void {
    const testCase: PolygonClippingTestCase = model.getCurrentTestCase();
    const clipper = new WeilerAthertonPolygonClipper();
    const operationResult = new Polygon2D();
    const secondaryResult = new Polygon2D();
    const scratch = new Polygon2D();

    try {
      model.setClipPolygon(
        PolygonClippingModelingTools.buildPolygon(
          polygonTexts,
          testCase.clipFile,
          PolygonClippingModelingTools.CLIP_Y_OFFSET,
        ),
      );
      model.setSubjectPolygon(
        PolygonClippingModelingTools.buildPolygon(polygonTexts, testCase.subjectFile, 0.0),
      );
    } catch (error) {
      model.setErrorState(
        'Failed to load polygon file: ' + (error instanceof Error ? error.message : String(error)),
      );
      return;
    }
    model.setInnerPolygon(operationResult);
    model.setOuterPolygon(secondaryResult);

    switch (model.getOperation()) {
      case 'INTERSECTION':
        clipper.clipPolygons(
          model.getClipPolygon()!,
          model.getSubjectPolygon()!,
          operationResult,
          secondaryResult,
        );
        break;
      case 'UNION':
        clipper.unionPolygons(model.getClipPolygon(), model.getSubjectPolygon(), operationResult);
        PolygonClippingModelingTools.resetPolygonToEmpty(secondaryResult);
        break;
      case 'A_MINUS_B':
        clipper.clipPolygons(
          model.getSubjectPolygon()!,
          model.getClipPolygon()!,
          scratch,
          operationResult,
        );
        PolygonClippingModelingTools.resetPolygonToEmpty(secondaryResult);
        break;
      case 'B_MINUS_A':
        clipper.clipPolygons(
          model.getClipPolygon()!,
          model.getSubjectPolygon()!,
          scratch,
          operationResult,
        );
        PolygonClippingModelingTools.resetPolygonToEmpty(secondaryResult);
        break;
    }

    model.setClipPolygonWA(clipper.getClipPolyWA());
    model.setSubjectPolygonWA(clipper.getSubjectPolyWA());
  }

  static calculateSceneCenter(model: PolygonClippingDebuggerModel): Vector3Dd {
    const bounds = new Bounds2D();

    PolygonClippingModelingTools.expandBounds(bounds, model.getClipPolygon());
    PolygonClippingModelingTools.expandBounds(bounds, model.getSubjectPolygon());
    PolygonClippingModelingTools.expandBounds(bounds, model.getInnerPolygon());
    PolygonClippingModelingTools.expandBounds(bounds, model.getOuterPolygon());

    if (!bounds.initialized) {
      return new Vector3Dd(0, 0, 0);
    }

    const panelWidth: number = Math.max(1.0, bounds.maxX - bounds.minX);
    const panelDepth: number = Math.max(1.0, bounds.maxY - bounds.minY);
    let centerX: number = (bounds.minX + bounds.maxX) / 2.0;
    let centerZ: number = (bounds.minY + bounds.maxY) / 2.0;

    // The renderer shows the outer result on a panel translated in +X and
    // the inner result on a panel translated in -Z.
    centerX += panelWidth * 0.4;
    centerZ -= panelDepth * 0.2;

    return new Vector3Dd(centerX, 0, centerZ);
  }

  static countPairedVertices(polygon: _Polygon2DWA | null): number {
    let paired = 0;

    if (polygon === null) {
      return 0;
    }

    for (const loop of polygon.loops) {
      const head: _DoubleLinkedListNode<_VertexNode2D> | null = loop.vertices.getHead();
      if (head === null) {
        continue;
      }
      let cursor: _DoubleLinkedListNode<_VertexNode2D> = head;
      let j = 0;
      do {
        if (cursor.data.pairNode !== null) {
          paired++;
        }
        cursor = cursor.next;
        j++;
      } while (cursor !== head && j <= loop.vertices.size() + 1);
    }

    return Math.trunc(paired / 2);
  }

  private static buildPolygon(
    polygonTexts: ReadonlyMap<string, string>,
    filename: string,
    yOffset: number,
  ): Polygon2D | null {
    const content: string | undefined = polygonTexts.get(filename);
    if (content === undefined) {
      throw new Error(filename);
    }

    const tokens: string[] = [];
    for (const line of content.split('\n')) {
      const trimmed: string = line.trim();
      if (trimmed.length === 0) continue;
      for (const t of trimmed.split(/\s+/)) {
        if (t.length > 0) tokens.push(t);
      }
    }

    let idx = 0;
    const numberOfContours: number = Number.parseInt(tokens[idx++]!, 10);
    let polygon: Polygon2D | null = null;

    for (let c = 0; c < numberOfContours; c++) {
      if (polygon === null) {
        polygon = new Polygon2D();
      } else {
        polygon.nextLoop();
      }
      const numberOfPoints: number = Number.parseInt(tokens[idx++]!, 10);
      for (let i = 0; i < numberOfPoints; i++) {
        const x: number = Number.parseFloat(tokens[idx++]!);
        const y: number = Number.parseFloat(tokens[idx++]!);
        polygon.addVertex(x, y + yOffset);
      }
    }
    return polygon;
  }

  private static expandBounds(bounds: Bounds2D, polygon: Polygon2D | null): void {
    if (polygon === null) {
      return;
    }

    for (const loop of polygon.loops) {
      for (const vertex of loop.vertices) {
        bounds.include(vertex.x, vertex.y);
      }
    }
  }

  private static resetPolygonToEmpty(polygon: Polygon2D): void {
    polygon.loops.length = 0;
    polygon.nextLoop();
  }
}
