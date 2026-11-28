import { NullPointerException } from "../../../../java/lang/NullPointerException.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { PersistenceElement } from "../PersistenceElement.js";
import { XmlNode } from "./XmlNode.js";

/** Tokenizes a line into tags and non-tag spans with inclusive byte offsets. */
export class XmlImporter extends PersistenceElement {
    public importFromLine(line: string): ArrayList<XmlNode> {
        return this.importFromByteArray(new TextEncoder().encode(line));
    }

    public importFromByteArray(bytes: Uint8Array | Int8Array): ArrayList<XmlNode> {
        let onString = false;
        let currentString = "";
        let notagString: string | null = null;
        let onTag = false;
        let onTagName = true;
        let currentIdentifier = "";

        const nodes = new ArrayList<XmlNode>();
        let currentNode: XmlNode | null = null;
        let notagStart = -1;
        let currentTagStart = -1;
        let utf8charindex = -1;
        const utf8parts = new Uint8Array(2);
        let currentStringDelimiter = "";

        for (let i = 0; i < bytes.length; i++) {
            // Match Java's conversion from signed byte to char for raw spans.
            const signedByte = (bytes[i]! << 24) >> 24;
            const char = String.fromCharCode(signedByte);
            if (char === '"' || char === "'") {
                if (!onString) {
                    onString = true;
                    currentString = "";
                    currentStringDelimiter = char;
                } else if (char === currentStringDelimiter) {
                    onString = false;
                    currentNode?.addString(currentString);
                }
                continue;
            }

            if (onString) {
                if (signedByte < 0) {
                    utf8charindex++;
                    utf8parts[utf8charindex] = signedByte;
                    if (utf8charindex >= 1) {
                        utf8charindex = -1;
                        currentString += XmlImporter.buildUtf8Char(utf8parts);
                    }
                } else {
                    currentString += char;
                }
            } else if (char === "<" && !onTag) {
                if (notagString !== null && notagStart !== -1) {
                    const notagNode = new XmlNode("_NOTAG_", notagString);
                    notagNode.setSourceEnd(i - 1);
                    notagNode.setSourceStart(notagStart);
                    nodes.add(notagNode);
                    notagString = null;
                    notagStart = -1;
                }
                onTag = true;
                onTagName = true;
                currentTagStart = i;
            } else if (char === ">" && onTag) {
                onTag = false;
                if (currentIdentifier.length > 0) {
                    if (onTagName) {
                        if (currentNode !== null) nodes.add(currentNode);
                        currentNode = new XmlNode(currentIdentifier);
                    }
                    onTagName = false;
                }
                if (currentNode === null) throw new NullPointerException();
                if (currentIdentifier === "/") currentNode.setClosed(true);

                currentIdentifier = "";
                currentNode.setSourceStart(currentTagStart);
                currentNode.setSourceEnd(i);
                nodes.add(currentNode);
                currentNode = null;
            } else if (!onTag) {
                if (notagStart === -1) {
                    notagStart = i;
                    notagString = "";
                }
                notagString += char;
            } else if (char === " " || char === "=") {
                if (currentIdentifier.length > 0) {
                    if (onTagName) {
                        if (currentNode !== null) nodes.add(currentNode);
                        currentNode = new XmlNode(currentIdentifier);
                    } else {
                        currentNode?.addIdentifier(currentIdentifier);
                    }
                    onTagName = false;
                }
                currentIdentifier = "";
            } else {
                currentIdentifier += char;
            }
        }

        if (currentNode !== null) {
            currentNode.setSourceStart(currentTagStart);
            currentNode.setSourceEnd(bytes.length - 1);
            nodes.add(currentNode);
        }
        return nodes;
    }
}
