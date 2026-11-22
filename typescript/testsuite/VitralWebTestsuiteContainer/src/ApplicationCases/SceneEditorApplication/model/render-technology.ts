import { Viewport, ViewportSetCommands } from '@vitral/base';

/**
 * Port of `model.RenderTechnology`.
 *
 * Technologies the editor can use to present the viewport set of its drawing
 * area. Each one offers a set of render modes to the viewports: a canvas
 * drawn with the operations of its 2D context, without any WebGL context, has
 * no GPU z-buffer, so its viewports are drawn by the processor, as raytraced
 * images, as projected lines (wireframe or hidden line removal) or as
 * projected visible polygons (hidden surface removal).
 *
 * The Java port presents with OpenGL 4 (JOGL) or with AWT 2D drawing, and the
 * cpp port with OpenGL or with Xlib 2D drawing; this one with WebGL 2 or with
 * the 2D context of an HTML canvas. All the ports share the commands of the
 * `VIEWPORT_SET_RENDER_TECHNOLOGY` popup: the `..._OPENGL4` one selects the
 * 3D API (WebGL here) and the `..._AWT` one the 2D canvas of the GUI toolkit.
 * Java's enum constants are the instances below, in declaration order.
 */
export class RenderTechnology {
  /** WebGL 2 canvas (Java's `OPENGL4`) */
  static readonly WEBGL = new RenderTechnology(
    'WEBGL',
    ViewportSetCommands.IDV_RENDER_TECHNOLOGY_OPENGL4,
    Viewport.RENDER_MODE_Z_BUFFER,
    Viewport.RENDER_MODE_RAYTRACING,
  );
  /** HTML canvas drawn with 2D context operations only (Java's `AWT`) */
  static readonly CANVAS2D = new RenderTechnology(
    'CANVAS2D',
    ViewportSetCommands.IDV_RENDER_TECHNOLOGY_AWT,
    Viewport.RENDER_MODE_HIDDEN_LINES,
    Viewport.RENDER_MODE_RAYTRACING,
    Viewport.RENDER_MODE_HIDDEN_SURFACES,
  );

  /** Java's `RenderTechnology.values()` */
  static values(): RenderTechnology[] {
    return [RenderTechnology.WEBGL, RenderTechnology.CANVAS2D];
  }

  private readonly constantName: string;
  private readonly command: string;
  private readonly renderModes: number[];

  private constructor(constantName: string, command: string, ...renderModes: number[]) {
    this.constantName = constantName;
    this.command = command;
    this.renderModes = renderModes;
  }

  /** Java's `Enum.name()` */
  name(): string {
    return this.constantName;
  }

  toString(): string {
    return this.constantName;
  }

  /**
   * @return the standard command of the viewport set that selects this
   * technology (see `ViewportSetCommands`)
   */
  getCommand(): string {
    return this.command;
  }

  /**
   * @return the render modes (`Viewport.RENDER_MODE_*`) available in this
   * technology, the default one first
   */
  getRenderModes(): number[] {
    return this.renderModes.slice();
  }

  /**
   * @param renderMode a `Viewport.RENDER_MODE_*`
   * @param current the technology in use
   * @return the technology offering the render mode: the current one if it
   * offers it (raytracing is offered by every technology), or null if none
   * does
   */
  static forRenderMode(
    renderMode: number,
    current: RenderTechnology | null,
  ): RenderTechnology | null {
    if (current !== null && current.offers(renderMode)) {
      return current;
    }
    for (const technology of RenderTechnology.values()) {
      if (technology.offers(renderMode)) {
        return technology;
      }
    }
    return null;
  }

  private offers(renderMode: number): boolean {
    for (const mode of this.renderModes) {
      if (mode === renderMode) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param command a command of the `VIEWPORT_SET_RENDER_TECHNOLOGY` popup
   * @return the technology selected by the command, or null if it is not a
   * render technology command
   */
  static fromCommand(command: string): RenderTechnology | null {
    for (const technology of RenderTechnology.values()) {
      if (technology.command === command) {
        return technology;
      }
    }
    return null;
  }

  /**
   * Java's `RenderTechnology.valueOf(name.toUpperCase())`, as used by the
   * automation service. As the cpp port does with its `xlib` technology,
   * the names of the other ports are accepted too: `opengl4`, `opengl` and
   * `gpu` for WebGL, and `awt` and `xlib` for the 2D canvas.
   * @param name a technology name, ignoring case
   * @return the technology, or null if the name is unknown
   */
  static fromName(name: string): RenderTechnology | null {
    switch (name.toLowerCase()) {
      case 'webgl':
      case 'opengl4':
      case 'opengl':
      case 'gpu':
        return RenderTechnology.WEBGL;
      case 'canvas2d':
      case 'awt':
      case 'xlib':
        return RenderTechnology.CANVAS2D;
      default:
        return null;
    }
  }
}
