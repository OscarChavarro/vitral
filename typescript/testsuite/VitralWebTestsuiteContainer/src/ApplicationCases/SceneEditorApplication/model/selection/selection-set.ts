import { ArrayList } from '@vitral/base';

/**
 * Port of `model.selection.SelectionSet`.
 *
 * Java keeps a raw `ArrayList` association; here it is an `ArrayList` of
 * unknown elements, since the set never reads them.
 */
export class SelectionSet {
  // This is an association to an external ArrayList (generic, not template)
  private readonly elements: ArrayList<unknown>;
  private readonly selection: ArrayList<boolean>;

  constructor(externalList: ArrayList<unknown>) {
    this.elements = externalList;
    this.selection = new ArrayList<boolean>();
    this.sync();
  }

  /**
   * Checks the size of the element list. If it is different to current
   * selection list, selection list is updated.
   */
  sync(): void {
    if (this.elements.size() === this.selection.size()) {
      return;
    }
    while (this.elements.size() > this.selection.size()) {
      this.selection.add(false);
    }
    while (this.elements.size() < this.selection.size()) {
      this.selection.remove(this.selection.size() - 1);
    }
  }

  toString(): string {
    let msg = 'Selection set: [';
    for (let i = 0; i < this.selection.size(); i++) {
      msg = msg + (this.selection.get(i) ? '*' : '-');
    }
    msg = msg + ']';
    return msg;
  }

  isSelected(i: number): boolean {
    if (i < 0 || i >= this.selection.size() || !this.selection.get(i)) {
      return false;
    }
    return true;
  }

  firstSelected(): number {
    let index = -1;
    for (let i = 0; i < this.selection.size(); i++) {
      if (this.selection.get(i)) {
        index = i;
        break;
      }
    }
    return index;
  }

  selectAll(): void {
    for (let i = 0; i < this.selection.size(); i++) {
      this.selection.set(i, true);
    }
  }

  unselectAll(): void {
    for (let i = 0; i < this.selection.size(); i++) {
      this.selection.set(i, false);
    }
  }

  select(i: number): void {
    this.sync();
    if (i < 0 || i >= this.selection.size()) return;
    this.selection.set(i, true);
  }

  unselect(i: number): void {
    this.sync();
    if (i < 0 || i >= this.selection.size()) return;
    this.selection.set(i, false);
  }

  change(i: number): void {
    this.sync();
    if (i < 0 || i >= this.selection.size()) return;
    if (this.isSelected(i)) {
      this.selection.set(i, false);
    } else {
      this.selection.set(i, true);
    }
  }

  selectPrevious(): void {
    this.sync();
    if (this.selection.size() < 1) {
      return;
    }
    let f = this.firstSelected();
    if (f < 0) {
      this.select(this.selection.size() - 1);
      return;
    }
    this.unselect(f);
    f--;
    if (f < 0) {
      return;
    }
    this.select(f);
  }

  selectNext(): void {
    this.sync();
    if (this.selection.size() < 1) {
      return;
    }
    let f = this.firstSelected();
    if (f < 0) {
      this.select(0);
      return;
    }
    this.unselect(f);
    f++;
    if (f >= this.selection.size()) {
      return;
    }
    this.select(f);
  }

  /**
   * Removes from the external list the selected elements, keeping the
   * selection of the remaining ones.
   * @return the removed elements, in decreasing order of their former index
   */
  removeSelected(): ArrayList<unknown> {
    const removed = new ArrayList<unknown>();
    this.sync();
    for (let i = this.selection.size() - 1; i >= 0; i--) {
      if (this.selection.get(i)) {
        removed.add(this.elements.remove(i));
        this.selection.remove(i);
      }
    }
    return removed;
  }

  /**
   * Inserts an element in the external list, keeping the selection marks of
   * the other elements aligned with them (`sync` only works at the end of the
   * list).
   * @param index position of the new element in the external list
   * @param element element to insert
   * @param selected true if the inserted element must be selected
   */
  insertElement(index: number, element: unknown, selected: boolean): void {
    this.sync();
    this.elements.add(index, element);
    this.selection.add(index, selected);
  }

  /**
   * Removes an element from the external list, keeping the selection marks of
   * the other elements aligned with them.
   * @param index position of the element in the external list
   * @return the removed element
   */
  removeElement(index: number): unknown {
    this.sync();
    this.selection.remove(index);
    return this.elements.remove(index);
  }

  numberOfSelections(): number {
    let acum = 0;
    this.sync();
    for (let i = 0; i < this.selection.size(); i++) {
      if (this.isSelected(i)) {
        acum++;
      }
    }
    return acum;
  }

  size(): number {
    return this.selection.size();
  }
}
