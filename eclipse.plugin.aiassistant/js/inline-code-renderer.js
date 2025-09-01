function renderInlineCode() {
    document.querySelectorAll('.inline-code').forEach(elem => {
        let decodedCode = decodeBase64UTF8(elem.innerHTML);
        elem.outerHTML = '<code>' + decodedCode + '</code>';
    });
    hljs.highlightAll();
}