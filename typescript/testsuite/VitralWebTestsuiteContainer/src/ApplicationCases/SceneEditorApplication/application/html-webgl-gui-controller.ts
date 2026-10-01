import { GuiPersistence, Logger, VSDK, type RGBAImageUncompressed, type RGBImageUncompressed, type Widget } from '@vitral/base';
import { HtmlGuiRenderer, HtmlSplitPane, HtmlTabbedPane, type HtmlMenuBar } from '@vitral/webgl';
import { HtmlApplicationModel } from '../gui/html/html-application-model';
import { HtmlButtonsPanel } from '../gui/html/html-buttons-panel';
import { HtmlLookAndFeelTuner } from '../gui/html/html-look-and-feel-tuner';
import { HtmlModifyPanel } from '../gui/html/html-modify-panel';
import { HtmlModifyTabChangeListener } from '../gui/html/html-modify-tab-change-listener';
import { ImageFiles } from '../io/image-files';
import type { GuiState } from '../model/gui-state';
import type { HtmlWebGLApplicationController } from './html-webgl-application-controller';
import { HtmlWebGLGuiEventExecutor } from './html-webgl-gui-event-executor';
import type { HtmlWebGLSceneEditorApplication } from './html-webgl-scene-editor-application';

/**
 * Port of `application.AwtJogl4GuiController`.
 *
 * Builds (and rebuilds, when the language, the look and feel or the full
 * screen mode change) the DOM GUI of the editor inside the root element of the
 * application: the menu bar, the icon bar, the drawing area with the tabbed
 * panel at its right (separated by a divider the user drags), and the status
 * bar; or only the drawing area, in full screen mode. The GUI definition is the
 * I18N JSON file of the language selected (`etc/gui/<language>.json`).
 *
 * Java's main `JFrame` is the root element of the page area of the application
 * (sized by the page, as Java sizes the frame to the screen), and its full
 * screen mode also asks the browser for full screen.
 */
export class HtmlWebGLGuiController {
  private readonly parent: HtmlWebGLSceneEditorApplication;
  private readonly model: HtmlApplicationModel;
  private readonly webglController: HtmlWebGLApplicationController;
  private readonly root: HTMLElement;
  private menubar: HtmlMenuBar | null = null;

  /**
   * @param parent the application
   * @param model model of the HTML GUI
   * @param webglController controller of the drawing area
   * @param root element where the GUI is built
   */
  constructor(
    parent: HtmlWebGLSceneEditorApplication,
    model: HtmlApplicationModel,
    webglController: HtmlWebGLApplicationController,
    root: HTMLElement,
  ) {
    this.parent = parent;
    this.model = model;
    this.webglController = webglController;
    this.root = root;
  }

  private guiState(): GuiState {
    return this.parent.getApplicationModel().getGuiState();
  }

  private i18n(): Widget {
    return this.parent.getApplicationModel().getI18nContext()!;
  }

  async setLookAndFeel(lookAndFeel: string): Promise<void> {
    this.model.setLookAndFeel(lookAndFeel);
    this.destroyGUI();
    await this.createGUI();
  }

  async setGuiLanguage(lang: string): Promise<void> {
    this.guiState().setLanguageGuiFile(lang);
    this.destroyGUI();
    await this.createGUI();
  }

  private createStatusBar(): HTMLElement {
    const statusMessage: HTMLDivElement = document.createElement('div');
    statusMessage.className = 'vitral-status-message';
    statusMessage.textContent = this.i18n().getMessage('IDM_INTRO_MESSAGE');
    this.model.setStatusMessage(statusMessage);

    const newStatusBarPanel: HTMLDivElement = document.createElement('div');
    newStatusBarPanel.className = 'vitral-status-bar';
    newStatusBarPanel.appendChild(statusMessage);

    return newStatusBarPanel;
  }

  private createPanel(): HtmlTabbedPane {
    const container: HtmlTabbedPane = new HtmlTabbedPane();
    const tabListener: HtmlModifyTabChangeListener = new HtmlModifyTabChangeListener(this.parent);

    container.addChangeListener((selectedIndex: number): void => tabListener.stateChanged(selectedIndex));

    let panel: HtmlButtonsPanel = new HtmlButtonsPanel(this.parent, 1, this.model.getExecutor()!);
    container.addTab(this.i18n().getMessage('IDM_CREATION_TAB'), panel.element, 'Object creation operations');

    const modifyPanel: HtmlModifyPanel = new HtmlModifyPanel(this.parent);
    this.model.setModifyPanel(modifyPanel);
    container.addTab(this.i18n().getMessage('IDM_MODIFY_TAB'), modifyPanel.element, 'Modify selected body');

    panel = new HtmlButtonsPanel(this.parent, 2, this.model.getExecutor()!);
    container.addTab(this.i18n().getMessage('IDM_GUI_TAB'), panel.element, 'GUI Control');

    panel = new HtmlButtonsPanel(this.parent, 3, this.model.getExecutor()!);
    container.addTab(this.i18n().getMessage('IDM_OTHERS_TAB'), panel.element, 'Control the scene components');

    panel = new HtmlButtonsPanel(this.parent, 4, this.model.getExecutor()!);
    container.addTab(this.i18n().getMessage('IDM_RENDER_TAB'), panel.element, 'Control the scene components');

    return container;
  }

