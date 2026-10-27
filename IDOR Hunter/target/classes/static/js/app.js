/* IDOR Hunter Security Lab - frontend application logic (vanilla JS, no build step). */

const state = {
  user: null,
  labMode: 'SECURE',
  switchingAllowed: false,
};

/* ---------------- low level helpers ---------------- */

function getCookie(name) {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'));
  return match ? decodeURIComponent(match[2]) : null;
}

async function api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  if (method !== 'GET') {
    const token = getCookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  const response = await fetch(path, {
    method,
    headers,
    credentials: 'include',
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  let data = null;
  const text = await response.text();
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = text; }
  }
  return { ok: response.ok, status: response.status, data };
}

function showToast(message, type) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.className = 'toast' + (type ? ' ' + type : '');
  toast.hidden = false;
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => { toast.hidden = true; }, 3800);
}

function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

function fmtTime(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return d.toLocaleString();
}

/* ---------------- auth ---------------- */

async function checkSession() {
  await fetch('/api/auth/status', { credentials: 'include' }); // primes CSRF cookie too
  const res = await api('/api/auth/status');
  if (res.ok) {
    state.user = res.data;
    enterApp();
  } else {
    showLanding();
  }
}

async function login(username, password) {
  const res = await api('/api/auth/login', { method: 'POST', body: { username, password } });
  if (res.ok) {
    state.user = res.data;
    enterApp();
  } else {
    const errEl = document.getElementById('login-error');
    errEl.textContent = (res.data && res.data.message) || 'Login failed';
    errEl.hidden = false;
  }
}

async function logout() {
  await api('/api/auth/logout', { method: 'POST' });
  state.user = null;
  showLanding();
}

function showLanding() {
  document.getElementById('landing').hidden = false;
  document.getElementById('app').hidden = true;
}

function enterApp() {
  document.getElementById('landing').hidden = true;
  document.getElementById('app').hidden = false;
  document.getElementById('session-user').textContent =
    `${state.user.displayName} (${state.user.role})`;
  document.getElementById('settings-session-info').textContent =
    `Signed in as ${state.user.username} - role ${state.user.role}`;
  document.getElementById('settings-admin-card').hidden = state.user.role !== 'ADMIN';
  refreshLabMode();
  switchTab('dashboard');
}

/* ---------------- tabs ---------------- */

function switchTab(tab) {
  document.querySelectorAll('.tab').forEach((btn) => btn.classList.toggle('active', btn.dataset.tab === tab));
  document.querySelectorAll('.view').forEach((view) => { view.hidden = view.id !== `view-${tab}`; });

  if (tab === 'dashboard') loadDashboard();
  if (tab === 'documents') loadDocuments();
  if (tab === 'accounts') loadAccounts();
  if (tab === 'lab') loadLabTab();
  if (tab === 'results') loadResults();
  if (tab === 'alerts') loadIncidentsAndAlerts();
}

/* ---------------- lab mode ---------------- */

async function refreshLabMode() {
  const res = await api('/api/lab/mode');
  if (res.ok) {
    state.labMode = res.data.mode;
    state.switchingAllowed = res.data.switchingAllowed;
  }
  const badge = document.getElementById('mode-badge');
  badge.textContent = state.labMode === 'VULNERABLE' ? 'VULNERABLE MODE' : 'SECURE MODE';
  badge.classList.toggle('vulnerable', state.labMode === 'VULNERABLE');
}

function renderModeSwitch(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;
  const canSwitch = state.switchingAllowed && state.user && state.user.role === 'ADMIN';
  container.innerHTML = `
    <button class="btn ${state.labMode === 'SECURE' ? 'btn-primary' : ''}" id="${containerId}-secure" ${canSwitch ? '' : 'disabled'}>Secure</button>
    <button class="btn ${state.labMode === 'VULNERABLE' ? 'btn-primary' : ''}" id="${containerId}-vuln" ${canSwitch ? '' : 'disabled'}>Vulnerable</button>
  `;
  if (canSwitch) {
    document.getElementById(`${containerId}-secure`).onclick = () => setMode('SECURE');
    document.getElementById(`${containerId}-vuln`).onclick = () => setMode('VULNERABLE');
  }
}

