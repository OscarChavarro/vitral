import {
  ArrayList,
  ColorRgb,
  Cone,
  Double,
  JavaMath,
  PointLight,
  RendererConfiguration,
  ShadingType,
  Sphere,
  Vector3Dd,
  Viewport,
  type Geometry,
  type Light,
  type SimpleBody,
  type ViewportSet,
} from '@vitral/base';
import { ImageFiles } from '../../io/image-files';
import type { EditHistory } from '../../model/history/edit-history';
import type { UndoQueue } from '../../model/history/undo-queue';
import { InteractionMode } from '../../model/interaction-mode';
import { javaSimpleClassName } from '../../model/java-class-name';
import type { Scene } from '../../model/scene';
import { SceneLightFactory } from '../../model/scene-light-factory';
import { GuiEventExecutor, type CommandResult } from '../gui-event-executor';
import type { HtmlWebGLApplicationController } from '../html-webgl-application-controller';
import type { HtmlWebGLSceneEditorApplication } from '../html-webgl-scene-editor-application';

/**
 * Port of `application.mcp.AwtJogl4VitralEditorMCPProtocol`.
 *
 * Handles the requests of the automation service: reads JSON-RPC requests,
 * one per line, executes the tools (a page has one thread: Java's
 * `invokeAndWait` on the thread of the GUI is a plain call) and answers one
 * JSON line per response (see `MCP.md`). The requests are read with Java's
 * regular expressions and the responses are built as Java builds them, so an
 * agent sees the same protocol. Tools that wait for the browser (raytracing,
 * exports, language changes) answer when they are done, so `handle` is
 * asynchronous. Paths name the downloads that hand the written files to the
 * user.
 */
export class HtmlWebGLVitralEditorMCPProtocol {
  private readonly parent: HtmlWebGLSceneEditorApplication;

  constructor(parent: HtmlWebGLSceneEditorApplication) {
    this.parent = parent;
  }

  /**
   * @param request one JSON-RPC request
   * @return its JSON-RPC response
   */
  async handle(request: string): Promise<string> {
    const id: string = HtmlWebGLVitralEditorMCPProtocol.idProperty(request);
    const method: string = HtmlWebGLVitralEditorMCPProtocol.stringProperty(request, 'method', '');

    try {
      if (method === 'initialize') {
        return HtmlWebGLVitralEditorMCPProtocol.result(
          id,
          '{"protocolVersion":"2024-11-05","serverInfo":{"name":"VitralEditorMCP","version":"0.1"},"capabilities":{"tools":{}}}',
        );
      }
      if (method === 'tools/list') {
        return HtmlWebGLVitralEditorMCPProtocol.result(id, HtmlWebGLVitralEditorMCPProtocol.toolsJson());
      }
      if (method === 'tools/call') {
        const tool: string = HtmlWebGLVitralEditorMCPProtocol.nestedStringProperty(request, 'name', '');
        return HtmlWebGLVitralEditorMCPProtocol.result(id, await this.callTool(tool, request));
      }
      return HtmlWebGLVitralEditorMCPProtocol.error(id, -32601, 'Unknown method: ' + method);
    } catch (e) {
      return HtmlWebGLVitralEditorMCPProtocol.error(id, -32000, e instanceof Error ? e.message : String(e));
    }
  }

  private async callTool(tool: string, request: string): Promise<string> {
    let result: string;
    try {
      result = await this.executeTool(tool, request);
    } catch (e) {
      result = '{"error":"' + HtmlWebGLVitralEditorMCPProtocol.escape(e instanceof Error ? e.message : String(e)) + '"}';
    }
    return HtmlWebGLVitralEditorMCPProtocol.content(result);
  }