  private async loadGuiDefinition(): Promise<void> {
    const response: Response = await fetch(this.guiState().getLanguageGuiFile());
    if (!response.ok) {
      // Java ends the program (System.exit) when the GUI file can not be read
      throw new Error('Fatal error: can not open GUI file ' + this.guiState().getLanguageGuiFile());
    }
    const files = ImageFiles.get();
    // The presentation of the viewport sets follows the language
    this.parent.getApplicationModel().setI18nContext(
      await GuiPersistence.importAquynzaGui(await response.text(), '.', {
        importRGBA: (path: string): Promise<RGBAImageUncompressed> => files.importRGBA(path),
        importRGB: async (path: string): Promise<RGBImageUncompressed> =>
          (await files.importRGB(path)) as RGBImageUncompressed,
      }),
    );
  }

  private async createGUIFullScreen(): Promise<void> {
    const mainWindowWidget: HTMLDivElement = document.createElement('div');
    mainWindowWidget.className = 'vitral-gui scene-editor-main-window scene-editor-full-screen';
    HtmlLookAndFeelTuner.apply(mainWindowWidget, this.model.getLookAndFeel());
    this.model.setMainWindowWidget(mainWindowWidget);

    await this.loadGuiDefinition();

    const canvasArea: HTMLDivElement = document.createElement('div');
    canvasArea.className = 'scene-editor-canvas-area';
    canvasArea.appendChild(this.webglController.getCanvas(this.parent));
    mainWindowWidget.appendChild(canvasArea);
    this.root.appendChild(mainWindowWidget);
    if (document.fullscreenElement === null && mainWindowWidget.requestFullscreen !== undefined) {
      mainWindowWidget.requestFullscreen().catch((): void => {
        // Without a user gesture the browser refuses: the GUI keeps the page area
      });
    }
    this.webglController.requestFocusInWindow();

    this.model.setImageControlWindow(null);
    this.model.setSelectorDialog(null);
    this.guiState().setModifyPanelSelected(false);
  }

  private async createGUIWindowed(): Promise<void> {
    const mainWindowWidget: HTMLDivElement = document.createElement('div');
    mainWindowWidget.className = 'vitral-gui scene-editor-main-window';
    if (!HtmlLookAndFeelTuner.apply(mainWindowWidget, this.model.getLookAndFeel())) {
      Logger.reportMessage(this, VSDK.WARNING, 'createGUIWindowed',
        'Warning: Can not set ' + this.model.getLookAndFeel() + ' look and feel');
    }
    this.model.setMainWindowWidget(mainWindowWidget);

    await this.loadGuiDefinition();

    const executor: HtmlWebGLGuiEventExecutor = new HtmlWebGLGuiEventExecutor(this.parent);
    this.model.setExecutor(executor);
    this.model.setExecutorPanel(new HtmlButtonsPanel(this.parent, 101, executor));

    this.menubar = HtmlGuiRenderer.buildMenubar(this.i18n(), null, this.model.getExecutorPanel()!);

    this.model.setStatusBarPanel(this.createStatusBar());

    const left: HTMLDivElement = document.createElement('div');
    left.className = 'scene-editor-canvas-area';
    left.appendChild(this.webglController.getCanvas(this.parent));
    const right: HtmlTabbedPane = this.createPanel();

    const splitPane: HtmlSplitPane = new HtmlSplitPane(left, right.element);
    splitPane.element.classList.add('scene-editor-split-pane');

    const iconsAndWorkAreasPanel: HTMLDivElement = document.createElement('div');
    iconsAndWorkAreasPanel.className = 'scene-editor-work-area';
    iconsAndWorkAreasPanel.appendChild(this.model.getExecutorPanel()!.element);
    iconsAndWorkAreasPanel.appendChild(splitPane.element);

    mainWindowWidget.appendChild(this.menubar.element);
    mainWindowWidget.appendChild(iconsAndWorkAreasPanel);
    mainWindowWidget.appendChild(this.model.getStatusBarPanel()!);
    this.root.appendChild(mainWindowWidget);

    // Java gives the panel the last 320 pixels of the screen
    splitPane.setMinimumWidths(160, 320);
    splitPane.setRightWidth(320);

    this.webglController.requestFocusInWindow();

    this.model.setImageControlWindow(null);
    this.model.setSelectorDialog(null);
    this.guiState().setModifyPanelSelected(false);
  }

  async createGUI(): Promise<void> {
    if (this.guiState().isFullScreenGuiMode()) {
      await this.createGUIFullScreen();
    } else {
      await this.createGUIWindowed();
    }
  }

  destroyGUI(): void {
    const mainWindowWidget: HTMLElement | null = this.model.getMainWindowWidget();
    if (mainWindowWidget !== null) {
      if (document.fullscreenElement === mainWindowWidget) {
        void document.exitFullscreen();
      }
      mainWindowWidget.remove();
    }
    this.menubar?.dispose();
    this.menubar = null;
    // As in Java, the image and selector windows stay open: they are windows
    // of their own, not part of the main one
    this.model.setMainWindowWidget(null);
  }
}
