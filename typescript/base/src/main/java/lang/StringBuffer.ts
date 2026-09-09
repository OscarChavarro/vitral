import { StringBuilder } from "./StringBuilder.js";
/** StringBuffer's synchronization is supplied by worker ownership; API remains mutable. */
export class StringBuffer extends StringBuilder {}