  private async executeTool(tool: string, request: string): Promise<string> {
    if (tool === 'scene.describe') {
      return this.describeScene();
    }
    if (tool === 'scene.clear') {
      this.recordSceneChange(tool, (): void => this.clearScene());
      return '{"ok":true}';
    }
    if (tool === 'scene.add_point_light') {
      this.recordSceneChange(tool, (): void => this.addPointLight(request));
      return this.describeScene();
    }
    if (tool === 'scene.add_sphere') {
      this.recordSceneChange(tool, (): void => this.addSphere(request));
      return this.describeScene();
    }
    if (tool === 'scene.add_cone') {
      this.recordSceneChange(tool, (): void => this.addCone(request));
      return this.describeScene();
    }
    if (tool === 'scene.add_cylinder') {
      this.recordSceneChange(tool, (): void => this.addCylinder(request));
      return this.describeScene();
    }
    if (tool === 'scene.move_body') {
      this.recordSceneChange(tool, (): void => this.moveBody(request));
      return this.describeScene();
    }
    if (tool === 'edit.history') {
      return this.describeEditHistory();
    }
    if (tool === 'gui.command') {
      return this.executeGuiCommand(request);
    }
    if (tool === 'scene.select_body') {
      this.selectBody(request);
      return this.describeScene();
    }
    if (tool === 'gui.set_mode') {
      return this.setInteractionMode(request);
    }
    if (tool === 'gui.mouse') {
      return this.injectMouse(request);
    }
    if (tool === 'gui.key') {
      return this.injectKey(request);
    }
    if (tool === 'viewport.project') {
      return this.projectSelectedBody(request);
    }
    if (tool === 'render.get_configuration') {
      return this.describeRendererConfigurations(request);
    }
    if (tool === 'render.set_configuration') {
      this.setRendererConfiguration(request);
      return this.describeRendererConfigurations(request);
    }
    if (tool === 'render.raytrace_png') {
      return this.raytracePng(request);
    }
    if (tool === 'viewport.export_jpg') {
      return this.viewportJpg(request);
    }
    if (tool === 'workspace.export_jpg') {
      return this.workspaceJpg(request);
    }
    if (tool === 'gui.list_languages') {
      return this.listLanguages();
    }
    if (tool === 'gui.set_language') {
      return this.setLanguage(request);
    }
    if (tool === 'app.exit') {
      return this.exitApplication();
    }
    throw new Error('Unknown tool: ' + tool);
  }

  private listLanguages(): string {
    let sb = '';
    const current: string = this.parent.getCurrentGuiLanguage();
    let first = true;

    sb += '{"current":"' + HtmlWebGLVitralEditorMCPProtocol.escape(current) + '","languages":[';
    for (const language of this.parent.getGuiLanguages()) {
      if (!first) {
        sb += ',';
      }
      first = false;
      sb += '{"id":"' + HtmlWebGLVitralEditorMCPProtocol.escape(language) + '"' +
        ',"current":' + (language === current) + '}';
    }
    sb += ']}';
    return sb;
  }

  private async setLanguage(request: string): Promise<string> {
    const language: string = HtmlWebGLVitralEditorMCPProtocol.stringProperty(request, 'language', '');

    if (!(await this.parent.setGuiLanguageById(language))) {
      throw new Error('Unknown language "' + language + '". Available languages: [' +
        this.parent.getGuiLanguages().join(', ') + ']');
    }
    return '{"ok":true,"language":"' + HtmlWebGLVitralEditorMCPProtocol.escape(language) + '"}';
  }

  /**
   * The exit is deferred a short time, so the response of this call can be
   * sent to the client before the application closes.
   */
  private exitApplication(): string {
    setTimeout((): void => this.parent.closeApplication(), 300);
    return '{"ok":true,"message":"The application is closing"}';
  }

  /**
   * Executes a change of the scene requested by the agent, recording it in the
   * scene history, as the ones of the user, so it can be undone.
   */
  private recordSceneChange(tool: string, change: () => void): void {
    this.parent.getApplicationModel().getEditHistory().getSceneHistory().perform(tool, change);
    this.parent.getWebGLController().repaint();
  }

