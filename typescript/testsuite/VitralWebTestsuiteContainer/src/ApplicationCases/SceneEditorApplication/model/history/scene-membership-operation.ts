import type { Entity } from '@vitral/base';
import type { Scene } from '../scene';
import { SceneElementKinds, type SceneElementKind } from './scene-element-kind';
import type { UndoableOperation } from './undoable-operation';

/**
 * An element of the list and its position and selection state (Java's record
 * `SceneMembershipOperation.Entry`).
 */
export class SceneMembershipEntry {
  /**
   * @param indexValue position of the element in the list
   * @param elementValue the element
   * @param selectedValue true if the element was selected
   */
  constructor(
    private readonly indexValue: number,
    private readonly elementValue: Entity,
    private readonly selectedValue: boolean,
  ) {}

  index(): number {
    return this.indexValue;
  }

  element(): Entity {
    return this.elementValue;
  }

  selected(): boolean {
    return this.selectedValue;
  }
}

/**
 * Port of `model.history.SceneMembershipOperation`.
 *
 * Creation and deletion of elements of one kind (bodies, lights, cameras or
 * debug groups) of a scene. The elements themselves are kept, so undoing a
 * deletion puts back the very same objects, at their former positions in the
 * list and with their former selection state.
 *
 * Undo goes from the list after the operation to the list before it: the
 * inserted elements are removed (from the last position to the first one) and
 * then the removed elements are inserted again (from the first position to the
 * last one). Redo does the opposite.
 */
export class SceneMembershipOperation implements UndoableOperation {
  private readonly name: string;
  private readonly scene: Scene;
  private readonly kind: SceneElementKind;
  /// Elements not present after the operation, with their positions before it
  private readonly removed: SceneMembershipEntry[];
  /// Elements not present before the operation, with their positions after it
  private readonly inserted: SceneMembershipEntry[];

  /**
   * @param name name of the user action
   * @param scene scene holding the elements
   * @param kind kind of the elements
   * @param removed elements deleted by the operation, sorted by increasing
   * position in the list before the operation
   * @param inserted elements created by the operation, sorted by increasing
   * position in the list after the operation
   */
  constructor(
    name: string,
    scene: Scene,
    kind: SceneElementKind,
    removed: readonly SceneMembershipEntry[],
    inserted: readonly SceneMembershipEntry[],
  ) {
    this.name = name;
    this.scene = scene;
    this.kind = kind;
    this.removed = [...removed];
    this.inserted = [...inserted];
  }

  undo(): void {
    this.apply(this.inserted, this.removed);
  }

  redo(): void {
    this.apply(this.removed, this.inserted);
  }

  private apply(toRemove: SceneMembershipEntry[], toInsert: SceneMembershipEntry[]): void {
    for (let i = toRemove.length - 1; i >= 0; i--) {
      SceneElementKinds.remove(this.kind, this.scene, toRemove[i].index());
    }
    for (const entry of toInsert) {
      SceneElementKinds.insert(this.kind, this.scene, entry.index(), entry.element(), entry.selected());
    }
  }

  getName(): string {
    return this.name;
  }

  /**
   * @return kind of the elements created or deleted
   */
  getKind(): SceneElementKind {
    return this.kind;
  }

  /**
   * @return number of elements deleted by the operation
   */
  getRemovedCount(): number {
    return this.removed.length;
  }

  /**
   * @return number of elements created by the operation
   */
  getInsertedCount(): number {
    return this.inserted.length;
  }
}
