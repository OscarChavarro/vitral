import type { ArrayList, Entity } from '@vitral/base';
import type { Scene } from '../scene';
import type { SelectionSet } from '../selection/selection-set';

/**
 * Port of `model.history.SceneElementKind`.
 *
 * Kinds of elements of a scene whose creation and deletion are undoable: each
 * kind is kept in its own list of the scene, most of them with a selection set
 * over it. Java's enum with methods is an enum plus the `SceneElementKinds`
 * functions below.
 */
export enum SceneElementKind {
  BODY = 'BODY',
  LIGHT = 'LIGHT',
  CAMERA = 'CAMERA',
  DEBUG_GROUP = 'DEBUG_GROUP',
}

/**
 * Methods of Java's `SceneElementKind` enum.
 */
export class SceneElementKinds {
  /** Java's `SceneElementKind.values()`, in declaration order */
  static readonly VALUES: readonly SceneElementKind[] = [
    SceneElementKind.BODY,
    SceneElementKind.LIGHT,
    SceneElementKind.CAMERA,
    SceneElementKind.DEBUG_GROUP,
  ];

  private constructor() {}

  /**
   * @param kind kind of element
   * @param scene scene holding the elements
   * @return the list of the scene holding the elements of this kind
   */
  static getList(kind: SceneElementKind, scene: Scene): ArrayList<Entity> {
    switch (kind) {
      case SceneElementKind.BODY:
        return scene.scene.getSimpleBodies() as unknown as ArrayList<Entity>;
      case SceneElementKind.LIGHT:
        return scene.scene.getLights() as unknown as ArrayList<Entity>;
      case SceneElementKind.CAMERA:
        return scene.scene.getCameras() as unknown as ArrayList<Entity>;
      default:
        return scene.debugThingGroups as unknown as ArrayList<Entity>;
    }
  }

  /**
   * @param kind kind of element
   * @param scene scene holding the elements
   * @return the selection set over the elements of this kind, or null if this
   * kind can not be selected (scene cameras)
   */
  static getSelection(kind: SceneElementKind, scene: Scene): SelectionSet | null {
    switch (kind) {
      case SceneElementKind.BODY:
        return scene.selectedThings;
      case SceneElementKind.LIGHT:
        return scene.selectedLights;
      case SceneElementKind.DEBUG_GROUP:
        return scene.selectedDebugThingGroups;
      default:
        return null;
    }
  }

  /**
   * Inserts an element in its list, keeping the selection aligned.
   * @param kind kind of element
   * @param scene scene holding the elements
   * @param index position of the element in the list
   * @param element element to insert
   * @param selected true if the element must be selected
   */
  static insert(
    kind: SceneElementKind,
    scene: Scene,
    index: number,
    element: Entity,
    selected: boolean,
  ): void {
    const selection: SelectionSet | null = SceneElementKinds.getSelection(kind, scene);

    if (selection !== null) {
      selection.insertElement(index, element, selected);
    } else {
      SceneElementKinds.getList(kind, scene).add(index, element);
    }
  }

  /**
   * Removes an element from its list, keeping the selection aligned.
   * @param kind kind of element
   * @param scene scene holding the elements
   * @param index position of the element in the list
   */
  static remove(kind: SceneElementKind, scene: Scene, index: number): void {
    const selection: SelectionSet | null = SceneElementKinds.getSelection(kind, scene);

    if (selection !== null) {
      selection.removeElement(index);
    } else {
      SceneElementKinds.getList(kind, scene).remove(index);
    }
  }
}
