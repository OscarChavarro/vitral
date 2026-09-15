import {
  PolyhedralBoundedSolidValidationEngine,
  type PolyhedralBoundedSolid,
  type _PolyhedralBoundedSolidFace,
  type _PolyhedralBoundedSolidHalfEdge,
  type _PolyhedralBoundedSolidLoop,
} from '@vitral/base';
import type { WebKeyEvent } from '@vitral/webgl';
import { csgOperationNextCircular } from '../models/csg-operation-names';
import { csgSampleNextCircular } from '../models/csg-sample-names';
import type { DebuggerModel } from '../models/debugger-model';
import { solidModelNextClamped, solidModelPreviousClamped } from '../models/solid-model-names';
import { CameraFaceFocusInteraction } from './camera-face-focus-interaction';

/**
 * The actions `DebuggerKeyboardInteractionTechniques.Actions` declares.
 *
 * Java's `requestExit` destroys the canvas, disposes the frame and calls
 * `System.exit(0)`; a module inside the container returns to the explorer.
 * `toggleFullscreen` rebuilds the `JFrame`; the component asks the browser for
 * its fullscreen element. `requestScreenshot` and `requestStlExport` write
 * `screenshot.png` and `output.stl` beside the program; the component hands the
 * same files to the browser as downloads.
 */
