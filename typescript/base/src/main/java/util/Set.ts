import { Collection } from "./Collection.js";
export interface Set<T> extends Collection<T> { add(value: T): boolean; remove(value: T): boolean; }
