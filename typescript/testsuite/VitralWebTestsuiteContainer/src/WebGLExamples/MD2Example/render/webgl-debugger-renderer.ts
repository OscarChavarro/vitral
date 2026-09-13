import { LightGizmoStyle, type Light } from '@vitral/base';
import { WebGLLightRenderer, WebGLMd2MeshRenderer } from '@vitral/webgl';
import type { DebuggerModel } from '../model/debugger-model';
import { WebGLDebuggerHudRenderer } from './webgl-debugger-hud-renderer';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MD2Example/src/render/Jogl4DebuggerRenderer.java`.
 *
 * The `GLEventListener` callbacks keep their Java names and their Java
 * contents; what changes is who calls them, which in the browser is the example
 * component's lifecycle rather than JOGL's animator.
 *
 * Java compiles its shaders inside the draw call, reading the sources from
 * disk; a browser reaches a GLSL source over `fetch`, and a frame must not
 * await a network read, because the drawing buffer is presented and cleared at
 * task boundaries. So `init` is asynchronous and asks both the MD2 mesh
 * renderer and the HUD renderer to compile what they will need, and `display`
 * is asynchronous because the calls it makes are.
 */
export class WebGLDebuggerRenderer {
  private readonly hudRenderer: WebGLDebuggerHudRenderer;

  constructor(private readonly model: DebuggerModel) {
    this.hudRenderer = new WebGLDebuggerHudRenderer(model);
  }

  async init(gl: WebGL2RenderingContext): Promise<void> {
    WebGLMd2MeshRenderer.initGL(gl, this.model.getMd2Mesh());
    await WebGLMd2MeshRenderer.prepare(gl);
    this.hudRenderer.init(gl);
    await this.hudRenderer.prepare(gl);
  }

  async display(gl: WebGL2RenderingContext): Promise<void> {
    gl.enable(gl.DEPTH_TEST);
    gl.clearColor(0.5, 0.5, 0.9, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT);
    gl.clear(gl.DEPTH_BUFFER_BIT);
    const lights: Light[] = this.model.getLights();
    if (lights.length !== 0) {
      await WebGLMd2MeshRenderer.draw(
        gl,
        this.model.getMd2Mesh(),
        this.model.getCamera(),
        lights[0]!,
        this.model.getQualitySelection(),
        this.model.getX(),
      );

      for (const light of lights) {
        await WebGLLightRenderer.draw(
          gl,
          light,
          this.model.getCamera(),
          LightGizmoStyle.OMNI_BILLBOARD,
        );
      }
    }
    await this.hudRenderer.draw(gl);
  }

  reshape(gl: WebGL2RenderingContext, width: number, height: number): void {
    gl.viewport(0, 0, width, height);
    this.model.getCamera().updateViewportResize(width, height);
    this.hudRenderer.updateViewportSize(width, height);
  }

  dispose(gl: WebGL2RenderingContext): void {
    WebGLMd2MeshRenderer.dispose(gl);
    this.hudRenderer.dispose(gl);
  }
}
