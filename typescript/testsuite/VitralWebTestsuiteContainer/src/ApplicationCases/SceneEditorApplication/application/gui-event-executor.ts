import {
  Arrow,
  Box,
  ByteArrayOutputStream,
  CommandListener,
  Cone,
  EnvironmentPersistence,
  FunctionalExplicitSurface,
  InfinitePlane,
  Logger,
  Matrix4x4d,
  ParametricBiCubicPatch,
  ParametricCurve,
  PolyhedralBoundedSolidEulerOperators,
  PolyhedralBoundedSolidModeler,
  PolyhedralBoundedSolidValidationEngine,
  ProgressMonitorConsole,
  RGBAImageUncompressed,
  RGBColorPalettePersistence,
  SimpleBody,
  SimpleBodyGroup,
  Sphere,
  StringReader,
  Torus,
  VSDK,
  Vector3Dd,
  VoxelVolume,
  Voxelization,
  type Geometry,
  type Light,
  type PolyhedralBoundedSolid,
} from '@vitral/base';
import { WebEnvironmentPersistence } from '@vitral/webgl';
import type { ChosenFile } from '../io/chosen-file';
import { ImageFiles } from '../io/image-files';
import type { ApplicationModel } from '../model/application-model';
import type { SceneHistory } from '../model/history/scene-history';
import { InteractionMode } from '../model/interaction-mode';
import type { Scene } from '../model/scene';

/**
 * Result of executing a command (Java's enum `GuiEventExecutor.CommandResult`).
 */
export enum CommandResult {
  /** The command was executed */
  DONE = 'DONE',
  /** The command was recognized, but it could not be executed */
  FAILED = 'FAILED',
  /** The command is not executed by this class */
  NOT_HANDLED = 'NOT_HANDLED',
}

/**
 * Formats to export the objects of the scene (Java's enum
 * `GuiEventExecutor.ExportFormat`).
 */
export enum ExportFormat {
  OBJ = 'OBJ',
  GTS = 'GTS',
  VTK = 'VTK',
}

/**
 * What the GUI technology presents for this executor (Java's nested interface
 * `GuiEventExecutor.Presenter`).
 */
export interface GuiEventExecutorPresenter {
  /**
   * @param message text to show in the status bar
   */
  showStatusMessage(message: string): void;
}

/**
 * Port of `application.GuiEventExecutor`.
 *
 * Executes the commands of the GUI of the editor (identified by the `IDC_*`
 * names of the I18N GUI definition) that only work over the application model:
 * creation of objects and lights, capture requests, interaction modes, viewport
 * and debugging toggles. It also offers the persistence operations whose files
 * are chosen by the GUI. It does not depend on any GUI or rendering technology:
 * each GUI technology executes here what it does not present itself (file
 * dialogs, windows, look and feel...), and presents the status messages
 * requested through its `Presenter`.
 *
 * What the commands change in the scene (i.e. objects and lights created,
 * objects imported) is recorded in the scene history of the model, so it can be
 * undone.
 *
 * Java reads and writes `java.io.File`s; here a file to read is a `ChosenFile`
 * (fetched from the server or picked from the user's computer, read with
 * `WebEnvironmentPersistence`), and a written file is handed to the user by the
 * installed `ImageFiles` (a download). Those operations are asynchronous.
 */
export class GuiEventExecutor extends CommandListener {
  private readonly model: ApplicationModel;
  private readonly presenter: GuiEventExecutorPresenter;

  /**
   * @param model application model the commands work over
   * @param presenter presents the status messages of the commands
   */
  constructor(model: ApplicationModel, presenter: GuiEventExecutorPresenter) {
    super();
    this.model = model;
    this.presenter = presenter;
  }

  private scene(): Scene {
    return this.model.getScene();
  }

  /**
   * @param label identifier of the command (`IDC_*`)
   * @return true if the command was executed
   */
  override executeCommand(label: string): boolean {
    return this.execute(label) === CommandResult.DONE;
  }

