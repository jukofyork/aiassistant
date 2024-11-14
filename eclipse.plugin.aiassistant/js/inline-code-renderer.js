function renderInlineCode() {
    document.querySelectorAll('.inline-code').forEach(elem => {
        let decodedCode = atob(elem.innerHTML);
        elem.outerHTML = '<code>' + decodedCode + '</code>';
    });
    hljs.highlightAll();
}