  /**
   * @return the state of the scene history and of the view history of each
   * viewport of the active viewport set
   */
  private describeEditHistory(): string {
    const history: EditHistory = this.parent.getApplicationModel().getEditHistory();
    const viewportSet: ViewportSet = this.parent.getApplicationModel().getActiveViewportSet();
    let sb = '';

    sb += '{"scene":' + HtmlWebGLVitralEditorMCPProtocol.queueJson(history.getSceneHistory().getQueue()) +
      ',"viewports":[';
    for (let i = 0; i < viewportSet.getViewportCount(); i++) {
      const viewport: Viewport = viewportSet.getViewport(i);

      if (i > 0) {
        sb += ',';
      }
      sb += '{"index":' + i +
        ',"title":"' + HtmlWebGLVitralEditorMCPProtocol.escape(viewport.getTitle()) + '"' +
        ',"selected":' + (viewport === viewportSet.getSelectedViewport()) +
        ',"history":' + HtmlWebGLVitralEditorMCPProtocol.queueJson(history.getViewportHistory().getQueue(viewport)) +
        '}';
    }
    sb += ']}';
    return sb;
  }

  private static queueJson(queue: UndoQueue): string {
    const undoName: string | null = queue.getUndoName();
    const redoName: string | null = queue.getRedoName();

    return '{"undo":' + queue.getUndoCount() +
      ',"redo":' + queue.getRedoCount() +
      ',"nextUndo":' + (undoName === null ? 'null' : '"' + HtmlWebGLVitralEditorMCPProtocol.escape(undoName) + '"') +
      ',"nextRedo":' + (redoName === null ? 'null' : '"' + HtmlWebGLVitralEditorMCPProtocol.escape(redoName) + '"') +
      '}';
  }

  /**
   * Executes a command of the GUI that works only over the model (i.e. the
   * `IDC_CREATE_*` ones), as its menu item or button does. What it changes in
   * the scene is recorded in the scene history.
   */
  private executeGuiCommand(request: string): string {
    const command: string = HtmlWebGLVitralEditorMCPProtocol.stringProperty(request, 'command', '');
    let message = '';
    const executor: GuiEventExecutor = new GuiEventExecutor(this.parent.getApplicationModel(), {
      showStatusMessage: (text: string): void => {
        message = text;
      },
    });
    const result: CommandResult = executor.execute(command);

    this.parent.getWebGLController().repaint();
    return '{"command":"' + HtmlWebGLVitralEditorMCPProtocol.escape(command) + '","result":"' + result +
      '","message":"' + HtmlWebGLVitralEditorMCPProtocol.escape(message) + '"}';
  }

  private clearScene(): void {
    const scene: Scene = this.parent.getApplicationModel().getScene();
    scene.scene.getSimpleBodies().clear();
    scene.scene.getLights().clear();
    scene.debugThingGroups.clear();
  }

  private addPointLight(request: string): void {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const explicitPosition: boolean = P.hasProperty(request, 'x') || P.hasProperty(request, 'y') ||
      P.hasProperty(request, 'z');
    const explicitColor: boolean = P.hasProperty(request, 'r') || P.hasProperty(request, 'g') ||
      P.hasProperty(request, 'b');

    if (!explicitPosition && !explicitColor) {
      this.parent.getApplicationModel().addNewLight();
      return;
    }
    // Missing values follow the default policy of the first light
    const defaults: PointLight | null = new SceneLightFactory().createLight(
      new ArrayList<Light>(),
      this.parent.getApplicationModel().getActiveViewportSet(),
    );
    const p: Vector3Dd = defaults === null ? new Vector3Dd() : defaults.getPosition();
    const x: number = P.numberProperty(request, 'x', p.x());
    const y: number = P.numberProperty(request, 'y', p.y());
    const z: number = P.numberProperty(request, 'z', p.z());
    const r: number = P.numberProperty(request, 'r', 1.0);
    const g: number = P.numberProperty(request, 'g', 1.0);
    const b: number = P.numberProperty(request, 'b', 1.0);
    const scene: Scene = this.parent.getApplicationModel().getScene();
    scene.scene.addLight(new PointLight(new Vector3Dd(x, y, z), new ColorRgb(r, g, b)));
  }

  private static hasProperty(json: string, key: string): boolean {
    return new RegExp('"' + HtmlWebGLVitralEditorMCPProtocol.quote(key) + '"\\s*:').test(json);
  }

