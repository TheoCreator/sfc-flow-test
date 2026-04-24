const STATUS_LABEL = {
    DRAFT: "草稿",
    PENDING_1ST: "待一审",
    PENDING_2ND: "待二审",
    PENDING_3RD: "待三审",
    REJECTED: "已退回",
    COMPLETED: "已完成"
};

const STATUS_BADGE = {
    DRAFT: "b-draft",
    PENDING_1ST: "b-p1",
    PENDING_2ND: "b-p2",
    PENDING_3RD: "b-p3",
    REJECTED: "b-rej",
    COMPLETED: "b-done"
};

function qs(selector) {
    return document.querySelector(selector);
}

function qsa(selector) {
    return document.querySelectorAll(selector);
}

function fmtDate(v) {
    if (!v) {
        return "-";
    }
    return v.replace("T", " ").slice(0, 19);
}

function statusBadge(status) {
    const cls = STATUS_BADGE[status] || "b-draft";
    const label = STATUS_LABEL[status] || status;
    return `<span class="badge ${cls}">${label} (${status})</span>`;
}

function showMessage(el, ok, text) {
    if (!el) {
        return;
    }
    el.className = `message ${ok ? "ok" : "error"}`;
    el.textContent = text;
}

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: {"Content-Type": "application/json"},
        ...options
    });
    const data = await response.json();
    if (!response.ok || data.code !== "0") {
        throw new Error(data.message || "请求失败");
    }
    return data.data;
}

function setMyApprovalBadgeCount(count) {
    qsa(".menu-link[data-module='my-approvals']").forEach(link => {
        let badge = link.querySelector(".menu-count");
        if (!badge) {
            badge = document.createElement("span");
            badge.className = "menu-count";
            link.appendChild(badge);
        }
        if (count > 0) {
            badge.style.display = "inline-flex";
            badge.textContent = count > 99 ? "99+" : String(count);
        } else {
            badge.style.display = "none";
        }
    });
}

async function refreshMyApprovalBadgeCount() {
    try {
        const {total} = await api("/api/v1/manuscripts/count?statuses=PENDING_1ST,PENDING_2ND,PENDING_3RD");
        setMyApprovalBadgeCount(total);
    } catch (e) {
        setMyApprovalBadgeCount(0);
    }
}
