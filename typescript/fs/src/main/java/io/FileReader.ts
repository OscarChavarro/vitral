import { FileInputStream } from "./FileInputStream.js";
import { InputStreamReader } from "@vitral/base";
import { File } from "./File.js";
/** Node/server-only UTF-8 FileReader adapter. */
export class FileReader extends InputStreamReader { public constructor(file: string | File) { super(new FileInputStream(file)); } }