  private addSphere(request: string): void {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const radius: number = P.numberProperty(request, 'radius', 1.0);
    const x: number = P.numberProperty(request, 'x', 0.0);
    const y: number = P.numberProperty(request, 'y', 0.0);
    const z: number = P.numberProperty(request, 'z', 0.0);
    const body: SimpleBody = this.parent.getApplicationModel().getScene().addThing(new Sphere(radius));
    body.setPosition(new Vector3Dd(x, y, z));
  }

  private addCone(request: string): void {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const baseRadius: number = P.numberProperty(request, 'baseRadius', 1.0);
    const topRadius: number = P.numberProperty(request, 'topRadius', 0.0);
    const height: number = P.numberProperty(request, 'height', 2.0);
    this.placeNewBody(new Cone(baseRadius, topRadius, height), request);
  }

  private addCylinder(request: string): void {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const radius: number = P.numberProperty(request, 'radius', 1.0);
    const height: number = P.numberProperty(request, 'height', 2.0);
    this.placeNewBody(new Cone(radius, radius, height), request);
  }

  private placeNewBody(geometry: Geometry, request: string): void {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const body: SimpleBody = this.parent.getApplicationModel().getScene().addThing(geometry);
    body.setPosition(new Vector3Dd(
      P.numberProperty(request, 'x', 0.0),
      P.numberProperty(request, 'y', 0.0),
      P.numberProperty(request, 'z', 0.0),
    ));
  }

  private moveBody(request: string): void {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const bodies = this.parent.getApplicationModel().getScene().scene.getSimpleBodies();
    const index: number = Math.trunc(P.numberProperty(request, 'index', bodies.size() - 1));

    if (index < 0 || index >= bodies.size()) {
      throw new Error('Body index out of range: ' + index);
    }
    const body: SimpleBody = bodies.get(index);
    const p: Vector3Dd = body.getPosition();

    body.setPosition(new Vector3Dd(
      P.numberProperty(request, 'x', p.x()),
      P.numberProperty(request, 'y', p.y()),
      P.numberProperty(request, 'z', p.z()),
    ));
  }

  private setInteractionMode(request: string): string {
    const mode: string = HtmlWebGLVitralEditorMCPProtocol.stringProperty(request, 'mode', '');
    let value: InteractionMode;

    switch (mode) {
      case 'camera':
        value = InteractionMode.CAMERA;
        break;
      case 'select':
        value = InteractionMode.SELECT;
        break;
      case 'translate':
        value = InteractionMode.TRANSLATE;
        break;
      case 'rotate':
        value = InteractionMode.ROTATE;
        break;
      case 'scale':
        value = InteractionMode.SCALE;
        break;
      default:
        throw new Error('Unknown mode "' + mode + '". Use camera, select, translate, rotate or scale');
    }
    this.parent.getApplicationModel().getDrawingArea().setInteractionMode(value);
    return '{"ok":true,"mode":"' + mode + '"}';
  }

  /**
   * @return the controller of the drawing area, once its canvas was created
   */
  private getDrawingAreaController(): HtmlWebGLApplicationController {
    const controller: HtmlWebGLApplicationController = this.parent.getWebGLController();

    if (!controller.isDrawingAreaCreated()) {
      throw new Error('The drawing area has not been created');
    }
    return controller;
  }

  private injectMouse(request: string): string {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const drawingArea: HtmlWebGLApplicationController = this.getDrawingAreaController();
    const type: string = P.stringProperty(request, 'type', 'move');
    const x: number = JavaMath.round(P.numberProperty(request, 'x', 0));
    const y: number = JavaMath.round(P.numberProperty(request, 'y', 0));
    const button: number = Math.trunc(P.numberProperty(request, 'button', 1));

    drawingArea.injectMouseEvent(type, x, y, button);
    return this.describeScene();
  }

  private injectKey(request: string): string {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const drawingArea: HtmlWebGLApplicationController = this.getDrawingAreaController();
    const key: string = P.stringProperty(request, 'key', '');
    const shift: boolean = P.booleanProperty(request, 'shift') === true;
    const ctrl: boolean = P.booleanProperty(request, 'ctrl') === true;

    drawingArea.injectKeyEvent(key, shift, ctrl);
    return this.describeScene();
  }

