import { ArrayList } from "./ArrayList.js";
import { IndexOutOfBoundsException } from "../lang/IndexOutOfBoundsException.js";
export class Stack<T> extends ArrayList<T> {
    public push(item: T): T {
        this.add(item);
        return item;
    }
    public pop(): T {
        if (this.empty()) throw new IndexOutOfBoundsException("Stack is empty");
        return this.removeAt(this.size() - 1);
    }
    public peek(): T {
        if (this.empty()) throw new IndexOutOfBoundsException("Stack is empty");
        return this.get(this.size() - 1);
    }
    public empty(): boolean {
        return this.isEmpty();
    }
    public search(item: T): number {
        const index = this.lastIndexOf(item);
        return index < 0 ? -1 : this.size() - index;
    }
}