export interface DebuggerKeyboardActions {
  requestExit(): void;
  rebuildSolid(): void;
  toggleFullscreen(): void;
  toggleSolidAnimation(): void;
  requestScreenshot(): void;
  requestStlExport(): void;
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/gui/DebuggerKeyboardInteractionTechniques.java`.
 *
 * Every binding is the Java one, in the Java order and with the Java
 * bookkeeping: the escape key exits before anything else and answers `false`;
 * the camera controller and then the quality controller see every other key,
 * the latter printing the configuration when it changes; the switch then
 * handles the debugger's own keys; and the face index, the edge index and the
 * subdivisions are clamped after every keystroke. The faces-subset dump `[I]`
 * prints, with its fixed-width columns, is Java's text.
 *
 * The controllers are the container's stand-ins for `CameraControllerOrbiter`
 * and `RendererConfigurationController`, whose Phase 38 group is not ported;
 * the event that reaches them carries the same `keycode` names the Java
 * `vsdk.toolkit.gui.KeyEvent` constants have.
 */
export class DebuggerKeyboardInteractionTechniques {
  private readonly cameraFaceFocusInteraction: CameraFaceFocusInteraction;

  constructor() {
    this.cameraFaceFocusInteraction = new CameraFaceFocusInteraction();
  }

  processPressed(
    model: DebuggerModel,
    event: WebKeyEvent,
    actions: DebuggerKeyboardActions,
  ): boolean {
    let repaint = false;
    let handled = false;

    if (event.keycode === 'KEY_ESC') {
      actions.requestExit();
      return false;
    }

    if (model.getCameraController().processKeyPressed(event)) {
      repaint = true;
    }
    if (model.getQualityController().processKeyPressedEvent(event)) {
      console.log(model.getQuality().toString());
      repaint = true;
    }

    switch (event.keycode) {
      // Show vertex numbers
      case 'KEY_v': {
        model.setDebugVertices(model.notDebugVertices());
        handled = true;
        break;
      }

      // Full screen
      case 'KEY_g': {
        actions.toggleFullscreen();
        handled = true;
        break;
      }
      case 'KEY_h': {
        model.setHudEnabled(!model.isHudEnabled());
        handled = true;
        break;
      }
      case 'KEY_r': {
        actions.toggleSolidAnimation();
        handled = true;
        break;
      }

      // Reference frame
      case 'KEY_SPACE': {
        model.setShowCoordinateSystem(!model.isShowCoordinateSystem());
        handled = true;
        break;
      }

      // Console print
      case 'KEY_I': {
        DebuggerKeyboardInteractionTechniques.printSolidForCurrentFaceSelection(model);
        if (PolyhedralBoundedSolidValidationEngine.validateIntermediate(model.getSolid()!)) {
          console.log('SOLID MODEL IS VALID!');
        } else {
          console.log('SOLID MODEL IS INVALID!');
        }
        handled = true;
        break;
      }

      // Highlighted face(s)
      case 'KEY_1': {
        model.setFaceIndex(model.getFaceIndex() - 1);
        handled = true;
        break;
      }
      case 'KEY_2': {
        model.setFaceIndex(model.getFaceIndex() + 1);
        handled = true;
        break;
      }
      case 'KEY_c': {
        handled = this.cameraFaceFocusInteraction.focusSelectedFace(model);
        break;
      }
      case 'KEY_PERIOD': {
        actions.requestScreenshot();
        handled = true;
        break;
      }
      case 'KEY_m':
      case 'KEY_M': {
        actions.requestStlExport();
        handled = true;
        break;
      }

      // Model selection
      case 'KEY_3': {
        model.setSolidModelName(solidModelPreviousClamped(model.getSolidModelName()));
        actions.rebuildSolid();
        handled = true;
        break;
      }
      case 'KEY_4': {
        model.setSolidModelName(solidModelNextClamped(model.getSolidModelName()));
        actions.rebuildSolid();
        handled = true;
        break;
      }

      // Sphere / cylinder subdivisions
      case 'KEY_q': {
        model.setSubdivisionCircumference(model.getSubdivisionCircumference() - 1);
        model.clampSubdivisions();
        actions.rebuildSolid();
        handled = true;
        break;
      }
      case 'KEY_Q': {
        model.setSubdivisionCircumference(model.getSubdivisionCircumference() + 1);
        actions.rebuildSolid();
        handled = true;
        break;
      }
      case 'KEY_w': {
        model.setSubdivisionHeight(model.getSubdivisionHeight() - 1);
        model.clampSubdivisions();
        actions.rebuildSolid();
        handled = true;
        break;
      }
      case 'KEY_W': {
        model.setSubdivisionHeight(model.getSubdivisionHeight() + 1);
        actions.rebuildSolid();
        handled = true;
        break;
      }

      // Hidden line algorithm debug
      case 'KEY_0': {
        model.setDebugEdges(!model.isDebugEdges());
        handled = true;
        break;
      }
      case 'KEY_8': {
        model.cycleAppelDisplayMode();
        handled = true;
        break;
      }
      case 'KEY_9': {
        model.setEdgeIndex(model.getEdgeIndex() + 1);
        handled = true;
        break;
      }

      // CSG special debug cases
      case 'KEY_5': {
        model.setCsgOperation(csgOperationNextCircular(model.getCsgOperation()));
        actions.rebuildSolid();
        handled = true;
        break;
      }
      case 'KEY_6': {
        model.setCsgSample(csgSampleNextCircular(model.getCsgSample()));
        actions.rebuildSolid();
        handled = true;
        break;
      }
      case 'KEY_d': {
        model.setDebugCsg(!model.isDebugCsg());
        actions.rebuildSolid();
        handled = true;
        break;
      }
      case 'KEY_e': {
        if (model.usesKurlanderBowlSingleMotifControls()) {
          model.setKurlanderBowlSingleMotifIndex(model.getKurlanderBowlSingleMotifIndex() - 1);
          actions.rebuildSolid();
          handled = true;
        }
        break;
      }
      case 'KEY_E': {
        if (model.usesKurlanderBowlSingleMotifControls()) {
          model.setKurlanderBowlSingleMotifIndex(model.getKurlanderBowlSingleMotifIndex() + 1);
          actions.rebuildSolid();
          handled = true;
        }
        break;
      }
    }

    model.clampFaceIndex();
    if (model.getEdgeIndex() < -3) {
      model.setEdgeIndex(-3);
    }
    model.clampSubdivisions();

    return repaint || handled;
  }

  /**
   * Java's `keyReleased` hands the event to the camera controller. The camera
   * stand-in keeps no key-release state, so there is nothing for it to answer
   * and no repaint follows.
   */
  processReleased(_model: DebuggerModel, _event: WebKeyEvent): boolean {
    return false;
  }

  private static printSolidForCurrentFaceSelection(model: DebuggerModel): void {
    const solid: PolyhedralBoundedSolid | null = model.getSolid();
    if (solid === null) {
      console.log('null');
      return;
    }

    const selectedFaceIndex: number = model.getFaceIndex();
    if (selectedFaceIndex < 0 || solid.getPolygonsList() === null) {
      console.log(solid.toString());
      return;
    }
    if (selectedFaceIndex >= solid.getPolygonsList().size()) {
      console.log(solid.toString());
      return;
    }

    const selectedFace: _PolyhedralBoundedSolidFace = solid
      .getPolygonsList()
      .get(selectedFaceIndex)!;
    const facesToPrint: Set<_PolyhedralBoundedSolidFace> =
      DebuggerKeyboardInteractionTechniques.collectSelectedAndNeighborFaces(selectedFace);

    console.log(
      DebuggerKeyboardInteractionTechniques.buildFacesSubsetDump(solid, facesToPrint, selectedFace),
    );
  }

  /** Java's `LinkedHashSet`: a JavaScript `Set` keeps insertion order and identity membership. */
  private static collectSelectedAndNeighborFaces(
    selectedFace: _PolyhedralBoundedSolidFace,
  ): Set<_PolyhedralBoundedSolidFace> {
    const result = new Set<_PolyhedralBoundedSolidFace>();
    result.add(selectedFace);

    for (let i = 0; i < selectedFace.boundariesList.size(); i++) {
      const loop: _PolyhedralBoundedSolidLoop | null = selectedFace.boundariesList.get(i);
      if (loop === null || loop.boundaryStartHalfEdge === null) {
        continue;
      }

      const start: _PolyhedralBoundedSolidHalfEdge = loop.boundaryStartHalfEdge;
      let he: _PolyhedralBoundedSolidHalfEdge = start;
      do {
        const mirror: _PolyhedralBoundedSolidHalfEdge | null = he.mirrorHalfEdge();
        if (
          mirror !== null &&
          mirror.parentLoop !== null &&
          mirror.parentLoop.parentFace !== null
        ) {
          result.add(mirror.parentLoop.parentFace);
        }
        he = he.next()!;
      } while (he !== start);
    }
    return result;
  }

  private static buildFacesSubsetDump(
    solid: PolyhedralBoundedSolid,
    facesToPrint: Set<_PolyhedralBoundedSolidFace>,
    selectedFace: _PolyhedralBoundedSolidFace,
  ): string {
    let msg = '';
    const pad = DebuggerKeyboardInteractionTechniques.intPreSpaces;

    msg += '= POLYHEDRAL BOUNDED SOLID STRUCTURE (FILTERED) ===============================\n';
    msg += '= showing face [' + selectedFace.id + '] and neighbors sharing an edge\n';
    msg +=
      '= total faces in solid: ' +
      solid.getPolygonsList().size() +
      ', printed: ' +
      facesToPrint.size +
      '\n';
    msg += '=-------------------------------------------------------------------------------\n';

    for (let i = 0; i < solid.getPolygonsList().size(); i++) {
      const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(i)!;
      if (!facesToPrint.has(face)) {
        continue;
      }
      msg += '  - ' + face.toString() + '\n';
      for (let j = 0; j < face.boundariesList.size(); j++) {
        const loop: _PolyhedralBoundedSolidLoop = face.boundariesList.get(j)!;
        let he: _PolyhedralBoundedSolidHalfEdge | null;

        msg += '    . Loop ' + j + ', with half-edges: \n';
        msg +=
          '      | HeID  | StartVertex | End Vertex | nccw He | pccw He | parentEdge | mirror He | neighbor face\n';
        msg +=
          '      +-------+-------------+------------+---------+---------+------------+-----------+-------------+\n';

        he = loop.boundaryStartHalfEdge;
        if (he === null) {
          msg += '<Loop without starting half-edge!>\n';
          continue;
        }
        const heStart: _PolyhedralBoundedSolidHalfEdge = he;
        do {
          he = he!.next();
          if (he === null) {
            msg += '      |  - (not closed loop)\n';
            break;
          }

          msg +=
            '      | ' +
            pad(he.id, 4) +
            (he === loop.boundaryStartHalfEdge ? '*' : ' ') +
            ' | ' +
            pad(he.startingVertex.id, 11) +
            ' | ' +
            pad(he.next()!.startingVertex.id, 10) +
            ' | ' +
            pad(he.next()!.id, 7) +
            ' | ' +
            pad(he.previous()!.id, 7) +
            ' | ';
          msg += he.parentEdge !== null ? pad(he.parentEdge.id, 10) : '    <null>';
          msg += ' | ';
          const mirror: _PolyhedralBoundedSolidHalfEdge | null = he.mirrorHalfEdge();
          if (
            mirror !== null &&
            mirror.parentLoop !== null &&
            mirror.parentLoop.parentFace !== null
          ) {
            msg += pad(mirror.id, 9) + ' | ' + pad(mirror.parentLoop.parentFace.id, 11) + ' | ';
          } else {
            msg += ' No Mirror Half Edge!   | ';
          }

          msg += '\n';
        } while (he !== heStart);
      }
    }

    msg += '= END OF FILTERED POLYHEDRAL BOUNDED SOLID STRUCTURE ==========================\n';
    return msg;
  }

  private static intPreSpaces(a: number, n: number): string {
    let sb = '';
    let i: number;
    if (a < 0) {
      sb += '-';
      a = -a;
      n--;
    }
    if (a < 10) {
      i = 1;
    } else if (a < 100) {
      i = 2;
    } else if (a < 1000) {
      i = 3;
    } else if (a < 10000) {
      i = 4;
    } else if (a < 100000) {
      i = 5;
    } else if (a < 1000000) {
      i = 6;
    } else if (a < 10000000) {
      i = 7;
    } else if (a < 100000000) {
      i = 8;
    } else if (a < 1000000000) {
      i = 9;
    } else {
      i = 10;
    }

    while (i < n) {
      sb += ' ';
      i++;
    }
    sb += a;
    return sb;
  }
}