  /**
   * Reports the canvas pixel of the first selected body and of the tips of its
   * three axes (one unit long), as seen by a viewport.
   */
  private projectSelectedBody(request: string): string {
    const drawingArea: HtmlWebGLApplicationController = this.getDrawingAreaController();
    const scene: Scene = this.parent.getApplicationModel().getScene();
    const set: ViewportSet = this.parent.getApplicationModel().getActiveViewportSet();
    const viewportIndex: number = Math.trunc(HtmlWebGLVitralEditorMCPProtocol.numberProperty(request, 'viewport', 0));
    const selected: number = scene.selectedThings.firstSelected();

    if (selected < 0) {
      throw new Error('No body is selected');
    }
    const viewport: Viewport = set.getViewport(viewportIndex);
    const p: Vector3Dd = scene.scene.getSimpleBodies().get(selected).getPosition();
    const names: string[] = ['origin', 'x', 'y', 'z'];
    const points: Vector3Dd[] = [
      p,
      p.add(new Vector3Dd(1, 0, 0)),
      p.add(new Vector3Dd(0, 1, 0)),
      p.add(new Vector3Dd(0, 0, 1)),
    ];
    let sb: string = '{"viewport":"' + HtmlWebGLVitralEditorMCPProtocol.escape(viewport.getTitle()) + '"';

    for (let i = 0; i < names.length; i++) {
      const pixel: number[] | null = drawingArea.projectToCanvas(viewport, points[i]);

      sb += ',"' + names[i] + '":';
      sb += pixel === null ? 'null' : '[' + Double.toString(pixel[0]) + ',' + Double.toString(pixel[1]) + ']';
    }
    sb += '}';
    return sb;
  }

  private selectBody(request: string): void {
    const scene: Scene = this.parent.getApplicationModel().getScene();
    const index: number = Math.trunc(HtmlWebGLVitralEditorMCPProtocol.numberProperty(request, 'index', -1));

    scene.selectedThings.unselectAll();
    if (index >= 0) {
      if (index >= scene.scene.getSimpleBodies().size()) {
        throw new Error('Body index out of range: ' + index);
      }
      scene.selectedThings.select(index);
    }
  }

  /**
   * @return the viewports selected by the "viewport" argument: an index, or
   * all of them when it is missing
   */
  private selectedViewports(request: string): Viewport[] {
    const set: ViewportSet = this.parent.getApplicationModel().getActiveViewportSet();
    const out: Viewport[] = [];
    const index: number = HtmlWebGLVitralEditorMCPProtocol.numberProperty(request, 'viewport', -1);

    if (index < 0) {
      out.push(...set.getViewports());
    } else if (index < set.getViewportCount()) {
      out.push(set.getViewport(Math.trunc(index)));
    } else {
      throw new Error('Viewport index out of range: ' + Math.trunc(index));
    }
    return out;
  }

  private setRendererConfiguration(request: string): void {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    for (const viewport of this.selectedViewports(request)) {
      const q: RendererConfiguration = viewport.getRendererConfiguration();
      let value: boolean | null;

      value = P.booleanProperty(request, 'points');
      if (value !== null) q.setPoints(value);
      value = P.booleanProperty(request, 'wires');
      if (value !== null) q.setWires(value);
      value = P.booleanProperty(request, 'surfaces');
      if (value !== null) q.setSurfaces(value);
      value = P.booleanProperty(request, 'texture');
      if (value !== null) q.setTexture(value);
      value = P.booleanProperty(request, 'bumpMap');
      if (value !== null) q.setBumpMap(value);
      value = P.booleanProperty(request, 'boundingVolume');
      if (value !== null) q.setBoundingVolume(value);
      value = P.booleanProperty(request, 'normals');
      if (value !== null) q.setNormals(value);
      value = P.booleanProperty(request, 'trianglesNormals');
      if (value !== null) q.setTrianglesNormals(value);
      value = P.booleanProperty(request, 'selectionCorners');
      if (value !== null) q.setSelectionCorners(value);

      const shading: string = P.stringProperty(request, 'shading', '');
      if (shading.length !== 0) {
        q.setShadingType(ShadingType.valueOf(shading.toUpperCase()));
      }

      value = P.booleanProperty(request, 'grid');
      if (value !== null) viewport.setShowGrid(value);
      const renderMode: string = P.stringProperty(request, 'renderMode', '');
      if (renderMode.toLowerCase() === 'gpu') {
        viewport.setRenderMode(Viewport.RENDER_MODE_Z_BUFFER);
      } else if (renderMode.toLowerCase() === 'cpu') {
        viewport.setRenderMode(Viewport.RENDER_MODE_RAYTRACING);
      } else if (renderMode.length !== 0) {
        throw new Error('renderMode must be gpu or cpu');
      }
    }
  }

