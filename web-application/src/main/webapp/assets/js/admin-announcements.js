(() => {
    "use strict";

    const section = document.getElementById("admin-announcements");
    if (!section) return;

    const apiBase = section.dataset.apiBase;
    const form = document.getElementById("announcement-admin-form");
    const editId = document.getElementById("announcement-edit-id");
    const titleInput = document.getElementById("announcement-title");
    const bodyInput = document.getElementById("announcement-body");
    const audienceInput = document.getElementById("announcement-audience");
    const submitButton = document.getElementById("announcement-submit-button");
    const cancelButton = document.getElementById("announcement-cancel-edit");
    const message = document.getElementById("announcement-admin-message");
    const list = document.getElementById("announcement-admin-list");

    async function request(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers: {
                "Content-Type": "application/json",
                ...(options.headers || {})
            }
        });

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            throw new Error(data?.message || data?.error || "Request failed.");
        }

        return data;
    }

    function node(tag, className, value) {
        const item = document.createElement(tag);
        if (className) item.className = className;
        if (value !== undefined) item.textContent = value;
        return item;
    }

    function formatDate(value) {
        if (!value) return "Date unavailable";
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? "Date unavailable"
            : date.toLocaleString();
    }

    function resetForm() {
        form.reset();
        editId.value = "";
        submitButton.textContent = "Publish announcement";
        cancelButton.hidden = true;
        message.textContent = "";
    }

    function beginEdit(announcement) {
        editId.value = announcement.announcementId;
        titleInput.value = announcement.title;
        bodyInput.value = announcement.body;
        audienceInput.value = announcement.audience;
        submitButton.textContent = "Save changes";
        cancelButton.hidden = false;
        message.textContent = "Editing announcement.";
        form.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    async function archiveAnnouncement(announcement) {
        if (!confirm(`Archive "${announcement.title}"?`)) return;

        try {
            await request(
                `${apiBase}/${encodeURIComponent(announcement.announcementId)}/archive`,
                { method: "PATCH" }
            );
            message.textContent = "Announcement archived.";
            await loadAnnouncements();
        } catch (error) {
            message.textContent = error.message;
        }
    }

    function render(announcements) {
        list.replaceChildren();

        if (!announcements.length) {
            list.append(node("p", "", "No announcements have been published."));
            return;
        }

        announcements.forEach(announcement => {
            const card = node(
                "article",
                `announcement-admin-card${announcement.archived ? " archived" : ""}`
            );

            card.append(node("h4", "", announcement.title));
            card.append(node("p", "", announcement.body));

            const status = announcement.archived ? "Archived" : "Published";
            card.append(node(
                "div",
                "announcement-list-meta",
                `${announcement.audience} · ${status} · ${formatDate(announcement.publishedAt)}`
            ));

            const actions = node("div", "announcement-list-actions");

            if (!announcement.archived) {
                const edit = node("button", "announcement-edit-button", "Edit");
                edit.type = "button";
                edit.addEventListener("click", () => beginEdit(announcement));
                actions.append(edit);

                const archive = node(
                    "button",
                    "announcement-archive-button",
                    "Archive"
                );
                archive.type = "button";
                archive.addEventListener("click", () =>
                    archiveAnnouncement(announcement)
                );
                actions.append(archive);
            }

            card.append(actions);
            list.append(card);
        });
    }

    async function loadAnnouncements() {
        list.replaceChildren(node("p", "", "Loading announcements..."));

        try {
            const announcements = await request(apiBase);
            render(Array.isArray(announcements) ? announcements : []);
        } catch (error) {
            list.replaceChildren(node(
                "p",
                "",
                `Unable to load announcements: ${error.message}`
            ));
        }
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();

        const payload = {
            title: titleInput.value.trim(),
            body: bodyInput.value.trim(),
            audience: audienceInput.value
        };

        if (!payload.title || !payload.body) {
            message.textContent = "Enter both a title and announcement details.";
            return;
        }

        const id = editId.value;
        submitButton.disabled = true;

        try {
            await request(
                id ? `${apiBase}/${encodeURIComponent(id)}` : apiBase,
                {
                    method: id ? "PUT" : "POST",
                    body: JSON.stringify(payload)
                }
            );

            resetForm();
            message.textContent = id
                ? "Announcement updated successfully."
                : "Announcement published successfully.";

            await loadAnnouncements();
        } catch (error) {
            message.textContent = error.message;
        } finally {
            submitButton.disabled = false;
        }
    });

    cancelButton.addEventListener("click", resetForm);
    document.getElementById("announcement-refresh")
        .addEventListener("click", loadAnnouncements);

    loadAnnouncements();
})();
