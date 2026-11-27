import { Logger } from "../../../../common/logging/Logger.js";
import { VSDK } from "../../../../common/VSDK.js";
import { _DoubleLinkedListNode } from "./_DoubleLinkedListNode.js";

/** Circular doubly linked list which deliberately exposes its nodes. */
export class _CircularDoubleLinkedList<E> {
    private head: _DoubleLinkedListNode<E> | null = null;
    private currentSize = 0;

    public size(): number {
        return this.currentSize;
    }

    public add(data: E): void;
    public add(ind: number, data: E): void;
    public add(indOrData: number | E, possibleData?: E): void {
        if (possibleData === undefined) {
            this.append(indOrData as E);
            return;
        }
        const ind = indOrData as number;
        if (this.head === null || ind >= this.currentSize) {
            this.append(possibleData);
            return;
        }
        const newNode = this.node(possibleData);
        if (ind === 0) {
            const oldHead = this.head;
            oldHead.previous.next = newNode;
            newNode.previous = oldHead.previous;
            newNode.next = oldHead;
            newNode.isHead = true;
            oldHead.previous = newNode;
            oldHead.isHead = false;
            this.head = newNode;
            this.currentSize++;
            return;
        }
        let iterator = this.head;
        for (let i = 0; i < ind; i++) iterator = iterator.next;
        iterator.previous.next = newNode;
        newNode.previous = iterator.previous;
        newNode.next = iterator;
        iterator.previous = newNode;
        this.currentSize++;
    }

    public remove(ind: number): void {
        if (ind >= this.currentSize) {
            Logger.reportMessage(
                this,
                VSDK.FATAL_ERROR,
                "remove",
                "Circ double linked list error: index out of bounds for remove operation.",
            );
            return;
        }
        const oldHead = this.head!;
        if (this.currentSize === 1) {
            this.head = null;
            this.currentSize = 0;
            return;
        }
        if (ind === 0) {
            oldHead.previous.next = oldHead.next;
            oldHead.next.previous = oldHead.previous;
            this.head = oldHead.next;
            this.head.isHead = true;
        } else {
            let iterator = oldHead;
            for (let i = 0; i < ind; i++) iterator = iterator.next;
            iterator.previous.next = iterator.next;
            iterator.next.previous = iterator.previous;
        }
        this.currentSize--;
    }

    public insertBefore(data: E, node: _DoubleLinkedListNode<E>): _DoubleLinkedListNode<E> | null {
        if ((node.isHead && node !== this.head) || this.head === null) {
            Logger.reportMessage(
                this,
                VSDK.FATAL_ERROR,
                "insertBefore",
                "Circ double linked list error: the node not belongs to thislinked list, insert before this node will corrupt both linked lists.",
            );
            return null;
        }
        const newNode = this.node(data);
        if (node === this.head) {
            node.previous.next = newNode;
            newNode.isHead = true;
            newNode.previous = node.previous;
            newNode.next = node;
            node.isHead = false;
            node.previous = newNode;
            this.head = newNode;
        } else {
            node.previous.next = newNode;
            newNode.previous = node.previous;
            newNode.next = node;
            node.previous = newNode;
        }
        this.currentSize++;
        return newNode;
    }

    public getHead(): _DoubleLinkedListNode<E> | null {
        return this.head;
    }

    private append(data: E): void {
        const newNode = this.node(data);
        if (this.head === null) {
            newNode.previous = newNode;
            newNode.next = newNode;
            newNode.isHead = true;
            this.head = newNode;
            this.currentSize = 1;
            return;
        }
        newNode.previous = this.head.previous;
        newNode.next = this.head;
        this.head.previous.next = newNode;
        this.head.previous = newNode;
        this.currentSize++;
    }

    private node(data: E): _DoubleLinkedListNode<E> {
        const node = new _DoubleLinkedListNode<E>();
        node.data = data;
        return node;
    }
}
