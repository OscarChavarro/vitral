import { NullPointerException } from "../../../../java/lang/NullPointerException.js";
import { ArrayList } from "../../../../java/util/ArrayList.js";
import { PersistenceElement } from "../PersistenceElement.js";
import { XmlNodeParam } from "./XmlNodeParam.js";

export class XmlNode extends PersistenceElement {
    private readonly params = new ArrayList<XmlNodeParam>();
    private sourceStart = 0;
    private sourceEnd = 0;
    private closed = false;
    private currentParam: XmlNodeParam | null = null;

    public constructor(
        private name: string,
        content?: string | null,
    ) {
        super();
        if (content !== undefined) {
            const param = new XmlNodeParam("content");
            param.value = content;
            this.params.add(param);
        }
    }

    public isClosed(): boolean {
        return this.closed;
    }

    public setClosed(closed: boolean): void {
        this.closed = closed;
    }

    public isEmpty(): boolean {
        if (this.name !== "_NOTAG_" || this.params.size() > 1) return false;
        const entry = this.params.get(0);
        if (entry.param === "contents") return false;
        if (entry.value === null) throw new NullPointerException();
        for (const char of entry.value) {
            if (char !== " " && char !== "\t") return false;
        }
        return true;
    }

    public addIdentifier(value: string): void {
        this.currentParam = new XmlNodeParam(value);
        this.params.add(this.currentParam);
    }

    public addString(value: string): void {
        if (this.currentParam !== null) {
            this.currentParam.value = value;
        }
    }

    public getValueByParam(key: string): string | null {
        for (const param of this.params) {
            if (param.param === key) return param.value;
        }
        return null;
    }

    public override toString(): string {
        let message = this.getName();
        for (let i = 0; i < this.params.size(); i++) {
            const param = this.params.get(i);
            if (i === 0) message += ": ";
            message += param.param;
            if (param.value !== null && param.value.length > 0) message += "=" + param.value;
            if (i !== this.params.size() - 1) message += ", ";
        }
        return message;
    }

    public value(param: string): string | null {
        return this.getValueByParam(param);
    }

    public getName(): string {
        return this.name;
    }

    public setName(name: string): void {
        this.name = name;
    }

    public getSourceStart(): number {
        return this.sourceStart;
    }

    public setSourceStart(sourceStart: number): void {
        this.sourceStart = sourceStart;
    }

    public getSourceEnd(): number {
        return this.sourceEnd;
    }

    public setSourceEnd(sourceEnd: number): void {
        this.sourceEnd = sourceEnd;
    }
}
