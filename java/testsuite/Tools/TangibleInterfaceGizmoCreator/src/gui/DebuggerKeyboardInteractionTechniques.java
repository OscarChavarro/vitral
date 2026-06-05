package gui;

import models.TangibleInterfaceGizmosModel;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.gui.KeyEvent;

public class DebuggerKeyboardInteractionTechniques
{
    public interface Actions
    {
        void requestExit();
        void rebuildSolid();
        void exportCurrentModelStlIfMissing();
        void toggleFullscreen();
        void requestScreenshot();
        void requestStlExport();
    }

    public boolean processPressed(
        TangibleInterfaceGizmosModel model,
        KeyEvent event,
        Actions actions)
    {
        boolean repaint = false;
        boolean handled = false;

        if ( event.keycode == KeyEvent.KEY_ESC ) {
            actions.requestExit();
            return false;
        }

        if ( model.getCameraController().processKeyPressedEvent(event) ) {
            repaint = true;
        }
        if ( model.getQualityController().processKeyPressedEvent(event) ) {
            System.out.println(model.getQuality());
            repaint = true;
        }

        switch ( event.keycode ) {
          case KeyEvent.KEY_f:
            actions.toggleFullscreen();
            handled = true;
            break;
          case KeyEvent.KEY_SPACE:
            model.setShowCoordinateSystem(!model.isShowCoordinateSystem());
            handled = true;
            break;
          case KeyEvent.KEY_I:
            System.out.println(model.getSolid());
            if ( PolyhedralBoundedSolidValidationEngine
                     .validateIntermediate(model.getSolid()) ) {
                System.out.println("SOLID MODEL IS VALID!");
            }
            else {
                System.out.println("SOLID MODEL IS INVALID!");
            }
            handled = true;
            break;
          case KeyEvent.KEY_PERIOD:
            actions.requestScreenshot();
            handled = true;
            break;
          case KeyEvent.KEY_m:
          case KeyEvent.KEY_M:
            actions.requestStlExport();
            handled = true;
            break;
          case KeyEvent.KEY_1:
            model.setSolidModelName(model.getSolidModelName().previousClamped());
            actions.rebuildSolid();
            actions.exportCurrentModelStlIfMissing();
            handled = true;
            break;
          case KeyEvent.KEY_2:
            model.setSolidModelName(model.getSolidModelName().nextClamped());
            actions.rebuildSolid();
            actions.exportCurrentModelStlIfMissing();
            handled = true;
            break;
          case KeyEvent.KEY_q:
            model.setSubdivisionCircumference(model.getSubdivisionCircumference() - 1);
            model.clampSubdivisions();
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_Q:
            model.setSubdivisionCircumference(model.getSubdivisionCircumference() + 1);
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_w:
            model.setSubdivisionHeight(model.getSubdivisionHeight() - 1);
            model.clampSubdivisions();
            actions.rebuildSolid();
            handled = true;
            break;
          case KeyEvent.KEY_W:
            model.setSubdivisionHeight(model.getSubdivisionHeight() + 1);
            actions.rebuildSolid();
            handled = true;
            break;
          default:
            break;
        }

        model.clampSubdivisions();
        return repaint || handled;
    }
}
