import { PersistenceElement } from "../PersistenceElement.js";

export class XmlNodeParam extends PersistenceElement {
    public value: string | null = null;

    public constructor(public param: string) {
        super();
    }
}
