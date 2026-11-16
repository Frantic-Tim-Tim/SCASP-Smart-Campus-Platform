(() => {
    "use strict";

    const root = document.getElementById("serviceRequests");
    if (!root) return;

    const API = root.dataset.apiBase;
    const form = document.getElementById("srForm");
    const submitButton = document.getElementById("srSubmit");
    const message = document.getElementById("srMessage");
    const list = document.getElementById("srList");
    const count = document.getElementById("srCount");

    function showMessage(text, type = "") {
        message.textContent = text;
        message.className = "sr-message " + type;
    }

    async function apiRequest(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers: {
                Accept: "application/json",
                ...(options.body
                    ? { "Content-Type": "application/json" }
                    : {}),
                ...options.headers
            }
        });

        const contentType = response.headers.get("content-type") || "";
        const data = contentType.includes("application/json")
            ? await response.json()
            : null;

        if (!response.ok) {
            throw new Error(
                data?.error || data?.message ||
                `Request failed with HTTP ${response.status}`
            );
        }

        return data;
    }

    function addTextElement(parent, tag, text, className = "") {
        const element = document.createElement(tag);
        element.textContent = text ?? "";
        if (className) element.className = className;
        parent.appendChild(element);
        return element;
    }

    function renderRequests(requests) {
        list.replaceChildren();
        count.textContent = requests.length;

        if (!requests.length) {
            addTextElement(
                list, "p",
                "You have not submitted any service requests yet.",
                "sr-empty"
            );
            return;
        }

        requests.forEach(request => {
            const card = document.createElement("article");
            card.className = "sr-card";

            addTextElement(card, "h3", request.title);

            addTextElement(
                card, "p", request.description, "sr-muted"
            );

            const meta = document.createElement("div");
            meta.className = "sr-meta";

            addTextElement(meta, "span", request.status, "sr-tag");
            addTextElement(meta, "span", "Priority: " + request.priority, "sr-tag");
            addTextElement(meta, "span", "Category: " + request.category, "sr-tag");

            card.appendChild(meta);

            addTextElement(
                card,
                "p",
                "Assigned to: " + (request.assignedTo || "Not yet assigned"),
                "sr-muted"
            );

            addTextElement(
                card,
                "p",
                "Submitted: " + new Date(request.createdAt).toLocaleString(),
                "sr-muted"
            );

            const historyButton = document.createElement("button");
            historyButton.type = "button";
            historyButton.className = "sr-button";
            historyButton.textContent = "View Progress History";

            const historyContainer = document.createElement("div");
            historyContainer.hidden = true;
            historyContainer.className = "sr-history";

            historyButton.addEventListener("click", async () => {
                if (!historyContainer.hidden) {
                    historyContainer.hidden = true;
                    historyButton.textContent = "View Progress History";
                    return;
                }

                historyContainer.hidden = false;
                historyContainer.textContent = "Loading progress history...";

                try {
                    const updates = await apiRequest(
                        API + "/" + encodeURIComponent(request.requestId)
                            + "/updates"
                    );

                    historyContainer.replaceChildren();

                    if (!updates.length) {
                        addTextElement(
                            historyContainer, "p",
                            "No progress updates are available yet.",
                            "sr-muted"
                        );
                    } else {
                        const updateList = document.createElement("ul");

                        updates.forEach(update => {
                            const item = document.createElement("li");

                            addTextElement(item, "strong", update.note);
                            addTextElement(
                                item,
                                "p",
                                "By " + update.authorUserId + " • "
                                    + new Date(update.createdAt).toLocaleString(),
                                "sr-muted"
                            );

                            updateList.appendChild(item);
                        });

                        historyContainer.appendChild(updateList);
                    }

                    historyButton.textContent = "Hide Progress History";

                } catch (error) {
                    historyContainer.textContent = error.message;
                }
            });

            card.appendChild(historyButton);
            card.appendChild(historyContainer);
            list.appendChild(card);
        });
    }

    async function loadRequests() {
        list.textContent = "Loading your requests...";

        try {
            const requests = await apiRequest(API + "/mine");
            renderRequests(requests || []);
        } catch (error) {
            list.textContent = error.message;
            showMessage("Unable to load service requests.", "sr-error");
        }
    }

    form.addEventListener("submit", async event => {
        event.preventDefault();
        showMessage("");

        const request = {
            title: document.getElementById("srTitle").value.trim(),
            description: document.getElementById("srDescription").value.trim(),
            category: document.getElementById("srCategory").value,
            priority: document.getElementById("srPriority").value
        };

        if (!request.title || !request.description) {
            showMessage("Enter a title and description.", "sr-error");
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "Submitting...";

        try {
            await apiRequest(API, {
                method: "POST",
                body: JSON.stringify(request)
            });

            form.reset();
            document.getElementById("srPriority").value = "NORMAL";

            showMessage(
                "Your service request was submitted successfully.",
                "sr-success"
            );

            await loadRequests();

        } catch (error) {
            showMessage(error.message, "sr-error");
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Submit Request";
        }
    });

    loadRequests();
})();
