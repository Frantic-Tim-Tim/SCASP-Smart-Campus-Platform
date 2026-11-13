<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>SCASP | Administration Dashboard</title>

    <style>
        * {
            box-sizing: border-box;
        }

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

        header h1 {
            margin: 0;
            font-size: 23px;
        }

        header span {
            color: #bfdbfe;
            font-size: 14px;
        }

        main {
            max-width: 1200px;
            margin: 35px auto;
            padding: 0 20px;
        }

        .welcome {
            margin-bottom: 25px;
        }

        .welcome h2 {
            margin-bottom: 6px;
        }

        .welcome p {
            color: #64748b;
        }

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
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }

        .stat-card p {
            color: #64748b;
            margin: 0 0 12px;
        }

        .stat-card h3 {
            font-size: 30px;
            margin: 0;
        }

        .panel {
            margin-bottom: 25px;
            overflow-x: auto;
        }

        .panel h2 {
            margin-top: 0;
            font-size: 19px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            min-width: 600px;
        }

        th, td {
            padding: 13px;
            border-bottom: 1px solid #e2e8f0;
            text-align: left;
        }

        th {
            background: #f8fafc;
        }

        .status {
            font-weight: bold;
        }

        .empty {
            color: #64748b;
            padding: 20px 0;
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

        footer {
            text-align: center;
            color: #64748b;
            padding: 25px;
            font-size: 13px;
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

    <section class="stats" aria-label="User statistics">

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

    <section class="panel">
        <h2>User Management</h2>

        <c:choose>
            <c:when test="${not empty users}">
                <table>
                    <thead>
                        <tr>
                            <th>User ID</th>
                            <th>Name</th>
                            <th>Role</th>
                            <th>Account Status</th>
                        </tr>
                    </thead>

                    <tbody>
                        <c:forEach items="${users}" var="user">
                            <tr>
                                <td><c:out value="${user.userId}"/></td>
                                <td><c:out value="${user.name}"/></td>
                                <td><c:out value="${user.role}"/></td>
                                <td class="status">
                                    <c:out value="${user.status}"/>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:when>

            <c:otherwise>
                <p class="empty">No registered users were found.</p>
            </c:otherwise>
        </c:choose>
    </section>

</main>

<footer>
    SCASP &copy; 2026 | Administration Portal
</footer>

</body>
</html>