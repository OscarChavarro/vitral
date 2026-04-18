import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

public class DebuggerKeyboardInteractionTechniques
{
    public interface Actions
    {
        void requestExit();
        void rebuildSolid();
        void toggleFullscreen();
    }

    public boolean processPressed(DebuggerModel model, KeyEvent event, Actions actions)
    {
        boolean repaint = false;
        boolean handled = false;

        if ( event.keycode == KeyEvent.KEY_ESC ) {
            actions.requestExit();
            return false;
        }

        if ( model.cameraController.processKeyPressedEvent(event) ) {
            repaint = true;
        }
        if ( model.qualityController.processKeyPressedEvent(event) ) {
            System.out.println(model.quality);
            repaint = true;
        }

        switch (event.keycode) {
            // Show vertex numbers
            case KeyEvent.KEY_v -> {
                model.debugVertices = !model.debugVertices;
                handled = true;
            }

            // Full screen
            case KeyEvent.KEY_f -> {
                actions.toggleFullscreen();
                handled = true;
            }

            // Reference frame
            case KeyEvent.KEY_SPACE -> {
                model.showCoordinateSystem = !model.showCoordinateSystem;
                handled = true;
            }

            // Console print
            case KeyEvent.KEY_I -> {
                System.out.println(model.solid);
                if (PolyhedralBoundedSolidValidationEngine.validateIntermediate(model.solid)) {
                    System.out.println("SOLID MODEL IS VALID!");
                } else {
                    System.out.println("SOLID MODEL IS INVALID!");
                }
                handled = true;
            }

            // Highlighted face(s)
            case KeyEvent.KEY_1 -> {
                model.faceIndex--;
                handled = true;
            }
            case KeyEvent.KEY_2 -> {
                model.faceIndex++;
                handled = true;
            }

            // Model selection
            case KeyEvent.KEY_3 -> {
                model.solidModelName = model.solidModelName.previousClamped();
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_4 -> {
                model.solidModelName = model.solidModelName.nextClamped();
                actions.rebuildSolid();
                handled = true;
            }

            // Sphere / cylinder subdivisions
            case KeyEvent.KEY_q -> {
                model.subdivisionCircumference--;
                model.clampSubdivisions();
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_Q -> {
                model.subdivisionCircumference++;
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_w -> {
                model.subdivisionHeight--;
                model.clampSubdivisions();
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_W -> {
                model.subdivisionHeight++;
                actions.rebuildSolid();
                handled = true;
            }

            // Hidden line algorithm debug
            case KeyEvent.KEY_0 -> {
                model.debugEdges = !model.debugEdges;
                handled = true;
            }
            case KeyEvent.KEY_8 -> {
                model.edgeIndex--;
                handled = true;
            }
            case KeyEvent.KEY_9 -> {
                model.edgeIndex++;
                handled = true;
            }

            // CSG special debug cases
            case KeyEvent.KEY_5 -> {
                model.csgOperation = model.csgOperation.nextCircular();
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_6 -> {
                model.csgSample = model.csgSample.nextCircular();
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_d -> {
                model.debugCsg = !model.debugCsg;
                actions.rebuildSolid();
                handled = true;
            }
        }

        model.clampFaceIndex();
        if ( model.edgeIndex < -3 ) {
            model.edgeIndex = -3;
        }
        model.clampSubdivisions();

        return repaint || handled;
    }
}
