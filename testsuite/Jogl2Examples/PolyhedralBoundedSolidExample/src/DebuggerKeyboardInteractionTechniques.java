import vsdk.toolkit.gui.KeyEvent;
import models.DebuggerModel;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

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

        if ( model.getCameraController().processKeyPressedEvent(event) ) {
            repaint = true;
        }
        if ( model.getQualityController().processKeyPressedEvent(event) ) {
            System.out.println(model.getQuality());
            repaint = true;
        }

        switch (event.keycode) {
            // Show vertex numbers
            case KeyEvent.KEY_v -> {
                model.setDebugVertices(model.notDebugVertices());
                handled = true;
            }

            // Full screen
            case KeyEvent.KEY_f -> {
                actions.toggleFullscreen();
                handled = true;
            }

            // Reference frame
            case KeyEvent.KEY_SPACE -> {
                model.setShowCoordinateSystem(!model.isShowCoordinateSystem());
                handled = true;
            }

            // Console print
            case KeyEvent.KEY_I -> {
                System.out.println(model.getSolid());
                if (PolyhedralBoundedSolidValidationEngine.validateIntermediate(model.getSolid())) {
                    System.out.println("SOLID MODEL IS VALID!");
                } else {
                    System.out.println("SOLID MODEL IS INVALID!");
                }
                handled = true;
            }

            // Highlighted face(s)
            case KeyEvent.KEY_1 -> {
                model.setFaceIndex(model.getFaceIndex() - 1);
                handled = true;
            }
            case KeyEvent.KEY_2 -> {
                model.setFaceIndex(model.getFaceIndex() + 1);
                handled = true;
            }

            // Model selection
            case KeyEvent.KEY_3 -> {
                model.setSolidModelName(model.getSolidModelName().previousClamped());
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_4 -> {
                model.setSolidModelName(model.getSolidModelName().nextClamped());
                actions.rebuildSolid();
                handled = true;
            }

            // Sphere / cylinder subdivisions
            case KeyEvent.KEY_q -> {
                model.setSubdivisionCircumference(model.getSubdivisionCircumference() - 1);
                model.clampSubdivisions();
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_Q -> {
                model.setSubdivisionCircumference(model.getSubdivisionCircumference() + 1);
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_w -> {
                model.setSubdivisionHeight(model.getSubdivisionHeight() - 1);
                model.clampSubdivisions();
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_W -> {
                model.setSubdivisionHeight(model.getSubdivisionHeight() + 1);
                actions.rebuildSolid();
                handled = true;
            }

            // Hidden line algorithm debug
            case KeyEvent.KEY_0 -> {
                model.setDebugEdges(!model.isDebugEdges());
                handled = true;
            }
            case KeyEvent.KEY_8 -> {
                model.setEdgeIndex(model.getEdgeIndex() - 1);
                handled = true;
            }
            case KeyEvent.KEY_9 -> {
                model.setEdgeIndex(model.getEdgeIndex() + 1);
                handled = true;
            }

            // CSG special debug cases
            case KeyEvent.KEY_5 -> {
                model.setCsgOperation(model.getCsgOperation().nextCircular());
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_6 -> {
                model.setCsgSample(model.getCsgSample().nextCircular());
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_d -> {
                model.setDebugCsg(!model.isDebugCsg());
                actions.rebuildSolid();
                handled = true;
            }
            case KeyEvent.KEY_e -> {
                if ( model.usesKurlanderBowlSingleMotifControls() ) {
                    model.setKurlanderBowlSingleMotifIndex(
                        model.getKurlanderBowlSingleMotifIndex() - 1);
                    actions.rebuildSolid();
                    handled = true;
                }
            }
            case KeyEvent.KEY_E -> {
                if ( model.usesKurlanderBowlSingleMotifControls() ) {
                    model.setKurlanderBowlSingleMotifIndex(
                        model.getKurlanderBowlSingleMotifIndex() + 1);
                    actions.rebuildSolid();
                    handled = true;
                }
            }
        }

        model.clampFaceIndex();
        if ( model.getEdgeIndex() < -3 ) {
            model.setEdgeIndex(-3);
        }
        model.clampSubdivisions();

        return repaint || handled;
    }
}
