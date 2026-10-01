import type { ViewportSet } from '@vitral/base';
import type { EditHistory } from '../../model/history/edit-history';

/**
 * Port of `gui.history.InteractionEditRecorder`.
 *
 * Brackets the processing of vitral events by the interaction techniques, so
 * what each user action changes is recorded in the edition history: the scene
 * changes in the global history of the scene, the view changes in the history
 * of each changed viewport.
 *
 * Two kinds of actions are recorded:
 *
 * - Gestures: from a mouse press to its release (a drag of a gizmo, a camera
 *   movement). They are one step of the history, whatever the events in
 *   between do (keys included).
 * - Single actions: a key press, a click, a command of a menu.
 *
 * A gesture whose release never came (i.e. consumed by a popup menu) is
 * finished by the next gesture, or before an undo or redo.
 */
export class InteractionEditRecorder {
  private readonly history: EditHistory;
  private readonly viewportSet: ViewportSet;
  private gestureOpen: boolean;
  private gestureSceneOperationName = '';

  /**
   * @param history where the actions are recorded
   * @param viewportSet viewports whose view changes are recorded
   */
  constructor(history: EditHistory, viewportSet: ViewportSet) {
    this.history = history;
    this.viewportSet = viewportSet;
    this.gestureOpen = false;
  }

  /**
   * @return the recorded history
   */
  getHistory(): EditHistory {
    return this.history;
  }

  /**
   * @return true if a mouse gesture is being recorded
   */
  isGestureOpen(): boolean {
    return this.gestureOpen;
  }

  /**
   * Starts recording a mouse gesture (at a mouse press), finishing first the
   * one in course, if any.
   * @param sceneOperationName name of the operation over the scene (the ones
   * over the views are named after what changed)
   */
  beginGesture(sceneOperationName: string): void {
    this.endGesture();
    this.gestureSceneOperationName = sceneOperationName;
    this.beginAction();
    this.gestureOpen = true;
  }

  /**
   * Ends recording the mouse gesture in course (at a mouse release). Nothing
   * is done if there is none.
   */
  endGesture(): void {
    if (!this.gestureOpen) {
      return;
    }
    this.gestureOpen = false;
    this.endAction(this.gestureSceneOperationName, false);
  }

  /**
   * Starts recording a single action. Inside a gesture, the action becomes
   * part of the gesture.
   */
  beginAction(): void {
    this.history.getSceneHistory().begin();
    this.history.getViewportHistory().begin(this.viewportSet);
  }

  /**
   * Ends recording a single action.
   * @param sceneOperationName name of the operation over the scene
   * @param mergeable true if the operations can be merged with the ones of
   * the next action over the same things (keyboard actions)
   */
  endAction(sceneOperationName: string, mergeable: boolean): void {
    this.history.getSceneHistory().end(sceneOperationName, mergeable);
    this.history.getViewportHistory().end(mergeable);
  }

  /**
   * Executes an action recording it.
   * @param sceneOperationName name of the operation over the scene
   * @param action what the user does
   */
  record(sceneOperationName: string, action: () => void): void {
    this.beginAction();
    try {
      action();
    } finally {
      this.endAction(sceneOperationName, false);
    }
  }
}
