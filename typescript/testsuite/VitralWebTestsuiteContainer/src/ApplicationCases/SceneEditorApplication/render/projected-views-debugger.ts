//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

import {
  ColorRgb,
  ImageProcessing,
  IndexedColorImageUncompressed,
  Matrix4x4d,
  NormalMap,
  RendererConfiguration,
  SimpleBody,
  SimpleBodyGroup,
  Triangle,
  TriangleMesh,
  Vector3Dd,
  Vertex,
  type Image,
  type Quaterniond,
  type RGBAImageUncompressed,
  type SimpleMaterial,
  type ZBuffer,
} from '@vitral/base';
import type { ApplicationModel } from '../model/application-model';
import type { DrawingArea } from '../model/drawing-area';
import { ProjectedViewsDebugPlan, type ViewPlacement } from '../model/projected-views-debug-plan';
import type { Scene } from '../model/scene';
import type { DrawingAreaHost } from './drawing-area-host';
import type { ProjectedViewRenderer } from './projected-view-renderer';

/**
 * Port of `render.ProjectedViewsDebugger`.
 *
 * Debugging tool that renders the projected views of the selected bodies (13
 * views from the faces and corners of their bounding cube) and presents them as
 * textured boxes in a visual debug group of the scene. Views are rendered
 * through a `ProjectedViewRenderer`, so this class does not depend on the
 * rendering technology (and it is asynchronous, as WebGL drawing is).
 */
export class ProjectedViewsDebugger {
  private static readonly DO_DISTANCE_FIELD: boolean = false;
  private static readonly DISTANCE_FIELD_SIDE = 320;

  private readonly scene: Scene;
  private readonly drawingArea: DrawingArea;
  private readonly host: DrawingAreaHost;
  private readonly quality: RendererConfiguration;
  private readonly viewSize: number;
  private readonly isTransparent: boolean;

  constructor(model: ApplicationModel, host: DrawingAreaHost) {
    this.scene = model.getScene();
    this.drawingArea = model.getDrawingArea();
    this.host = host;

    this.quality = new RendererConfiguration();
    this.quality.setWires(false);
    this.quality.setSurfaces(true);
    this.isTransparent = true;

    if (ProjectedViewsDebugger.DO_DISTANCE_FIELD) {
      this.viewSize = ProjectedViewsDebugger.DISTANCE_FIELD_SIDE;
    } else {
      this.viewSize = 640;
    }
  }

  /**
   * Creates the debug group with the projected views of the selected bodies,
   * if it was requested in the drawing area.
   * @param renderer renders the projected views
   */
  async debugIfNeeded(renderer: ProjectedViewRenderer): Promise<void> {
    //-----------------------------------------------------------------
    if (!this.drawingArea.isProjectedViewsDebugRequested()) {
      return;
    }
    this.drawingArea.setProjectedViewsDebugRequested(false);

    //-----------------------------------------------------------------
    const selectedThing: number = this.scene.selectedThings.firstSelected();
    let referenceBody: SimpleBody | null = null;

    if (selectedThing >= 0) {
      referenceBody = this.scene.scene.getSimpleBodies().get(selectedThing);
    }

    if (referenceBody === null) {
      this.host.showStatusMessage(
        'ERROR: An object must be selected for projected views debugging to be created',
      );
    } else {
      const bodySet: SimpleBodyGroup = new SimpleBodyGroup();

      for (let i = 0; i < this.scene.selectedThings.size(); i++) {
        if (this.scene.selectedThings.isSelected(i)) {
          referenceBody = this.scene.scene.getSimpleBodies().get(i);
          bodySet.getBodies().add(referenceBody);
        }
      }

      const group: SimpleBodyGroup | null = await this.addDebugProjectedView(renderer, bodySet);

      if (group !== null) {
        this.scene.debugThingGroups.add(group);
      } else {
        this.host.showStatusMessage(
          'ERROR: cannot create Pbuffer, you need recent 3D hardware acceleration for this function',
        );
      }
    }
  }

