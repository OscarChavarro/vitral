import { Collection } from "./Collection.js";
export interface List<T> extends Collection<T> { get(index: number): T; set(index: number, value: T): T; }
