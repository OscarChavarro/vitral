/**
 * Port of `gui.awt.AwtLookAndFeelTuner` and `gui.awt.AwtCompactBevelButtonBorder`.
 *
 * Swing's look and feel classes are CSS themes of the DOM GUI (see
 * `vitral-html-gui.scss` of `@vitral/webgl`): the class of the root element of
 * the application selects one. This tuner maps the Java class names to those
 * themes, and applies the adjustment Java does for Motif: buttons are
 * compacted (a thin bevel border, so adjacent buttons touch each other).
 */
export class HtmlLookAndFeelTuner {
  private static readonly MOTIF_ID = 'Motif';
  private static readonly THEMES: readonly [string, string, string][] = [
    ['com.sun.java.swing.plaf.motif.MotifLookAndFeel', 'vitral-laf-motif', 'Motif'],
    ['javax.swing.plaf.metal.MetalLookAndFeel', 'vitral-laf-metal', 'Metal'],
    ['com.sun.java.swing.plaf.gtk.GTKLookAndFeel', 'vitral-laf-gtk', 'GTK'],
    ['com.sun.java.swing.plaf.windows.WindowsLookAndFeel', 'vitral-laf-windows', 'Windows'],
  ];

  private constructor() {}

  /**
   * @param lookAndFeel Java class name of a look and feel
   * @return the id of the look and feel (Swing's `LookAndFeel.getID`), or null
   * if it is not one of the known ones
   */
  static getId(lookAndFeel: string): string | null {
    const theme = HtmlLookAndFeelTuner.THEMES.find((t) => t[0] === lookAndFeel);
    return theme !== undefined ? theme[2] : null;
  }

  /**
   * @param lookAndFeel Java class name of a look and feel
   * @return true if it is Motif
   */
  static isMotif(lookAndFeel: string): boolean {
    return HtmlLookAndFeelTuner.MOTIF_ID === HtmlLookAndFeelTuner.getId(lookAndFeel);
  }

  /**
   * Applies (or removes) the theme and the adjustments of a look and feel to
   * the root element of the application.
   * @param root root element of the GUI
   * @param lookAndFeel Java class name of the look and feel
   * @return false if the look and feel is not known (the root keeps its theme)
   */
  static apply(root: HTMLElement, lookAndFeel: string): boolean {
    const theme = HtmlLookAndFeelTuner.THEMES.find((t) => t[0] === lookAndFeel);

    if (theme === undefined) {
      return false;
    }
    for (const [, cssClass] of HtmlLookAndFeelTuner.THEMES) {
      root.classList.remove(cssClass);
    }
    root.classList.add(theme[1]);
    root.classList.toggle('scene-editor-compact-buttons', HtmlLookAndFeelTuner.isMotif(lookAndFeel));
    return true;
  }
}
