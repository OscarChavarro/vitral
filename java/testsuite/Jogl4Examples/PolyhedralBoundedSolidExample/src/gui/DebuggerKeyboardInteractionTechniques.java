package gui;

import java.util.LinkedHashSet;
import java.util.Set;
import models.DebuggerModel;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.gui.KeyEvent;

public class DebuggerKeyboardInteractionTechniques
{
    private final CameraFaceFocusInteraction cameraFaceFocusInteraction;

    public DebuggerKeyboardInteractionTechniques()
    {
        cameraFaceFocusInteraction = new CameraFaceFocusInteraction();
    }

    public interface Actions
    {
        void requestExit();
        void rebuildSolid();
        void toggleFullscreen();
        void toggleSolidAnimation();
        void requestScreenshot();
        void requestStlExport();
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
            case KeyEvent.KEY_g -> {
                actions.toggleFullscreen();
                handled = true;
            }
            case KeyEvent.KEY_h -> {
                model.setHudEnabled(!model.isHudEnabled());
                handled = true;
            }
            case KeyEvent.KEY_r -> {
                actions.toggleSolidAnimation();
                handled = true;
            }

            // Reference frame
            case KeyEvent.KEY_SPACE -> {
                model.setShowCoordinateSystem(!model.isShowCoordinateSystem());
                handled = true;
            }

            // Console print
            case KeyEvent.KEY_I -> {
                printSolidForCurrentFaceSelection(model);
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
            case KeyEvent.KEY_c -> {
                handled = cameraFaceFocusInteraction.focusSelectedFace(model);
            }
            case KeyEvent.KEY_PERIOD -> {
                actions.requestScreenshot();
                handled = true;
            }
            case KeyEvent.KEY_m, KeyEvent.KEY_M -> {
                actions.requestStlExport();
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
                model.cycleAppelDisplayMode();
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

    private static void printSolidForCurrentFaceSelection(DebuggerModel model)
    {
        PolyhedralBoundedSolid solid = model.getSolid();
        if ( solid == null ) {
            System.out.println((Object)null);
            return;
        }

        int selectedFaceIndex = model.getFaceIndex();
        if ( selectedFaceIndex < 0 || solid.getPolygonsList() == null ) {
            System.out.println(solid);
            return;
        }
        if ( selectedFaceIndex >= solid.getPolygonsList().size() ) {
            System.out.println(solid);
            return;
        }

        _PolyhedralBoundedSolidFace selectedFace =
            solid.getPolygonsList().get(selectedFaceIndex);
        Set<_PolyhedralBoundedSolidFace> facesToPrint =
            collectSelectedAndNeighborFaces(selectedFace);

        System.out.print(buildFacesSubsetDump(solid, facesToPrint, selectedFace));
    }

    private static Set<_PolyhedralBoundedSolidFace> collectSelectedAndNeighborFaces(
        _PolyhedralBoundedSolidFace selectedFace)
    {
        Set<_PolyhedralBoundedSolidFace> result =
            new LinkedHashSet<_PolyhedralBoundedSolidFace>();
        result.add(selectedFace);

        for ( int i = 0; i < selectedFace.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = selectedFace.boundariesList.get(i);
            if ( loop == null || loop.boundaryStartHalfEdge == null ) {
                continue;
            }

            _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge he = start;
            do {
                _PolyhedralBoundedSolidHalfEdge mirror = he.mirrorHalfEdge();
                if ( mirror != null &&
                     mirror.parentLoop != null &&
                     mirror.parentLoop.parentFace != null ) {
                    result.add(mirror.parentLoop.parentFace);
                }
                he = he.next();
            } while ( he != start );
        }
        return result;
    }

    private static String buildFacesSubsetDump(
        PolyhedralBoundedSolid solid,
        Set<_PolyhedralBoundedSolidFace> facesToPrint,
        _PolyhedralBoundedSolidFace selectedFace)
    {
        StringBuilder msg = new StringBuilder();

        msg.append("= POLYHEDRAL BOUNDED SOLID STRUCTURE (FILTERED) ===============================\n");
        msg.append("= showing face [").append(selectedFace.id)
            .append("] and neighbors sharing an edge\n");
        msg.append("= total faces in solid: ").append(solid.getPolygonsList().size())
            .append(", printed: ").append(facesToPrint.size()).append("\n");
        msg.append("=-------------------------------------------------------------------------------\n");

        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( !facesToPrint.contains(face) ) {
                continue;
            }
            msg.append("  - ").append(face).append("\n");
            for ( int j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(j);
                _PolyhedralBoundedSolidHalfEdge he;
                _PolyhedralBoundedSolidHalfEdge heStart;

                msg.append("    . Loop ").append(j).append(", with half-edges: \n");
                msg.append("      | HeID  | StartVertex | End Vertex | nccw He | pccw He | parentEdge | mirror He | neighbor face\n");
                msg.append("      +-------+-------------+------------+---------+---------+------------+-----------+-------------+\n");

                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    msg.append("<Loop without starting half-edge!>\n");
                    continue;
                }
                heStart = he;
                do {
                    he = he.next();
                    if ( he == null ) {
                        msg.append("      |  - (not closed loop)\n");
                        break;
                    }

                    msg.append("      | ")
                        .append(intPreSpaces(he.id, 4))
                        .append((he == loop.boundaryStartHalfEdge) ? "*" : " ")
                        .append(" | ")
                        .append(intPreSpaces(he.startingVertex.id, 11))
                        .append(" | ")
                        .append(intPreSpaces(he.next().startingVertex.id, 10))
                        .append(" | ")
                        .append(intPreSpaces(he.next().id, 7))
                        .append(" | ")
                        .append(intPreSpaces(he.previous().id, 7))
                        .append(" | ");
                    msg.append((he.parentEdge != null) ?
                        intPreSpaces(he.parentEdge.id, 10) : "    <null>");
                    msg.append(" | ");
                    if ( he.mirrorHalfEdge() != null &&
                         he.mirrorHalfEdge().parentLoop != null &&
                         he.mirrorHalfEdge().parentLoop.parentFace != null ) {
                        msg.append(intPreSpaces(he.mirrorHalfEdge().id, 9))
                            .append(" | ")
                            .append(intPreSpaces(
                                he.mirrorHalfEdge().parentLoop.parentFace.id, 11))
                            .append(" | ");
                    }
                    else {
                        msg.append(" No Mirror Half Edge!   | ");
                    }

                    msg.append("\n");

                } while ( he != heStart );
            }
        }

        msg.append("= END OF FILTERED POLYHEDRAL BOUNDED SOLID STRUCTURE ==========================\n");
        return msg.toString();
    }

    private static String intPreSpaces(int a, int n)
    {
        StringBuilder sb = new StringBuilder();
        int i;
        if ( a < 0 ) {
            sb.append("-");
            a = -a;
            n--;
        }
        if ( a < 10 ) {
            i = 1;
        }
        else if ( a < 100 ) {
            i = 2;
        }
        else if ( a < 1000 ) {
            i = 3;
        }
        else if ( a < 10000 ) {
            i = 4;
        }
        else if ( a < 100000 ) {
            i = 5;
        }
        else if ( a < 1000000 ) {
            i = 6;
        }
        else if ( a < 10000000 ) {
            i = 7;
        }
        else if ( a < 100000000 ) {
            i = 8;
        }
        else if ( a < 1000000000 ) {
            i = 9;
        }
        else {
            i = 10;
        }

        while ( i < n ) {
            sb.append(" ");
            i++;
        }
        sb.append(a);
        return sb.toString();
    }
}
