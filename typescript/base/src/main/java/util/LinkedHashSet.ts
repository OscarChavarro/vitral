import { HashSet } from "./HashSet.js";
/** HashSet already preserves insertion order, as does Java LinkedHashSet. */
export class LinkedHashSet<T> extends HashSet<T> {}
