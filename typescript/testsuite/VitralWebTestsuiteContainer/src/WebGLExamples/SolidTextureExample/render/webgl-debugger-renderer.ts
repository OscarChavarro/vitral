import { InfinitePlane, Light, LightGizmoStyle } from '@vitral/base';
import {
  WebGLCameraRenderer,
  WebGLInfinitePlaneGizmoRenderer,
  WebGLLightRenderer,
  WebGLRayGizmoRenderer,
  WebGLRendererConfigurationShaderSelector,
  WebGLSolidTextureRenderer,
} from '@vitral/webgl';
import { OperationMode } from '../model/operation-mode';
import type { SolidTextureModel } from '../model/solid-texture-model';
import { WebGLSolidTextureHudRenderer } from './webgl-solid-texture-hud-renderer';
import { WebGLSolidTexturePlanesRenderer } from './webgl-solid-texture-planes-renderer';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/render/Jogl4DebuggerRenderer.java`.
 *
 * The frame is the Java one: clear, take the two gizmo snapshots so the frame
 * sees a stable pose, then draw either the solid-textured scene with its light
 * gizmos or the stack of texture slices, depending on the operation mode, then
 * the ray and plane gizmos, then the HUD if it is showing. The clipping plane
 * handed to both drawing paths is the plane gizmo's own, and only while that
 * gizmo is visible, exactly as `activeClippingPlane` decides in Java.
 *
 * The `GLEventListener` callbacks keep their Java names and their Java
 * contents; what changes is who calls them, which in the browser is the example
 * component's lifecycle rather than JOGL's animator. A GLSL source arrives over
 * `fetch`, so `display` is asynchronous and `prepare` exists to compile every
 * program this renderer can reach before the first frame; see the
 * drawing-buffer rule recorded for the WebGL example programs.
 *
 * One part of the Java file is deliberately not carried over: its private
 * `drawSimpleBody` and the uniform, material and mesh-upload helpers beneath
 * it, together with the vertex array and three vertex buffers `init` creates
 * for them. Nothing calls any of it — the file was derived from
 * `MeshExample`'s renderer and kept that path, while `display` here goes
 * through `Jogl4SolidTextureRenderer` instead. It is unreachable code with no
 * behavior to preserve, and porting it would mean inventing WebGL substitutes
 * for `glPolygonMode` and `glPointSize` in a path that never runs. The live
 * counterpart of those passes is `MeshExample`'s own `WebGLDebuggerRenderer`.
 */
export class WebGLDebuggerRenderer {
  private readonly hudRenderer: WebGLSolidTextureHudRenderer;
  private readonly planesRenderer: WebGLSolidTexturePlanesRenderer;
  private readonly solidTextureRenderer: WebGLSolidTextureRenderer;

  constructor(private readonly model: SolidTextureModel) {
    this.hudRenderer = new WebGLSolidTextureHudRenderer(model);
    this.planesRenderer = new WebGLSolidTexturePlanesRenderer();
    this.solidTextureRenderer = new WebGLSolidTextureRenderer();
  }

  /**
   * Java's `init(GLAutoDrawable)` generates the vertex array and buffers of the
   * unreachable path described on the class; what this port does there instead
   * is compile the shader programs, which is the work a browser cannot do
   * inside a frame.
   */
  async init(gl: WebGL2RenderingContext): Promise<void> {
    await this.solidTextureRenderer.prepare(gl);
    await this.hudRenderer.prepare(gl);
  }

  async display(gl: WebGL2RenderingContext): Promise<void> {
    gl.enable(gl.DEPTH_TEST);
    gl.depthMask(true);
    gl.depthFunc(gl.LESS);
    gl.disable(gl.CULL_FACE);

    gl.clearColor(0.5, 0.5, 0.9, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    this.acquireGizmoSnapshots();

    if (this.model.getOperationMode() === OperationMode.MESH_MODEL) {
      await this.drawMeshModel(gl);
    } else if (this.model.getOperationMode() === OperationMode.TEXTURE_2D_STACK) {
      await this.planesRenderer.draw(
        gl,
        this.model.getTexture2DStack(),
        this.model.getCamera(),
        this.activeClippingPlane(),
      );
    }
    await this.drawGizmos(gl);
    if (this.model.isHudVisible()) {
      await this.hudRenderer.draw(gl);
    }
  }

  reshape(gl: WebGL2RenderingContext, x: number, y: number, width: number, height: number): void {
    gl.viewport(x, y, width, height);
    this.model.getCamera().updateViewportResize(width, height);
  }

  dispose(gl: WebGL2RenderingContext): void {
    WebGLRendererConfigurationShaderSelector.dispose(gl);
    WebGLCameraRenderer.dispose(gl);
    WebGLRayGizmoRenderer.dispose(gl);
    WebGLInfinitePlaneGizmoRenderer.dispose(gl);
    this.hudRenderer.dispose(gl);
    this.planesRenderer.dispose(gl);
    this.solidTextureRenderer.dispose(gl);
  }

  private async drawMeshModel(gl: WebGL2RenderingContext): Promise<void> {
    const activeLights: Light[] = this.model.getLights();
    if (activeLights.length === 0) {
      return;
    }

    await this.solidTextureRenderer.draw(
      gl,
      this.model.getScene(),
      this.model.getCamera(),
      activeLights,
      this.model.getSolidTextureVolumeRgb8(),
      this.model.getSolidTextureSize(),
      this.model.getSolidTextureRevision(),
      this.activeClippingPlane(),
    );

    for (const light of activeLights) {
      await WebGLLightRenderer.draw(
        gl,
        light,
        this.model.getCamera(),
        LightGizmoStyle.OMNI_BILLBOARD,
      );
    }
  }

  private async drawGizmos(gl: WebGL2RenderingContext): Promise<void> {
    const activeLights: Light[] = this.model.getLights();

    await WebGLRayGizmoRenderer.draw(
      gl,
      this.model.getRayGizmo(),
      this.model.getCamera(),
      activeLights,
    );

    await WebGLInfinitePlaneGizmoRenderer.draw(
      gl,
      this.model.getInfinitePlaneGizmo(),
      this.model.getCamera(),
    );
  }

  /**
   * Applies any pending network update before anything is drawn, so that both
   * gizmo poses are stable for the whole frame.
   */
  private acquireGizmoSnapshots(): void {
    this.model.getRayGizmo().acquireSnapshot();
    this.model.getInfinitePlaneGizmo().acquireSnapshot();
  }

  private activeClippingPlane(): InfinitePlane | null {
    if (!this.model.getInfinitePlaneGizmo().isVisible()) {
      return null;
    }
    return this.model.getInfinitePlaneGizmo().getPlane();
  }
}
