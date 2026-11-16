<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>SCASP | Administration Dashboard</title>

    <style>
        * { box-sizing: border-box; }

        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f1f5f9;
            color: #1e293b;
        }

        header {
            background: #172554;
            color: white;
            padding: 20px 5%;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 12px;
        }

        header h1 { margin: 0; font-size: 23px; }
        header span { color: #bfdbfe; font-size: 14px; }

        main {
            max-width: 1250px;
            margin: 35px auto;
            padding: 0 20px;
        }

        .welcome { margin-bottom: 25px; }
        .welcome p, .muted { color: #64748b; }

        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
            gap: 18px;
            margin-bottom: 30px;
        }

        .stat-card, .panel {
            background: white;
            padding: 22px;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,.05);
        }

        .stat-card p { color: #64748b; margin: 0 0 12px; }
        .stat-card h3 { font-size: 30px; margin: 0; }

        .panel { margin-bottom: 25px; overflow-x: auto; }
        .panel h2 { margin-top: 0; font-size: 19px; }

        table {
            width: 100%;
            border-collapse: collapse;
            min-width: 650px;
        }

        th, td {
            padding: 12px;
            border-bottom: 1px solid #e2e8f0;
            text-align: left;
        }

        th { background: #f8fafc; }

        input, select {
            width: 100%;
            padding: 10px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font: inherit;
        }

        button {
            border: 0;
            padding: 10px 14px;
            border-radius: 6px;
            cursor: pointer;
            font-weight: bold;
            margin: 3px;
        }

        button:disabled { opacity: .5; cursor: not-allowed; }

        .primary { background: #1d4ed8; color: white; }
        .secondary { background: #e2e8f0; color: #1e293b; }
        .danger { background: #dc2626; color: white; }

        .course-form {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 14px;
            align-items: end;
            margin-bottom: 20px;
        }

        .field label {
            display: block;
            font-weight: bold;
            font-size: 13px;
            margin-bottom: 7px;
        }

        .summary-list {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
        }

        .summary-item {
            background: #eff6ff;
            padding: 12px 16px;
            border-radius: 7px;
        }

        .message {
            padding: 12px;
            margin: 12px 0;
            border-radius: 6px;
            display: none;
        }

        .success { background: #dcfce7; color: #166534; }
        .error { background: #fee2e2; color: #991b1b; }

        .user-toolbar {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 14px;
            align-items: end;
            margin: 18px 0;
        }

        .user-toolbar button { width: 100%; }
        .user-actions { min-width: 190px; }
        .user-actions button { margin: 3px 2px; }
        .user-role-select { min-width: 110px; margin-bottom: 5px; }
        .user-status-active { color: #15803d; }
        .user-status-suspended { color: #b91c1c; }

        hr {
            border: 0;
            border-top: 1px solid #e2e8f0;
            margin: 25px 0;
        }

        .status { font-weight: bold; }
        footer {
            text-align: center;
            color: #64748b;
            padding: 25px;
            font-size: 13px;
        }

        @media(max-width: 600px) {
            main { padding: 0 12px; }
            .panel { padding: 15px; }
        }
    </style>
</head>

<body>

<header>
    <h1>SCASP Administration</h1>
    <span>Smart Campus Academic Services Platform</span>
</header>

<main>

    <section class="welcome">
        <h2>Administration Dashboard</h2>
        <p>
            Welcome,
            <strong><c:out value="${administrator.name}"/></strong>.
            You are signed in as an administrator.
        </p>
    </section>

    <section class="stats">
        <article class="stat-card">
            <p>Total registered users</p>
            <h3><c:out value="${userCount}"/></h3>
        </article>

        <article class="stat-card">
            <p>Active accounts</p>
            <h3><c:out value="${activeCount}"/></h3>
        </article>

        <article class="stat-card">
            <p>Suspended accounts</p>
            <h3><c:out value="${suspendedCount}"/></h3>
        </article>

        <article class="stat-card">
            <p>Total courses</p>
            <h3 id="courseCount">—</h3>
        </article>
    </section>

    <section class="panel">
        <h2>Users by Role</h2>
        <div class="summary-list">
            <c:forEach items="${roleCounts}" var="entry">
                <div class="summary-item">
                    <strong><c:out value="${entry.key}"/></strong>:
                    <c:out value="${entry.value}"/> user(s)
                </div>
            </c:forEach>
        </div>
    </section>

    <section class="panel">
        <h2>Accounts by Status</h2>
        <div class="summary-list">
            <c:forEach items="${statusCounts}" var="entry">
                <div class="summary-item">
                    <strong><c:out value="${entry.key}"/></strong>:
                    <c:out value="${entry.value}"/> account(s)
                </div>
            </c:forEach>
        </div>
    </section>

    <!-- COURSE MANAGEMENT -->
    <section class="panel">
        <h2>Course Management</h2>
        <p class="muted">
            Create, update, search and manage academic courses.
        </p>

        <div id="courseMessage" class="message" role="alert"></div>

        <h3>Create a New Course</h3>

        <form id="createCourseForm" class="course-form">
            <div class="field">
                <label for="newCode">Course Code</label>
                <input id="newCode" maxlength="30"
                       placeholder="e.g. BSC3200" required>
            </div>

            <div class="field">
                <label for="newTitle">Course Title</label>
                <input id="newTitle" maxlength="200"
                       placeholder="Course title" required>
            </div>

            <div class="field">
                <label for="newCapacity">Capacity</label>
                <input id="newCapacity" type="number"
                       min="1" required placeholder="80">
            </div>

            <div class="field">
                <label for="newStatus">Registration Status</label>
                <select id="newStatus">
                    <option value="OPEN">OPEN</option>
                    <option value="CLOSED">CLOSED</option>
                </select>
            </div>

            <div>
                <button class="primary" type="submit">Create Course</button>
            </div>
        </form>

        <hr>

        <div style="display:flex;justify-content:space-between;
                    align-items:center;gap:12px;flex-wrap:wrap;">
            <h3>Existing Courses</h3>
            <button type="button" class="secondary"
                    id="refreshCourses">Refresh Courses</button>
        </div>

        <div style="max-width:400px;margin:12px 0;">
            <input id="courseSearch" type="search"
                   placeholder="Search by course code or title">
        </div>

        <div style="overflow-x:auto;">
            <table>
                <thead>
                    <tr>
                        <th>Course Code</th>
                        <th>Course Title</th>
                        <th>Capacity</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="courseTableBody">
                    <tr><td colspan="5">Loading courses...</td></tr>
                </tbody>
            </table>
        </div>
    </section>

    <!-- USER MANAGEMENT -->
    <section class="panel">
        <h2>User Management</h2>

        <div id="userMessage" class="message" role="status" aria-live="polite"></div>

        <h3>Create Student Account</h3>

        <form id="createStudentForm" class="course-form">
            <div class="field">
                <label for="studentUserId">Login User ID</label>
                <input id="studentUserId" type="text" maxlength="100" required>
            </div>

            <div class="field">
                <label for="studentName">Full Name</label>
                <input id="studentName" type="text" maxlength="200" required>
            </div>

            <div class="field">
                <label for="studentId">Student ID</label>
                <input id="studentId" type="text" maxlength="100" required>
            </div>

            <div class="field">
                <label for="studentProgramme">Programme</label>
                <input id="studentProgramme" type="text" maxlength="150" required>
            </div>

            <div class="field">
                <label for="studentYear">Year of Study</label>
                <input id="studentYear" type="number" min="1" step="1" required>
            </div>

            <div class="field">
                <label for="studentPassword">Initial Password</label>
                <input id="studentPassword" type="password" autocomplete="new-password" required>
            </div>

            <div class="field">
                <button type="submit" class="primary">Create Student Account</button>
            </div>
        </form>

        <hr>

        <div class="user-toolbar">
            <div class="field">
                <label for="userSearch">Search users</label>
                <input id="userSearch" type="search"
                       placeholder="Search by user ID, name, role or status">
            </div>

            <div class="field">
                <label for="userStatusFilter">Filter by status</label>
                <select id="userStatusFilter">
                    <option value="ALL">All statuses</option>
                    <option value="ACTIVE">Active</option>
                    <option value="SUSPENDED">Suspended</option>
                </select>
            </div>

            <div class="field">
                <label for="userRoleFilter">Filter by role</label>
                <select id="userRoleFilter">
                    <option value="ALL">All roles</option>
                    <option value="STUDENT">Student</option>
                    <option value="STAFF">Staff</option>
                    <option value="ADMIN">Administrator</option>
                </select>
            </div>

            <div class="field">
                <label>&nbsp;</label>
                <button type="button" id="refreshUsers" class="secondary">
                    Refresh Users
                </button>
            </div>
        </div>

        <p class="muted">
            Displaying <strong id="userCount">0</strong> user(s).
        </p>

        <div style="overflow-x:auto;">
            <table>
                <thead>
                    <tr>
                        <th>User ID</th>
                        <th>Name</th>
                        <th>Role</th>
                        <th>Account Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody id="userTableBody">
                    <tr><td colspan="5">Loading users...</td></tr>
                </tbody>
            </table>
        </div>
    </section>

    <%@ include file="/WEB-INF/views/admin/service-requests.jspf" %>
    <%@ include file="/WEB-INF/views/admin/announcements.jspf" %>
    <%@ include file="/WEB-INF/views/admin/reports.jspf" %>
</main>

<footer>
    SCASP &copy; 2026 | Administration Portal
</footer>

<script>
(() => {
    "use strict";

    const API = "<%= request.getContextPath() %>/api/admin/courses";

    const tableBody = document.getElementById("courseTableBody");
    const messageBox = document.getElementById("courseMessage");
    const countElement = document.getElementById("courseCount");
    const searchInput = document.getElementById("courseSearch");

    let courses = [];

    function showMessage(message, isError = false) {
        messageBox.textContent = message;
        messageBox.className = "message " + (isError ? "error" : "success");
        messageBox.style.display = "block";
    }

    function clearMessage() {
        messageBox.style.display = "none";
    }

    async function apiRequest(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers: {
                ...(options.body ? {"Content-Type": "application/json"} : {}),
                ...options.headers
            }
        });

        if (response.status === 204) {
            if (!response.ok) throw new Error("Request failed.");
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        const data = contentType.includes("application/json")
            ? await response.json()
            : null;

        if (!response.ok) {
            throw new Error(data?.error || data?.message ||
                `Request failed with HTTP ${response.status}`);
        }

        return data;
    }

    function makeCell(value) {
        const cell = document.createElement("td");
        cell.textContent = value ?? "";
        return cell;
    }

    function makeInput(value, type = "text", min = null) {
        const input = document.createElement("input");
        input.type = type;
        input.value = value ?? "";

        if (min !== null) input.min = min;
        if (type === "text") input.maxLength = 200;

        return input;
    }

    function renderCourses() {
        const search = searchInput.value.trim().toLowerCase();

        const filtered = courses.filter(course =>
            course.courseCode.toLowerCase().includes(search) ||
            course.title.toLowerCase().includes(search)
        );

        tableBody.replaceChildren();

        if (filtered.length === 0) {
            const row = document.createElement("tr");
            const cell = makeCell("No matching courses found.");
            cell.colSpan = 5;
            row.appendChild(cell);
            tableBody.appendChild(row);
            return;
        }

        filtered.forEach(course => {
            const row = document.createElement("tr");

            row.appendChild(makeCell(course.courseCode));

            const titleCell = document.createElement("td");
            const titleInput = makeInput(course.title);
            titleCell.appendChild(titleInput);
            row.appendChild(titleCell);

            const capacityCell = document.createElement("td");
            const capacityInput = makeInput(course.capacity, "number", 1);
            capacityCell.appendChild(capacityInput);
            row.appendChild(capacityCell);

            const statusCell = document.createElement("td");
            const statusSelect = document.createElement("select");

            ["OPEN", "CLOSED"].forEach(status => {
                const option = document.createElement("option");
                option.value = status;
                option.textContent = status;
                statusSelect.appendChild(option);
            });

            statusSelect.value = course.status;
            statusCell.appendChild(statusSelect);
            row.appendChild(statusCell);

            const actionsCell = document.createElement("td");

            const saveButton = document.createElement("button");
            saveButton.textContent = "Save";
            saveButton.className = "primary";

            saveButton.addEventListener("click", async () => {
                const capacity = Number(capacityInput.value);

                if (!titleInput.value.trim() ||
                    !Number.isInteger(capacity) || capacity < 1) {
                    showMessage("Enter a valid title and capacity.", true);
                    return;
                }

                saveButton.disabled = true;

                try {
                    await apiRequest(
                        API + "/" + encodeURIComponent(course.courseCode),
                        {
                            method: "PUT",
                            body: JSON.stringify({
                                courseCode: course.courseCode,
                                title: titleInput.value.trim(),
                                capacity,
                                status: statusSelect.value
                            })
                        }
                    );

                    showMessage("Course updated successfully.");
                    await loadCourses();

                } catch (error) {
                    showMessage(error.message, true);
                } finally {
                    saveButton.disabled = false;
                }
            });

            const deleteButton = document.createElement("button");
            deleteButton.textContent = "Delete";
            deleteButton.className = "danger";

            deleteButton.addEventListener("click", async () => {
                if (!confirm("Delete course " + course.courseCode + "?")) {
                    return;
                }

                deleteButton.disabled = true;

                try {
                    await apiRequest(
                        API + "/" + encodeURIComponent(course.courseCode),
                        {method: "DELETE"}
                    );

                    showMessage("Course deleted successfully.");
                    await loadCourses();

                } catch (error) {
                    showMessage(error.message, true);
                } finally {
                    deleteButton.disabled = false;
                }
            });

            actionsCell.append(saveButton, deleteButton);
            row.appendChild(actionsCell);
            tableBody.appendChild(row);
        });
    }

    async function loadCourses() {
        tableBody.textContent = "Loading courses...";

        try {
            courses = await apiRequest(API) || [];
            countElement.textContent = courses.length;
            renderCourses();
        } catch (error) {
            tableBody.replaceChildren();

            const row = document.createElement("tr");
            const cell = makeCell(error.message);
            cell.colSpan = 5;
            row.appendChild(cell);
            tableBody.appendChild(row);

            showMessage(
                "Could not load courses. Check your administrator session and API access: " +
                error.message,
                true
            );
        }
    }

    document.getElementById("createCourseForm")
        .addEventListener("submit", async event => {
            event.preventDefault();
            clearMessage();

            const course = {
                courseCode: document.getElementById("newCode").value.trim(),
                title: document.getElementById("newTitle").value.trim(),
                capacity: Number(document.getElementById("newCapacity").value),
                status: document.getElementById("newStatus").value
            };

            if (!course.courseCode || !course.title ||
                !Number.isInteger(course.capacity) || course.capacity < 1) {
                showMessage("Please enter a valid course code, title and capacity.", true);
                return;
            }

            const submitButton = event.submitter;
            submitButton.disabled = true;

            try {
                await apiRequest(API, {
                    method: "POST",
                    body: JSON.stringify(course)
                });

                showMessage("Course created successfully.");
                event.target.reset();
                await loadCourses();

            } catch (error) {
                showMessage(error.message, true);
            } finally {
                submitButton.disabled = false;
            }
        });

    document.getElementById("refreshCourses")
        .addEventListener("click", loadCourses);

    searchInput.addEventListener("input", renderCourses);

    loadCourses();
})();
</script>

<script>
(() => {
    "use strict";

    const API = "<%= request.getContextPath() %>/api/admin/users";

    const tableBody = document.getElementById("userTableBody");
    const messageBox = document.getElementById("userMessage");
    const countElement = document.getElementById("userCount");
    const searchInput = document.getElementById("userSearch");
    const statusFilter = document.getElementById("userStatusFilter");
    const roleFilter = document.getElementById("userRoleFilter");
    const createForm = document.getElementById("createStudentForm");

    let users = [];

    function showMessage(message, isError = false) {
        messageBox.textContent = message;
        messageBox.className = "message " + (isError ? "error" : "success");
        messageBox.style.display = "block";
    }

    function clearMessage() {
        messageBox.textContent = "";
        messageBox.style.display = "none";
    }

    async function apiRequest(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            ...options,
            headers: {
                ...(options.body ? {"Content-Type": "application/json"} : {}),
                ...options.headers
            }
        });

        const contentType = response.headers.get("content-type") || "";
        const data = contentType.includes("application/json")
            ? await response.json()
            : null;

        if (!response.ok) {
            throw new Error(data?.error || data?.message ||
                `Request failed with HTTP ${response.status}`);
        }

        return data;
    }

    function makeCell(value) {
        const cell = document.createElement("td");
        cell.textContent = value ?? "";
        return cell;
    }

    function renderUsers() {
        const search = searchInput.value.trim().toLowerCase();

        const filtered = users.filter(user => {
            const matchesSearch = [
                user.userId, user.name, user.role, user.status
            ].some(value =>
                String(value ?? "").toLowerCase().includes(search)
            );

            const matchesStatus =
                statusFilter.value === "ALL" ||
                user.status === statusFilter.value;

            const matchesRole =
                roleFilter.value === "ALL" ||
                user.role === roleFilter.value;

            return matchesSearch && matchesStatus && matchesRole;
        });

        tableBody.replaceChildren();
        countElement.textContent = filtered.length;

        if (filtered.length === 0) {
            const row = document.createElement("tr");
            const cell = makeCell("No matching users found.");
            cell.colSpan = 5;
            row.appendChild(cell);
            tableBody.appendChild(row);
            return;
        }

        filtered.forEach(user => {
            const row = document.createElement("tr");

            row.appendChild(makeCell(user.userId));
            row.appendChild(makeCell(user.name));

            const roleCell = document.createElement("td");
            const roleSelect = document.createElement("select");
            roleSelect.className = "user-role-select";

            ["STUDENT", "STAFF", "ADMIN"].forEach(role => {
                const option = document.createElement("option");
                option.value = role;
                option.textContent = role;
                roleSelect.appendChild(option);
            });

            roleSelect.value = user.role;
            roleCell.appendChild(roleSelect);
            row.appendChild(roleCell);

            const statusCell = makeCell(user.status);
            statusCell.className = user.status === "ACTIVE"
                ? "status user-status-active"
                : "status user-status-suspended";

            row.appendChild(statusCell);

            const actionsCell = document.createElement("td");
            actionsCell.className = "user-actions";

            const saveRoleButton = document.createElement("button");
            saveRoleButton.type = "button";
            saveRoleButton.textContent = "Save Role";
            saveRoleButton.className = "primary";

            saveRoleButton.addEventListener("click", async () => {
                const newRole = roleSelect.value;

                if (newRole === user.role) {
                    showMessage("No role change was made.");
                    return;
                }

                if (!confirm(
                    `Change ${user.userId}'s role from ${user.role} to ${newRole}?`
                )) {
                    roleSelect.value = user.role;
                    return;
                }

                saveRoleButton.disabled = true;

                try {
                    await apiRequest(
                        API + "/" + encodeURIComponent(user.userId) + "/role",
                        {
                            method: "PUT",
                            body: JSON.stringify({role: newRole})
                        }
                    );

                    showMessage("Role updated for " + user.userId + ".");
                    await loadUsers(false);
                } catch (error) {
                    showMessage(error.message, true);
                    roleSelect.value = user.role;
                } finally {
                    saveRoleButton.disabled = false;
                }
            });

            actionsCell.appendChild(saveRoleButton);

            const statusButton = document.createElement("button");
            const activating = user.status === "SUSPENDED";
            const newStatus = activating ? "ACTIVE" : "SUSPENDED";

            statusButton.type = "button";
            statusButton.textContent = activating ? "Activate" : "Suspend";
            statusButton.className = activating ? "secondary" : "danger";

            statusButton.addEventListener("click", async () => {
                if (!confirm(
                    `${activating ? "Activate" : "Suspend"} account ${user.userId}?`
                )) {
                    return;
                }

                statusButton.disabled = true;

                try {
                    await apiRequest(
                        API + "/" + encodeURIComponent(user.userId) + "/status",
                        {
                            method: "PUT",
                            body: JSON.stringify({status: newStatus})
                        }
                    );

                    showMessage(
                        "Account " + user.userId + " is now " + newStatus + "."
                    );

                    await loadUsers(false);
                } catch (error) {
                    showMessage(error.message, true);
                } finally {
                    statusButton.disabled = false;
                }
            });

            actionsCell.appendChild(statusButton);
            row.appendChild(actionsCell);
            tableBody.appendChild(row);
        });
    }

    async function loadUsers(showLoading = true) {
        if (showLoading) {
            tableBody.textContent = "Loading users...";
        }

        try {
            users = await apiRequest(API) || [];
            renderUsers();
        } catch (error) {
            tableBody.replaceChildren();

            const row = document.createElement("tr");
            const cell = makeCell(error.message);
            cell.colSpan = 5;
            row.appendChild(cell);
            tableBody.appendChild(row);

            countElement.textContent = "0";
            showMessage("Could not load users: " + error.message, true);
        }
    }

    createForm.addEventListener("submit", async event => {
        event.preventDefault();
        clearMessage();

        const student = {
            userId: document.getElementById("studentUserId").value.trim(),
            name: document.getElementById("studentName").value.trim(),
            studentId: document.getElementById("studentId").value.trim(),
            programme: document.getElementById("studentProgramme").value.trim(),
            yearOfStudy: Number(
                document.getElementById("studentYear").value
            ),
            password: document.getElementById("studentPassword").value
        };

        if (
            !student.userId || !student.name || !student.studentId ||
            !student.programme ||
            !Number.isInteger(student.yearOfStudy) ||
            student.yearOfStudy < 1 ||
            !student.password.trim()
        ) {
            showMessage("Please provide valid student details and password.", true);
            return;
        }

        const submitButton = event.submitter;
        submitButton.disabled = true;

        try {
            await apiRequest(API + "/students", {
                method: "POST",
                body: JSON.stringify(student)
            });

            showMessage(
                "Student account " + student.userId + " created successfully."
            );

            createForm.reset();
            await loadUsers(false);
        } catch (error) {
            showMessage("Could not create student account: " + error.message, true);
        } finally {
            submitButton.disabled = false;
            student.password = "";
        }
    });

    document.getElementById("refreshUsers")
        .addEventListener("click", () => loadUsers());

    searchInput.addEventListener("input", renderUsers);
    statusFilter.addEventListener("change", renderUsers);
    roleFilter.addEventListener("change", renderUsers);

    loadUsers();
})();
</script>

</body>
</html>
