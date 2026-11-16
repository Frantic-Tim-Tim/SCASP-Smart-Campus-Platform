(() => {
    "use strict";

    const root = document.getElementById("adminAcademicReports");
    if (!root) return;

    const message = document.getElementById("academicReportsMessage");

    const reports = [
        {
            url: root.dataset.courseUrl,
            body: "courseEnrolmentRows",
            fields: [
                "courseCode", "courseTitle", "capacity",
                "courseStatus", "enrolledCount",
                "waitlistCount", "availableSeats"
            ],
            columns: 7
        },
        {
            url: root.dataset.registrationUrl,
            body: "studentRegistrationRows",
            fields: [
                "studentId", "courseCode", "courseTitle",
                "registrationStatus", "registeredAt"
            ],
            columns: 5
        },
        {
            url: root.dataset.waitlistUrl,
            body: "courseWaitlistRows",
            fields: [
                "courseCode", "courseTitle", "studentId",
                "position", "waitlistedAt"
            ],
            columns: 5
        }
    ];

    function renderRows(report, data) {
        const tbody = document.getElementById(report.body);
        tbody.replaceChildren();

        if (!Array.isArray(data) || data.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = report.columns;
            cell.textContent = "No records found.";
            row.appendChild(cell);
            tbody.appendChild(row);
            return;
        }

        data.forEach(record => {
            const row = document.createElement("tr");

            report.fields.forEach(field => {
                const cell = document.createElement("td");
                cell.textContent = record[field] ?? "—";
                row.appendChild(cell);
            });

            tbody.appendChild(row);
        });
    }

    async function loadReport(report) {
        const response = await fetch(report.url, {
            method: "GET",
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });

        if (!response.ok) {
            throw new Error(`Request failed (${response.status})`);
        }

        const data = await response.json();
        renderRows(report, data);
    }

    async function refreshReports() {
        message.textContent = "Loading academic reports...";

        const results = await Promise.allSettled(
            reports.map(loadReport)
        );

        const failures = results.filter(
            result => result.status === "rejected"
        );

        if (failures.length === 0) {
            message.textContent = "All academic reports loaded successfully.";
        } else {
            message.textContent =
                `${failures.length} report(s) could not be loaded. ` +
                failures.map(result => result.reason.message).join("; ");
        }
    }

    document.getElementById("refreshAcademicReports")
        .addEventListener("click", refreshReports);

    refreshReports();
})();
