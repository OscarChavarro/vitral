import type { TangibleInterfaceEvent } from "./TangibleInterfaceEvent.js";

/** Listener notified for each tangible-interface pose update. */
export interface TangibleInterfaceListener {
  tangibleInterfaceEventReceived(event: TangibleInterfaceEvent): void;
}
