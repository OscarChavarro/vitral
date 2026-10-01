/**
 * Mouse events of the drawing area, as seen by the filter (Java's enum
 * `PopupDismissClickFilter.MouseEventKind`).
 */
export enum MouseEventKind {
  PRESS = 'PRESS',
  DRAG = 'DRAG',
  RELEASE = 'RELEASE',
  CLICK = 'CLICK',
  OTHER = 'OTHER',
}

/**
 * Port of `gui.PopupDismissClickFilter`.
 *
 * A click outside a popup menu closes it, and that click must not act over the
 * drawing area (i.e. selecting another viewport): the user only wanted to leave
 * the menu. The GUI technology notifies here when the menu closes and asks,
 * for each mouse event of the drawing area, if it must be ignored: the press
 * arriving shortly after the menu closed, and the drag, release and click that
 * follow it, are consumed. It does not depend on any GUI technology.
 */
export class PopupDismissClickFilter {
  /** The press that closes the popup is delivered to the canvas right after */
  private static readonly OUTSIDE_PRESS_WINDOW_MILLIS = 300;

  private closedAtMillis: number;
  private swallowingClick: boolean;

  constructor() {
    this.closedAtMillis = 0;
    this.swallowingClick = false;
  }

  /**
   * Must be called when the popup is shown.
   */
  popupShown(): void {
    this.swallowingClick = false;
  }

  /**
   * Must be called when the popup closes.
   * @param nowMillis current time, in milliseconds
   */
  popupClosed(nowMillis: number): void {
    this.closedAtMillis = nowMillis;
  }

  /**
   * @param kind kind of the mouse event of the drawing area
   * @param nowMillis current time, in milliseconds
   * @return true if the event must be ignored by the drawing area
   */
  consumes(kind: MouseEventKind, nowMillis: number): boolean {
    switch (kind) {
      case MouseEventKind.PRESS:
        this.swallowingClick =
          this.closedAtMillis !== 0 &&
          nowMillis - this.closedAtMillis <= PopupDismissClickFilter.OUTSIDE_PRESS_WINDOW_MILLIS;
        this.closedAtMillis = 0;
        return this.swallowingClick;
      case MouseEventKind.DRAG:
      case MouseEventKind.RELEASE:
        return this.swallowingClick;
      case MouseEventKind.CLICK: {
        const swallowed: boolean = this.swallowingClick;
        this.swallowingClick = false;
        return swallowed;
      }
      default:
        return false;
    }
  }
}
