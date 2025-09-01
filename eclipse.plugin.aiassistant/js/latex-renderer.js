function renderLatex() {
    // Convert block latex tags
    document.querySelectorAll('.block-latex').forEach(elem => {
        let decodedLatex = decodeBase64UTF8(elem.innerHTML);
        elem.outerHTML = '\\\[' + decodedLatex + '\\\]';
    });
    
    // Convert inline latex tags
    document.querySelectorAll('.inline-latex').forEach(elem => {
        let decodedLatex = decodeBase64UTF8(elem.innerHTML);
        elem.outerHTML = '\\\(' + decodedLatex + '\\\)';
    });
    
    MathJax.typeset();
}