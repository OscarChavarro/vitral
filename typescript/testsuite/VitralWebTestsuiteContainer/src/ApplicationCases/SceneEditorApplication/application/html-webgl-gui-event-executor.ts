import { Logger, VSDK } from '@vitral/base';
import { HtmlFileChooser, HtmlFileFilter, type HtmlChosenFile } from '@vitral/webgl';
import { HtmlImageControlWindow } from '../gui/html/html-image-control-window';
import type { HtmlCommandExecutor } from '../gui/html/html-command-executor';
import { GuiState } from '../model/gui-state';
import { CommandResult, ExportFormat, GuiEventExecutor } from './gui-event-executor';
import type { HtmlWebGLSceneEditorApplication } from './html-webgl-scene-editor-application';

/**
 * Port of `application.AwtJogl4GuiEventExecutor`.
 *
 * Executes the commands of the GUI of the editor that need the browser (file
 * choosers, the image window, look and feel and language changes, that
 * rebuild the DOM GUI) and repaints the drawing area after each command. The
 * rest of the commands are executed by the technology independent
 * `GuiEventExecutor`.
 *
 * Swing's `JFileChooser` is `HtmlFileChooser` (it browses the `etc` folder of
 * the server, and offers files of the user's computer); a written file is
 * handed to the user as a download. Java's `System.exit` of `IDC_FILE_QUIT`
 * closes the application (see `closeApplication`).
 */
export class HtmlWebGLGuiEventExecutor implements HtmlCommandExecutor {
  private readonly parent: HtmlWebGLSceneEditorApplication;
  private readonly commands: GuiEventExecutor;

  constructor(parent: HtmlWebGLSceneEditorApplication) {
    this.parent = parent;
    this.commands = new GuiEventExecutor(parent.getApplicationModel(), {
      showStatusMessage: (message: string): void => this.showStatusMessage(message),
    });
  }

  private guiState(): GuiState {
    return this.parent.getApplicationModel().getGuiState();
  }

  private showStatusMessage(message: string): void {
    const statusMessage: HTMLElement | null = this.parent.getHtmlModel().getStatusMessage();
    if (statusMessage !== null) {
      statusMessage.textContent = message;
    }
  }

  async executeCommand(label: string, mainWindowWidget: HTMLElement | null): Promise<boolean> {
    const result: CommandResult = this.commands.execute(label);

    if (result === CommandResult.FAILED) {
      return false;
    }
    if (result === CommandResult.NOT_HANDLED && !(await this.executeHtmlCommand(label, mainWindowWidget))) {
      return false;
    }

    //-----------------------------------------------------------------
    this.parent.getWebGLController().repaint();
    return true;
  }