async function setMode(mode) {
  const res = await api('/api/admin/lab/mode', { method: 'POST', body: { mode } });
  if (res.ok) {
    showToast(`Lab mode switched to ${mode}`, 'success');
    await refreshLabMode();
    renderModeSwitch('lab-mode-switch');
    renderModeSwitch('settings-mode-switch');
    document.getElementById('lab-mode-text').textContent = state.labMode;
    updateLabModeDescription();
  } else {
    showToast((res.data && (res.data.message || res.data.error)) || 'Could not switch mode', 'error');
  }
}

function updateLabModeDescription() {
  const desc = document.getElementById('lab-mode-desc');
  if (!desc) return;
  desc.textContent = state.labMode === 'VULNERABLE'
    ? 'Server-side ownership checks are disabled on the document API. Cross-user document access will succeed.'
    : 'Server-side ownership checks are enforced. Cross-user document access is rejected with 403 Forbidden.';
}

/* ---------------- dashboard ---------------- */

async function loadDashboard() {
  const statsEl = document.getElementById('dashboard-stats');
  statsEl.innerHTML = '<div class="skeleton"></div>'.repeat(4);

  const [incidentsRes, resultsRes] = await Promise.all([
    api('/api/admin/incidents').catch(() => ({ ok: false })),
    api('/api/pentest/results'),
  ]);

  const incidents = incidentsRes.ok ? incidentsRes.data : [];
  const results = resultsRes.ok ? resultsRes.data : [];

  const breaches = incidents.filter((i) => i.outcome === 'CONFIRMED_BREACH').length;
  const blocked = incidents.filter((i) => i.outcome === 'BLOCKED_ATTEMPT').length;
  const passed = results.filter((r) => r.passed).length;

  statsEl.innerHTML = `
    <div class="stat-card"><div class="stat-value">${state.labMode}</div><div class="stat-label">Lab mode</div></div>
    <div class="stat-card"><div class="stat-value">${breaches}</div><div class="stat-label">Confirmed breaches</div></div>
    <div class="stat-card"><div class="stat-value">${blocked}</div><div class="stat-label">Blocked attempts</div></div>
    <div class="stat-card"><div class="stat-value">${passed}/${results.length}</div><div class="stat-label">Latest tests passed</div></div>
  `;

  document.getElementById('dashboard-incidents').innerHTML = incidentsRes.ok
    ? renderIncidentsTable(incidents.slice(0, 5))
    : '<p class="empty-state">Sign in as Administrator to view incidents.</p>';

  document.getElementById('dashboard-tests').innerHTML = results.length
    ? renderResultsTable(results.slice(0, 5))
    : '<p class="empty-state">No tests run yet. Go to Security Lab and run the suite.</p>';
}

/* ---------------- documents ---------------- */

async function loadDocuments() {
  const listEl = document.getElementById('doc-list');
  listEl.innerHTML = '<div class="skeleton"></div>'.repeat(3);

  const res = await api('/api/documents');
  if (!res.ok) {
    listEl.innerHTML = '<p class="empty-state">Could not load documents.</p>';
    return;
  }
  renderDocList(res.data);

  document.getElementById('doc-search').oninput = (e) => {
    const q = e.target.value.toLowerCase();
    renderDocList(res.data.filter((d) => d.title.toLowerCase().includes(q)));
  };
  document.getElementById('doc-open-btn').onclick = () => {
    const id = document.getElementById('doc-id-input').value;
    if (id) openDocument(id);
  };
}

function renderDocList(docs) {
  const listEl = document.getElementById('doc-list');
  if (!docs.length) {
    listEl.innerHTML = '<p class="empty-state">No documents match.</p>';
    return;
  }
  listEl.innerHTML = docs.map((d) => `
    <div class="doc-tile" data-id="${d.id}">
      <div class="doc-title">${escapeHtml(d.title)}</div>
      <div class="doc-meta"><span>#${d.id} &middot; ${d.classification}</span><span>${escapeHtml(d.ownerUsername)}</span></div>
    </div>
  `).join('');
  listEl.querySelectorAll('.doc-tile').forEach((tile) => {
    tile.onclick = () => openDocument(tile.dataset.id);
  });
}

