import {
  GenericEditor,
  ImageProcessing,
  Ray,
  RGBColorPalettePersistence,
  RGBImageUncompressed,
  StringReader,
  Vector3Dd,
  ZBuffer,
} from '@vitral/base';
import type { HtmlApplicationHost } from '../gui/html/html-application-host';
import { HtmlApplicationModel } from '../gui/html/html-application-model';
import { ImageFiles } from '../io/image-files';
import { WebImageFiles } from '../io/web-image-files';
import { ApplicationModel } from '../model/application-model';
import { GuiState } from '../model/gui-state';
import { javaSimpleClassName } from '../model/java-class-name';
import { Scene } from '../model/scene';
import { WebSceneRaytracingBackend } from '../render/web-scene-raytracing-backend';
import { HtmlWebGLApplicationController } from './html-webgl-application-controller';
import { HtmlWebGLGuiController } from './html-webgl-gui-controller';
import type { HtmlWebGLVitralEditorMCP } from './mcp/html-webgl-vitral-editor-mcp';

/**
 * Port of `application.AwtJogl4SceneEditorApplication`.
 *
 * The scene editor with an HTML (DOM) GUI and WebGL rendering: it creates the
 * technology independent model of the application and the GUI that presents
 * it, inside a root element of the page.
 *
 * Runtime boundaries (recorded next to the code):
 *   - Java's `main` builds the application on the Swing thread; here
 *     `HtmlWebGLSceneEditorApplication.create` builds it inside a root
 *     element, asynchronously (resources are fetched).
 *   - Java's `-s` argument starts the MCP server over TCP; here the in-page
 *     agent API is installed (see `application/mcp`).
 *   - Java's `System.exit` closes the application: the page area is released
 *     and `onClose` is called.
 *   - The palette, the GUI definitions and the languages (`GuiState`) are
 *     fetched from the `etc` folder of the server.
 */
export class HtmlWebGLSceneEditorApplication implements HtmlApplicationHost {
  // Application model
  private applicationModel!: ApplicationModel;

  // Application GUI
  private htmlModel!: HtmlApplicationModel;
  private htmlGuiController!: HtmlWebGLGuiController;
  private webglController!: HtmlWebGLApplicationController;
  private raytracingBackend: WebSceneRaytracingBackend | null = null;
  private mcp: HtmlWebGLVitralEditorMCP | null = null;
  private closed = false;

  private constructor(
    private readonly root: HTMLElement,
    private readonly onClose: () => void,
  ) {}

  /**
   * Creates the application inside an element (Java's constructor).
   * @param root element where the GUI is built
   * @param args Java's command line arguments (`-s` installs the agent API)
   * @param onClose called when the application closes itself
   * @return the application, with its GUI built
   */
  static async create(root: HTMLElement, args: readonly string[], onClose: () => void): Promise<HtmlWebGLSceneEditorApplication> {
    const application: HtmlWebGLSceneEditorApplication = new HtmlWebGLSceneEditorApplication(root, onClose);

    // Editor titles use Java's class names, which a production bundle renames
    GenericEditor.setSimpleClassNameResolver((entity: object): string => javaSimpleClassName(entity));

    await application.createModel();
    await application.createGUI();

    for (const arg of args) {
      if (arg === '-s') {
        // Installs the agent API of the page (Java starts its TCP listener)
        const mcpModule = await import('./mcp/html-webgl-vitral-editor-mcp');
        application.mcp = new mcpModule.HtmlWebGLVitralEditorMCP(application);
      }
    }
    return application;
  }

  async setLookAndFeel(lookAndFeel: string): Promise<void> {
    await this.htmlGuiController.setLookAndFeel(lookAndFeel);
  }

  /**
   * Could be better: if the GUI is not destroyed, but all labels are
   * renamed... but ... what if language files are not exactly equal?
   */
  async setGuiLanguage(lang: string): Promise<void> {
    await this.htmlGuiController.setGuiLanguage(lang);
  }

  /**
   * @return the identifiers of the languages available for the GUI, sorted:
   * the names (without extension) of the JSON files in the GUI language folder
   */
  getGuiLanguages(): string[] {
    return GuiState.listLanguages();
  }

  /**
   * @return the identifier of the language currently used by the GUI
   */
  getCurrentGuiLanguage(): string {
    return this.applicationModel.getGuiState().getCurrentLanguage();
  }

  /**
   * Changes the language of the GUI, rebuilding it (and so, propagating the new
   * messages to the application model).
   * @param language one of the identifiers given by `getGuiLanguages`
   * @return true if the language exists and was selected
   */
  async setGuiLanguageById(language: string | null): Promise<boolean> {
    if (language === null || !this.getGuiLanguages().includes(language)) {
      return false;
    }
    await this.setGuiLanguage(GuiState.languageFile(language));
    return true;
  }