  /**
   * Executes the commands that need the browser.
   * @return false if the command failed
   */
  private async executeHtmlCommand(label: string, _mainWindowWidget: HTMLElement | null): Promise<boolean> {
    //- FILE ----------------------------------------------------------
    if (label === 'IDC_FILE_QUIT') {
      this.parent.closeApplication();
    } else if (label === 'IDC_IMPORT_OBJECTS_FROM_FILE') {
      const jfc: HtmlFileChooser = new HtmlFileChooser(this.guiState().getReadFolder());
      jfc.addChoosableFileFilter(new HtmlFileFilter('3ds', '3ds Kinetix/Discreet 3DStudio/3DStudioMax binary scene file'));
      jfc.addChoosableFileFilter(new HtmlFileFilter('vtk', 'vtk Kitware vtk legacy binary file (mesh only)'));
      jfc.addChoosableFileFilter(new HtmlFileFilter('gts', 'gts Gts mesh ASCII file'));
      jfc.addChoosableFileFilter(new HtmlFileFilter('obj', 'obj Alias/Wavefront text mesh'));
      jfc.addChoosableFileFilter(new HtmlFileFilter('ply', 'ply Ply mesh'));

      const file: HtmlChosenFile | null = await jfc.showOpenDialog();
      if (file !== null) {
        try {
          await this.commands.importObjects(file);

          const parentPath: string | null = file.getParentPath();
          if (parentPath !== null) {
            this.guiState().setReadFolder(parentPath);
          }
        } catch (ex) {
          Logger.reportMessage(this, VSDK.WARNING, 'executeCommand', 'Failed to read file...\n' + ex);
          return false;
        }
      }
    } else if (label === 'IDC_EXPORT_OBJECTS_TO_OBJ') {
      return this.exportObjects(ExportFormat.OBJ);
    } else if (label === 'IDC_EXPORT_OBJECTS_TO_GTS') {
      return this.exportObjects(ExportFormat.GTS);
    } else if (label === 'IDC_EXPORT_OBJECTS_TO_VTK') {
      return this.exportObjects(ExportFormat.VTK);
    }
    //- RENDERING -----------------------------------------------------
    else if (label === 'Select palette for depthmap display' || label === 'IDC_RENDERING_SELECTPALETTEDEPTH') {
      const jfc: HtmlFileChooser = new HtmlFileChooser('./etc/palettes');
      jfc.addChoosableFileFilter(new HtmlFileFilter('gpl', 'gpl Gimp Palettes'));
      const file: HtmlChosenFile | null = await jfc.showOpenDialog();
      if (file !== null) {
        try {
          await this.commands.loadPalette(file);
        } catch {
          console.log('Failed to read file');
          return false;
        }
      }
    } else if (label === 'IDC_RENDERING_RAYTRACING') {
      this.showStatusMessage(this.parent.getApplicationModel().getI18nContext()!.getMessage('IDM_COMPUTING_RAYTRACING'));
      await this.parent.doRaytracingImage();
      const htmlModel = this.parent.getHtmlModel();
      if (htmlModel.getImageControlWindow() === null) {
        htmlModel.setImageControlWindow(
          new HtmlImageControlWindow(
            this.parent.getApplicationModel().getRaytracedImage(),
            this.parent.getApplicationModel().getI18nContext(),
            htmlModel.getExecutorPanel()!,
          ),
        );
      } else {
        htmlModel.getImageControlWindow()!.setImage(this.parent.getApplicationModel().getRaytracedImage());
      }
      htmlModel.getImageControlWindow()!.redrawImage();
    }
    //- CUSTOMIZE -----------------------------------------------------
    else if (label === 'IDC_CUSTOMIZE_LAF_MOTIF') {
      await this.parent.setLookAndFeel('com.sun.java.swing.plaf.motif.MotifLookAndFeel');
    } else if (label === 'IDC_CUSTOMIZE_LAF_JAVA') {
      await this.parent.setLookAndFeel('javax.swing.plaf.metal.MetalLookAndFeel');
    } else if (label === 'IDC_CUSTOMIZE_LAF_GTK') {
      await this.parent.setLookAndFeel('com.sun.java.swing.plaf.gtk.GTKLookAndFeel');
    } else if (label === 'IDC_CUSTOMIZE_LAF_WINDOWS') {
      await this.parent.setLookAndFeel('com.sun.java.swing.plaf.windows.WindowsLookAndFeel');
    } else if (label === 'IDC_CUSTOMIZE_LANGUAGE_ENGLISH') {
      await this.parent.setGuiLanguage(GuiState.languageFile('english'));
    } else if (label === 'IDC_CUSTOMIZE_LANGUAGE_SPANISH') {
      await this.parent.setGuiLanguage(GuiState.languageFile('spanish'));
    }
    return true;
  }

  private async exportObjects(format: ExportFormat): Promise<boolean> {
    const jfc: HtmlFileChooser = new HtmlFileChooser(this.guiState().getWriteFolder());
    const fileName: string | null = await jfc.showSaveDialog('Save');

    if (fileName !== null) {
      try {
        await this.commands.exportObjects(fileName, format);
      } catch (ex) {
        Logger.reportMessage(this, VSDK.WARNING, 'execute', 'Failed to write file...\n' + ex);
        return false;
      }
    }
    return true;
  }

  /**
   * Java's `executeCommand(String)` (the `CommandListener` contract): executes
   * a command of the technology independent `GuiEventExecutor`. TypeScript can
   * not overload one name with two results (this one answers at once, the
   * other one a promise), so it has its own name.
   * @param commandId identifier of the command (`IDC_*`)
   * @return true if the command was executed
   */
  executeModelCommand(commandId: string): boolean {
    return this.commands.executeCommand(commandId);
  }
}
