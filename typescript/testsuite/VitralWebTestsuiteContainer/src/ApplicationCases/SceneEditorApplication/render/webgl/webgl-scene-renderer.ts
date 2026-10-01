import {
  LightGizmoStyle,
  Matrix4x4d,
  RendererConfiguration,
  RGBImageUncompressed,
  Sphere,
  type Camera,
  type Geometry,
  type Image,
  type Light,
  type SimpleBody,
  type SimpleBodyGroup,
} from '@vitral/base';
import {
  WebGLBackgroundRenderer,
  WebGLGeometryRenderer,
  WebGLLightRenderer,
  WebGLMinMaxRenderer,
  WebGLSelectionCornersRenderer,
} from '@vitral/webgl';
import type { Scene } from '../../model/scene';
import type { BodyEditFeedbackProvider } from '../body-edit-feedback-provider';
import { WebGLRenderPrimitiveRenderer } from './webgl-render-primitive-renderer';

/**
 * Port of `render.jogl.Jogl4SceneRenderer`.
 *
 * Draws the scene of the editor into the current viewport with the WebGL
 * pipeline: every geometry goes through `WebGLGeometryRenderer`, so all of them
 * honor the same bits of the `RendererConfiguration` of the viewport. Drawing
 * is asynchronous, as every WebGL renderer of the toolkit is.
 */
export class WebGLSceneRenderer {
  private constructor() {}

  /**
   * Draws the background, the lights and the bodies of the scene, with the
   * feedback of the editor over the body under edition.
   */
  private static async drawBase(
    gl: WebGL2RenderingContext,
    s: Scene,
    editor: BodyEditFeedbackProvider | null,
  ): Promise<void> {
    //- Draw scene background -----------------------------------------
    await WebGLBackgroundRenderer.draw(gl, s.scene.getBackgrounds().get(s.scene.getActiveBackgroundIndex()));

    gl.enable(gl.DEPTH_TEST);
    gl.depthMask(true);

    //- Draw scene bodies ---------------------------------------------
    const lights = s.scene.getLights();

    for (let i = 0; i < s.scene.getSimpleBodies().size(); i++) {
      const quality: RendererConfiguration = s.qualityTemplate.clone() as RendererConfiguration;

      quality.setSelectionCorners(s.selectedThings.isSelected(i));
      const gi: SimpleBody = s.scene.getSimpleBodies().get(i);

      await WebGLSceneRenderer.drawBody(gl, gi, s.activeCamera, lights, quality);
      if (editor !== null && editor.getTarget() === gi) {
        await WebGLRenderPrimitiveRenderer.drawAll(gl, editor.buildEditFeedback(), s.activeCamera, lights, quality);
      }
    }
  }

  /**
   * Draws one body of the scene.
   *
   * @param gl WebGL context
   * @param body body to draw
   * @param camera camera that views the body
   * @param lights lights of the scene, or null or empty for a light at the camera
   * @param quality bits of rendering configuration
   * @param parentTransform transformation of the group of the body (Java's
   * private overload); the identity for a body of the scene
   */
  static async drawBody(
    gl: WebGL2RenderingContext,
    body: SimpleBody,
    camera: Camera,
    lights: Iterable<Light | null> | null,
    quality: RendererConfiguration,
    parentTransform: Matrix4x4d = Matrix4x4d.identityMatrix(),
  ): Promise<void> {
    const transform: Matrix4x4d = parentTransform.multiply(body.getTransformationMatrix());
    const geometry: Geometry | null = body.getGeometry();
    const texture: Image | null = body.getTexture();
    const textureMap: RGBImageUncompressed | null = texture instanceof RGBImageUncompressed ? texture : null;

    await WebGLGeometryRenderer.draw(
      gl,
      geometry,
      camera,
      lights,
      body.getMaterial(),
      quality,
      textureMap,
      body.getNormalMapRgb(),
      transform,
    );
  }

  /**
   * Draws all the bodies of a group, with the transformation of the group. The
   * bounding volume and the selection corners are drawn once around the whole
   * group, not around each body.
   *
   * @param gl WebGL context
   * @param group group to draw
   * @param camera camera that views the group
   * @param lights lights of the scene, or null or empty for a light at the camera
   * @param quality bits of rendering configuration
   */
  static async drawBodyGroup(
    gl: WebGL2RenderingContext,
    group: SimpleBodyGroup,
    camera: Camera,
    lights: Iterable<Light | null> | null,
    quality: RendererConfiguration,
  ): Promise<void> {
    const memberQuality: RendererConfiguration = quality.clone() as RendererConfiguration;
    memberQuality.setSelectionCorners(false);
    memberQuality.setBoundingVolume(false);

    for (const body of group.getBodies()) {
      await WebGLSceneRenderer.drawBody(gl, body, camera, lights, memberQuality, group.getTransformationMatrix());
    }
    if (quality.isBoundingVolumeSet()) {
      await WebGLMinMaxRenderer.drawMinMax(gl, group.getMinMax(), camera, group.getTransformationMatrix());
    }
    if (quality.isSelectionCornersSet()) {
      await WebGLSelectionCornersRenderer.drawMinMax(gl, group.getMinMax(), camera, group.getTransformationMatrix());
    }
  }

