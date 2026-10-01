import { JavaMath, type ViewportElementScaler } from '@vitral/base';

/**
 * Port of `gui.awt.AwtViewportElementScaler`.
 *
 * Platform part of `ViewportElementScaler`: it finds out the resolution of the
 * screen with the browser's `screen` and `devicePixelRatio`, and informs it to
 * the (platform independent) scaler injected in the constructor.
 *
 * The resolution informed is in physical pixels, which are the ones text is
 * drawn with: the screen size (that the browser reports in CSS pixels)
 * multiplied by the device pixel ratio (i.e. 2 in HiDPI displays).
 */
export class HtmlViewportElementScaler {
  private readonly elementScaler: ViewportElementScaler;

  constructor(elementScaler: ViewportElementScaler) {
    this.elementScaler = elementScaler;
  }

  getElementScaler(): ViewportElementScaler {
    return this.elementScaler;
  }

  /**
   * Informs the scaler the resolution of the screen showing the page.
   */
  updateFromDefaultScreen(): void {
    const ratio: number = window.devicePixelRatio || 1;

    this.elementScaler.setScreenResolution(
      JavaMath.round(window.screen.width * ratio),
      JavaMath.round(window.screen.height * ratio),
    );
  }

  /**
   * Informs the scaler the resolution of the screen where an element is
   * shown (which changes if the window is moved to other screen or the
   * resolution or zoom are changed). A page sees the screen of its window.
   * @param _element element showing the drawing area
   */
  updateFromComponent(_element: HTMLElement | null): void {
    this.updateFromDefaultScreen();
  }
}
