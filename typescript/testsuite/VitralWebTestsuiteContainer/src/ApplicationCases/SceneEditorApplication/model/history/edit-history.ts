import type { Scene } from '../scene';
import { SceneHistory } from './scene-history';
import { ViewportHistory } from './viewport-history';

/**
 * Port of `model.history.EditHistory`.
 *
 * Edition history of the editor: the global history of the scene (creation,
 * deletion and placement of bodies, lights and cameras of the scene) and one
 * history per viewport (how each viewport shows the scene). Which user
 * actions are recorded, and which history is undone or redone, is decided by
 * the interaction techniques of the GUI (see `gui/history`).
 */
export class EditHistory {
  private readonly sceneHistory: SceneHistory;
  private readonly viewportHistory: ViewportHistory;

  /**
   * @param sceneSource gives the scene currently edited
   */
  constructor(sceneSource: () => Scene | null) {
    this.sceneHistory = new SceneHistory(sceneSource);
    this.viewportHistory = new ViewportHistory();
  }

  /**
   * @return the global history of the scene
   */
  getSceneHistory(): SceneHistory {
    return this.sceneHistory;
  }

  /**
   * @return the histories of the views of the viewports
   */
  getViewportHistory(): ViewportHistory {
    return this.viewportHistory;
  }

  /**
   * @return true if a user action is being recorded in any history
   */
  isRecording(): boolean {
    return this.sceneHistory.isRecording() || this.viewportHistory.isRecording();
  }
}
