/**
 * Port of `gui.awt.AwtCursorWarper`.
 *
 * Platform part of the wrapping of the cursor while dragging (see
 * `TranslateGizmoInteractionTechnique`): it would place the pointer of the
 * system over a position of an element. A page can not move the pointer of
 * the system (browsers only offer to hide and lock it), which is the case Java
 * meets when the application has no permission to control the pointer: the
 * warper is not available, and the techniques survive it (the translation
 * gizmo then drags within the viewport).
 */
export class HtmlCursorWarper {
  /**
   * @return true if the pointer of the system can be placed by this object:
   * never, in a page
   */
  isAvailable(): boolean {
    return false;
  }

  /**
   * Would place the pointer of the system over a position of an element.
   * @param _element an element showing on screen
   * @param _x horizontal position in element's coordinates
   * @param _y vertical position in element's coordinates
   * @return false: the pointer was not placed
   */
  warp(_element: HTMLElement | null, _x: number, _y: number): boolean {
    return false;
  }
}
