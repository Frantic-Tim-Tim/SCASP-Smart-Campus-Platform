(() => {
    "use strict";

    const root = document.getElementById("staffDashboard");
    if (!root) return;

    const apiBase = root.dataset.apiBase;
    const currentUser = root.dataset.currentUser;
    const list = document.getElementById("requestList");
    const message = document.getElementById("staffMessage");

    let requests = [];

    async function api(path = "", options = {}) {
        const config = {
            method: options.method || "GET",
            credentials: "same-origin",
            headers: { "Accept": "application/json" }
        };

        if (options.body !== undefined) {
            config.headers["Content-Type"] = "application/json";
            config.body = JSON.stringify(options.body);
        }

        const response = await fetch(apiBase + path, config);
        const text = await response.text();

        let data = null;
        if (text) {
            try { data = JSON.parse(text); }
            catch (_) { data = text; }
        }

        if (!response.ok) {
            const detail = data && typeof data === "object"
                ? (data.error || data.message || JSON.stringify(data))
                : data;

            throw new Error(detail || `Request failed (${response.status})`);
        }

        return data;
    }

    function showMessage(text, type = "success") {
        message.className = `message ${type}`;
        message.textContent = text;
        message.style.display = "block";
    }

    function clearMessage() {
        message.textContent = "";
        message.style.display = "none";
    }

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function makeSelect(options, selected) {
        const select = document.createElement("select");

        options.forEach(value => {
            const option = document.createElement("option");
            option.value = value;
            option.textContent = value.replaceAll("_", " ");
            option.selected = value === selected;
            select.appendChild(option);
        });

        return select;
    }

    function updateStatistics() {
        document.getElementById("totalCount").textContent = requests.length;
        document.getElementById("submittedCount").textContent =
            requests.filter(r => r.status === "SUBMITTED").length;
        document.getElementById("progressCount").textContent =
            requests.filter(r => r.status === "IN_PROGRESS").length;
        document.getElementById("resolvedCount").textContent =
            requests.filter(r => r.status === "RESOLVED").length;
    }

    function renderRequests() {
        const status = document.getElementById("statusFilter").value;
        const priority = document.getElementById("priorityFilter").value;
        const search = document.getElementById("requestSearch").value
            .trim().toLowerCase();

        const filtered = requests.filter(request => {
            const matchesStatus = !status || request.status === status;
            const matchesPriority = !priority || request.priority === priority;
            const searchable = `${request.title || ""} ${request.studentId || ""}`
                .toLowerCase();

            return matchesStatus && matchesPriority &&
                (!search || searchable.includes(search));
        });

        list.replaceChildren();

        if (!filtered.length) {
            list.appendChild(element("p", "empty",
                requests.length
                    ? "No requests match your filters."
                    : "There are currently no service requests."));
            return;
        }

        filtered.forEach(request => list.appendChild(createRequestCard(request)));
    }

    function createRequestCard(request) {
        const card = element("article", "request-card");
        const heading = element("div", "request-heading");
        const titleGroup = document.createElement("div");

        titleGroup.appendChild(element("h3", "", request.title || "Untitled request"));
        titleGroup.appendChild(element("small", "muted",
            `Student: ${request.studentId || "Unknown"}`));

        heading.appendChild(titleGroup);
        heading.appendChild(element("strong", "", request.status || "UNKNOWN"));
        card.appendChild(heading);

        card.appendChild(element("p", "request-description",
            request.description || "No description provided."));

        const tags = element("div", "tags");
        [
            `Category: ${request.category || "N/A"}`,
            `Priority: ${request.priority || "N/A"}`,
            `Assigned to: ${request.assignedTo || "Unassigned"}`
        ].forEach(value => tags.appendChild(element("span", "tag", value)));

        card.appendChild(tags);

        const submitted = request.createdAt
            ? new Date(request.createdAt).toLocaleString()
            : "Unknown";

        card.appendChild(element("small", "muted", `Submitted: ${submitted}`));

        const actions = element("div", "request-actions");

        // Assignment
        const assignment = element("div", "action-field");
        const assignmentLabel = element("label", "", "Assign to user ID");
        const assignee = document.createElement("input");
        assignee.value = request.assignedTo || currentUser;
        assignee.placeholder = "Staff user ID";
        const assignButton = element("button", "primary", "Assign request");

        assignButton.addEventListener("click", async () => {
            await performAction(assignButton, async () => {
                const staffUserId = assignee.value.trim();
                if (!staffUserId) throw new Error("Enter a staff user ID.");

                await api(`/${encodeURIComponent(request.requestId)}/assign`, {
                    method: "POST",
                    body: { staffUserId }
                });

                await loadRequests();
                showMessage("Request assignment updated.");
            });
        });

        assignment.append(assignmentLabel, assignee, assignButton);

        // Status
        const statusBox = element("div", "action-field");
        const statusLabel = element("label", "", "Update status");
        const statusSelect = makeSelect(
            ["SUBMITTED", "IN_PROGRESS", "RESOLVED", "CLOSED", "CANCELLED"],
            request.status
        );
        const statusButton = element("button", "primary", "Save status");

        statusButton.addEventListener("click", async () => {
            await performAction(statusButton, async () => {
                await api(`/${encodeURIComponent(request.requestId)}/status`, {
                    method: "PATCH",
                    body: { status: statusSelect.value }
                });

                await loadRequests();
                showMessage("Request status updated.");
            });
        });

        statusBox.append(statusLabel, statusSelect, statusButton);

        // Priority
        const priorityBox = element("div", "action-field");
        const priorityLabel = element("label", "", "Change priority");
        const prioritySelect = makeSelect(
            ["LOW", "NORMAL", "HIGH", "URGENT"],
            request.priority
        );
        const priorityButton = element("button", "primary", "Save priority");

        priorityButton.addEventListener("click", async () => {
            await performAction(priorityButton, async () => {
                await api(`/${encodeURIComponent(request.requestId)}/priority`, {
                    method: "PATCH",
                    body: { priority: prioritySelect.value }
                });

                await loadRequests();
                showMessage("Request priority updated.");
            });
        });

        priorityBox.append(priorityLabel, prioritySelect, priorityButton);

        // Progress note
        const noteBox = element("div", "action-field");
        const noteLabel = element("label", "", "Add progress note");
        const note = document.createElement("textarea");
        note.placeholder = "Enter an update for the student...";
        const noteButton = element("button", "primary", "Add note");

        noteButton.addEventListener("click", async () => {
            await performAction(noteButton, async () => {
                const value = note.value.trim();
                if (!value) throw new Error("Enter a progress note.");

                await api(`/${encodeURIComponent(request.requestId)}/updates`, {
                    method: "POST",
                    body: { note: value }
                });

                note.value = "";
                showMessage("Progress note added.");
                await showHistory(request.requestId, history);
            });
        });

        noteBox.append(noteLabel, note, noteButton);

        actions.append(assignment, statusBox, priorityBox, noteBox);
        card.appendChild(actions);

        const historyButton = element("button", "secondary", "View progress history");
        const history = element("div", "history");

        historyButton.addEventListener("click", async () => {
            const isVisible = history.style.display === "block";
            if (isVisible) {
                history.style.display = "none";
                historyButton.textContent = "View progress history";
                return;
            }

            await showHistory(request.requestId, history);
            history.style.display = "block";
            historyButton.textContent = "Hide progress history";
        });

        card.append(historyButton, history);
        return card;
    }

    async function showHistory(requestId, container) {
        try {
            const updates = await api(`/${encodeURIComponent(requestId)}/updates`);
            container.replaceChildren();

            if (!Array.isArray(updates) || updates.length === 0) {
                container.appendChild(element("p", "empty", "No progress updates yet."));
                return;
            }

            updates.forEach(update => {
                const entry = element("div", "history-entry");
                entry.appendChild(element("strong", "",
                    update.authorUserId || "System"));
                entry.appendChild(element("p", "", update.note || ""));
                entry.appendChild(element("small", "muted",
                    update.createdAt
                        ? new Date(update.createdAt).toLocaleString()
                        : ""));
                container.appendChild(entry);
            });
        } catch (error) {
            container.replaceChildren(element("p", "error", error.message));
        }
    }

    async function performAction(button, action) {
        button.disabled = true;
        clearMessage();

        try {
            await action();
        } catch (error) {
            showMessage(error.message, "error");
        } finally {
            button.disabled = false;
        }
    }

    async function loadRequests() {
        clearMessage();
        list.replaceChildren(element("p", "empty", "Loading service requests..."));

        try {
            const result = await api();
            requests = Array.isArray(result) ? result : [];
            updateStatistics();
            renderRequests();
        } catch (error) {
            requests = [];
            updateStatistics();
            list.replaceChildren(element("p", "error",
                `Unable to load requests: ${error.message}`));
        }
    }

    document.getElementById("refreshRequests")
        .addEventListener("click", loadRequests);

    document.getElementById("statusFilter")
        .addEventListener("change", renderRequests);

    document.getElementById("priorityFilter")
        .addEventListener("change", renderRequests);

    document.getElementById("requestSearch")
        .addEventListener("input", renderRequests);

    loadRequests();
})();
