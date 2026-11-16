#include "java/util/ArrayList.txx"
#include "gui/DebuggerKeyboardInteractionTechniques.h"

#include "java/lang/System.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/gui/CameraController.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "models/DebuggerModel.h"

DebuggerKeyboardInteractionTechniques::DebuggerKeyboardInteractionTechniques()
{
}

bool DebuggerKeyboardInteractionTechniques::processPressed(DebuggerModel& model,
    const KeyEvent& event, Actions& actions)
{
    bool repaint = false;
    bool handled = false;

    if ( event.keycode == KeyEvent::KEY_ESC ) {
        actions.requestExit();
        return false;
    }

    if ( model.getCameraController()->processKeyPressedEvent(event) ) {
        repaint = true;
    }
    if ( model.getQualityController()->processKeyPressedEvent(event) ) {
        java::System::out.println(model.getQuality()->toString().c_str());
        repaint = true;
    }

    switch ( event.keycode ) {
        // Show vertex numbers
        case KeyEvent::KEY_v:
            model.setDebugVertices(model.notDebugVertices());
            handled = true;
            break;

        // Full screen
        case KeyEvent::KEY_g:
            actions.toggleFullscreen();
            handled = true;
            break;
        case KeyEvent::KEY_h:
            model.setHudEnabled(!model.isHudEnabled());
            handled = true;
            break;
        case KeyEvent::KEY_r:
            actions.toggleSolidAnimation();
            handled = true;
            break;

        // Reference frame
        case KeyEvent::KEY_SPACE:
            model.setShowCoordinateSystem(!model.isShowCoordinateSystem());
            handled = true;
            break;

        // Console print
        case KeyEvent::KEY_I:
            printSolidForCurrentFaceSelection(model);
            if ( PolyhedralBoundedSolidValidationEngine::validateIntermediate(model.getSolid()) ) {
                java::System::out.println("SOLID MODEL IS VALID!");
            } else {
                java::System::out.println("SOLID MODEL IS INVALID!");
            }
            handled = true;
            break;

        // Highlighted face(s)
        case KeyEvent::KEY_1:
            model.setFaceIndex(model.getFaceIndex() - 1);
            handled = true;
            break;
        case KeyEvent::KEY_2:
            model.setFaceIndex(model.getFaceIndex() + 1);
            handled = true;
            break;
        case KeyEvent::KEY_c:
            handled = cameraFaceFocusInteraction.focusSelectedFace(model);
            break;
        case KeyEvent::KEY_PERIOD:
            actions.requestScreenshot();
            handled = true;
            break;
        case KeyEvent::KEY_m:
        case KeyEvent::KEY_M:
            actions.requestStlExport();
            handled = true;
            break;

        // Model selection
        case KeyEvent::KEY_3:
            model.setSolidModelName(
                SolidModelNamesUtil::previousClamped(model.getSolidModelName()));
            actions.rebuildSolid();
            handled = true;
            break;
        case KeyEvent::KEY_4:
            model.setSolidModelName(
                SolidModelNamesUtil::nextClamped(model.getSolidModelName()));
            actions.rebuildSolid();
            handled = true;
            break;

        // Sphere / cylinder subdivisions
        case KeyEvent::KEY_q:
            model.setSubdivisionCircumference(model.getSubdivisionCircumference() - 1);
            model.clampSubdivisions();
            actions.rebuildSolid();
            handled = true;
            break;
        case KeyEvent::KEY_Q:
            model.setSubdivisionCircumference(model.getSubdivisionCircumference() + 1);
            actions.rebuildSolid();
            handled = true;
            break;
        case KeyEvent::KEY_w:
            model.setSubdivisionHeight(model.getSubdivisionHeight() - 1);
            model.clampSubdivisions();
            actions.rebuildSolid();
            handled = true;
            break;
        case KeyEvent::KEY_W:
            model.setSubdivisionHeight(model.getSubdivisionHeight() + 1);
            actions.rebuildSolid();
            handled = true;
            break;

        // Hidden line algorithm debug
        case KeyEvent::KEY_0:
            model.setDebugEdges(!model.isDebugEdges());
            handled = true;
            break;
        case KeyEvent::KEY_8:
            model.cycleAppelDisplayMode();
            handled = true;
            break;
        case KeyEvent::KEY_9:
            model.setEdgeIndex(model.getEdgeIndex() + 1);
            handled = true;
            break;

        // CSG special debug cases
        case KeyEvent::KEY_5:
            model.setCsgOperation(
                CsgOperationNamesUtil::nextCircular(model.getCsgOperation()));
            actions.rebuildSolid();
            handled = true;
            break;
        case KeyEvent::KEY_6:
            model.setCsgSample(
                CsgSampleNamesUtil::nextCircular(model.getCsgSample()));
            actions.rebuildSolid();
            handled = true;
            break;
        case KeyEvent::KEY_d:
            model.setDebugCsg(!model.isDebugCsg());
            actions.rebuildSolid();
            handled = true;
            break;
        case KeyEvent::KEY_e:
            if ( model.usesKurlanderBowlSingleMotifControls() ) {
                model.setKurlanderBowlSingleMotifIndex(
                    model.getKurlanderBowlSingleMotifIndex() - 1);
                actions.rebuildSolid();
                handled = true;
            }
            break;
        case KeyEvent::KEY_E:
            if ( model.usesKurlanderBowlSingleMotifControls() ) {
                model.setKurlanderBowlSingleMotifIndex(
                    model.getKurlanderBowlSingleMotifIndex() + 1);
                actions.rebuildSolid();
                handled = true;
            }
            break;
        default:
            break;
    }

    model.clampFaceIndex();
    if ( model.getEdgeIndex() < -3 ) {
        model.setEdgeIndex(-3);
    }
    model.clampSubdivisions();

    return repaint || handled;
}

