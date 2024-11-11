function renderLatex() {
    // Convert block latex tags
    document.querySelectorAll('BLOCK_LATEX').forEach(elem => {
        elem.outerHTML = '\\[' + elem.innerHTML + '\\]';
    });
    
    // Convert inline latex tags
    document.querySelectorAll('INLINE_LATEX').forEach(elem => {
        elem.outerHTML = '\\(' + elem.innerHTML + '\\)';
    });
    
    // Trigger MathJax rendering
    MathJax.typeset();
}