  /**
   * @param label identifier of the command (`IDC_*`)
   * @return whether the command was executed, failed, or is not one of the
   * commands of this class
   */
  execute(label: string): CommandResult {
    const history: SceneHistory = this.model.getEditHistory().getSceneHistory();
    let result: CommandResult;

    history.begin();
    try {
      result = this.executeModelCommand(label);
    } finally {
      history.end(label, false);
    }
    return result;
  }

  private executeModelCommand(label: string): CommandResult {
    //- CREATE --------------------------------------------------------
    if (label === 'IDC_CREATE_SPHERE') {
      this.scene().addThing(new Sphere(1.0));
    } else if (label === 'IDC_CREATE_CONE') {
      this.scene().addThing(new Cone(1, 0, 2));
    } else if (label === 'IDC_CREATE_CYLINDER') {
      this.scene().addThing(new Cone(1, 1, 2));
    } else if (label === 'IDC_CREATE_CUBE') {
      this.scene().addThing(new Box(1, 1, 1));
    } else if (label === 'IDC_CREATE_BOX') {
      this.scene().addThing(new Box(1, 3, 2));
    } else if (label === 'IDC_CREATE_ARROW') {
      this.scene().addThing(new Arrow(0.7, 0.3, 0.05, 0.1));
    } else if (label === 'IDC_CREATE_TORUS') {
      this.scene().addThing(new Torus(2, 1));
    } else if (label === 'IDC_CREATE_PLANE') {
      const plane: InfinitePlane = new InfinitePlane(new Vector3Dd(-0.2, 0, 1), new Vector3Dd(0, 0, -1));
      console.log('' + plane);
      this.scene().addThing(plane);

      /*
            scene().activeCamera.updateVectors();
            InfinitePlane planes[];
            planes = scene().activeCamera.getBoundingPlanes();
            for ( int i = 0; i < 6; i++ ) {
                scene().addThing(planes[i]);
            }
      */
    } else if (label === 'IDC_CREATE_SPHERE_HARMONIC') {
      this.createSphereHarmonicDebugGroup();
    } else if (label === 'IDC_CREATE_PROJECTED_VIEWS') {
      this.model.getDrawingArea().setProjectedViewsDebugRequested(true);
    } else if (label === 'IDC_CREATE_VOLUME') {
      this.createVolume();
    } else if (label === 'IDC_CREATE_BREP') {
      this.createBrep();
    } else if (label === 'IDC_CREATE_PARAMETRICCUBICCURVE') {
      this.createParametricCubicCurve();
    } else if (label === 'IDC_CREATE_FUNCTIONALEXPLICITSURFACE') {
      const functionalSurface: FunctionalExplicitSurface = new FunctionalExplicitSurface('cos((PI*x)/2)');
      functionalSurface.setBounds(-10, -10, -10, 10, 10, 10);
      functionalSurface.setTesselationHint(100, 100);
      const newThing: SimpleBody = this.scene().addThing(functionalSurface);
      newThing.setMaterial(newThing.getMaterial()!.withDoubleSided(true));
    } else if (label === 'IDC_CREATE_PARAMETRICBICUBICPATCH') {
      this.createParametricBiCubicPatch();
    } else if (label === 'IDC_CREATE_OMNILIGHT') {
      const light: Light | null = this.model.addNewLight();
      if (light === null) {
        Logger.reportMessage(this, VSDK.WARNING, 'execute', 'No visible viewport where to create the light');
        return CommandResult.FAILED;
      }
    }
    //- RENDERING -----------------------------------------------------
    else if (label === 'IDC_RENDERING_OBTAINZBUFFERIMAGE') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_PENDING_ZBUFFER_COLOR_IMAGE'));
      this.model.getDrawingArea().setColorCaptureRequested(true);
    } else if (label === 'IDC_RENDERING_OBTAINZBUFFERDEPTHMAP') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_PENDING_ZBUFFER_DEPTH'));
      this.model.getDrawingArea().setDepthCaptureRequested(true);
    } else if (label === 'IDC_RENDERING_OBTAINCONTOURNS') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_PENDING_CONTOURNS'));
      this.model.getDrawingArea().setDepthCaptureRequested(true);
      this.model.getDrawingArea().setContoursRequested(true);
    }
    //-----------------------------------------------------------------
    else if (label === 'IDC_OTHERS_CYCLE_BACKGROUND') {
      this.scene().rotateBackground();
    } else if (label === 'IDC_OTHERS_TOGGLE_TEST_CORRIDOR') {
      if (this.scene().showCorridor === true) {
        this.scene().showCorridor = false;
      } else {
        this.scene().showCorridor = true;
      }
    } else if (label === 'IDC_OTHERS_TOGGLE_GRID') {
      this.model.getDrawingArea().toggleSelectedViewportGrid();
    } else if (label === 'IDC_OTHERS_PRINT_SCENE_ON_CONSOLE') {
      this.scene().print();
    }
    //-----------------------------------------------------------------
    else if (label === 'IDC_TOOLS_CAMERA') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_CAMERA_MODE'));
      this.model.getDrawingArea().setInteractionMode(InteractionMode.CAMERA);
    } else if (label === 'IDC_TOOLS_SELECT') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_SELECTION_MODE'));
      this.model.getDrawingArea().setInteractionMode(InteractionMode.SELECT);
    } else if (label === 'IDC_TOOLS_TRANSLATE') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_TRANSLATION_MODE'));
      this.model.getDrawingArea().setInteractionMode(InteractionMode.TRANSLATE);
    } else if (label === 'IDC_TOOLS_ROTATE') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_ROTATION_MODE'));
      this.model.getDrawingArea().setInteractionMode(InteractionMode.ROTATE);
    } else if (label === 'IDC_TOOLS_SCALE') {
      this.presenter.showStatusMessage(this.model.getI18nContext()!.getMessage('IDM_SCALE_MODE'));
      this.model.getDrawingArea().setInteractionMode(InteractionMode.SCALE);
    } else if (label === 'IDC_TOOLS_RAY') {
      this.model.setWithVisualDebugRay(!this.model.isWithVisualDebugRay());
    } else if (label === 'IDC_NEW_VIEW') {
      this.model.getDrawingArea().addViewport();
    } else if (label === 'IDC_DEL_VIEW') {
      this.model.getDrawingArea().removeLastViewport();
    } else {
      return CommandResult.NOT_HANDLED;
    }
    return CommandResult.DONE;
  }

  private createSphereHarmonicDebugGroup(): void {
    let voxelBody: SimpleBody | null = null;
    const selectedThing: number = this.scene().selectedThings.firstSelected();

    let referenceGeometry: Geometry | null = null;

    if (selectedThing >= 0) {
      voxelBody = this.scene().scene.getSimpleBodies().get(selectedThing);
      referenceGeometry = voxelBody.getGeometry();
    }

    if (referenceGeometry === null || !(referenceGeometry instanceof VoxelVolume) || voxelBody === null) {
      this.presenter.showStatusMessage(
        'ERROR: A VoxelVolume must be selected for spherical harmonic debugging sphere to be created',
      );
      return;
    }
    //- Calculate the VoxelVolume's center of mass ---------------
    const vv: VoxelVolume = referenceGeometry;
    const cm: Vector3Dd = vv.doCenterOfMass();

    //- Calculate average distance from nonzero voxels to cm -----
    // This accounts for scale normalization as in [FUNK2003].4.1.
    let numberOfNonZeroVoxels = 0;
    let averageDistance = 0;

    for (let x = 0; x < vv.getXSize(); x++) {
      for (let y = 0; y < vv.getYSize(); y++) {
        for (let z = 0; z < vv.getZSize(); z++) {
          if (vv.getVoxel(x, y, z) !== 0) {
            const p: Vector3Dd = vv.getVoxelPosition(x, y, z);
            averageDistance += Vector3Dd.distance(cm, p);
            numberOfNonZeroVoxels++;
          }
        }
      }
    }
    averageDistance /= numberOfNonZeroVoxels;

    //- Create spheres -------------------------------------------
    const group: SimpleBodyGroup = new SimpleBodyGroup();
    for (let i = 0; i < 32; i++) {
      const body: SimpleBody = this.addDebugSphere(voxelBody, i, cm, averageDistance);
      group.getBodies().add(body);
    }
    this.scene().debugThingGroups.add(group);
    // Subspheres account for translation & scale
    group.setRotation(voxelBody.getRotation());
  }

  private createVolume(): void {
    //- Select current object, if empty selection take a temp. sphere -
    const selectedThing: number = this.scene().selectedThings.firstSelected();
    let referenceGeometry: Geometry;
    let thing: SimpleBody | null = null;

    if (selectedThing < 0) {
      referenceGeometry = new Sphere(0.5);
    } else {
      thing = this.scene().scene.getSimpleBodies().get(selectedThing);
      referenceGeometry = thing.getGeometry()!;
    }

    //- Calculate transform matrix ------------------------------------
    const minmax: number[] = Array.from(referenceGeometry.getMinMax());
    // Transform from voxelspace to geometry minmax space
    const M: Matrix4x4d = VoxelVolume.getTransformFromVoxelFrameToMinMax(minmax);

    //- Auxiliary variables -------------------------------------------
    const nx = 64;
    const ny = 64;
    const nz = 64;

    //- Primitive rasterization ---------------------------------------
    const vv: VoxelVolume = new VoxelVolume();
    vv.init(nx, ny, nz);

    const reporter: ProgressMonitorConsole = new ProgressMonitorConsole();
    Voxelization.doVoxelization(referenceGeometry, vv, M, reporter);

    //- Append newly created volume to scene, matching reference form -
    const newThing: SimpleBody = this.scene().addThing(vv);
    let pos: Vector3Dd = M.extractTranslation();
    if (thing !== null) {
      pos = pos.add(thing.getPosition());
      newThing.setRotation(thing.getRotation());
      newThing.setScale(thing.getScale());
    }
    newThing.setPosition(pos);
    const size: Vector3Dd = new Vector3Dd(M.get(0, 0), M.get(1, 1), M.get(2, 2));
    newThing.setScale(size);
  }

  private createBrep(): void {
    const brep: PolyhedralBoundedSolid = new Box(0.9, 0.9, 0.9).exportToPolyhedralBoundedSolid();
    let R: Matrix4x4d = new Matrix4x4d();
    R = R.translation(0.55, 0.55, 0.55);
    PolyhedralBoundedSolidModeler.applyTransformation(brep, R);
    //- Cube modification to holed box ----------------------------
    PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 5, 9, new Vector3Dd(0.3, 0.3, 1));
    PolyhedralBoundedSolidEulerOperators.kemr(brep, 6, 6, 5, 9, 9, 5);
    PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 9, 10, new Vector3Dd(0.8, 0.3, 1));
    PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 10, 11, new Vector3Dd(0.8, 0.8, 1));
    PolyhedralBoundedSolidEulerOperators.smev(brep, 6, 11, 12, new Vector3Dd(0.3, 0.8, 1));
    PolyhedralBoundedSolidEulerOperators.mef(brep, 6, 6, 9, 10, 12, 11, 7);

    //- Box extrusion ---------------------------------------------
    PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 9, 13, new Vector3Dd(0.3, 0.3, 0.1));
    PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 10, 14, new Vector3Dd(0.8, 0.3, 0.1));
    PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 13, 9, 14, 10, 8);
    PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 11, 15, new Vector3Dd(0.8, 0.8, 0.1));
    PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 14, 10, 15, 11, 9);
    PolyhedralBoundedSolidEulerOperators.smev(brep, 7, 12, 16, new Vector3Dd(0.3, 0.8, 0.1));
    PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 15, 11, 16, 12, 10);
    PolyhedralBoundedSolidEulerOperators.mef(brep, 7, 7, 13, 14, 16, 12, 11);

    //- Hole creation ---------------------------------------------
    PolyhedralBoundedSolidEulerOperators.kfmrh(brep, 2, 11);

    R = R.translation(-0.55, -0.55, -0.55);
    PolyhedralBoundedSolidModeler.applyTransformation(brep, R);
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(brep);

    //brep = createCircle(0.5, 0.5, 0.5, 0.1, 12);

    //
    PolyhedralBoundedSolidValidationEngine.validateIntermediate(brep);
    this.scene().addThing(brep);
  }

  private createParametricCubicCurve(): void {
    // Case 1: curve hard-coded in source
    const curve: ParametricCurve = new ParametricCurve();
    // Note that an HERMITE curve uses tangent vectors, BEZIER curves
    // uses control points (tangent vectors are control point minus
    // knot position)
    curve.addPoint(
      [
        new Vector3Dd(0, 0, 0), // Position 0
        new Vector3Dd(0, 0, 0), // Not used
        new Vector3Dd(0, 1, 0), // Salient tangent end
      ],
      ParametricCurve.BEZIER,
    );
    curve.addPoint(
      [
        new Vector3Dd(1, 1, 0), // Position 1
        new Vector3Dd(0, 1, 0), // Entry tangent end
        new Vector3Dd(2, 1, 0), // Salient tangent end
      ],
      ParametricCurve.BEZIER,
    );
    curve.addPoint(
      [
        new Vector3Dd(2, 0, 1), // Position 2
        new Vector3Dd(2, 0, 0), // Entry tangent end
        new Vector3Dd(0, 0, 0), // Not used
      ],
      ParametricCurve.BEZIER,
    );

    this.scene().addThing(curve);

    // Java keeps here, commented out, the export of the curve to
    // "curveTest.xml" and its import back, with `XmlManager`
  }

  private createParametricBiCubicPatch(): void {
    //- Create a Ferguson patch ---------------------------------------
    const contourHermiteLine: ParametricCurve = new ParametricCurve();
    // Note that an HERMITE curve uses tangent vectors, BEZIER curves
    // uses control points (tangent vectors are control point minus
    // knot position)
    contourHermiteLine.addPoint(
      [
        new Vector3Dd(0, 0, 0), // Position 0
        new Vector3Dd(0, -1, 0), // Entry tangent
        new Vector3Dd(1, 0, 0), // Salient tangent
      ],
      ParametricCurve.HERMITE,
    );
    contourHermiteLine.addPoint(
      [
        new Vector3Dd(1, 0, 0), // Position 1
        new Vector3Dd(1, 0, 0), // Entry tangent
        new Vector3Dd(0, 1, 0), // Salient tangent
      ],
      ParametricCurve.HERMITE,
    );
    contourHermiteLine.addPoint(
      [
        new Vector3Dd(1, 1, 0.4), // Position 2
        new Vector3Dd(0, 1, 0), // Entry tangent
        new Vector3Dd(-1, 0, 0), // Salient tangent
      ],
      ParametricCurve.HERMITE,
    );
    contourHermiteLine.addPoint(
      [
        new Vector3Dd(0, 1, 0), // Position 3
        new Vector3Dd(-1, 0, 0), // Entry tangent
        new Vector3Dd(0, -1, 0), // Salient tangent
      ],
      ParametricCurve.HERMITE,
    );

    contourHermiteLine.addPoint(contourHermiteLine.getPoint(0), ParametricCurve.HERMITE);

    const patch: ParametricBiCubicPatch = new ParametricBiCubicPatch();
    patch.buildFergusonPatch(contourHermiteLine);
    patch.setApproximationSteps(20);
    //scene().addThing(contourHermiteLine);
    const newThing: SimpleBody = this.scene().addThing(patch);
    newThing.setMaterial(newThing.getMaterial()!.withDoubleSided(true));
    //-----------------------------------------------------------------

    // Java keeps here, commented out, the creation of a Bezier patch from
    // a 4x4 grid of control points, and the export of the patch to
    // "patchTest.xml" and its import back, with `XmlManager`
  }

  /**
   * Adds the objects of a file to the scene.
   * @param file 3ds, vtk, gts, obj or ply file
   * @throws Error if the file can not be read
   */
  async importObjects(file: ChosenFile): Promise<void> {
    const history: SceneHistory = this.model.getEditHistory().getSceneHistory();

    history.begin();
    try {
      if (file.path !== null) {
        await WebEnvironmentPersistence.importEnvironment(file.path, this.scene().scene);
      } else {
        await WebEnvironmentPersistence.importEnvironmentFromBytes(file.name, await file.readBytes(),
          this.scene().scene);
      }
    } finally {
      history.end('Import of ' + file.name, false);
    }
  }

  /**
   * Writes the objects of the scene to a file.
   * @param fileName name of the destination file
   * @param format format of the file
   * @throws Error if the file can not be written
   */
  async exportObjects(fileName: string, format: ExportFormat): Promise<void> {
    const fos: ByteArrayOutputStream = new ByteArrayOutputStream();

    switch (format) {
      case ExportFormat.OBJ:
        EnvironmentPersistence.exportEnvironmentObj(fos, this.scene().scene);
        break;
      case ExportFormat.GTS:
        EnvironmentPersistence.exportEnvironmentGts(fos, this.scene().scene);
        break;
      default:
        EnvironmentPersistence.exportEnvironmentVtk(fos, this.scene().scene);
        break;
    }

    fos.close();
    await ImageFiles.get().writeBytes(fileName, fos.toByteArray());
  }

  /**
   * Replaces the palette used to present depth maps.
   * @param file Gimp palette (gpl) file
   * @throws Error if the file can not be read
   */
  async loadPalette(file: ChosenFile): Promise<void> {
    this.model.setPalette(RGBColorPalettePersistence.importGimpPalette(new StringReader(await file.readText())));
  }

  private addDebugSphere(voxelBody: SimpleBody, groupIndex: number, cm: Vector3Dd, averageDistance: number): SimpleBody {
    const r: number = (groupIndex / 31.0) * (2 * averageDistance);
    const vv: VoxelVolume = voxelBody.getGeometry() as VoxelVolume;
    let S: Matrix4x4d = new Matrix4x4d();

    const sphere: Sphere = new Sphere(r);
    const body: SimpleBody = new SimpleBody();
    body.setGeometry(sphere);
    body.setMaterial(this.scene().defaultMaterial());
    body.setMaterial(body.getMaterial()!.withDoubleSided(true));
    let scale: Vector3Dd = voxelBody.getScale();
    S = S.scale(scale.x(), scale.y(), scale.z());
    scale = scale.multiply(r);
    body.setScale(scale);
    const cm2: Vector3Dd = S.multiply(cm);
    const pos: Vector3Dd = voxelBody.getPosition().add(cm2);
    body.setPosition(pos);
    body.setRotation(new Matrix4x4d());
    body.setRotationInverse(new Matrix4x4d());
    body.setName('Debug sphere for harmonics ' + groupIndex);

    const texture: RGBAImageUncompressed = new RGBAImageUncompressed();
    texture.init(64, 64);

    //- Build sphere's texture map from voxel grid --------------------
    for (let s = 0; s < texture.getXSize(); s++) {
      for (let t = 0; t < texture.getYSize(); t++) {
        const tetha: number = (s / texture.getXSize()) * Math.PI * 2;
        const phi: number = (t / texture.getYSize()) * Math.PI;
        let p: Vector3Dd = Vector3Dd.fromSpherical(r, tetha, phi);
        p = cm.add(p);
        const voxelValue: number = vv.getVoxelAtPosition(p.x(), p.y(), p.z());
        if (voxelValue < 128) {
          texture.putPixel(s, t, 0, 0, 0, 0);
        } else {
          // Java's (byte)255
          texture.putPixel(s, t, 0, 0, 0, -1);
        }
      }
    }

    //-----------------------------------------------------------------
    body.setTexture(texture);
    return body;
  }
}
