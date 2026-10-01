import type { Entity } from '@vitral/base';
import type { Scene } from '../scene';
import type { SelectionSet } from '../selection/selection-set';
import { BodyTransformState } from './body-transform-state';
import { CameraState } from './camera-state';
import { CompositeOperation } from './composite-operation';
import type { EntityTransformState } from './entity-transform-state';
import { LightTransformState } from './light-transform-state';
import { SceneElementKind, SceneElementKinds } from './scene-element-kind';
import { SceneMembershipEntry, SceneMembershipOperation } from './scene-membership-operation';
import { SceneTransformationOperation } from './scene-transformation-operation';
import type { UndoableOperation } from './undoable-operation';

/**
 * Port of `model.history.SceneSnapshot`.
 *
 * What a user action can change of a scene, captured before and after the
 * action to find out the operations it did: which elements each list of the
 * scene holds (bodies, lights, cameras and debug groups, with their selection
 * state) and the placement of each body, light and camera. Only references and
 * immutable values are kept, so a capture is cheap.
 *
 * Elements (vitral `Entity`s) are compared by identity: the scene lists may
 * hold entities that are `equals` without being the same one. Java's
 * `IdentityHashMap` is a JavaScript `Map`, which compares object keys by
 * identity.
 */
export class SceneSnapshot {
  private readonly scene: Scene;
  private readonly elements: Map<SceneElementKind, Entity[]>;
  private readonly selections: Map<SceneElementKind, boolean[]>;
  private readonly transforms: Map<Entity, EntityTransformState>;

  private constructor(scene: Scene) {
    this.scene = scene;
    this.elements = new Map<SceneElementKind, Entity[]>();
    this.selections = new Map<SceneElementKind, boolean[]>();
    this.transforms = new Map<Entity, EntityTransformState>();
  }

  /**
   * @param scene scene to capture
   * @return the current state of the scene
   */
  static capture(scene: Scene): SceneSnapshot {
    const snapshot = new SceneSnapshot(scene);

    for (const kind of SceneElementKinds.VALUES) {
      const list: Entity[] = SceneElementKinds.getList(kind, scene).toArray();
      const selection: SelectionSet | null = SceneElementKinds.getSelection(kind, scene);
      const selected: boolean[] = new Array<boolean>(list.length).fill(false);

      if (selection !== null) {
        selection.sync();
        for (let i = 0; i < selected.length; i++) {
          selected[i] = selection.isSelected(i);
        }
      }
      snapshot.elements.set(kind, list);
      snapshot.selections.set(kind, selected);
    }
    for (const body of scene.scene.getSimpleBodies()) {
      snapshot.transforms.set(body, BodyTransformState.capture(body));
    }
    for (const light of scene.scene.getLights()) {
      snapshot.transforms.set(light, LightTransformState.capture(light));
    }
    for (const camera of scene.scene.getCameras()) {
      snapshot.transforms.set(camera, CameraState.capture(camera));
    }
    return snapshot;
  }

  /**
   * @return the captured scene
   */
  getScene(): Scene {
    return this.scene;
  }

  /**
   * Finds out the operations that go from this state of the scene to a later
   * one.
   * @param later state of the same scene captured after the user action
   * @param name name of the user action
   * @param mergeable true if the placement changes of the action can be merged
   * with the ones of a following action over the same elements
   * @return the operation done by the action, or null if nothing changed
   */
  operationTo(later: SceneSnapshot | null, name: string, mergeable: boolean): UndoableOperation | null {
    const operations: UndoableOperation[] = [];

    if (later === null || later.scene !== this.scene) {
      return null;
    }
    const transformation: UndoableOperation | null = this.transformationTo(later, name, mergeable);
    if (transformation !== null) {
      operations.push(transformation);
    }
    for (const kind of SceneElementKinds.VALUES) {
      const membership: UndoableOperation | null = this.membershipTo(later, kind, name);

      if (membership !== null) {
        operations.push(membership);
      }
    }
    if (operations.length === 0) {
      return null;
    }
    if (operations.length === 1) {
      return operations[0];
    }
    return new CompositeOperation(name, operations);
  }

  /**
   * @return the placement changes of the elements present in both states, or
   * null if there are none
   */
  private transformationTo(later: SceneSnapshot, name: string, mergeable: boolean): UndoableOperation | null {
    const before: EntityTransformState[] = [];
    const after: EntityTransformState[] = [];

    for (const kind of SceneElementKinds.VALUES) {
      for (const element of this.elements.get(kind) ?? []) {
        const oldState: EntityTransformState | undefined = this.transforms.get(element);
        const newState: EntityTransformState | undefined = later.transforms.get(element);

        if (oldState !== undefined && newState !== undefined && !oldState.isSameState(newState)) {
          before.push(oldState);
          after.push(newState);
        }
      }
    }
    if (before.length === 0) {
      return null;
    }
    return new SceneTransformationOperation(name, before, after, mergeable);
  }

  /**
   * @return the creation and deletion of elements of a kind, or null if the
   * same elements are present in both states
   */
  private membershipTo(later: SceneSnapshot, kind: SceneElementKind, name: string): UndoableOperation | null {
    const removed: SceneMembershipEntry[] = this.entriesMissingIn(later, kind);
    const inserted: SceneMembershipEntry[] = later.entriesMissingIn(this, kind);

    if (removed.length === 0 && inserted.length === 0) {
      return null;
    }
    return new SceneMembershipOperation(name, this.scene, kind, removed, inserted);
  }

  /**
   * @return the elements of a kind in this state that are not in the other
   * one, sorted by position
   */
  private entriesMissingIn(other: SceneSnapshot, kind: SceneElementKind): SceneMembershipEntry[] {
    const list: Entity[] = this.elements.get(kind) ?? [];
    const selected: boolean[] = this.selections.get(kind) ?? [];
    const otherElements: Set<Entity> = new Set<Entity>(other.elements.get(kind) ?? []);
    const missing: SceneMembershipEntry[] = [];

    for (let i = 0; i < list.length; i++) {
      if (!otherElements.has(list[i])) {
        missing.push(new SceneMembershipEntry(i, list[i], selected[i]));
      }
    }
    return missing;
  }
}
