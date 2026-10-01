/**
Parent element for the floating elements of the GUIs of the toolkit (popup
menus, windows and their modal backdrops).

While an element is in full screen, the browser renders only that element and
its descendants: anything posted on the document body is not shown. Floating
elements are therefore posted on the full screen element when there is one,
and are moved there (or back to the body) when full screen changes.
*/

const hostedElements: Set<HTMLElement> = new Set<HTMLElement>();
let listening: boolean = false;

/**
@return the element floating elements are posted on: the full screen element,
or the document body when nothing is in full screen
*/
export function overlayHost(): HTMLElement {
    const fullscreenElement: Element | null = document.fullscreenElement;
    if (fullscreenElement instanceof HTMLElement) {
        return fullscreenElement;
    }
    return document.body;
}

/**
Posts a floating element on `overlayHost()` and keeps it there while it is
shown, even if full screen changes.
@param element element to show
*/
export function attachToOverlayHost(element: HTMLElement): void {
    if (!listening) {
        document.addEventListener("fullscreenchange", moveHostedElements);
        listening = true;
    }
    hostedElements.add(element);
    overlayHost().appendChild(element);
}

/**
Removes a floating element posted with `attachToOverlayHost`.
@param element element to hide
*/
export function detachFromOverlayHost(element: HTMLElement): void {
    hostedElements.delete(element);
    element.remove();
}

function moveHostedElements(): void {
    const host: HTMLElement = overlayHost();
    for (const element of hostedElements) {
        if (element.parentElement !== host) {
            host.appendChild(element);
        }
    }
}
