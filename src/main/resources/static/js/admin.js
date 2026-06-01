const $ = (sel) => document.querySelector(sel);

let adminKey = null;
let allUsers = [];

const adminAuth = $('#admin-auth');
const adminDashboard = $('#admin-dashboard');
const adminAuthForm = $('#admin-auth-form');
const withdrawalsList = $('#withdrawals-list');
const usersList = $('#users-list');
const userSearch = $('#user-search');

adminAuthForm.addEventListener('submit', (e) => {
  e.preventDefault();
  adminKey = $('#admin-key-input').value.trim();
  if (!adminKey) return;
  adminAuth.classList.add('hidden');
  adminDashboard.classList.remove('hidden');
  loadWithdrawals();
  loadUsers();
});

$('#refresh-users-btn').addEventListener('click', () => loadUsers());

userSearch.addEventListener('input', () => {
  const q = userSearch.value.trim().toLowerCase();
  renderUsers(q);
});

async function loadWithdrawals() {
  try {
    const res = await fetch(`/api/admin/withdrawals?adminKey=${encodeURIComponent(adminKey)}`);
    const data = await res.json();
    if (data.error) {
      withdrawalsList.innerHTML = `<p class="error-msg">${data.error}</p>`;
      return;
    }

    const pending = data.withdrawals || [];
    if (pending.length === 0) {
      withdrawalsList.innerHTML = '<p class="info">No pending withdrawals.</p>';
      return;
    }

    withdrawalsList.innerHTML = pending.map(w => `
      <div class="withdrawal-row">
        <div class="withdrawal-info">
          <span class="user">${w.username || w.torn_id}</span>
          &mdash;
          <span class="amount">${w.moola_amount} Moola (${w.xanax_amount} Xanax)</span>
          &mdash; ${w.status}
          <br><small>Torn ID: ${w.torn_id} | ID: ${w.id}</small>
        </div>
        <button class="btn-accent" onclick="completeWithdrawal(${w.id})">Mark Completed</button>
      </div>
    `).join('');
  } catch (err) {
    withdrawalsList.innerHTML = '<p class="error-msg">Failed to load.</p>';
  }
}

async function completeWithdrawal(id) {
  try {
    const res = await fetch(`/api/admin/withdrawals/${id}/complete`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ adminKey })
    });
    const data = await res.json();
    if (data.success) {
      loadWithdrawals();
    } else {
      alert(data.error || 'Failed');
    }
  } catch (err) {
    alert('Network error');
  }
}

async function loadUsers() {
  try {
    const res = await fetch(`/api/admin/users?adminKey=${encodeURIComponent(adminKey)}`);
    const data = await res.json();
    if (data.error) {
      usersList.innerHTML = `<p class="error-msg">${data.error}</p>`;
      return;
    }
    allUsers = data.users || [];
    renderUsers(userSearch.value.trim().toLowerCase());
  } catch (err) {
    usersList.innerHTML = '<p class="error-msg">Failed to load users.</p>';
  }
}

function renderUsers(query) {
  const filtered = query
      ? allUsers.filter(u => u.username && u.username.toLowerCase().includes(query))
      : allUsers;

  if (filtered.length === 0) {
    usersList.innerHTML = '<p class="info">No users found.</p>';
    return;
  }

  usersList.innerHTML = filtered.map(u => `
    <div class="withdrawal-row">
      <div class="withdrawal-info">
        <span class="user">${u.username}</span>
        &mdash;
        <span class="amount">${u.siteBalance} Moola</span>
        <br><small>Torn ID: ${u.tornId} | Matches: ${u.totalMatchesPlayed} | Won: ${u.totalMatchesWon}</small>
      </div>
      <button class="btn-accent btn-small" onclick="prefillCredit('${u.username.replace(/'/g, "\\'")}')">Send Moola</button>
    </div>
  `).join('');
}

function prefillCredit(username) {
  $('#target-username').value = username;
  $('#credit-amount').focus();
  $('#credit-user-form').scrollIntoView({ behavior: 'smooth' });
}

const creditForm = $('#credit-user-form');
const creditMessage = $('#credit-message');

creditForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const username = $('#target-username').value.trim();
  const amount = parseInt($('#credit-amount').value, 10);

  if (!username || !amount || amount <= 0) {
    creditMessage.textContent = 'Enter a valid username and amount.';
    creditMessage.className = 'error-msg';
    creditMessage.classList.remove('hidden');
    return;
  }

  try {
    const res = await fetch('/api/admin/credit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, amount, adminKey })
    });
    const data = await res.json();
    if (data.success) {
      creditMessage.textContent = data.message;
      creditMessage.className = 'success-msg';
      creditMessage.classList.remove('hidden');
      creditForm.reset();
      loadUsers();
    } else {
      creditMessage.textContent = data.error || 'Failed to credit user.';
      creditMessage.className = 'error-msg';
      creditMessage.classList.remove('hidden');
    }
  } catch (err) {
    creditMessage.textContent = 'Network error.';
    creditMessage.className = 'error-msg';
    creditMessage.classList.remove('hidden');
  }
});
