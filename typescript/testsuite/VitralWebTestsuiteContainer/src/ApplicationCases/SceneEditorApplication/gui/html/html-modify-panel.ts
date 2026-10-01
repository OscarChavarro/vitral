import {
  FunctionalExplicitSurface,
  type Entity,
  type SimpleBody,
} from '@vitral/base';
import {
  FunctionalExplicitSurfaceEditor,
  FunctionalExplicitSurfaceParameter,
} from '../../model/editor/functional-explicit-surface-editor';
import { HtmlGenericEditor } from '@vitral/webgl';
import type { BodyEditFeedbackProvider } from '../../render/body-edit-feedback-provider';
import type { RenderPrimitive } from '../../render/render-primitive';
import type { HtmlApplicationHost } from './html-application-host';

/**
 * Port of `gui.awt.AwtModifyPanel`.
 *
 * Panel of the GUI that edits the selected body with an editor specific to its
 * geometry (subclasses of this panel). It does not draw: renderers ask it,
 * through `BodyEditFeedbackProvider`, for the feedback geometry to present over
 * the body under edition, so editors do not depend on any rendering technology.
 *
 * Java's subclass `gui.awt.editor.AwtModifyPanelForFunctionalExplicitSurface`
 * is `HtmlModifyPanelForFunctionalExplicitSurface` below, in this same module:
 * this panel creates it, and an ES module can not import a subclass of a class
 * it defines (the subclass would extend a class not yet initialized).
 */
export class HtmlModifyPanel implements BodyEditFeedbackProvider {
  readonly element: HTMLDivElement;
  protected parent: HtmlApplicationHost;
  protected target: SimpleBody | null = null;

  // Implementations
  private functionalExplicitSurfaceEditor: HtmlModifyPanelForFunctionalExplicitSurface | null = null;
  /// Fallback editor, built from the control specifications of the geometry
  private genericEditor: HtmlGenericEditor | null = null;
  /// Editor for the current target, or null if there is none
  private activeEditor: HtmlModifyPanel | null = null;

  constructor(parent: HtmlApplicationHost) {
    this.parent = parent;
    this.element = document.createElement('div');
    this.element.className = 'scene-editor-modify-panel';
    this.notifyTargetEndEdit();
    this.functionalExplicitSurfaceEditor = null;
    this.genericEditor = null;
    this.activeEditor = null;
  }

  getTarget(): SimpleBody | null {
    return this.target;
  }

  notifyTargetBeginEdit(target: SimpleBody): void {
    this.target = target;
    this.element.replaceChildren();
    this.activeEditor = null;

    if (target.getGeometry() instanceof FunctionalExplicitSurface) {
      if (this.genericEditor !== null) {
        this.genericEditor.detach();
      }
      if (this.functionalExplicitSurfaceEditor === null) {
        this.functionalExplicitSurfaceEditor = new HtmlModifyPanelForFunctionalExplicitSurface(this.parent);
      }
      this.functionalExplicitSurfaceEditor.notifyTargetBeginEditIn(target, this.element);
      this.activeEditor = this.functionalExplicitSurfaceEditor;
    } else {
      if (this.genericEditor === null) {
        this.genericEditor = new HtmlGenericEditor(this.element);
        this.genericEditor.setListener({
          notifyEntityChanged: (_entity: Entity): void => this.parent.repaintDrawingArea(),
        });
      }
      this.genericEditor.build(target.getGeometry());
    }
  }

  notifyTargetEndEdit(): void {
    this.target = null;
    this.activeEditor = null;
    if (this.genericEditor !== null && this.genericEditor.isEntityDeleted()) {
      // Keep the "Entity deleted" message the editor is showing
      return;
    }
    if (this.genericEditor !== null) {
      this.genericEditor.detach();
    }
    this.element.replaceChildren();
    const label: HTMLDivElement = document.createElement('div');
    label.textContent = 'No selected object for modifying.';
    this.element.appendChild(label);
    this.target = null;
    this.activeEditor = null;
  }

  /**
   * Editors for specific geometries override this method to present their
   * feedback (handles, bounds...) over the target. This one delegates to the
   * editor of the current target.
   * @return the feedback geometry to present over the target, in world
   * coordinates (empty if there is none)
   */
  buildEditFeedback(): RenderPrimitive[] {
    if (this.activeEditor !== null) {
      return this.activeEditor.buildEditFeedback();
    }
    return [];
  }
}

