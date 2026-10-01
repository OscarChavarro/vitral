import type { MouseEvent as VitralMouseEvent, Vector3Dd, Viewport } from '@vitral/base';
import { WebSystem } from '@vitral/webgl';
import type { DrawingArea } from '../../model/drawing-area';
import type { DrawingAreaInteractionListener } from '../drawing-area-interaction-listener';
import type { DrawingAreaInteractionTechniques } from '../drawing-area-interaction-techniques';
import { MouseEventKind } from '../popup-dismiss-click-filter';
import { HtmlKeyEventMapper } from './html-key-event-mapper';
import { HtmlProjectionLocationPopup } from './html-projection-location-popup';

/** Physical keys and characters of the named keys of `injectKeyEvent` */
const NAMED_KEYS: Readonly<Record<string, readonly [string, string]>> = {
  tab: ['Tab', 'Tab'],
  enter: ['Enter', 'Enter'],
  numenter: ['Enter', 'NumpadEnter'],
  backspace: ['Backspace', 'Backspace'],
  delete: ['Delete', 'Delete'],
  escape: ['Escape', 'Escape'],
  left: ['ArrowLeft', 'ArrowLeft'],
  right: ['ArrowRight', 'ArrowRight'],
  up: ['ArrowUp', 'ArrowUp'],
  down: ['ArrowDown', 'ArrowDown'],
  pageup: ['PageUp', 'PageUp'],
  pagedown: ['PageDown', 'PageDown'],
  'num/': ['/', 'NumpadDivide'],
  'num*': ['*', 'NumpadMultiply'],
  'num-': ['-', 'NumpadSubtract'],
  'num+': ['+', 'NumpadAdd'],
  'num.': ['.', 'NumpadDecimal'],
};

/**
 * Port of `gui.awt.AwtDrawingAreaController`.
 *
 * Maps the DOM events of the canvas presenting the drawing area to vitral
 * events, which are processed by the `DrawingAreaInteractionTechniques` (key
 * events through `HtmlKeyEventMapper`, so Ctrl chords such as the undo/redo
 * ones keep their key). It also owns the DOM services of the interaction: the
 * projection location popup menu and the focus of the canvas.
 *
 * AWT's mouse model is rebuilt from pointer events: the pointer is captured on
 * a press, so the drag goes on outside the canvas (as AWT keeps dragging over
 * the component that got the press); a move with a button down is a drag; and
 * a release at the place of its press is followed by a click. The context menu
 * of the browser is disabled on the canvas, and so is the default action of
 * the keys the drawing area uses (i.e. TAB is a command of the drawing area,
 * not a request to move the focus), but not the one of the browser chords
 * the drawing area does not use (Ctrl or Cmd with other letters).
 */
export class HtmlDrawingAreaController {
  private readonly canvas: HTMLElement;
  private readonly drawingArea: DrawingArea;
  private readonly techniques: DrawingAreaInteractionTechniques;
  private readonly listener: DrawingAreaInteractionListener;
  private readonly projectionLocationPopup: HtmlProjectionLocationPopup;
  private pressX = 0;
  private pressY = 0;
  private pressMoved = false;
  private pressed = false;
  private readonly removers: (() => void)[] = [];

  /**
   * Creates the controller and starts listening to the events of the canvas.
   * @param canvas element presenting the drawing area
   * @param drawingArea state of the drawing area
   * @param techniques techniques processing the (vitral) events
   * @param listener receives the application level commands that depend on
   * the raw DOM event (full screen toggle)
   */
  constructor(
    canvas: HTMLElement,
    drawingArea: DrawingArea,
    techniques: DrawingAreaInteractionTechniques,
    listener: DrawingAreaInteractionListener,
  ) {
    this.canvas = canvas;
    this.drawingArea = drawingArea;
    this.techniques = techniques;
    this.listener = listener;

    this.projectionLocationPopup = new HtmlProjectionLocationPopup(
      drawingArea.getViewportSet(),
      techniques,
      canvas,
      (): void => listener.repaintRequested(),
    );
    techniques.getViewportSetTechniques().setListener({
      projectionLocationMenuRequested: (viewport: Viewport, x: number, y: number): void => {
        this.projectionLocationPopup.show(viewport, drawingArea.scaleXToCanvas(x), drawingArea.scaleYToCanvas(y));
      },
    });

    // TAB is a command of the drawing area (it cycles the boxes of the input
    // gizmo of the translation gizmo), not a request to move the focus
    canvas.tabIndex = 0;
    this.listen('pointerenter', (e: Event): void => this.mouseEntered(e as PointerEvent));
    this.listen('pointerdown', (e: Event): void => this.mousePressed(e as PointerEvent));
    this.listen('pointermove', (e: Event): void => this.mouseMoved(e as PointerEvent));
    this.listen('pointerup', (e: Event): void => this.mouseReleased(e as PointerEvent));
    this.listen('pointercancel', (e: Event): void => this.mouseReleased(e as PointerEvent));
    this.listen('contextmenu', (e: Event): void => e.preventDefault());
    this.listen('keydown', (e: Event): void => this.keyPressed(e as KeyboardEvent));
    this.listen('keyup', (e: Event): void => this.keyReleased(e as KeyboardEvent));
  }

