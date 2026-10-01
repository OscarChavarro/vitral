import { PresentationElement } from "./PresentationElement.js";

/**
This class represents the concept of event language as explained in section
[FOLE1992.10.6] and figure [FOLE1992.10.24].
*/
export abstract class CommandListener extends PresentationElement {
    public abstract executeCommand(commandId: string): boolean;
}
