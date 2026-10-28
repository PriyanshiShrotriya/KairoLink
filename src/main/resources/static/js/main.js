(() => {
    const menuToggle = document.querySelector(".nav-menu-toggle");
    const navLinks = document.querySelector(".nav-links");

    if (!menuToggle || !navLinks) {
        return;
    }

    const syncMenuState = () => {
        menuToggle.setAttribute("aria-expanded", String(menuToggle.checked));
    };

    menuToggle.addEventListener("change", syncMenuState);
    navLinks.addEventListener("click", (event) => {
        if (event.target.closest("a")) {
            menuToggle.checked = false;
            syncMenuState();
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && menuToggle.checked) {
            menuToggle.checked = false;
            syncMenuState();
            menuToggle.focus();
        }
    });

    syncMenuState();
})();