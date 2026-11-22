// Port of java/base/src/test/vsdk/toolkit/render/gizmo/CameraControlGizmo2DPainterTest.java
import { describe, expect, it } from "vitest";

import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { CameraControlGizmo } from "vsdk/toolkit/gui/gizmo/CameraControlGizmo.js";
import { CameraControlGizmo2DPainter } from "vsdk/toolkit/render/gizmo/CameraControlGizmo2DPainter.js";
import { Gizmo2DDrawList } from "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.js";

/**
Exercises the 2D presentation of the `CameraControlGizmo`: hidden line
removal of its parts with Appel's algorithm, filled faces and labels, all in
its HUD area.
*/
describe("CameraControlGizmo2DPainterTest", () => {
    function cornerCamera(): Camera {
        const camera = new Camera();
        camera.setPosition(new Vector3Dd(5, -5, 5));
        camera.setRotation(CameraControlGizmo.createCameraRotation(new Vector3Dd(-1, 1, -1), new Vector3Dd(0, 0, 1))!);
        camera.setFocusedPositionDirect(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    it("given_cornerView_when_painted_then_visibleLinesFacesAndThreeLabelsInHud", () => {
        const gizmo = new CameraControlGizmo();
        gizmo.setCamera(cornerCamera());
        gizmo.placeInViewport(400, 80, 20, null);
        const painter = new CameraControlGizmo2DPainter();
        const list = new Gizmo2DDrawList();

        painter.paint(list, gizmo, null);

        let lines = 0,
            polygons = 0;
        const labels: string[] = [];
        const minX = gizmo.getHudX() - 1,
            maxX = gizmo.getHudX() + gizmo.getSizeInPixels() + 1;
        const minY = gizmo.getHudY() - 1,
            maxY = gizmo.getHudY() + gizmo.getSizeInPixels() + 1;
        for (const p of list.getPrimitivesInDrawingOrder()) {
            switch (p.getKind()) {
                case Gizmo2DDrawList.Kind.LINE:
                    lines++;
                    break;
                case Gizmo2DDrawList.Kind.POLYGON:
                    polygons++;
                    break;
                case Gizmo2DDrawList.Kind.LABEL:
                    labels.push(p.getText()!);
                    break;
            }
            for (let i = 0; i < p.getNumberOfVertices(); i++) {
                expect(p.getX(i)).toBeGreaterThanOrEqual(minX);
                expect(p.getX(i)).toBeLessThanOrEqual(maxX);
                expect(p.getY(i)).toBeGreaterThanOrEqual(minY);
                expect(p.getY(i)).toBeLessThanOrEqual(maxY);
            }
        }
        expect(lines).toBeGreaterThan(50);
        expect(polygons).toBeGreaterThan(26);
        expect([...labels].sort()).toEqual(["FRONT", "RIGHT", "TOP"]);

        // The lines are kept while the orientation does not change
        const start = performance.now();
        list.clear();
        painter.paint(list, gizmo, null);
        expect(performance.now() - start).toBeLessThan(200.0);
    });
});