  /**
   * Draws the scene into the current viewport.
   * @param gl WebGL context
   * @param s scene to draw
   * @param editor editor of the selected body, whose feedback is drawn over
   * it, or null if there is none
   */
  static async draw(gl: WebGL2RenderingContext, s: Scene, editor: BodyEditFeedbackProvider | null): Promise<void> {
    await s.activateSelectedBackground();

    await WebGLSceneRenderer.drawBase(gl, s, editor);
    await WebGLSceneRenderer.drawLightsAndDebugEntities(gl, s);
  }

  /**
   * Draws, over an image of the scene computed by other means (i.e. the
   * raytracer, whose depth is already in the depth buffer), the editor
   * elements that are not part of the rendered scene: the elements asked by
   * the `RendererConfiguration` of the viewport that are not surfaces (selection
   * corners, bounding volumes, normals), the feedback of the body under
   * edition, the light gizmos and the visual debug entities. They are depth
   * tested against the image, as they are over the rasterized scene.
   * @param gl WebGL context
   * @param s scene whose editor elements are drawn
   * @param editor editor of the selected body, whose feedback is drawn over
   * it, or null if there is none
   */
  static async drawEditorOverlays(
    gl: WebGL2RenderingContext,
    s: Scene,
    editor: BodyEditFeedbackProvider | null,
  ): Promise<void> {
    const lights = s.scene.getLights();

    gl.enable(gl.DEPTH_TEST);
    gl.depthMask(true);

    for (let i = 0; i < s.scene.getSimpleBodies().size(); i++) {
      const quality: RendererConfiguration = s.qualityTemplate.clone() as RendererConfiguration;
      // The body itself is in the image: only its annotations are drawn
      quality.setSurfaces(false);
      quality.setWires(false);
      quality.setPoints(false);
      quality.setSelectionCorners(s.selectedThings.isSelected(i));
      const gi: SimpleBody = s.scene.getSimpleBodies().get(i);

      if (
        quality.isSelectionCornersSet() ||
        quality.isBoundingVolumeSet() ||
        quality.isNormalsSet() ||
        quality.isTrianglesNormalsSet()
      ) {
        await WebGLSceneRenderer.drawBody(gl, gi, s.activeCamera, lights, quality);
      }
      if (editor !== null && editor.getTarget() === gi) {
        await WebGLRenderPrimitiveRenderer.drawAll(gl, editor.buildEditFeedback(), s.activeCamera, lights, quality);
      }
    }

    await WebGLSceneRenderer.drawLightsAndDebugEntities(gl, s);
  }

  /**
   * Draws the light gizmos and the visual debug entities of the scene.
   */
  private static async drawLightsAndDebugEntities(gl: WebGL2RenderingContext, s: Scene): Promise<void> {
    //- Draw 3D Gizmos ------------------------------------------------
    s.selectedLights.sync();
    // The model owns the gizmo size, so picking matches what is drawn
    WebGLLightRenderer.setScale(s.getLightGizmoScale());
    for (let i = 0; i < s.scene.getLights().size(); i++) {
      await WebGLLightRenderer.draw(gl, s.scene.getLights().get(i), s.activeCamera,
        LightGizmoStyle.OMNI_BILLBOARD, s.selectedLights.isSelected(i));
    }

    //- Draw visual debug entities (usually transparent) --------------
    const lights = s.scene.getLights();

    for (let i = 0; i < s.debugThingGroups.size(); i++) {
      const quality: RendererConfiguration = s.qualityTemplate.clone() as RendererConfiguration;

      quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
      quality.setSelectionCorners(s.selectedDebugThingGroups.isSelected(i));
      const ggi: SimpleBodyGroup = s.debugThingGroups.get(i);
      if (ggi.getBodies().get(0).getGeometry() instanceof Sphere) {
        gl.disable(gl.DEPTH_TEST);
      }
      await WebGLSceneRenderer.drawBodyGroup(gl, ggi, s.activeCamera, lights, quality);
      gl.enable(gl.DEPTH_TEST);
    }
  }
}