  private listen(type: string, handler: (event: Event) => void): void {
    this.canvas.addEventListener(type, handler);
    this.removers.push((): void => this.canvas.removeEventListener(type, handler));
  }

  /**
   * Stops listening to the canvas (when the GUI is destroyed).
   */
  dispose(): void {
    for (const remove of this.removers) {
      remove();
    }
    this.removers.length = 0;
  }

  private syncCanvasSize(): void {
    this.drawingArea.updateCanvasSize(this.canvas.clientWidth, this.canvas.clientHeight);
  }

  /**
   * Delivers a synthetic mouse event to the canvas, as if it came from the
   * user's pointer. Intended for automated agents (see `application/mcp`).
   * @param type one of "move", "press", "drag", "release"
   * @param x canvas x coordinate, in CSS pixels
   * @param y canvas y coordinate, in CSS pixels
   * @param button AWT button number (1 = left, 2 = middle, 3 = right)
   */
  injectMouseEvent(type: string, x: number, y: number, button: number): void {
    const box: DOMRect = this.canvas.getBoundingClientRect();
    const domButton: number = button === 2 ? 1 : button === 3 ? 2 : 0;
    const buttonMask: number = button === 2 ? 4 : button === 3 ? 2 : 1;
    let domType: string;
    let buttons = 0;

    switch (type) {
      case 'move':
        domType = 'pointermove';
        break;
      case 'press':
        domType = 'pointerdown';
        buttons = buttonMask;
        break;
      case 'drag':
        domType = 'pointermove';
        buttons = buttonMask;
        break;
      case 'release':
        domType = 'pointerup';
        break;
      default:
        throw new Error('Unknown mouse event type "' + type + '". Use move, press, drag or release');
    }
    const event: PointerEvent = new PointerEvent(domType, {
      bubbles: true,
      cancelable: true,
      clientX: box.left + x,
      clientY: box.top + y,
      button: type === 'move' || type === 'drag' ? -1 : domButton,
      buttons,
      pointerId: 1,
      pointerType: 'mouse',
      isPrimary: true,
    });
    this.canvas.dispatchEvent(event);
  }

  /**
   * Delivers a synthetic key press to this controller, as if it came from the
   * user's keyboard (whatever element has the focus). Intended for automated
   * agents (see `application/mcp`). As AWT does, the character of a
   * Ctrl+letter chord is the control one.
   * @param key a single character (i.e. "5", "x") or one of the names "tab",
   * "enter", "backspace", "delete", "escape", "left", "right", "up", "down",
   * "pageup", "pagedown", "num0".."num9", "num/", "num*", "num-", "num+",
   * "num.", "numenter"
   * @param shift true to press it with the SHIFT key down
   * @param ctrl true to press it with the CTRL key down
   */
  injectKeyEvent(key: string, shift: boolean, ctrl: boolean = false): void {
    let domKey: string;
    let code: string;
    const named: readonly [string, string] | undefined = NAMED_KEYS[key];

    if (named !== undefined) {
      [domKey, code] = named;
    } else if (/^num[0-9]$/.test(key)) {
      domKey = key.charAt(3);
      code = 'Numpad' + key.charAt(3);
    } else if (key.length === 1) {
      domKey = key;
      if (/[a-zA-Z]/.test(key)) {
        code = 'Key' + key.toUpperCase();
      } else if (/[0-9]/.test(key)) {
        code = 'Digit' + key;
      } else {
        code = HtmlDrawingAreaController.codeOfSymbol(key);
      }
    } else {
      throw new Error(
        'Unknown key "' + key + '". Use a single character, tab, enter, backspace, delete, escape, left, ' +
          'right, up, down, pageup, pagedown, num0..num9, num/, num*, num-, num+, num. or numenter',
      );
    }
    const event: KeyboardEvent = new KeyboardEvent('keydown', {
      key: domKey,
      code,
      shiftKey: shift,
      ctrlKey: ctrl,
      bubbles: true,
      cancelable: true,
    });

    // Delivered directly: through the canvas, the browser would send it to
    // the element that has the focus, if it is not the canvas
    this.keyPressed(event);
  }

  private static codeOfSymbol(symbol: string): string {
    switch (symbol) {
      case '-': return 'Minus';
      case '=': return 'Equal';
      case '.': return 'Period';
      case ',': return 'Comma';
      case ' ': return 'Space';
      case '/': return 'Slash';
      default: return '';
    }
  }

