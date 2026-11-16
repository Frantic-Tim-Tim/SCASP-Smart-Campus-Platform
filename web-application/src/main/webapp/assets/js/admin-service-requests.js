(() => {
    "use strict";

    const root = document.getElementById("adminServiceRequests");
    if (!root) return;

    const apiBase = root.dataset.apiBase;
    const rows = document.getElementById("adminRequestRows");
    const message = document.getElementById("adminRequestMessage");

    let requests = [];

    async function loadRequests() {
        message.textContent = "Loading service requests...";
        rows.replaceChildren();

        try {
            const response = await fetch(apiBase, {
                method: "GET",
                credentials: "same-origin",
                headers: { "Accept": "application/json" }
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || `Request failed (${response.status})`);
            }

            requests = Array.isArray(data) ? data : [];
            updateStatistics();
            renderRows();
            message.textContent = "";
        } catch (error) {
            message.textContent = `Unable to load service requests: ${error.message}`;
            rows.appendChild(makeCellRow(error.message));
        }
    }

    function updateStatistics() {
        const count = status =>
            requests.filter(request => request.status === status).length;

        document.getElementById("adminRequestTotal").textContent =
            requests.length;

        document.getElementById("adminRequestPending").textContent =
            count("SUBMITTED");

        document.getElementById("adminRequestProgress").textContent =
            count("IN_PROGRESS");

        document.getElementById("adminRequestResolved").textContent =
            count("RESOLVED");

        document.getElementById("adminRequestUrgent").textContent =
            requests.filter(request => request.priority === "URGENT").length;
    }

    function makeCellRow(text) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 7;
        cell.className = "admin-request-empty";
        cell.textContent = text;
        row.appendChild(cell);
        return row;
    }

    function renderRows() {
        const status = document.getElementById("adminRequestStatus").value;
        const priority = document.getElementById("adminRequestPriority").value;
        const search = document.getElementById("adminRequestSearch")
            .value.trim().toLowerCase();

        const filtered = requests.filter(request => {
            const matchesStatus = !status || request.status === status;
            const matchesPriority = !priority || request.priority === priority;
            const searchable =
                `${request.title || ""} ${request.studentId || ""}`.toLowerCase();

            return matchesStatus && matchesPriority &&
                (!search || searchable.includes(search));
        });

        rows.replaceChildren();

        if (filtered.length === 0) {
            rows.appendChild(makeCellRow(
                requests.length
                    ? "No requests match the selected filters."
                    : "No service requests have been submitted."
            ));
            return;
        }

        filtered.forEach(request => {
            const row = document.createElement("tr");

            const values = [
                request.title || "Untitled request",
                request.studentId || "Unknown",
                request.category || "N/A",
                request.priority || "N/A",
                request.status || "N/A",
                request.assignedTo || "Unassigned",
                request.createdAt
                    ? new Date(request.createdAt).toLocaleString()
                    : "Unknown"
            ];

            values.forEach(value => {
                const cell = document.createElement("td");
                cell.textContent = value;
                row.appendChild(cell);
            });

            rows.appendChild(row);
        });
    }

    document.getElementById("adminRequestRefresh")
        .addEventListener("click", loadRequests);

    document.getElementById("adminRequestStatus")
        .addEventListener("change", renderRows);

    document.getElementById("adminRequestPriority")
        .addEventListener("change", renderRows);

    document.getElementById("adminRequestSearch")
        .addEventListener("input", renderRows);

    loadRequests();
})();