  private describeRendererConfigurations(request: string): string {
    let sb = '{"viewports":[';
    const set: ViewportSet = this.parent.getApplicationModel().getActiveViewportSet();
    let first = true;

    for (const viewport of this.selectedViewports(request)) {
      const q: RendererConfiguration = viewport.getRendererConfiguration();

      if (!first) {
        sb += ',';
      }
      first = false;
      sb += '{"index":' + set.getViewports().indexOf(viewport) +
        ',"title":"' + HtmlWebGLVitralEditorMCPProtocol.escape(viewport.getTitle()) + '"' +
        ',"points":' + q.isPointsSet() +
        ',"wires":' + q.isWiresSet() +
        ',"surfaces":' + q.isSurfacesSet() +
        ',"texture":' + q.isTextureSet() +
        ',"bumpMap":' + q.isBumpMapSet() +
        ',"boundingVolume":' + q.isBoundingVolumeSet() +
        ',"normals":' + q.isNormalsSet() +
        ',"trianglesNormals":' + q.isTrianglesNormalsSet() +
        ',"selectionCorners":' + q.isSelectionCornersSet() +
        ',"shading":"' + ShadingType[q.getShadingTypeEnum()] + '"' +
        ',"grid":' + viewport.isShowGrid() +
        ',"renderMode":"' + (viewport.getRenderMode() === Viewport.RENDER_MODE_RAYTRACING ? 'cpu' : 'gpu') + '"}';
    }
    return sb + ']}';
  }

  private async raytracePng(request: string): Promise<string> {
    const P = HtmlWebGLVitralEditorMCPProtocol;
    const path: string = P.stringProperty(request, 'path', './mcp-raytrace.png');
    const width: number = Math.trunc(P.numberProperty(request, 'width', 640));
    const height: number = Math.trunc(P.numberProperty(request, 'height', 480));
    this.parent.getApplicationModel().setRaytracedImageWidth(width);
    this.parent.getApplicationModel().setRaytracedImageHeight(height);
    await this.parent.doRaytracingImage();
    await ImageFiles.get().exportPNG(path, this.parent.getApplicationModel().getRaytracedImage());
    return '{"ok":true,"path":"' + P.escape(path) + '"}';
  }

  private async viewportJpg(request: string): Promise<string> {
    const path: string = HtmlWebGLVitralEditorMCPProtocol.stringProperty(request, 'path', './outputSelectedViewport.jpg');
    const drawingArea: HtmlWebGLApplicationController = this.getDrawingAreaController();
    await drawingArea.exportViewportJpg(path);
    return '{"ok":true,"path":"' + HtmlWebGLVitralEditorMCPProtocol.escape(path) + '"}';
  }

  private async workspaceJpg(request: string): Promise<string> {
    const path: string = HtmlWebGLVitralEditorMCPProtocol.stringProperty(request, 'path', './outputViewport.jpg');
    const drawingArea: HtmlWebGLApplicationController = this.getDrawingAreaController();
    await drawingArea.exportWorkspaceJpg(path);
    return '{"ok":true,"path":"' + HtmlWebGLVitralEditorMCPProtocol.escape(path) + '"}';
  }

