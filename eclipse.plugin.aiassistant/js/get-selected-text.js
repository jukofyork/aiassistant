function getSelectedTextFromElement(elementId) {
    const element = document.getElementById(elementId);
    const selection = window.getSelection();
    
    if (selection.rangeCount > 0) {
        const range = selection.getRangeAt(0);
        if (element.contains(range.commonAncestorContainer) && selection.toString().trim() !== '') {
            return range.toString();
        }
    }
    return element.innerText; // fallback to full text if no selection or empty selection
}