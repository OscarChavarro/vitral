import { HtmlWindow } from '@vitral/webgl';
import type { ApplicationModel } from '../../model/application-model';
import type { Scene } from '../../model/scene';
import { SceneSelectionEditor } from '../../model/selection/scene-selection-editor';

/**
 * Port of `gui.awt.AwtSelectorDialog`: a dialog of 526x530 pixels, with a test
 * button at its top and another at its bottom.
 */
export class HtmlSelectorDialog {
  private readonly dialog: HtmlWindow;

  constructor(private readonly model: ApplicationModel, private readonly repaintRequested: () => void) {
    this.dialog = new HtmlWindow('');

    //-----------------------------------------------------------------
    const mainFrameWidget: HTMLDivElement = document.createElement('div');
    mainFrameWidget.className = 'scene-editor-selector-dialog';
    this.dialog.element.style.minWidth = '526px';
    this.dialog.element.style.minHeight = '530px';
    this.dialog.setSize(526, 530);

    //-----------------------------------------------------------------
    const bottomAreaWidget: HTMLDivElement = document.createElement('div');
    const centralAreaWidget: HTMLDivElement = document.createElement('div');

    mainFrameWidget.appendChild(centralAreaWidget);
    mainFrameWidget.appendChild(bottomAreaWidget);

    //-----------------------------------------------------------------
    const tree = document.createElement('select');
    tree.multiple = true;
    tree.size = 18;
    centralAreaWidget.appendChild(tree);
    const rebuild = (): void => {
      const scene: Scene = this.model.getScene();
      tree.replaceChildren();
      for (let i = 0; i < scene.scene.getSimpleBodies().size(); i++) {
        const body = scene.scene.getSimpleBodies().get(i);
        const option = document.createElement('option');
        option.value = String(i);
        option.selected = scene.selectedThings.isSelected(i);
        option.textContent = body.getName() || `${body.getGeometry()?.constructor.name ?? 'Geometry'} ${i + 1}` +
          (scene.isHidden(body) ? ' (hidden)' : '');
        tree.appendChild(option);
      }
    };
    tree.onchange = (): void => {
      const selected = new Set(Array.from(tree.selectedOptions, (option) => Number(option.value)));
      const scene = this.model.getScene(); scene.selectedThings.unselectAll();
      for (const index of selected) scene.selectedThings.select(index);
      this.repaintRequested();
    };
    let b: HTMLButtonElement = document.createElement('button');
    b.type = 'button';
    b.className = 'vitral-button';
    b.textContent = 'Delete selected';
    b.onclick = (): void => { new SceneSelectionEditor(this.model.getScene()).deleteSelected(); rebuild(); this.repaintRequested(); };
    bottomAreaWidget.appendChild(b);

    b = document.createElement('button');
    b.type = 'button';
    b.className = 'vitral-button';
    b.textContent = 'Hide / show selected';
    b.onclick = (): void => {
      const scene = this.model.getScene();
      const selected = Array.from(tree.selectedOptions, (option) => scene.scene.getSimpleBodies().get(Number(option.value)));
      const hide = !selected.some((body) => scene.isHidden(body));
      selected.forEach((body) => scene.setHidden(body, hide)); rebuild(); this.repaintRequested();
    };
    centralAreaWidget.appendChild(b);

    this.dialog.content.appendChild(mainFrameWidget);
    rebuild();
    //-----------------------------------------------------------------
  }

  setVisible(visible: boolean): void {
    this.dialog.setVisible(visible);
  }

  /**
   * Swing's `repaint`: the DOM repaints by itself.
   */
  repaint(): void {
    // The browser presents the dialog as soon as it changes
  }

  dispose(): void {
    this.dialog.dispose();
  }
}