  private describeScene(): string {
    const scene: Scene = this.parent.getApplicationModel().getScene();
    const P = HtmlWebGLVitralEditorMCPProtocol;
    let sb = '{"bodies":[';
    const bodies = scene.scene.getSimpleBodies();
    for (let i = 0; i < bodies.size(); i++) {
      if (i > 0) {
        sb += ',';
      }
      const body: SimpleBody = bodies.get(i);
      const geometry: Geometry = body.getGeometry()!;
      const position: Vector3Dd = body.getPosition();
      const scale: Vector3Dd = body.getScale();
      sb += '{"index":' + i +
        ',"name":"' + P.escape(body.getName()) + '"' +
        ',"geometry":"' + javaSimpleClassName(geometry) + '"' +
        ',"position":' + P.vector(position) +
        ',"scale":' + P.vector(scale);
      if (geometry instanceof Sphere) {
        sb += ',"radius":' + Double.toString(geometry.getRadius());
      }
      sb += '}';
    }
    sb += '],"lights":[';
    const lights = scene.scene.getLights();
    for (let i = 0; i < lights.size(); i++) {
      if (i > 0) {
        sb += ',';
      }
      const light: Light = lights.get(i);
      sb += '{"index":' + i +
        ',"type":"' + javaSimpleClassName(light) + '"' +
        ',"position":' + P.vector(light.getPosition()) +
        ',"emission":' + P.color(light.getEmission()) +
        '}';
    }
    sb += ']}';
    return sb;
  }

  private static toolsJson(): string {
    const tool = HtmlWebGLVitralEditorMCPProtocol.tool;
    return '{"tools":[' +
      tool('scene.describe', 'Return the bodies (index, name, geometry, position, scale, radius of spheres) and lights (index, type, position, emission) as JSON.') +
      ',' + tool('scene.clear', 'Remove all bodies, lights and debug groups (undoable).') +
      ',' + tool('scene.add_point_light', 'Create a point light inside the view of a viewport (first light white, the rest random light colors and positions). Optional arguments: x,y,z,r,g,b override the automatic values.') +
      ',' + tool('scene.add_sphere', 'Create a sphere. Arguments: radius,x,y,z.') +
      ',' + tool('scene.add_cone', 'Create a cone (or truncated cone). Arguments: baseRadius,topRadius,height,x,y,z.') +
      ',' + tool('scene.add_cylinder', 'Create a cylinder. Arguments: radius,height,x,y,z.') +
      ',' + tool('scene.move_body', 'Set the position of a body (default: the last one; undoable). Arguments: index,x,y,z (missing coordinates are kept).') +
      ',' + tool('scene.select_body', 'Select one body (a negative index clears the selection). Arguments: index.') +
      ',' + tool('gui.set_mode', 'Set the interaction mode. Arguments: mode (camera|select|translate|rotate|scale).') +
      ',' + tool('gui.mouse', 'Send a mouse event to the drawing area. Arguments: type (move|press|drag|release), x, y (logical pixels of the drawing area, as given by viewport.project), button (1 left, 2 middle, 3 right; default 1). Returns the scene state.') +
      ',' + tool('gui.key', 'Send a key press to the drawing area. Arguments: key (a single character, or tab|enter|backspace|delete|escape|left|right|up|down|pageup|pagedown|num0..num9|num/|num*|num-|num+|num.|numenter), shift (default false), ctrl (default false; i.e. key z with ctrl is undo, y with ctrl is redo, and with shift too they work over the view of the selected viewport). Returns the scene state.') +
      ',' + tool('gui.command', 'Execute a command of the GUI that works only over the model, as its menu item or button does (i.e. IDC_CREATE_SPHERE, IDC_CREATE_FUNCTIONALEXPLICITSURFACE, IDC_CREATE_OMNILIGHT, IDC_TOOLS_RAY, IDC_OTHERS_CYCLE_BACKGROUND). Arguments: command. Returns result (DONE, FAILED or NOT_HANDLED for commands that need the GUI, i.e. file dialogs) and the status message.') +
      ',' + tool('edit.history', 'Return the undo/redo state of the scene history and of the view history of each viewport: operations to undo and redo, and the names of the next ones.') +
      ',' + tool('viewport.project', 'Drawing area pixels (as used by gui.mouse) of the first selected body origin and its x, y, z unit-axis tips in a viewport. Arguments: viewport (index, default 0).') +
      ',' + tool('render.get_configuration', 'Return the rendering configuration of the viewports. Arguments: viewport (index; default all).') +
      ',' + tool('render.set_configuration', 'Set the rendering configuration of the viewports, only in the given values. Arguments: viewport (index; default all), and any of the booleans points,wires,surfaces,texture,bumpMap,boundingVolume,normals,trianglesNormals,selectionCorners,grid, shading (nolight|flat|gouraud|phong|cook_terrance) and renderMode (gpu|cpu).') +
      ',' + tool('render.raytrace_png', 'Raytrace the scene from the camera of the last drawn viewport and export a PNG (it also writes ./output.jpg). Arguments: path, width (default 640), height (default 480).') +
      ',' + tool('viewport.export_jpg', 'Export the selected viewport, as drawn, to a JPG. Arguments: path.') +
      ',' + tool('workspace.export_jpg', 'Export the whole drawing area, with all its viewports, to a JPG. Arguments: path.') +
      ',' + tool('gui.list_languages', 'List the languages available for the GUI (I18N files in etc/gui), marking the current one.') +
      ',' + tool('gui.set_language', 'Change the GUI language, rebuilding the GUI. Arguments: language (an id given by gui.list_languages).') +
      ',' + tool('app.exit', 'Close the application (after answering this call).') +
      ']}';
  }

