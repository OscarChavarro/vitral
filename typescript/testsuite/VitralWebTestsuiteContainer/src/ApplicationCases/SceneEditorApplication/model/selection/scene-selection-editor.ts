import { Entity, Matrix4x4d, SimpleBody, Vector3Dd, type ArrayList, type Geometry, type Light } from '@vitral/base';
import { javaClassName } from '../java-class-name';
import type { Scene } from '../scene';

/**
 * Port of `model.selection.SceneSelectionEditor`.
 *
 * Editing operations over the selected things (bodies, lights and debug groups)
 * of a scene. It does not depend on any GUI or rendering technology.
 */
export class SceneSelectionEditor {
  private readonly scene: Scene;

  constructor(scene: Scene) {
    this.scene = scene;
  }

  /**
   * Calculates the centroid of the group of selected things (bodies and
   * lights), as the mean of their positions. With a single selected thing this
   * is its position.
   * @return the centroid, or null if no thing is selected
   */
  computeSelectionCentroid(): Vector3Dd | null {
    let sum: Vector3Dd = new Vector3Dd();
    let count = 0;

    this.scene.selectedThings.sync();
    for (let i = 0; i < this.scene.selectedThings.size(); i++) {
      if (!this.scene.selectedThings.isSelected(i)) continue;
      sum = sum.add(this.scene.scene.getSimpleBodies().get(i).getPosition());
      count++;
    }
    this.scene.selectedLights.sync();
    for (let i = 0; i < this.scene.selectedLights.size(); i++) {
      if (!this.scene.selectedLights.isSelected(i)) continue;
      sum = sum.add(this.scene.scene.getLights().get(i).getPosition());
      count++;
    }
    if (count === 0) {
      return null;
    }
    return sum.multiply(1.0 / count);
  }

  /**
   * @param position where the translation gizmo is to be placed
   * @return a pure translation matrix for the translation gizmo
   */
  static createTranslationGizmoMatrix(position: Vector3Dd): Matrix4x4d {
    let composed: Matrix4x4d = new Matrix4x4d();

    composed = composed.withVal(0, 3, position.x());
    composed = composed.withVal(1, 3, position.y());
    composed = composed.withVal(2, 3, position.z());
    return composed;
  }

  /**
   * @param body body the rotation gizmo is to be placed at
   * @return a matrix with the orientation and position of the body, for the
   * rotation gizmo
   */
  static createRotationGizmoMatrix(body: SimpleBody): Matrix4x4d {
    return new Matrix4x4d(body.getRotation()).withTranslation(body.getPosition());
  }

  /**
   * Moves rigidly the group of selected things (bodies and lights), so that the group centroid
   * goes from oldCentroid to newCentroid. Relative positions are preserved.
   * @param oldCentroid centroid of the group before the movement
   * @param newCentroid centroid of the group after the movement
   */
  applyTranslationToSelectedObjects(oldCentroid: Vector3Dd, newCentroid: Vector3Dd): void {
    const delta: Vector3Dd = newCentroid.subtract(oldCentroid);

    for (let i = 0; i < this.scene.selectedThings.size(); i++) {
      if (!this.scene.selectedThings.isSelected(i)) continue;
      const gi: SimpleBody = this.scene.scene.getSimpleBodies().get(i);

      gi.setPosition(gi.getPosition().add(delta));
    }
    for (let i = 0; i < this.scene.selectedLights.size(); i++) {
      if (!this.scene.selectedLights.isSelected(i)) continue;
      const light: Light = this.scene.scene.getLights().get(i);

      light.setPosition(light.getPosition().add(delta));
    }
  }

  /**
   * @return the first selected body, or null if no body is selected
   */
  getFirstSelectedBody(): SimpleBody | null {
    const firstThingSelected: number = this.scene.selectedThings.firstSelected();

    if (firstThingSelected < 0) {
      return null;
    }
    return this.scene.scene.getSimpleBodies().get(firstThingSelected);
  }

  /**
   * Selects the previous body of the scene or, if there are no bodies to
   * select, the previous debug group. Lights are unselected.
   */
  selectPrevious(): void {
    this.scene.selectedLights.unselectAll();
    if (this.scene.selectedDebugThingGroups.numberOfSelections() < 1) {
      this.scene.selectedThings.selectPrevious();
    }
    if (this.scene.selectedThings.numberOfSelections() < 1) {
      this.scene.selectedDebugThingGroups.selectPrevious();
    }
  }

  /**
   * Selects the next body of the scene or, if there are no bodies to select,
   * the next debug group. Lights are unselected.
   */
  selectNext(): void {
    this.scene.selectedLights.unselectAll();
    if (this.scene.selectedDebugThingGroups.numberOfSelections() < 1) {
      this.scene.selectedThings.selectNext();
    }
    if (this.scene.selectedThings.numberOfSelections() < 1) {
      this.scene.selectedDebugThingGroups.selectNext();
    }
  }

  /**
   * Removes from the scene the selected bodies, lights and debug groups. The
   * removed bodies (with their geometries) and lights are disposed, so their
   * subscribers (i.e. editors) learn that they were deleted.
   */
  deleteSelected(): void {
    const removedThings: ArrayList<unknown> = this.scene.selectedThings.removeSelected();
    const removedLights: ArrayList<unknown> = this.scene.selectedLights.removeSelected();
    for (let i = this.scene.debugThingGroups.size() - 1; i >= 0; i--) {
      if (this.scene.selectedDebugThingGroups.isSelected(i)) {
        this.scene.debugThingGroups.remove(i);
      }
    }
    this.scene.selectedThings.sync();

    SceneSelectionEditor.disposeRemoved(removedThings);
    SceneSelectionEditor.disposeRemoved(removedLights);
  }

  private static disposeRemoved(removed: ArrayList<unknown>): void {
    for (const element of removed) {
      if (element instanceof SimpleBody && element.getGeometry() !== null) {
        (element.getGeometry() as Geometry).dispose();
      }
      if (element instanceof Entity) {
        element.dispose();
      }
    }
  }

  /**
   * @return a one line description of the current selection, for the user
   */
  describeSelection(): string {
    let msg = '';

    //-----------------------------------------------------------------
    this.scene.selectedThings.sync();
    let n: number = this.scene.selectedThings.numberOfSelections();
    if (n === 0) {
      msg += 'All things are UNSELECTED';
    } else if (n === 1) {
      const f: number = this.scene.selectedThings.firstSelected();
      const geometry: Geometry = this.scene.scene.getSimpleBodies().get(f).getGeometry()!;
      msg = 'Thing [' + f + '] selected, which is a [' + javaClassName(geometry) + ']';
    } else {
      msg += '' + n + ' things selected';
    }

    //-----------------------------------------------------------------
    this.scene.selectedLights.sync();
    n = this.scene.selectedLights.numberOfSelections();
    if (n === 1) {
      msg += '; Light [' + this.scene.selectedLights.firstSelected() + '] selected';
    } else if (n > 1) {
      msg += '; ' + n + ' lights selected';
    }

    //-----------------------------------------------------------------
    this.scene.selectedDebugThingGroups.sync();
    n = this.scene.selectedDebugThingGroups.numberOfSelections();
    if (n === 0) {
      msg += '; All visual debug groups are UNSELECTED';
    } else if (n === 1) {
      const f: number = this.scene.selectedDebugThingGroups.firstSelected();
      msg += '; Debug group [' + f + '] selected.';
    } else {
      msg += '; ' + n + ' debug groups selected';
    }
    return msg;
  }
}
