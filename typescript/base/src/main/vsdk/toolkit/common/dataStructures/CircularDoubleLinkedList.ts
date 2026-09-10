import { FundamentalEntity } from "../FundamentalEntity.js";
import { VSDK } from "../VSDK.js";
import { Logger } from "../logging/Logger.js";
import { _CircularDoubleLinkedListNode } from "./_CircularDoubleLinkedListNode.js";

/** Mutable circular, double-linked list with the Java window-cursor contract. */
export class CircularDoubleLinkedList<E> extends FundamentalEntity {
    private head: _CircularDoubleLinkedListNode<E> | null = null;
    private window: _CircularDoubleLinkedListNode<E> | null = null;
    private currentSize = 0;
    private accessMemoIndex = -1;
    private accessMemoNode: _CircularDoubleLinkedListNode<E> | null = null;

    public size(): number {
        return this.currentSize;
    }
    public add(element: E): void {
        this.insertAtEnd(element);
    }
    public push(element: E): void {
        this.insertBeforeHead(element);
    }

    public insertBefore(newElement: E, pivot: E): void {
        this.locateWindowAtElem(pivot);
        if (this.head === null || this.window === null || this.window === this.head) this.insertBeforeHead(newElement);
        else this.insertBeforeNode(newElement, this.window);
    }

    public locateWindowAtIndex(index: number): void {
        if (!Number.isInteger(index) || index < 0 || index >= this.currentSize || this.head === null) return;
        let node = this.head;
        for (let i = 0; i < index; i++) node = node.next!;
        this.window = node;
    }

    public locateWindowAtElem(element: E): boolean {
        if (this.head === null) {
            this.window = null;
            return false;
        }
        let node = this.head;
        for (let i = 0; i < this.currentSize; i++, node = node.next!) {
            if (node.data === element) {
                this.window = node;
                return true;
            }
        }
        this.window = null;
        return false;
    }

    public swapElements(first: E, second: E): void {
        if (!this.locateWindowAtElem(first)) return;
        const firstNode = this.window!;
        if (!this.locateWindowAtElem(second)) return;
        const secondNode = this.window!;
        const data = firstNode.data;
        firstNode.data = secondNode.data;
        secondNode.data = data;
        this.invalidateAccessMemo();
    }

    public next(): E | null {
        if (this.window === null) this.window = this.head;
        if (this.window === null) return null;
        const data = this.window.data;
        this.window = this.window.next;
        return data;
    }
    public previous(): E | null {
        if (this.window === null) this.window = this.head;
        if (this.window === null) return null;
        const data = this.window.data;
        this.window = this.window.previous;
        return data;
    }
    public getWindow(): E | null {
        if (this.head === null) return null;
        if (this.window === null) this.window = this.head;
        return this.window.data;
    }

    public nextOf(element: E): E | null {
        const node = this.nodeFor(element);
        return node?.next?.data ?? null;
    }
    public previousOf(element: E): E | null {
        const node = this.nodeFor(element);
        return node?.previous?.data ?? null;
    }

    public get(index: number): E | null {
        if (!Number.isInteger(index) || index < 0 || index >= this.currentSize) {
            Logger.reportMessage(
                this,
                VSDK.FATAL_ERROR,
                "get",
                `IndexOutOfBounds Exception! - Trying to \`get\` with index ${index} in a list with ${this.currentSize} elements.`,
            );
            return null;
        }
        let i =
            this.accessMemoNode !== null && this.accessMemoIndex >= 0 && this.accessMemoIndex <= index
                ? this.accessMemoIndex
                : 0;
        let node =
            i === 0 && (this.accessMemoNode === null || this.accessMemoIndex > index)
                ? this.head!
                : this.accessMemoNode!;
        for (; i < index; i++) node = node.next!;
        this.accessMemoIndex = index;
        this.accessMemoNode = node;
        return node.data;
    }

    public remove(position: number): void {
        this.locateWindowAtIndex(position);
        this.removeElemAtWindow();
    }
    public removeElemAtWindow(): void {
        const node = this.window;
        if (node === null) return;
        if (this.currentSize === 1) this.head = null;
        else {
            if (node === this.head) this.head = node.next;
            node.previous!.next = node.next;
            node.next!.previous = node.previous;
        }
        this.window = null;
        this.currentSize--;
        this.invalidateAccessMemo();
    }
    public reverse(): void {
        if (this.head === null || this.currentSize < 2) return;
        let left = this.head;
        let right = this.head.previous!;
        for (let i = 0; i < Math.floor(this.currentSize / 2); i++) {
            const value = left.data;
            left.data = right.data;
            right.data = value;
            left = left.next!;
            right = right.previous!;
        }
        this.invalidateAccessMemo();
    }

    private insertAtEnd(element: E): void {
        const node = new _CircularDoubleLinkedListNode<E>();
        node.data = element;
        if (this.head === null) {
            node.next = node;
            node.previous = node;
            this.head = node;
        } else {
            const tail = this.head.previous!;
            node.previous = tail;
            node.next = this.head;
            tail.next = node;
            this.head.previous = node;
        }
        this.currentSize++;
        this.invalidateAccessMemo();
    }
    private insertBeforeHead(element: E): void {
        const node = new _CircularDoubleLinkedListNode<E>();
        node.data = element;
        if (this.head === null) {
            node.next = node;
            node.previous = node;
        } else {
            const tail = this.head.previous!;
            node.previous = tail;
            node.next = this.head;
            tail.next = node;
            this.head.previous = node;
        }
        this.head = node;
        this.currentSize++;
        this.invalidateAccessMemo();
    }
    private insertBeforeNode(element: E, pivot: _CircularDoubleLinkedListNode<E>): void {
        const node = new _CircularDoubleLinkedListNode<E>();
        node.data = element;
        node.previous = pivot.previous;
        node.next = pivot;
        pivot.previous!.next = node;
        pivot.previous = node;
        this.currentSize++;
        this.invalidateAccessMemo();
    }
    private nodeFor(element: E): _CircularDoubleLinkedListNode<E> | null {
        if (this.head === null) return null;
        let node = this.head;
        for (let i = 0; i < this.currentSize; i++, node = node.next!) if (node.data === element) return node;
        return null;
    }
    private invalidateAccessMemo(): void {
        this.accessMemoIndex = -1;
        this.accessMemoNode = null;
    }
}