  private async createProjectedView(
    renderer: ProjectedViewRenderer,
    referenceBodies: SimpleBodyGroup,
    cam: number,
  ): Promise<Image> {
    //- Will render a normalized body inside the unit cube ------------
    const bodySet: SimpleBodyGroup = new SimpleBodyGroup();

    {
      //-----------------------------------------------------------------
      const minmax: number[] = referenceBodies.getMinMax();
      const min: Vector3Dd = new Vector3Dd(minmax[0], minmax[1], minmax[2]);
      const max: Vector3Dd = new Vector3Dd(minmax[3], minmax[4], minmax[5]);
      let s: Vector3Dd = new Vector3Dd(max.x() - min.x(), max.y() - min.y(), max.z() - min.z());

      let maxsize: number = s.x();
      if (s.y() > maxsize) maxsize = s.y();
      if (s.z() > maxsize) maxsize = s.z();
      // The 95% scale factor is to allow a full render of the object to
      // fit inside the rendered view
      s = new Vector3Dd((2 / maxsize) * 0.95, (2 / maxsize) * 0.95, (2 / maxsize) * 0.95);

      let p: Vector3Dd = max.add(min);
      p = p.multiply(-1 / maxsize);

      bodySet.setPosition(p);
      bodySet.setScale(s);
      //-----------------------------------------------------------------
      for (let i = 0; i < referenceBodies.getBodies().size(); i++) {
        const referenceBody: SimpleBody = referenceBodies.getBodies().get(i);
        const framedBody: SimpleBody = new SimpleBody();
        framedBody.setGeometry(referenceBody.getGeometry());
        framedBody.setPosition(referenceBody.getPosition());
        framedBody.setRotation(referenceBody.getRotation());
        framedBody.setRotationInverse(referenceBody.getRotationInverse());
        framedBody.setMaterial(this.scene.defaultMaterial());
        bodySet.getBodies().add(framedBody);
      }
      //-----------------------------------------------------------------
      const Mset: Matrix4x4d = bodySet.getTransformationMatrix();

      for (let i = 0; i < referenceBodies.getBodies().size(); i++) {
        const referenceBody: SimpleBody = referenceBodies.getBodies().get(i);
        if (cam === 1) {
          const copiedBody: SimpleBody = this.scene.addThing(referenceBody.getGeometry()!);
          const Mbody: Matrix4x4d = referenceBody.getTransformationMatrix();
          let M: Matrix4x4d = Mset.multiply(Mbody);
          p = M.extractTranslation();
          M = M.withVal(0, 3, 0.0);
          M = M.withVal(1, 3, 0.0);
          M = M.withVal(2, 3, 0.0);
          const q: Quaterniond = M.exportToQuaternion().normalized();
          let R: Matrix4x4d = new Matrix4x4d();
          R = R.importFromQuaternion(q);
          s = new Vector3Dd(M.get(0, 0), M.get(1, 1), M.get(2, 2));

          copiedBody.setPosition(p);
          copiedBody.setScale(s);
          copiedBody.setRotation(R);
        }
      }

      //-----------------------------------------------------------------
    }

    //- Render will proceed in an offscreen frame buffer ---------------
    const projectedView: Image = this.createContourImage(
      await renderer.renderDepth(
        bodySet,
        ProjectedViewsDebugPlan.createCamera(cam),
        this.quality,
        this.viewSize,
        this.viewSize,
      ),
    );

    //-----------------------------------------------------------------
    let finalImage: Image;
    if (!ProjectedViewsDebugger.DO_DISTANCE_FIELD) {
      finalImage = projectedView;
    } else {
      console.log('Processing maps for view ' + cam + '... ');
      const side: number = ProjectedViewsDebugger.DISTANCE_FIELD_SIDE;
      const distanceFieldIndexed: IndexedColorImageUncompressed = new IndexedColorImageUncompressed();
      distanceFieldIndexed.init(side, side);
      ImageProcessing.processDistanceFieldWithArray(projectedView, distanceFieldIndexed, 1);
      ImageProcessing.gammaCorrection(distanceFieldIndexed, 2.0);

      const distanceFieldRgba: RGBAImageUncompressed = distanceFieldIndexed.exportToRgbaImage();

      for (let x = 0; x < distanceFieldRgba.getXSize(); x++) {
        for (let y = 0; y < distanceFieldRgba.getYSize(); y++) {
          if (distanceFieldIndexed.getPixel(x, y) < 1) {
            distanceFieldRgba.putPixel(x, y, 255, 0, 0, 128);
          }
        }
      }
      finalImage = distanceFieldRgba;
      console.log('Ok!');
    }

    //- Obtain the rendered image --------------------------------------
    return finalImage;
  }