async function openDocument(id) {
  const wrap = document.getElementById('doc-preview-wrap');
  wrap.innerHTML = '<div class="card"><div class="skeleton"></div><div class="skeleton"></div></div>';

  const res = await api(`/api/documents/${id}`);
  if (res.ok) {
    const d = res.data;
    const isOwn = state.user && d.ownerUsername === state.user.username;
    const banner = isOwn
      ? ''
      : `<div class="alert-banner danger">&#9888; This document is owned by <strong>${escapeHtml(d.ownerUsername)}</strong>, not you. If you can read this, the lab is in <strong>vulnerable</strong> mode and an incident has just been logged.</div>`;
    wrap.innerHTML = `
      <div class="card doc-preview">
        <h3>${escapeHtml(d.title)} <span class="badge badge-status">#${d.id} &middot; ${d.classification}</span></h3>
        <p class="muted small">Owner: ${escapeHtml(d.ownerUsername)} &middot; Created ${fmtTime(d.createdAt)}</p>
        ${banner}
        <div class="doc-body">${escapeHtml(d.content)}</div>
      </div>`;
  } else {
    const message = (res.data && res.data.message) || `Request failed (${res.status})`;
    const type = res.status === 403 ? 'success' : 'danger';
    const label = res.status === 403
      ? '&#10003; Access correctly blocked by server-side authorisation.'
      : (res.status === 404 ? 'Document not found.' : (res.status === 401 ? 'You must be signed in.' : 'Error.'));
    wrap.innerHTML = `<div class="card"><div class="alert-banner ${type}">${label} <span class="muted small">(${escapeHtml(message)})</span></div></div>`;
  }
}

/* ---------------- accounts ---------------- */

async function loadAccounts() {
  const el = document.getElementById('accounts-table');
  if (state.user.role !== 'ADMIN') {
    el.innerHTML = `
      <table><thead><tr><th>Username</th><th>Display name</th><th>Role</th></tr></thead>
      <tbody><tr><td>${escapeHtml(state.user.username)}</td><td>${escapeHtml(state.user.displayName)}</td><td>${state.user.role}</td></tr></tbody></table>
      <p class="muted small" style="margin-top:10px;">Sign in as Administrator to view every lab account.</p>`;
    return;
  }
  const res = await api('/api/admin/users');
  if (!res.ok) { el.innerHTML = '<p class="empty-state">Could not load accounts.</p>'; return; }
  el.innerHTML = `
    <table><thead><tr><th>Username</th><th>Display name</th><th>Role</th></tr></thead>
    <tbody>${res.data.map((u) => `<tr><td>${escapeHtml(u.username)}</td><td>${escapeHtml(u.displayName)}</td><td>${u.role}</td></tr>`).join('')}</tbody></table>`;
}

/* ---------------- security lab ---------------- */

async function loadLabTab() {
  await refreshLabMode();
  document.getElementById('lab-mode-text').textContent = state.labMode;
  updateLabModeDescription();
  renderModeSwitch('lab-mode-switch');
  renderModeSwitch('settings-mode-switch');

  document.getElementById('run-tests-btn').onclick = runTests;
}

async function runTests() {
  const btn = document.getElementById('run-tests-btn');
  const status = document.getElementById('run-tests-status');
  btn.disabled = true;
  status.textContent = 'Running suite against the live API...';
  const res = await api('/api/pentest/run', { method: 'POST' });
  btn.disabled = false;

  if (!res.ok) {
    status.textContent = 'Run failed.';
    showToast((res.data && res.data.message) || 'Test run failed', 'error');
    return;
  }
  status.textContent = `Completed at ${new Date().toLocaleTimeString()}`;
  document.getElementById('lab-run-results').innerHTML = renderResultsTable(res.data);
}

/* ---------------- results / incidents / alerts ---------------- */

async function loadResults() {
  const el = document.getElementById('results-table');
  const res = await api('/api/pentest/results');
  el.innerHTML = res.ok && res.data.length
    ? renderResultsTable(res.data)
    : '<p class="empty-state">No test runs recorded yet. Visit Security Lab to run the suite.</p>';
}

async function loadIncidentsAndAlerts() {
  const incidentsEl = document.getElementById('incidents-table');
  const alertsEl = document.getElementById('alerts-table');

  if (state.user.role !== 'ADMIN') {
    incidentsEl.innerHTML = '<p class="empty-state">Sign in as Administrator to view incidents and alerts.</p>';
    alertsEl.innerHTML = '';
    return;
  }

  const [incidentsRes, alertsRes] = await Promise.all([api('/api/admin/incidents'), api('/api/admin/alerts')]);
  incidentsEl.innerHTML = incidentsRes.ok && incidentsRes.data.length
    ? renderIncidentsTable(incidentsRes.data)
    : '<p class="empty-state">No incidents recorded yet.</p>';

  alertsEl.innerHTML = alertsRes.ok && alertsRes.data.length
    ? renderAlertsTable(alertsRes.data)
    : '<p class="empty-state">No alert cycles triggered yet.</p>';

  alertsEl.querySelectorAll('[data-ack]').forEach((btn) => {
    btn.onclick = async () => {
      const res = await api(`/api/admin/alerts/${btn.dataset.ack}/acknowledge`, { method: 'POST' });
      if (res.ok) { showToast('Alert acknowledged', 'success'); loadIncidentsAndAlerts(); }
      else { showToast('Could not acknowledge', 'error'); }
    };
  });
}

