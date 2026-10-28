(() => {
    const input = document.querySelector("[data-vehicle-photo-input]");
    const preview = document.querySelector("[data-vehicle-photo-preview]");
    const removeButton = document.querySelector("[data-vehicle-photo-remove]");
    const removeField = document.querySelector("[data-remove-photo]");
    const error = document.querySelector("[data-vehicle-photo-error]");

    if (!input || !preview || !removeButton || !removeField || !error) {
        return;
    }

    const emptyMessage = "No vehicle photo selected yet.";
    const clearPreview = () => {
        preview.classList.remove("vehicle-photo-preview--has-image");
        preview.innerHTML = `<p data-vehicle-photo-empty>${emptyMessage}</p>`;
        removeButton.disabled = true;
    };

    input.addEventListener("change", () => {
        error.textContent = "";
        const file = input.files[0];
        if (!file) {
            return;
        }
        if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)
                || file.size > 5 * 1024 * 1024) {
            error.textContent = "Choose a JPG, PNG, or WebP image up to 5 MB.";
            input.value = "";
            clearPreview();
            return;
        }
        const imageUrl = URL.createObjectURL(file);
        preview.innerHTML = `<img src="${imageUrl}" alt="Selected vehicle photo">`;
        preview.classList.add("vehicle-photo-preview--has-image");
        removeButton.disabled = false;
        removeField.value = "false";
    });

    removeButton.addEventListener("click", () => {
        input.value = "";
        removeField.value = "true";
        clearPreview();
        error.textContent = "";
    });
})();
