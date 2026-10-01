import { Ray, type Camera, type Light, type SimpleBody } from '@vitral/base';
import type { Scene } from '../scene';
import { LightPicker } from './light-picker';
import type { SelectionSet } from './selection-set';

/**
 * Port of `model.selection.ScenePicker`.
 *
 * Selects the things of a scene (bodies and lights) under a pixel of the active
 * camera viewport. Bodies are picked with their geometry and lights with a
 * sphere of the size of their gizmo (see `LightPicker`). It does not depend on
 * any GUI or rendering technology.
 */
export class ScenePicker {
  private readonly scene: Scene;

  /**
   * @param scene scene whose things are picked and whose selection is updated
   */
  constructor(scene: Scene) {
    this.scene = scene;
  }

  /**
   * Selects the nearest thing (body or light) under a pixel of the active
   * camera viewport. Without `composite` the previous selection is discarded,
   * with it the picked thing changes its selection state.
   * @param x pixel column in the viewport
   * @param y pixel row in the viewport
   * @param composite true to modify the current selection
   * @return the ray fired through the pixel
   */
  selectObjectWithMouse(x: number, y: number, composite: boolean): Ray {
    const camera: Camera = this.scene.activeCamera;

    camera.updateVectors();
    const r: Ray = camera.generateRay(x, y);

    const selectedRay: Ray = Ray.copyOf(r);

    // Java's Float.MAX_VALUE
    let nearestDistance = 3.4028234663852886e38;
    let nearestBody = -1;
    let nearestLight = -1;

    const selectedThings: SelectionSet = this.scene.selectedThings;
    const selectedLights: SelectionSet = this.scene.selectedLights;
    selectedThings.sync();
    selectedLights.sync();

    const things = this.scene.scene.getSimpleBodies();
    for (let i = 0; i < things.size(); i++) {
      const gi: SimpleBody = things.get(i);
      const hit: Ray | null = gi.doIntersectionFirstHit(r);
      if (hit !== null && hit.getT() < nearestDistance) {
        nearestDistance = hit.getT();
        nearestBody = i;
      }
    }

    const lights = this.scene.scene.getLights();
    for (let i = 0; i < lights.size(); i++) {
      const light: Light = lights.get(i);
      const t: number = LightPicker.pick(r, camera, light, this.scene.getLightGizmoScale());
      if (t >= 0 && t < nearestDistance) {
        nearestDistance = t;
        nearestBody = -1;
        nearestLight = i;
      }
    }

    if (!composite) {
      selectedThings.unselectAll();
      selectedLights.unselectAll();
      selectedThings.select(nearestBody);
      selectedLights.select(nearestLight);
    } else {
      selectedThings.change(nearestBody);
      selectedLights.change(nearestLight);
    }
    return selectedRay;
  }
}