/**
 * Port of `gui.awt.editor.AwtModifyPanelForFunctionalExplicitSurface`.
 *
 * DOM presentation of a `FunctionalExplicitSurfaceEditor`: a list of
 * predefined configurations and one text field per parameter. What the user
 * types is passed to the editor, which changes the body (a field is applied
 * when the user presses enter, as Swing's `JTextField` fires its action).
 */
export class HtmlModifyPanelForFunctionalExplicitSurface extends HtmlModifyPanel {
  private readonly fields: Map<FunctionalExplicitSurfaceParameter, HTMLInputElement>;
  private editor: FunctionalExplicitSurfaceEditor | null;

  constructor(parent: HtmlApplicationHost) {
    super(parent);
    this.fields = new Map<FunctionalExplicitSurfaceParameter, HTMLInputElement>();
    this.editor = null;
  }

  /**
   * Java's `notifyTargetBeginEdit(SimpleBody, JPanel)` (the one parameter
   * overload is the inherited one).
   * @param target body to edit
   * @param parentPanel element where the editor is presented
   */
  notifyTargetBeginEditIn(target: SimpleBody, parentPanel: HTMLElement): void {
    //-----------------------------------------------------------------
    const container1: HTMLDivElement = document.createElement('div');
    const container2: HTMLDivElement = document.createElement('div');
    container2.className = 'scene-editor-surface-editor';

    container1.appendChild(container2);

    parentPanel.appendChild(container1);

    this.target = target;
    this.editor = new FunctionalExplicitSurfaceEditor(target);
    this.fields.clear();
    this.element.replaceChildren();

    //-----------------------------------------------------------------
    let label: HTMLDivElement = document.createElement('div');
    label.className = 'scene-editor-surface-editor-title';
    label.textContent = FunctionalExplicitSurfaceEditor.TITLE;
    container2.appendChild(label);

    //-----------------------------------------------------------------
    label = document.createElement('div');
    label.textContent = FunctionalExplicitSurfaceEditor.PRESETS_LABEL;
    container2.appendChild(label);

    const jcb: HTMLSelectElement = document.createElement('select');
    jcb.className = 'vitral-text-field';
    for (const preset of FunctionalExplicitSurfaceEditor.getPresets()) {
      const option: HTMLOptionElement = document.createElement('option');
      option.value = preset;
      option.textContent = preset;
      jcb.appendChild(option);
    }
    // Java skips the action fired by adding the first item (it is not a
    // user choice); a select fires its change only when the user chooses
    jcb.addEventListener('change', (): void => {
      if (this.editor !== null && this.editor.applyPreset(jcb.value)) {
        this.refreshFields();
        this.parent.repaintDrawingArea();
      }
    });
    container2.appendChild(jcb);

    //-----------------------------------------------------------------
    for (const parameter of FunctionalExplicitSurfaceEditor.PARAMETERS) {
      const field: HTMLInputElement = document.createElement('input');
      field.type = 'text';
      field.className = 'vitral-text-field';
      field.value = this.editor.getValue(parameter);
      field.addEventListener('keydown', (event: KeyboardEvent): void => {
        event.stopPropagation();
        if (event.key === 'Enter' && this.editor !== null) {
          try {
            this.editor.setValue(parameter, field.value);
            this.parent.repaintDrawingArea();
          } catch (e) {
            console.error(e);
          }
        }
      });
      this.fields.set(parameter, field);

      const parameterLabel: HTMLLabelElement = document.createElement('label');
      parameterLabel.textContent = FunctionalExplicitSurfaceEditor.parameterLabel(parameter);
      if (parameter === FunctionalExplicitSurfaceParameter.FUNCTION) {
        // The function is long: its label goes above it
        container2.appendChild(parameterLabel);
        container2.appendChild(field);
        continue;
      }
      const container3: HTMLDivElement = document.createElement('div');
      container3.className = 'scene-editor-surface-editor-row';
      container3.appendChild(parameterLabel);
      container3.appendChild(field);
      container2.appendChild(container3);
    }
  }

  private refreshFields(): void {
    for (const [parameter, field] of this.fields) {
      field.value = this.editor!.getValue(parameter);
    }
  }
}