  /**
   * Projects a point of the scene to canvas pixel coordinates using the
   * active camera of a viewport.
   * @param viewport viewport whose camera is used
   * @param point point in world coordinates
   * @return {x, y} in canvas pixels, or null if the point is behind the camera
   */
  projectToCanvas(viewport: Viewport, point: Vector3Dd): number[] | null {
    this.syncCanvasSize();
    return this.drawingArea.projectToCanvas(viewport, point);
  }

  //= Mouse =============================================================

  private mouseEntered(e: PointerEvent): void {
    // While the projection location menu is open, it has the keyboard focus
    if (!this.projectionLocationPopup.isVisible()) {
      this.canvas.focus({ preventScroll: true });
    }
    this.syncCanvasSize();
    this.techniques.processMouseEnteredEvent(WebSystem.toVitralMouseEvent(e, this.canvas));
  }

  private mousePressed(e: PointerEvent): void {
    this.canvas.focus({ preventScroll: true });
    try {
      this.canvas.setPointerCapture(e.pointerId);
    } catch {
      // A synthetic pointer can not be captured: the drag stays over the canvas
    }
    this.pressed = true;
    this.pressMoved = false;
    this.pressX = e.clientX;
    this.pressY = e.clientY;
    if (this.projectionLocationPopup.consumesMouseEvent(MouseEventKind.PRESS)) {
      return;
    }
    this.syncCanvasSize();
    this.techniques.processMousePressedEvent(WebSystem.toVitralMouseEvent(e, this.canvas));
  }

  private mouseReleased(e: PointerEvent): void {
    if (!this.pressed) {
      return;
    }
    this.pressed = false;
    if (this.canvas.hasPointerCapture(e.pointerId)) {
      this.canvas.releasePointerCapture(e.pointerId);
    }
    const consumed: boolean = this.projectionLocationPopup.consumesMouseEvent(MouseEventKind.RELEASE);
    const event: VitralMouseEvent = WebSystem.toVitralMouseEvent(e, this.canvas);

    if (!consumed) {
      this.syncCanvasSize();
      this.techniques.processMouseReleasedEvent(event);
    }
    // AWT clicks: a release at the place of its press
    if (!this.pressMoved && e.type === 'pointerup') {
      this.mouseClicked(event);
    }
  }

  private mouseClicked(event: VitralMouseEvent): void {
    if (this.projectionLocationPopup.consumesMouseEvent(MouseEventKind.CLICK)) {
      return;
    }
    this.syncCanvasSize();
    this.techniques.processMouseClickedEvent(event);
  }

  private mouseMoved(e: PointerEvent): void {
    if (this.pressed && e.buttons !== 0) {
      if (e.clientX !== this.pressX || e.clientY !== this.pressY) {
        this.pressMoved = true;
      }
      this.mouseDragged(e);
      return;
    }
    const event: VitralMouseEvent = WebSystem.toVitralMouseEvent(e, this.canvas);

    this.syncCanvasSize();
    if (!this.projectionLocationPopup.isVisible()) {
      this.techniques.updateModeCursor(event);
    }
    this.techniques.processMouseMovedEvent(event);
  }

  private mouseDragged(e: PointerEvent): void {
    if (this.projectionLocationPopup.consumesMouseEvent(MouseEventKind.DRAG)) {
      return;
    }
    this.syncCanvasSize();
    this.techniques.processMouseDraggedEvent(WebSystem.toVitralMouseEvent(e, this.canvas));
  }

  //= Keyboard ==========================================================

  /**
   * @return true if the default action of the browser for the key must be
   * kept: the chords with Ctrl or Cmd that the drawing area does not use
   */
  private static keepsBrowserAction(e: KeyboardEvent): boolean {
    if (!e.ctrlKey && !e.metaKey) {
      return false;
    }
    return !(e.ctrlKey && (e.code === 'KeyZ' || e.code === 'KeyY' || (e.shiftKey && e.code === 'KeyF')));
  }

  private keyPressed(e: KeyboardEvent): void {
    if (!HtmlDrawingAreaController.keepsBrowserAction(e) && e.cancelable) {
      e.preventDefault();
    }
    this.techniques.processKeyPressedEvent(HtmlKeyEventMapper.toVitralEvent(e));

    // Ctrl+Shift+F: the key identity is lost in the vitral event for
    // control characters, so the chord is detected here
    if (e.shiftKey && e.ctrlKey && e.code === 'KeyF') {
      this.listener.fullScreenGuiToggleRequested();
    }
  }

  private keyReleased(e: KeyboardEvent): void {
    this.techniques.processKeyReleasedEvent(HtmlKeyEventMapper.toVitralEvent(e));
  }
}
