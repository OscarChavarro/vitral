/*
Deep module specifiers rather than the `@vitral/base` barrel: this module is on
the import path of the raytracing worker thread, and the barrel makes every
worker compile the whole library (measured: 19.7 s versus 1.3 s to boot 72
workers on a 72-core host).
*/
import { FileInputStream } from "./FileInputStream.js";
import { InputStreamReader } from "@vitral/base/java/io/InputStreamReader";
import { File } from "./File.js";
/** Node/server-only UTF-8 FileReader adapter. */
export class FileReader extends InputStreamReader {
    public constructor(file: string | File) {
        super(new FileInputStream(file));
    }
}