  /**
   * Keeps just the silhouette of a rendered view: its depth frontier border.
   * @param depth depth buffer of the rendered view
   * @return contour image, as the gradient of the silhouette
   */
  private createContourImage(depth: ZBuffer): Image {
    const zbuffer: IndexedColorImageUncompressed = depth.exportIndexedColorImage();

    //- Erase internal details: keep just the depth frontier border ---
    for (let x = 0; x < zbuffer.getXSize(); x++) {
      for (let y = 0; y < zbuffer.getYSize(); y++) {
        const val: number = zbuffer.getPixel(x, y);
        if (val < 255) {
          zbuffer.putPixelByte(x, y, 0);
        } else {
          // Java's (byte)255
          zbuffer.putPixelByte(x, y, -1);
        }
      }
    }

    //- Get contourns from depth buffer's gradient --------------------
    const nm: NormalMap = new NormalMap();
    nm.importBumpMap(zbuffer, new Vector3Dd(1, 1, 0.1));

    //- Calculate borders (contourns) ---------------------------------
    if (this.isTransparent) {
      return nm.exportToRgbaImageGradient();
    }
    return nm.exportToRgbImageGradient();
  }

  private async addDebugProjectedView(
    renderer: ProjectedViewRenderer,
    referenceBodies: SimpleBodyGroup,
  ): Promise<SimpleBodyGroup | null> {
    const group: SimpleBodyGroup = new SimpleBodyGroup();
    for (let i = 1; i <= ProjectedViewsDebugPlan.VIEW_COUNT; i++) {
      const placement: ViewPlacement = ProjectedViewsDebugPlan.getPlacement(i)!;
      const R: Matrix4x4d = placement.getRotation();

      //-----------------------------------------------------------------
      const texture: Image | null = await this.createProjectedView(renderer, referenceBodies, i);
      if (texture === null) {
        return null;
      }

      //-----------------------------------------------------------------
      const n: Vector3Dd = new Vector3Dd(0, 0, 1);
      const vertexArray: Vertex[] = [
        new Vertex(new Vector3Dd(-1, -1, 0), n, 0.0, 0.0),
        new Vertex(new Vector3Dd(1, -1, 0), n, 1.0, 0.0),
        new Vertex(new Vector3Dd(1, 1, 0), n, 1.0, 1.0),
        new Vertex(new Vector3Dd(-1, 1, 0), n, 0.0, 1.0),
      ];
      const triangleArray: Triangle[] = [new Triangle(0, 1, 2), new Triangle(2, 3, 0)];
      const textureArray: Image[] = [texture];
      const textureRanges: number[][] = [[2, 1]];
      const materialArray: SimpleMaterial[] = [this.scene.defaultMaterial()];
      materialArray[0] = materialArray[0].withDoubleSided(true);
      materialArray[0] = materialArray[0].withAmbient(new ColorRgb(1, 1, 1));
      materialArray[0] = materialArray[0].withDiffuse(new ColorRgb(1, 1, 1));
      materialArray[0] = materialArray[0].withSpecular(new ColorRgb(1, 1, 1));
      const materialRanges: number[][] = [[2, 0]];

      const mesh: TriangleMesh = new TriangleMesh();
      mesh.setVertexes(vertexArray, true, false, false, true);
      mesh.setTriangles(triangleArray);
      mesh.setTextures(textureArray);
      mesh.setTextureRanges(textureRanges);
      mesh.setMaterials(materialArray);
      mesh.setMaterialRanges(materialRanges);

      //-----------------------------------------------------------------
      const boxBody: SimpleBody = new SimpleBody();
      boxBody.setGeometry(mesh);
      boxBody.setPosition(placement.getPosition());
      boxBody.setScale(placement.getScale());
      boxBody.setRotation(R);
      boxBody.setRotationInverse(R.inverse());
      boxBody.setMaterial(this.scene.defaultMaterial());
      boxBody.setMaterial(boxBody.getMaterial()!.withDoubleSided(true));
      boxBody.setMaterial(boxBody.getMaterial()!.withAmbient(new ColorRgb(1, 1, 1)));
      boxBody.setMaterial(boxBody.getMaterial()!.withDiffuse(new ColorRgb(1, 1, 1)));
      boxBody.setMaterial(boxBody.getMaterial()!.withSpecular(new ColorRgb(1, 1, 1)));
      boxBody.setName('Proyected view box');
      boxBody.setTexture(texture);
      //-----------------------------------------------------------------
      group.getBodies().add(boxBody);
    }
    return group;
  }
}