/* ---------------- renderers ---------------- */

function renderIncidentsTable(incidents) {
  if (!incidents.length) return '<p class="empty-state">No incidents.</p>';
  return `<table><thead><tr><th>Time</th><th>Requester</th><th>Owner</th><th>Resource</th><th>Mode</th><th>Outcome</th><th>Severity</th></tr></thead>
    <tbody>${incidents.map((i) => `
      <tr>
        <td>${fmtTime(i.detectedAt)}</td>
        <td>${escapeHtml(i.requestingUser)}</td>
        <td>${escapeHtml(i.resourceOwner)}</td>
        <td>${escapeHtml(i.resourceType)} #${escapeHtml(i.resourceId)}</td>
        <td>${i.labMode}</td>
        <td><span class="badge ${i.outcome === 'CONFIRMED_BREACH' ? 'badge-fail' : 'badge-pass'}">${i.outcome.replace('_', ' ')}</span></td>
        <td><span class="badge badge-sev-${i.severity.toLowerCase()}">${i.severity}</span></td>
      </tr>`).join('')}</tbody></table>`;
}

function renderResultsTable(results) {
  if (!results.length) return '<p class="empty-state">No results.</p>';
  return `<table><thead><tr><th>Time</th><th>Test</th><th>Mode</th><th>Endpoint</th><th>Expected</th><th>Actual</th><th>Result</th></tr></thead>
    <tbody>${results.map((r) => `
      <tr>
        <td>${fmtTime(r.executedAt)}</td>
        <td class="wrap">${escapeHtml(r.testName)}</td>
        <td>${r.labMode}</td>
        <td class="wrap">${escapeHtml(r.endpoint)}</td>
        <td class="wrap">${escapeHtml(r.expectedOutcome)}</td>
        <td class="wrap">${escapeHtml(r.actualOutcome)}</td>
        <td><span class="badge ${r.passed ? 'badge-pass' : 'badge-fail'}">${r.passed ? 'PASS' : 'FAIL'}</span></td>
      </tr>`).join('')}</tbody></table>`;
}

function renderAlertsTable(alerts) {
  if (!alerts.length) return '<p class="empty-state">No alerts.</p>';
  return `<table><thead><tr><th>Incident</th><th>Created</th><th>Attempts</th><th>Status</th><th>Last error</th><th></th></tr></thead>
    <tbody>${alerts.map((a) => `
      <tr>
        <td>#${a.incidentId}</td>
        <td>${fmtTime(a.createdAt)}</td>
        <td>${a.attemptsMade}</td>
        <td><span class="badge ${a.status === 'ACKNOWLEDGED' ? 'badge-pass' : (a.status === 'FAILED' ? 'badge-fail' : 'badge-status')}">${a.status}</span></td>
        <td class="wrap">${escapeHtml(a.lastError || '-')}</td>
        <td>${a.acknowledged ? '' : `<button class="btn btn-ghost" data-ack="${a.id}">Acknowledge</button>`}</td>
      </tr>`).join('')}</tbody></table>`;
}

/* ---------------- wiring ---------------- */

document.getElementById('login-form').addEventListener('submit', (e) => {
  e.preventDefault();
  document.getElementById('login-error').hidden = true;
  login(document.getElementById('login-username').value, document.getElementById('login-password').value);
});

document.querySelectorAll('.chip').forEach((chip) => {
  chip.addEventListener('click', () => {
    const user = chip.dataset.user;
    const passwords = { usera: 'UserA-Lab-Pass1!', userb: 'UserB-Lab-Pass1!', admin: 'Admin-Lab-Pass1!' };
    document.getElementById('login-username').value = user;
    document.getElementById('login-password').value = passwords[user];
  });
});

document.getElementById('tabs').addEventListener('click', (e) => {
  if (e.target.matches('.tab')) switchTab(e.target.dataset.tab);
});

document.getElementById('logout-btn').addEventListener('click', logout);
document.getElementById('settings-logout-btn').addEventListener('click', logout);

checkSession();