void DebuggerKeyboardInteractionTechniques::printSolidForCurrentFaceSelection(
    DebuggerModel& model)
{
    PolyhedralBoundedSolid* solid = model.getSolid();
    if ( solid == nullptr ) {
        java::System::out.println("null");
        return;
    }

    int selectedFaceIndex = model.getFaceIndex();
    if ( selectedFaceIndex < 0 ) {
        java::System::out.println(solid->toString().c_str());
        return;
    }
    if ( selectedFaceIndex >= (int)solid->getPolygonsList().size() ) {
        java::System::out.println(solid->toString().c_str());
        return;
    }

    _PolyhedralBoundedSolidFace* selectedFace =
        solid->getPolygonsList().get(selectedFaceIndex);
    java::ArrayList<_PolyhedralBoundedSolidFace*> facesToPrint =
        collectSelectedAndNeighborFaces(selectedFace);

    java::System::out.print(
        buildFacesSubsetDump(solid, facesToPrint, selectedFace).c_str());
}

java::ArrayList<_PolyhedralBoundedSolidFace*>
DebuggerKeyboardInteractionTechniques::collectSelectedAndNeighborFaces(
    _PolyhedralBoundedSolidFace* selectedFace)
{
    java::ArrayList<_PolyhedralBoundedSolidFace*> result;
    result.add(selectedFace);

    long int i;
    for ( i = 0; i < selectedFace->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = selectedFace->boundariesList.get(i);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            continue;
        }

        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        do {
            _PolyhedralBoundedSolidHalfEdge* mirror = he->mirrorHalfEdge();
            if ( mirror != nullptr &&
                 mirror->parentLoop != nullptr &&
                 mirror->parentLoop->parentFace != nullptr &&
                 !result.contains(mirror->parentLoop->parentFace) ) {
                result.add(mirror->parentLoop->parentFace);
            }
            he = he->next();
        } while ( he != start );
    }
    return result;
}

java::String DebuggerKeyboardInteractionTechniques::buildFacesSubsetDump(
    PolyhedralBoundedSolid* solid,
    const java::ArrayList<_PolyhedralBoundedSolidFace*>& facesToPrint,
    _PolyhedralBoundedSolidFace* selectedFace)
{
    java::String msg;

    msg += "= POLYHEDRAL BOUNDED SOLID STRUCTURE (FILTERED) ===============================\n";
    msg += java::String("= showing face [") + java::String::valueOf(selectedFace->id) +
        "] and neighbors sharing an edge\n";
    msg += java::String("= total faces in solid: ") +
        java::String::valueOf((long)solid->getPolygonsList().size()) +
        ", printed: " + java::String::valueOf((long)facesToPrint.size()) + "\n";
    msg += "=-------------------------------------------------------------------------------\n";

    long int i;
    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        if ( !facesToPrint.contains(face) ) {
            continue;
        }
        msg += java::String("  - ") + face->toString() + "\n";
        long int j;
        for ( j = 0; j < face->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
            _PolyhedralBoundedSolidHalfEdge* he;
            _PolyhedralBoundedSolidHalfEdge* heStart;

            msg += java::String("    . Loop ") + java::String::valueOf(j) +
                ", with half-edges: \n";
            msg += "      | HeID  | StartVertex | End Vertex | nccw He | pccw He | parentEdge | mirror He | neighbor face\n";
            msg += "      +-------+-------------+------------+---------+---------+------------+-----------+-------------+\n";

            he = loop->boundaryStartHalfEdge;
            if ( he == nullptr ) {
                msg += "<Loop without starting half-edge!>\n";
                continue;
            }
            heStart = he;
            do {
                he = he->next();
                if ( he == nullptr ) {
                    msg += "      |  - (not closed loop)\n";
                    break;
                }

                msg += java::String("      | ") +
                    intPreSpaces(he->id, 4) +
                    ((he == loop->boundaryStartHalfEdge) ? "*" : " ") +
                    " | " +
                    intPreSpaces(he->startingVertex->id, 11) +
                    " | " +
                    intPreSpaces(he->next()->startingVertex->id, 10) +
                    " | " +
                    intPreSpaces(he->next()->id, 7) +
                    " | " +
                    intPreSpaces(he->previous()->id, 7) +
                    " | ";
                msg += (he->parentEdge != nullptr) ?
                    intPreSpaces(he->parentEdge->id, 10) :
                    java::String("    <null>");
                msg += " | ";
                if ( he->mirrorHalfEdge() != nullptr &&
                     he->mirrorHalfEdge()->parentLoop != nullptr &&
                     he->mirrorHalfEdge()->parentLoop->parentFace != nullptr ) {
                    msg += intPreSpaces(he->mirrorHalfEdge()->id, 9) +
                        " | " +
                        intPreSpaces(
                            he->mirrorHalfEdge()->parentLoop->parentFace->id, 11) +
                        " | ";
                }
                else {
                    msg += " No Mirror Half Edge!   | ";
                }

                msg += "\n";

            } while ( he != heStart );
        }
    }

    msg += "= END OF FILTERED POLYHEDRAL BOUNDED SOLID STRUCTURE ==========================\n";
    return msg;
}

java::String DebuggerKeyboardInteractionTechniques::intPreSpaces(int a, int n)
{
    java::String sb;
    int i;
    if ( a < 0 ) {
        sb += "-";
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
        sb += " ";
        i++;
    }
    sb += java::String::valueOf(a);
    return sb;
}
