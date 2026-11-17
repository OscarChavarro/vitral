package vsdk.toolkit.render.gizmo;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.CameraControlGizmo;

import static org.assertj.core.api.Assertions.assertThat;

/**
Exercises the 2D presentation of the `CameraControlGizmo`: hidden line
removal of its parts with Appel's algorithm, filled faces and labels, all in
its HUD area.
*/
class CameraControlGizmo2DPainterTest
{
    private static Camera cornerCamera()
    {
        Camera camera = new Camera();
        camera.setPosition(new Vector3Dd(5, -5, 5));
        camera.setRotation(CameraControlGizmo.createCameraRotation(
            new Vector3Dd(-1, 1, -1), new Vector3Dd(0, 0, 1)));
        camera.setFocusedPositionDirect(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    @Test
    void given_cornerView_when_painted_then_visibleLinesFacesAndThreeLabelsInHud()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        gizmo.setCamera(cornerCamera());
        gizmo.placeInViewport(400, 80, 20, null);
        CameraControlGizmo2DPainter painter = new CameraControlGizmo2DPainter();
        Gizmo2DDrawList list = new Gizmo2DDrawList();

        painter.paint(list, gizmo, null);

        int lines = 0, polygons = 0;
        java.util.List<String> labels = new java.util.ArrayList<String>();
        double minX = gizmo.getHudX() - 1, maxX = gizmo.getHudX() + gizmo.getSizeInPixels() + 1;
        double minY = gizmo.getHudY() - 1, maxY = gizmo.getHudY() + gizmo.getSizeInPixels() + 1;
        for ( Gizmo2DDrawList.Primitive p : list.getPrimitivesInDrawingOrder() ) {
            switch ( p.getKind() ) {
              case LINE -> lines++;
              case POLYGON -> polygons++;
              case LABEL -> labels.add(p.getText());
            }
            for ( int i = 0; i < p.getNumberOfVertices(); i++ ) {
                assertThat(p.getX(i)).isBetween(minX, maxX);
                assertThat(p.getY(i)).isBetween(minY, maxY);
            }
        }
        assertThat(lines).isGreaterThan(50);
        assertThat(polygons).isGreaterThan(26);
        assertThat(labels).containsExactlyInAnyOrder("TOP", "FRONT", "RIGHT");

        // The lines are kept while the orientation does not change
        long start = System.nanoTime();
        list.clear();
        painter.paint(list, gizmo, null);
        assertThat((System.nanoTime() - start) / 1.0e6).isLessThan(200.0);
    }
}