  private async createModel(): Promise<void> {
    //-----------------------------------------------------------------
    ImageFiles.install(new WebImageFiles());
    this.raytracingBackend = new WebSceneRaytracingBackend();
    Scene.setRaytracingBackend(this.raytracingBackend);
    await HtmlWebGLSceneEditorApplication.loadLanguages();

    this.applicationModel = new ApplicationModel();
    this.applicationModel.setScene(new Scene());

    this.applicationModel.setRaytracedImage(new RGBImageUncompressed());
    this.applicationModel.setRaytracedImageWidth(320);
    this.applicationModel.setRaytracedImageHeight(240);

    this.applicationModel.setPalette(null);
    const paletteResponse: Response = await fetch('./etc/palettes/Cranes.gpl');
    if (!paletteResponse.ok) {
      // Java ends the program (System.exit) without its palette
      throw new Error('Can not read the palette ./etc/palettes/Cranes.gpl: HTTP ' + paletteResponse.status);
    }
    this.applicationModel.setPalette(
      RGBColorPalettePersistence.importGimpPalette(new StringReader(await paletteResponse.text())),
    );

    this.applicationModel.setVisualDebugRay(new Ray(new Vector3Dd(0, -3, 0), new Vector3Dd(0, 1, 0)));
    this.applicationModel.setVisualDebugRayLevels(2);
    this.applicationModel.setWithVisualDebugRay(false);
    this.webglController = new HtmlWebGLApplicationController(this.applicationModel);
    this.htmlModel = new HtmlApplicationModel();
    this.htmlModel.setLookAndFeel('com.sun.java.swing.plaf.motif.MotifLookAndFeel');
    this.htmlGuiController = new HtmlWebGLGuiController(this, this.htmlModel, this.webglController, this.root);
  }

  /**
   * Reads the languages of the GUI from the manifest of `etc/gui` (a browser
   * can not list a folder, see `GuiState`).
   */
  private static async loadLanguages(): Promise<void> {
    const response: Response = await fetch(GuiState.GUI_LANGUAGE_FOLDER + 'languages.json');
    if (response.ok) {
      GuiState.setAvailableLanguages((await response.json()) as string[]);
    }
  }

  async createGUI(): Promise<void> {
    await this.htmlGuiController.createGUI();
  }

  destroyGUI(): void {
    this.htmlGuiController.destroyGUI();
  }

  private prepareRaytracedImage(): void {
    this.applicationModel
      .getRaytracedImage()
      .init(this.applicationModel.getRaytracedImageWidth(), this.applicationModel.getRaytracedImageHeight());
    const scene: Scene = this.applicationModel.getScene();
    if (scene.selectedBackground === 1 && scene.fixedBackground !== null) {
      ImageProcessing.resize(scene.fixedBackground.getImage(), this.applicationModel.getRaytracedImage());
    }
  }

  async doRaytracingImage(): Promise<void> {
    this.prepareRaytracedImage();
    await this.applicationModel.getScene().raytrace(this.applicationModel.getRaytracedImage());
  }

  async doViewportRaytracingImage(): Promise<void> {
    this.prepareRaytracedImage();
    let depth: ZBuffer | null = this.applicationModel.getRaytracedDepth();
    const image: RGBImageUncompressed = this.applicationModel.getRaytracedImage();
    if (depth === null || depth.getXSize() !== image.getXSize() || depth.getYSize() !== image.getYSize()) {
      depth = new ZBuffer(image.getXSize(), image.getYSize());
      this.applicationModel.setRaytracedDepth(depth);
    }
    await this.applicationModel.getScene().raytraceViewport(image, depth);
  }

  closeApplication(): void {
    if (this.closed) {
      return;
    }
    this.closed = true;
    this.destroyGUI();
    this.htmlModel.getImageControlWindow()?.dispose();
    this.htmlModel.getSelectorDialog()?.dispose();
    this.webglController.dispose();
    this.raytracingBackend?.dispose();
    this.mcp?.dispose();
    this.onClose();
  }

  getApplicationModel(): ApplicationModel {
    return this.applicationModel;
  }

  getWebGLController(): HtmlWebGLApplicationController {
    return this.webglController;
  }

  /**
   * Requests to draw again the drawing area, whatever the rendering
   * technology presenting it.
   */
  repaintDrawingArea(): void {
    if (this.webglController !== undefined) {
      this.webglController.repaint();
    }
  }

  reportTargetToModifyPanel(): void {
    if (this.webglController !== undefined) {
      this.webglController.reportTargetToModifyPanel();
    }
  }

  getHtmlModel(): HtmlApplicationModel {
    return this.htmlModel;
  }
}
