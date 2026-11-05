/* Se carga en el <head>, antes de pintar la página: activa las animaciones de scroll solo si el usuario
   no pidió reducir el movimiento. Va en archivo aparte porque la política CSP no permite scripts dentro del HTML. */
if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    document.documentElement.classList.add('anim-js');
}
