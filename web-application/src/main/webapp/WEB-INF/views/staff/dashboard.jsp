<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SCASP | Staff Dashboard</title>

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
            margin: 32px auto;
            padding: 0 20px;
        }

        .welcome { margin-bottom: 25px; }
        .muted, .welcome p { color: #64748b; }

        .stats {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
            gap: 15px;
            margin-bottom: 25px;
        }

        .stat, .panel, .request-card {
            background: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 2px 8px rgba(0,0,0,.05);
        }

        .stat p { color: #64748b; margin: 0 0 10px; }
        .stat h2 { margin: 0; font-size: 28px; }

        .panel { margin-bottom: 22px; }
        .panel h2 { margin-top: 0; font-size: 19px; }

        .filters {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
            gap: 13px;
            align-items: end;
        }

        label {
            display: block;
            font-size: 13px;
            font-weight: bold;
            margin-bottom: 7px;
        }

        input, select, textarea {
            width: 100%;
            padding: 10px;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            font: inherit;
            background: white;
        }

        textarea { min-height: 75px; resize: vertical; }

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

        .request-list {
            display: grid;
            gap: 17px;
        }

        .request-card { border-left: 4px solid #2563eb; }
        .request-heading {
            display: flex;
            justify-content: space-between;
            gap: 12px;
            flex-wrap: wrap;
        }

        .request-heading h3 { margin: 0 0 8px; }
        .request-description { color: #475569; white-space: pre-wrap; }

        .tags {
            display: flex;
            flex-wrap: wrap;
            gap: 7px;
            margin: 12px 0;
        }

        .tag {
            background: #eff6ff;
            color: #1e40af;
            border-radius: 20px;
            padding: 5px 10px;
            font-size: 12px;
            font-weight: bold;
        }

        .request-actions {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 12px;
            margin-top: 18px;
            padding-top: 15px;
            border-top: 1px solid #e2e8f0;
        }

        .action-field { min-width: 0; }
        .action-field button { width: 100%; margin: 7px 0 0; }

        .history {
            display: none;
            margin-top: 15px;
            padding: 12px;
            background: #f8fafc;
            border-radius: 7px;
        }

        .history-entry {
            padding: 10px 0;
            border-bottom: 1px solid #e2e8f0;
        }

        .history-entry:last-child { border-bottom: 0; }
        .message { padding: 12px; margin: 12px 0; border-radius: 6px; }
        .success { background: #dcfce7; color: #166534; }
        .error { background: #fee2e2; color: #991b1b; }
        .empty { color: #64748b; padding: 15px 0; }

        footer {
            text-align: center;
            color: #64748b;
            padding: 25px;
            font-size: 13px;
        }

        @media(max-width: 600px) {
            main { padding: 0 12px; }
            .request-card, .panel { padding: 15px; }
        }
    </style>
</head>

<body>

<header>
    <h1>SCASP Staff Portal</h1>
    <span>Smart Campus Academic Services Platform</span>
</header>

<main
    id="staffDashboard"
    data-api-base="${pageContext.request.contextPath}/api/service-requests"
    data-current-user="<c:out value='${staffUserId}'/>">

    <section class="welcome">
        <h2>Service Request Management</h2>
        <p>
            Signed in as <strong><c:out value="${staffUserId}"/></strong>.
            Review student requests and manage their progress.
        </p>
    </section>

    <section class="stats">
        <article class="stat">
            <p>Total requests</p>
            <h2 id="totalCount">0</h2>
        </article>
        <article class="stat">
            <p>Submitted</p>
            <h2 id="submittedCount">0</h2>
        </article>
        <article class="stat">
            <p>In progress</p>
            <h2 id="progressCount">0</h2>
        </article>
        <article class="stat">
            <p>Resolved</p>
            <h2 id="resolvedCount">0</h2>
        </article>
    </section>

    <section class="panel">
        <h2>Find Service Requests</h2>
        <div class="filters">
            <div>
                <label for="statusFilter">Filter by status</label>
                <select id="statusFilter">
                    <option value="">All statuses</option>
                    <option value="SUBMITTED">Submitted</option>
                    <option value="IN_PROGRESS">In progress</option>
                    <option value="RESOLVED">Resolved</option>
                    <option value="CLOSED">Closed</option>
                    <option value="CANCELLED">Cancelled</option>
                </select>
            </div>

            <div>
                <label for="priorityFilter">Filter by priority</label>
                <select id="priorityFilter">
                    <option value="">All priorities</option>
                    <option value="LOW">Low</option>
                    <option value="NORMAL">Normal</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                </select>
            </div>

            <div>
                <label for="requestSearch">Search requests</label>
                <input id="requestSearch" type="search"
                       placeholder="Title or student ID">
            </div>

            <div>
                <button type="button" class="primary" id="refreshRequests">
                    Refresh requests
                </button>
            </div>
        </div>
    </section>

    <div id="staffMessage" role="status" aria-live="polite"></div>

    <section>
        <h2>Student Service Requests</h2>
        <div id="requestList" class="request-list">
            <p class="empty">Loading service requests...</p>
        </div>
    </section>

    <%@ include file="/WEB-INF/views/shared/announcements.jspf" %>
</main>

<footer>SCASP — Smart Campus Academic Services Platform</footer>

<script src="${pageContext.request.contextPath}/assets/js/staff-service-requests.js"></script>
</body>
</html>
