(() => {
    "use strict";

    const section = document.getElementById("scasp-announcements");
    if (!section) return;

    const apiBase = section.dataset.apiBase;
    const feed = document.getElementById("announcement-feed");
    const unreadCount = document.getElementById("announcement-unread-count");

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

    function element(tag, className, value) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (value !== undefined) node.textContent = value;
        return node;
    }

    function formatDate(value) {
        if (!value) return "Date unavailable";
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? "Date unavailable"
            : date.toLocaleString();
    }

    async function markRead(id, button) {
        button.disabled = true;

        try {
            await request(`${apiBase}/${encodeURIComponent(id)}/read`, {
                method: "POST"
            });
            await loadAnnouncements();
        } catch (error) {
            button.disabled = false;
            alert(error.message);
        }
    }

    function render(announcements) {
        feed.replaceChildren();

        const unread = announcements.filter(item => !item.read).length;
        unreadCount.textContent = `${unread} unread`;

        if (!announcements.length) {
            feed.append(element(
                "p",
                "",
                "There are no announcements for you at the moment."
            ));
            return;
        }

        announcements.forEach(item => {
            const announcement = item.announcement;
            const card = element(
                "article",
                `announcement-card${item.read ? "" : " unread"}`
            );

            card.append(element("h3", "", announcement.title));
            card.append(element("p", "", announcement.body));

            const meta = element("div", "announcement-meta");
            meta.append(element(
                "span",
                "",
                `Audience: ${announcement.audience}`
            ));
            meta.append(element(
                "span",
                "",
                `Published: ${formatDate(announcement.publishedAt)}`
            ));
            card.append(meta);

            if (item.read) {
                card.append(element(
                    "div",
                    "announcement-read-label",
                    `Read on ${formatDate(item.readAt)}`
                ));
            } else {
                const button = element(
                    "button",
                    "announcement-read-button",
                    "Mark as read"
                );
                button.type = "button";
                button.addEventListener("click", () =>
                    markRead(announcement.announcementId, button)
                );
                card.append(button);
            }

            feed.append(card);
        });
    }

    async function loadAnnouncements() {
        feed.replaceChildren(element("p", "", "Loading announcements..."));

        try {
            const announcements = await request(`${apiBase}/mine`);
            render(Array.isArray(announcements) ? announcements : []);
        } catch (error) {
            feed.replaceChildren(element(
                "p",
                "",
                `Unable to load announcements: ${error.message}`
            ));
        }
    }

    loadAnnouncements();
})();
