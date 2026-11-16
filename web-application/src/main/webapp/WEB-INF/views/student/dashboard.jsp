<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SCASP | Student Dashboard</title>

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
        }

        header h1 { margin: 0; font-size: 23px; }
        header p { margin: 6px 0 0; color: #bfdbfe; }

        main {
            max-width: 1200px;
            margin: 32px auto;
            padding: 0 20px;
        }

        .welcome { margin-bottom: 24px; }
        .welcome p { color: #64748b; }

        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 16px;
            margin-bottom: 24px;
        }

        .stat, .panel {
            background: white;
            padding: 21px;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,.05);
        }

        .stat p { color: #64748b; margin: 0 0 10px; }
        .stat h2 { margin: 0; font-size: 29px; }

        .panel { margin-bottom: 22px; }
        .panel h2 { margin-top: 0; font-size: 19px; }

        .profile-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 15px;
        }

        .profile-item small {
            display: block;
            color: #64748b;
            margin-bottom: 6px;
        }

        .profile-item strong { overflow-wrap: anywhere; }

        .search {
            display: flex;
            gap: 10px;
            margin-bottom: 16px;
        }

        input {
            flex: 1;
            min-width: 0;
            padding: 12px;
            border: 1px solid #cbd5e1;
            border-radius: 7px;
            font-size: 15px;
        }

        button {
            border: 0;
            border-radius: 7px;
            padding: 10px 15px;
            background: #2563eb;
            color: white;
            cursor: pointer;
            font-weight: bold;
        }

        button:hover { background: #1d4ed8; }
        button:disabled { opacity: .6; cursor: not-allowed; }

        .withdraw { background: #b91c1c; }
        .withdraw:hover { background: #991b1b; }

        .table-wrap { overflow-x: auto; }

        table {
            width: 100%;
            border-collapse: collapse;
            min-width: 570px;
        }

        th, td {
            text-align: left;
            padding: 13px;
            border-bottom: 1px solid #e2e8f0;
        }

        th { background: #f8fafc; }

        .status {
            display: inline-block;
            padding: 5px 9px;
            border-radius: 20px;
            background: #dcfce7;
            color: #166534;
            font-size: 12px;
            font-weight: bold;
        }

        .empty { color: #64748b; padding: 12px 0; }
        #message { margin: 12px 0; font-weight: bold; }
        .error { color: #b91c1c; }
        .success { color: #15803d; }

        footer {
            text-align: center;
            padding: 25px;
            color: #64748b;
            font-size: 13px;
        }

        @media (max-width: 520px) {
            .search { flex-direction: column; }
            .search button { width: 100%; }
        }
    </style>
</head>

<body>

<header>
    <h1>SCASP Student Portal</h1>
    <p>Smart Campus Academic Services Platform</p>
</header>

<main>
    <section class="welcome">
        <h2>Welcome, <c:out value="${studentName}"/>!</h2>
        <p>Manage your academic activities from your student dashboard.</p>
    </section>

    <section class="stats">
        <article class="stat">
            <p>Available courses</p>
            <h2><c:out value="${courseCount}"/></h2>
        </article>

        <article class="stat">
            <p>My registrations</p>
            <h2><c:out value="${registrationCount}"/></h2>
        </article>

        <article class="stat">
            <p>Waitlisted courses</p>
            <h2><c:out value="${waitlistCount}"/></h2>
        </article>
    </section>

    <section class="panel">
        <h2>My Student Profile</h2>

        <div class="profile-grid">
            <div class="profile-item">
                <small>Student name</small>
                <strong><c:out value="${studentName}"/></strong>
            </div>

            <div class="profile-item">
                <small>Student number</small>
                <strong><c:out value="${studentId}"/></strong>
            </div>

            <div class="profile-item">
                <small>Programme</small>
                <strong><c:out value="${programme}"/></strong>
            </div>

            <div class="profile-item">
                <small>Year of study</small>
                <strong><c:out value="${yearOfStudy}"/></strong>
            </div>
        </div>
    </section>

    <section class="panel">
        <h2>Available Course Catalogue</h2>

        <div class="search">
            <input
                type="search"
                id="courseSearch"
                placeholder="Search by course code or title"
                aria-label="Search courses">

            <button type="button" id="searchButton">Search</button>
        </div>

        <p id="message" role="status" aria-live="polite"></p>

        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Course code</th>
                        <th>Course title</th>
                        <th>Capacity</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>

                <tbody id="courseResults">
                    <c:forEach items="${courses}" var="course">
                        <tr>
                            <td><c:out value="${course.courseCode}"/></td>
                            <td><c:out value="${course.title}"/></td>
                            <td><c:out value="${course.capacity}"/></td>
                            <td><c:out value="${course.status}"/></td>
                            <td>
                                <button
                                    type="button"
                                    class="register-button"
                                    data-course="<c:out value='${course.courseCode}'/>">
                                    Register
                                </button>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty courses}">
                        <tr><td colspan="5" class="empty">
                            No courses are currently available.
                        </td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </section>

    <section class="panel">
        <h2>My Course Registrations</h2>

        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Course code</th>
                        <th>Status</th>
                        <th>Registered on</th>
                        <th>Action</th>
                    </tr>
                </thead>

                <tbody>
                    <c:forEach items="${registrations}" var="registration">
                        <tr>
                            <td><c:out value="${registration.courseCode}"/></td>
                            <td>
                                <span class="status">
                                    <c:out value="${registration.status}"/>
                                </span>
                            </td>
                            <td><c:out value="${registration.createdAt}"/></td>
                            <td>
                                <c:if test="${registration.status == 'ENROLLED'}">
                                    <button
                                        type="button"
                                        class="withdraw withdraw-button"
                                        data-course="<c:out value='${registration.courseCode}'/>">
                                        Withdraw
                                    </button>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty registrations}">
                        <tr><td colspan="4" class="empty">
                            You are not registered for any courses yet.
                        </td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </section>

    <section class="panel">
        <h2>My Waitlist</h2>

        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Course code</th>
                        <th>Queue position</th>
                        <th>Added on</th>
                    </tr>
                </thead>

                <tbody>
                    <c:forEach items="${waitlistEntries}" var="entry">
                        <tr>
                            <td><c:out value="${entry.courseCode}"/></td>
                            <td><c:out value="${entry.position}"/></td>
                            <td><c:out value="${entry.createdAt}"/></td>
                        </tr>
                    </c:forEach>

                    <c:if test="${empty waitlistEntries}">
                        <tr><td colspan="3" class="empty">
                            You are not currently on any course waitlists.
                        </td></tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </section>
    <%@ include file="/WEB-INF/views/student/service-requests.jspf" %>
    <%@ include file="/WEB-INF/views/shared/announcements.jspf" %>
</main>

<footer>
    SCASP &copy; 2026 | Student Portal
</footer>

<script>
    const contextPath = '${pageContext.request.contextPath}';
    const results = document.getElementById('courseResults');
    const message = document.getElementById('message');

    function showMessage(text, type) {
        message.textContent = text;
        message.className = type || '';
    }

    function addCell(row, value) {
        const cell = document.createElement('td');
        cell.textContent = value == null ? '' : value;
        row.appendChild(cell);
        return cell;
    }

    function createRegisterButton(courseCode) {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = 'Register';
        button.dataset.course = courseCode;
        button.className = 'register-button';
        return button;
    }

    async function searchCourses() {
        const query = document.getElementById('courseSearch').value.trim();
        const url = contextPath + '/api/student/courses'
            + (query ? '?search=' + encodeURIComponent(query) : '');

        showMessage('Searching courses...', '');

        try {
            const response = await fetch(url, {
                method: 'GET',
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Unable to search courses.');
            }

            results.replaceChildren();

            if (!data.courses || data.courses.length === 0) {
                const row = document.createElement('tr');
                const cell = addCell(row, 'No matching courses found.');
                cell.colSpan = 5;
                cell.className = 'empty';
                results.appendChild(row);
            } else {
                data.courses.forEach(course => {
                    const row = document.createElement('tr');
                    addCell(row, course.courseCode);
                    addCell(row, course.title);
                    addCell(row, course.capacity);
                    addCell(row, course.status);

                    const actionCell = document.createElement('td');
                    actionCell.appendChild(
                        createRegisterButton(course.courseCode)
                    );
                    row.appendChild(actionCell);
                    results.appendChild(row);
                });
            }

            showMessage(data.count + ' course(s) found.', 'success');

        } catch (error) {
            showMessage(error.message, 'error');
        }
    }

    async function registerForCourse(courseCode) {
        if (!confirm('Register for ' + courseCode + '?')) {
            return;
        }

        try {
            const response = await fetch(
                contextPath + '/api/student/registrations/'
                    + encodeURIComponent(courseCode),
                {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({})
                }
            );

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Registration failed.');
            }

            const status = data.status || 'COMPLETED';
            showMessage(
                courseCode + ': ' + status + '. Reloading dashboard...',
                'success'
            );

            window.location.reload();

        } catch (error) {
            showMessage(error.message, 'error');
        }
    }

    async function withdrawFromCourse(courseCode) {
        if (!confirm('Are you sure you want to withdraw from '
                + courseCode + '?')) {
            return;
        }

        try {
            const response = await fetch(
                contextPath + '/api/student/registrations/'
                    + encodeURIComponent(courseCode),
                {
                    method: 'DELETE',
                    credentials: 'same-origin',
                    headers: { 'Accept': 'application/json' }
                }
            );

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.error || 'Withdrawal failed.');
            }

            showMessage(
                courseCode + ' withdrawn successfully. Reloading dashboard...',
                'success'
            );

            window.location.reload();

        } catch (error) {
            showMessage(error.message, 'error');
        }
    }

    document.getElementById('searchButton')
        .addEventListener('click', searchCourses);

    document.getElementById('courseSearch')
        .addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                searchCourses();
            }
        });

    document.addEventListener('click', event => {
        const registerButton = event.target.closest('.register-button');
        const withdrawButton = event.target.closest('.withdraw-button');

        if (registerButton) {
            registerForCourse(registerButton.dataset.course);
        } else if (withdrawButton) {
            withdrawFromCourse(withdrawButton.dataset.course);
        }
    });
</script>

</body>
</html>