  private static tool(name: string, description: string): string {
    return '{"name":"' + name + '","description":"' + HtmlWebGLVitralEditorMCPProtocol.escape(description) +
      '","inputSchema":{"type":"object","additionalProperties":true}}';
  }

  private static content(json: string): string {
    return '{"content":[{"type":"text","text":"' + HtmlWebGLVitralEditorMCPProtocol.escape(json) +
      '"}],"isError":false}';
  }

  private static result(id: string, json: string): string {
    return '{"jsonrpc":"2.0","id":' + id + ',"result":' + json + '}';
  }

  private static error(id: string, code: number, message: string): string {
    return '{"jsonrpc":"2.0","id":' + id + ',"error":{"code":' + code + ',"message":"' +
      HtmlWebGLVitralEditorMCPProtocol.escape(message) + '"}}';
  }

  private static vector(v: Vector3Dd): string {
    return '{"x":' + Double.toString(v.x()) + ',"y":' + Double.toString(v.y()) + ',"z":' + Double.toString(v.z()) + '}';
  }

  private static color(c: ColorRgb): string {
    return '{"r":' + Double.toString(c.r()) + ',"g":' + Double.toString(c.g()) + ',"b":' + Double.toString(c.b()) + '}';
  }

  /** Java's `Pattern.quote` */
  private static quote(key: string): string {
    return key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  private static stringProperty(json: string, key: string, defaultValue: string): string {
    const pattern = new RegExp('"' + HtmlWebGLVitralEditorMCPProtocol.quote(key) + '"\\s*:\\s*"((?:\\\\.|[^"])*)"');
    const matcher: RegExpExecArray | null = pattern.exec(json);
    if (matcher === null) {
      return defaultValue;
    }
    return matcher[1];
  }

  private static nestedStringProperty(json: string, key: string, defaultValue: string): string {
    return HtmlWebGLVitralEditorMCPProtocol.stringProperty(json, key, defaultValue);
  }

  private static idProperty(json: string): string {
    const pattern = /"id"\s*:\s*("((?:\\.|[^"])*)"|[-0-9]+|null)/;
    const matcher: RegExpExecArray | null = pattern.exec(json);
    if (matcher === null) {
      return 'null';
    }
    return matcher[1];
  }

  private static numberProperty(json: string, key: string, defaultValue: number): number {
    const pattern = new RegExp('"' + HtmlWebGLVitralEditorMCPProtocol.quote(key) + '"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)');
    const matcher: RegExpExecArray | null = pattern.exec(json);
    if (matcher === null) {
      return defaultValue;
    }
    return Double.parseDouble(matcher[1]);
  }

  private static booleanProperty(json: string, key: string): boolean | null {
    const pattern = new RegExp('"' + HtmlWebGLVitralEditorMCPProtocol.quote(key) + '"\\s*:\\s*(true|false)');
    const matcher: RegExpExecArray | null = pattern.exec(json);
    if (matcher === null) {
      return null;
    }
    return matcher[1] === 'true';
  }

  private static escape(input: string | null): string {
    if (input === null) {
      return '';
    }
    return input.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  }
}
