const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

// ── STATE ──────────────────────────────────────────────
let currentUser = null;
let ws = null;
let currentRoomId = null;
let wsIdentified = false;
let authMode = 'login';
let currentWinsRequired = 2;
let pendingMatchEnd = null;
let currentP1Id = null;
let currentP2Id = null;
let currentMatchBet = 0;
let lastRoomConfig = null;
const matchPlayers = {
  p1: { id: null, name: '', avatar: '' },
  p2: { id: null, name: '', avatar: '' }
};

// ── ELEMENTS ───────────────────────────────────────────
const authSection = $('#auth-section');
const gameSection = $('#game-section');
const authForm = $('#auth-form');
const authError = $('#auth-error');
const tabLogin = $('#tab-login');
const tabSignup = $('#tab-signup');
const pinInput = $('#pin-input');
const displayUsername = $('#display-username');
const displayBalance = $('#display-balance');
const profileImage = $('#profile-image');
const profileButton = $('#profile-button');
const profileModal = $('#profile-modal');
const closeProfileModalBtn = $('#close-profile-modal');
const statsUsername = $('#stats-username');
const statBetted = $('#stat-betted');
const statWon = $('#stat-won');
const statLost = $('#stat-lost');
const statNet = $('#stat-net');
const scoreTarget = $('#score-target');
const statWinrate = $('#stat-winrate');
const logoutBtn = $('#logout-btn');
const verifyDepositBtn = $('#verify-deposit-btn');
const depositMessage = $('#deposit-message');
const withdrawBtn = $('#withdraw-btn');
const withdrawInput = $('#withdraw-input');
const withdrawMessage = $('#withdraw-message');
const createRoomBtn = $('#create-room-btn');
const joinRoomBtn = $('#join-room-btn');
const betInput = $('#bet-input');
const roomCodeInput = $('#room-code-input');
const publicRoomCheckbox = $('#public-room-checkbox');
const lobbyMessage = $('#lobby-message');
const gameSectionPanel = $('#game-section-panel');
const roundResult = $('#round-result');
const matchResult = $('#match-result');
const choiceBtns = $$('.choice-btn');
const publicRoomsList = $('#public-rooms-list');
const refreshPublicRoomsBtn = $('#refresh-public-rooms-btn');
const roundsSelect = $('#rounds-select');
const leaderboardList = $('#leaderboard-list');
const refreshLeaderboardBtn = $('#refresh-leaderboard-btn');
const playBotBtn = $('#play-bot-btn');
const roomChatSection = $('#room-chat-section');
const roomChatLog = $('#room-chat-log');
const roomChatForm = $('#room-chat-form');
const roomChatInput = $('#room-chat-input');
const roomChatCode = $('#room-chat-code');
const globalChatLog = $('#global-chat-log');
const globalChatForm = $('#global-chat-form');
const globalChatInput = $('#global-chat-input');
const matchRoomChat = $('#match-room-chat');
const matchRoomChatLog = $('#match-room-chat-log');
const matchRoomChatForm = $('#match-room-chat-form');
const matchRoomChatInput = $('#match-room-chat-input');
const choiceReveal = $('#choice-reveal');
const matchOverlay = $('#match-overlay');
const matchArena = $('#match-arena');
const matchConfetti = $('#match-confetti');
const matchFxLayer = $('#match-fx-layer');

// ── UTILITIES ──────────────────────────────────────────
function showMsg(el, msg, isError) {
  el.textContent = msg;
  el.className = isError ? 'error-msg' : 'success-msg';
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 6000);
}

function updateBalance(bal) {
  if (!currentUser) return;
  currentUser.site_balance = bal;
  if (typeof MoolaIcon !== 'undefined') {
    MoolaIcon.setBalanceElement(displayBalance, bal);
  } else {
    displayBalance.textContent = bal.toLocaleString();
  }
}

function asNumber(val) {
  const n = Number(val);
  return Number.isFinite(n) ? n : 0;
}

function setLoggedIn(user) {
  currentUser = {
    ...user,
    total_moola_betted: asNumber(user.total_moola_betted),
    total_moola_won: asNumber(user.total_moola_won),
    total_moola_lost: asNumber(user.total_moola_lost),
    total_matches_played: asNumber(user.total_matches_played),
    total_matches_won: asNumber(user.total_matches_won),
    win_rate: asNumber(user.win_rate),
    net_profit_loss: asNumber(user.net_profit_loss)
  };
  authSection.classList.add('hidden');
  gameSection.classList.remove('hidden');
  displayUsername.textContent = currentUser.username;
  updateBalance(currentUser.site_balance);
  if (profileImage) {
    profileImage.src = currentUser.profile_image_url || `https://images.torn.com/avatars/${currentUser.torn_id}.png`;
  }
  updateStatsModal();
  loadDepositInstructions();
  loadLeaderboard();
  syncNotebookDoodles();
  mountGameUiDecorations();
}

function mountGameUiDecorations() {
  if (typeof MoolaIcon !== 'undefined') {
    MoolaIcon.mountIcons();
  }
  if (typeof RpsSketch !== 'undefined') {
    RpsSketch.mountChoiceButtons();
    RpsSketch.mountSnowflakes();
  }
}

function syncNotebookDoodles() {
  if (typeof window.syncNotebookDoodleVisibility === 'function') {
    window.syncNotebookDoodleVisibility();
  }
  if (typeof window.repaintNotebookBackground === 'function') {
    window.repaintNotebookBackground();
  }
}

async function loadDepositInstructions() {
  const el = $('#deposit-instructions');
  if (!el) return;
  try {
    const res = await fetch('/api/deposit/instructions');
    if (!res.ok) throw new Error('failed');
    const data = await res.json();
    const name = data.recipientName || 'Hannath';
    const id = data.recipientId || '3961385';
    const msg = data.requiredMessage || 'RPS';
    const hours = data.maxAgeHours ?? 72;
    el.innerHTML =
      `Send Xanax to <strong>${escapeHtml(name)}</strong> [ID ${escapeHtml(id)}] ` +
      `with the exact message: <strong>${escapeHtml(msg)}</strong>. ` +
      `Deposits must appear within <strong>${hours}</strong> hours.`;
  } catch (_) {
    el.innerHTML =
      'Send Xanax to <strong>Hannath</strong> [ID 3961385] with the exact message: <strong>RPS</strong>. ' +
      'Deposits must appear within <strong>72</strong> hours.';
  }
}

function updateStatsModal() {
  if (!currentUser) return;
  if (statsUsername) statsUsername.textContent = `${currentUser.username} [${currentUser.torn_id}]`;
  const fmtStat = (el, val) => {
    if (!el) return;
    if (typeof MoolaIcon !== 'undefined') {
      el.innerHTML = MoolaIcon.amountHtml(val);
    } else {
      el.textContent = asNumber(val).toLocaleString();
    }
  };
  fmtStat(statBetted, currentUser.total_moola_betted);
  fmtStat(statWon, currentUser.total_moola_won);
  fmtStat(statLost, currentUser.total_moola_lost);
  if (statNet) {
    const net = asNumber(currentUser.total_moola_won) - asNumber(currentUser.total_moola_lost);
    statNet.textContent = net >= 0 ? `+${net.toLocaleString()}` : net.toLocaleString();
  }
  if (statWinrate) statWinrate.textContent = `${asNumber(currentUser.win_rate).toFixed(2)}%`;
}

async function loadLeaderboard() {
  if (!leaderboardList) return;
  try {
    const res = await fetch('/api/leaderboard', { credentials: 'same-origin' });
    if (!res.ok) throw new Error('failed');
    const users = await res.json();
    renderLeaderboard(users);
  } catch (_) {
    leaderboardList.innerHTML = '<li class="info">Failed to load leaderboard.</li>';
  }
}

function renderLeaderboard(users) {
  if (!users || !users.length) {
    leaderboardList.innerHTML = '<li class="info">No games won yet. Be the first!</li>';
    return;
  }
  leaderboardList.innerHTML = users.map((user, idx) => {
    let rankSymbol = `${idx + 1}.`;
    if (idx === 0) rankSymbol = '🥇';
    else if (idx === 1) rankSymbol = '🥈';
    else if (idx === 2) rankSymbol = '🥉';

    const net = user.net_profit_loss ?? 0;
    const netFormatted = net >= 0 ? `+${net.toLocaleString()}` : net.toLocaleString();

    return `
      <li class="public-room-item leaderboard-item">
        <div style="display: flex; align-items: center; gap: 0.75rem;">
          <span style="font-size: 1.25rem; font-weight: bold; width: 1.8rem; display: inline-block; text-align: center;">${rankSymbol}</span>
          <div>
            <strong>${escapeHtml(user.username)}</strong>
            <div class="room-meta">Wins: ${user.total_matches_won} / ${user.total_matches_played} played</div>
          </div>
        </div>
        <div style="text-align: right;">
          <span style="font-weight: bold; color: var(--accent); display: inline-flex; align-items: center; gap: 0.2rem;">${typeof MoolaIcon !== 'undefined' ? `${MoolaIcon.iconHtml()}<span>${netFormatted}</span>` : netFormatted}</span>
        </div>
      </li>
    `;
  }).join('');
}

function setLoggedOut() {
  if (ws) ws.close();
  currentUser = null;
  ws = null;
  wsIdentified = false;
  currentRoomId = null;
  gameSection.classList.add('hidden');
  authSection.classList.remove('hidden');
  gameSectionPanel.classList.add('hidden');
  roomChatSection.classList.add('hidden');
  matchRoomChat.classList.add('hidden');
  clearChatLog(globalChatLog);
  clearChatLog(roomChatLog);
  clearChatLog(matchRoomChatLog);
  if (profileModal) profileModal.classList.add('hidden');
  syncNotebookDoodles();
}

function formatChatTime(ts) {
  const d = new Date(ts);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function appendChatLine(logEl, username, message, timestamp, isSelf) {
  const line = document.createElement('div');
  line.className = 'chat-line' + (isSelf ? ' chat-self' : '');
  const userSpan = document.createElement('span');
  userSpan.className = 'chat-user';
  userSpan.textContent = username + ':';
  const timeSpan = document.createElement('span');
  timeSpan.className = 'chat-time';
  timeSpan.textContent = formatChatTime(timestamp);
  line.appendChild(userSpan);
  line.appendChild(document.createTextNode(' ' + message + ' '));
  line.appendChild(timeSpan);
  logEl.appendChild(line);
  logEl.scrollTop = logEl.scrollHeight;
}

function appendChatToRoomLogs(username, message, timestamp) {
  const isSelf = currentUser && username === currentUser.username;
  if (!roomChatSection.classList.contains('hidden')) {
    appendChatLine(roomChatLog, username, message, timestamp, isSelf);
  }
  if (!matchRoomChat.classList.contains('hidden')) {
    appendChatLine(matchRoomChatLog, username, message, timestamp, isSelf);
  }
}

function clearChatLog(logEl) {
  if (logEl) logEl.innerHTML = '';
}

function showRoomChat(roomId) {
  if (!roomId) {
    roomChatSection.classList.add('hidden');
    matchRoomChat.classList.add('hidden');
    return;
  }
  roomChatCode.textContent = roomId;
  roomChatSection.classList.remove('hidden');
}

// ── WEBSOCKET ──────────────────────────────────────────
function connectWS() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  ws = new WebSocket(`${protocol}//${location.host}/ws/game`);

  ws.onopen = () => {
    console.log('WebSocket connected');
    identifyWs();
    requestPublicRooms();
  };

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    handleWsMessage(data);
  };

  ws.onclose = () => {
    console.log('WebSocket disconnected');
    wsIdentified = false;
    setTimeout(connectWS, 3000);
  };

  ws.onerror = (err) => console.error('WebSocket error:', err);
}

function sendWs(data) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(data));
  }
}

function identifyWs() {
  if (!currentUser || !ws || ws.readyState !== WebSocket.OPEN) return;
  sendWs({
    action: 'identify',
    tornId: currentUser.torn_id,
    username: currentUser.username
  });
}

function requestPublicRooms() {
  sendWs({ action: 'listPublicRooms' });
}

function handleWsMessage(data) {
  switch (data.action) {
    case 'identified':
      wsIdentified = true;
      requestPublicRooms();
      break;

    case 'publicRooms':
      renderPublicRooms(data.rooms || []);
      break;

    case 'chatMessage':
      if (data.scope === 'global') {
        appendChatLine(
          globalChatLog,
          data.username,
          data.message,
          data.timestamp,
          currentUser && data.tornId === currentUser.torn_id
        );
      } else if (data.scope === 'room' && data.roomId === currentRoomId) {
        appendChatToRoomLogs(data.username, data.message, data.timestamp);
      }
      break;

    case 'chatError':
      showMsg(lobbyMessage, data.message, true);
      break;

    case 'roomCreated':
      currentRoomId = data.roomId;
      clearChatLog(roomChatLog);
      clearChatLog(matchRoomChatLog);
      showRoomChat(data.roomId);
      const pubLabel = data.isPublic ? ' (public)' : ' (private)';
      showMsg(lobbyMessage, `Room created${pubLabel}! Code: ${data.roomId}. Waiting for opponent...`, false);
      if ($('#room-id-display')) $('#room-id-display').textContent = data.roomId;
      break;

    case 'matchStarted':
      currentRoomId = data.roomId;
      currentWinsRequired = data.winsRequired || 2;
      currentMatchBet = asNumber(data.betAmount);
      const matchRounds = (data.winsRequired || 2) * 2 - 1;
      lastRoomConfig = {
        betAmount: currentMatchBet,
        rounds: matchRounds,
        playWithBot: data.player2Id === 'BOT_BAINING',
        isPublic: lastRoomConfig?.isPublic ?? false
      };
      currentP1Id = data.player1Id || null;
      currentP2Id = data.player2Id || null;
      pendingMatchEnd = null;
      matchPlayers.p1 = { id: data.player1Id, name: data.player1, avatar: '' };
      matchPlayers.p2 = { id: data.player2Id, name: data.player2, avatar: '' };
      gameSectionPanel.classList.remove('hidden');
      mountGameUiDecorations();
      matchRoomChat.classList.remove('hidden');
      matchOverlay.classList.add('hidden');
      matchOverlay.innerHTML = '';
      if (matchConfetti) matchConfetti.innerHTML = '';
      hideChoiceReveal();
      syncNotebookDoodles();
      const p1Btn = $('#p1-score-name');
      const p2Btn = $('#p2-score-name');
      setPlayerLabel(p1Btn, data.player1, data.player1Id);
      p1Btn.dataset.tornId = data.player1Id || '';
      setPlayerLabel(p2Btn, data.player2, data.player2Id);
      p2Btn.dataset.tornId = data.player2Id || '';
      const p1AvBtn = $('#p1-avatar-btn');
      const p2AvBtn = $('#p2-avatar-btn');
      p1AvBtn.dataset.tornId = data.player1Id || '';
      p2AvBtn.dataset.tornId = data.player2Id || '';
      $('#p1-avatar').src = '';
      $('#p2-avatar').src = '';
      $('#p1-record').textContent = '';
      $('#p2-record').textContent = '';
      syncDuelBanner(data.player1, data.player2, data.player1Id, data.player2Id);
      const potEl = $('#pot-display');
      if (potEl && typeof MoolaIcon !== 'undefined') {
        potEl.innerHTML = MoolaIcon.amountHtml(data.pot, 'gold');
      } else if (potEl) {
        potEl.textContent = data.pot;
      }
      const totalRoundsInit = currentWinsRequired * 2 - 1;
      $('#round-display').textContent = `${data.round} (Best of ${totalRoundsInit})`;
      if ($('#room-id-display')) $('#room-id-display').textContent = data.roomId;
      $('#p1-score').textContent = '0';
      $('#p2-score').textContent = '0';
      if (scoreTarget) {
        scoreTarget.textContent = `First to ${currentWinsRequired} wins`;
      }
      roundResult.classList.add('hidden');
      matchResult.classList.add('hidden');
      enableChoices(true);
      showMsg(lobbyMessage, 'Match started! Make your choice.', false);
      loadMatchPlayerProfile(data.player1Id, 'p1');
      loadMatchPlayerProfile(data.player2Id, 'p2');
      if (typeof MatchFx !== 'undefined') {
        MatchFx.spawnConfetti(matchConfetti, 35, false);
        MatchFx.floatText(matchFxLayer, 'FIGHT!');
      }
      break;

    case 'choiceReceived':
      break;

    case 'roundResult': {
      const winsReq = data.winsRequired || currentWinsRequired;
      if (data.betAmount) currentMatchBet = asNumber(data.betAmount);
      if (data.player1Wins >= winsReq || data.player2Wins >= winsReq) {
        data.matchOver = true;
      }
      const totalRoundsResult = currentWinsRequired * 2 - 1;
      if (data.nextRound) {
        $('#round-display').textContent = `${data.nextRound} (Best of ${totalRoundsResult})`;
      } else {
        const currVal = parseInt($('#round-display').textContent, 10) || 1;
        $('#round-display').textContent = `${currVal + 1} (Best of ${totalRoundsResult})`;
      }
      const prevP1 = parseInt($('#p1-score').textContent, 10) || 0;
      const prevP2 = parseInt($('#p2-score').textContent, 10) || 0;

      enableChoices(false);
      choiceBtns.forEach(b => b.classList.remove('selected'));

      animateChoiceReveal(
        data.player1, data.player2,
        data.player1Choice, data.player2Choice,
        data.roundWinner
      ).then(() => {
        $('#p1-score').textContent = data.player1Wins;
        $('#p2-score').textContent = data.player2Wins;
        if (data.player1Wins > prevP1 && typeof MatchFx !== 'undefined') {
          MatchFx.popScore($('#p1-score'));
        }
        if (data.player2Wins > prevP2 && typeof MatchFx !== 'undefined') {
          MatchFx.popScore($('#p2-score'));
        }

        let resultText = `${data.player1}: ${data.player1Choice.toUpperCase()} vs ${data.player2}: ${data.player2Choice.toUpperCase()} - `;
        const iWonRound = data.roundWinner === currentUser.username;
        const iLostRound = data.roundWinner && data.roundWinner !== 'tie' && !iWonRound;
        if (data.roundWinner === 'tie') {
          resultText += 'TIE!';
          roundResult.className = 'tie';
          if (typeof MatchFx !== 'undefined') {
            MatchFx.floatText(matchFxLayer, 'TIE!', 'fx-pop');
          }
        } else {
          resultText += `${data.roundWinner} wins!`;
          roundResult.className = iWonRound ? 'win' : 'lose';
          if (typeof MatchFx !== 'undefined') {
            if (iWonRound) {
              MatchFx.playRoundWin();
              MatchFx.spawnConfetti(matchConfetti, 45, true);
              MatchFx.floatText(matchFxLayer, '+1 ROUND!');
              MatchFx.flashArena(matchArena, true);
            } else if (iLostRound) {
              MatchFx.floatText(matchFxLayer, 'OUCH', 'fx-pop lose-pop');
              MatchFx.flashArena(matchArena, false);
            }
          }
        }
        roundResult.textContent = resultText;
        roundResult.classList.remove('hidden');

        if (data.matchOver) {
          enableChoices(false);
          if (scoreTarget) {
            scoreTarget.textContent = 'Match complete!';
          }
          setTimeout(() => showPendingMatchEnd(), 1600);
        } else {
          setTimeout(() => {
            hideChoiceReveal();
            roundResult.classList.add('hidden');
            enableChoices(true);
          }, 2000);
        }
      });
      break;
    }

    case 'matchEnd':
      pendingMatchEnd = data;
      if (!choiceReveal.classList.contains('hidden')) {
        break;
      }
      showPendingMatchEnd();
      break;

    case 'opponentDisconnected':
      enableChoices(false);
      refreshBalance();
      setTimeout(() => {
        showMatchOverlay(true, data.winner, data.winnings, data.rake, 0, 0, true, currentMatchBet);
      }, 1500);
      break;

    case 'roomCancelled':
      showMsg(lobbyMessage, data.message || 'Room closed.', true);
      currentRoomId = null;
      gameSectionPanel.classList.add('hidden');
      roomChatSection.classList.add('hidden');
      matchRoomChat.classList.add('hidden');
      matchOverlay.classList.add('hidden');
      matchOverlay.innerHTML = '';
      hideChoiceReveal();
      refreshBalance();
      syncNotebookDoodles();
      break;

    case 'error':
      showMsg(lobbyMessage, data.message, true);
      enableChoices(true);
      break;
  }
}

function renderPublicRooms(rooms) {
  if (!rooms.length) {
    publicRoomsList.innerHTML = '<li class="info">No public rooms open. Create one or check back soon.</li>';
    return;
  }

  publicRoomsList.innerHTML = rooms.map(room => {
    const isOwn = currentUser && room.hostId === currentUser.torn_id;
    const joinBtn = isOwn
      ? '<span class="room-meta">Your room</span>'
      : `<button type="button" class="btn-accent btn-small" data-join-public="${room.roomId}">Join</button>`;
    return `
      <li class="public-room-item">
        <div>
          <strong>${escapeHtml(room.host)}</strong>
          <div class="room-meta">${typeof MoolaIcon !== 'undefined' ? MoolaIcon.amountHtml(room.betAmount) : room.betAmount} · Best of ${room.rounds || 3} · Code: <span class="code">${room.roomId}</span></div>
        </div>
        ${joinBtn}
      </li>
    `;
  }).join('');

  publicRoomsList.querySelectorAll('[data-join-public]').forEach(btn => {
    btn.addEventListener('click', () => joinPublicRoom(btn.dataset.joinPublic));
  });
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

function joinPublicRoom(roomId) {
  sendWs({
    action: 'joinRoom',
    tornId: currentUser.torn_id,
    username: currentUser.username,
    roomId
  });
}

function enableChoices(enabled) {
  choiceBtns.forEach(btn => btn.disabled = !enabled);
}

// ── AUTH ───────────────────────────────────────────────
authForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const key = $('#api-key-input').value.trim();
  const pin = pinInput.value.trim();
  if (!key || !/^\d{4}$/.test(pin)) {
    authError.textContent = 'Enter API key and a valid 4-digit PIN.';
    authError.classList.remove('hidden');
    return;
  }

  authError.classList.add('hidden');
  $('#auth-btn').disabled = true;
  $('#auth-btn').textContent = authMode === 'signup' ? 'Signing up...' : 'Logging in...';

  try {
    const endpoint = authMode === 'signup' ? '/api/auth/signup' : '/api/auth/login';
    const res = await fetch(endpoint, {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ api_key: key, pin })
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      authError.textContent = data.error || 'Auth failed.';
      authError.classList.remove('hidden');
      return;
    }
    setLoggedIn(data.user);
    localStorage.setItem('tornId', data.user.torn_id);
    localStorage.setItem('rpsApiKey', key);
    localStorage.setItem('rpsPin', pin);
    connectWS();
  } catch (err) {
    authError.textContent = 'Network error.';
    authError.classList.remove('hidden');
  } finally {
    $('#auth-btn').disabled = false;
    $('#auth-btn').textContent = authMode === 'signup' ? 'Sign Up' : 'Log In';
  }
});

function setAuthMode(mode) {
  authMode = mode;
  const isSignup = mode === 'signup';
  tabSignup.classList.toggle('active', isSignup);
  tabLogin.classList.toggle('active', !isSignup);
  $('#auth-btn').textContent = isSignup ? 'Sign Up' : 'Log In';
}

tabLogin?.addEventListener('click', () => setAuthMode('login'));
tabSignup?.addEventListener('click', () => setAuthMode('signup'));
setAuthMode('login');

logoutBtn.addEventListener('click', async () => {
  await fetch('/api/auth', { method: 'DELETE', credentials: 'same-origin' }).catch(() => {});
  localStorage.removeItem('tornId');
  localStorage.removeItem('rpsApiKey');
  localStorage.removeItem('rpsPin');
  setLoggedOut();
});

// ── DEPOSIT ────────────────────────────────────────────
verifyDepositBtn.addEventListener('click', async () => {
  verifyDepositBtn.disabled = true;
  verifyDepositBtn.textContent = 'Verifying...';
  try {
    const res = await fetch('/api/deposit', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tornId: currentUser.torn_id })
    });
    const data = await res.json();
    if (data.success) {
      showMsg(depositMessage, data.message, false);
      if (data.site_balance !== undefined) updateBalance(data.site_balance);
    } else {
      showMsg(depositMessage, data.error || 'Deposit failed.', true);
    }
  } catch (err) {
    showMsg(depositMessage, 'Network error.', true);
  } finally {
    verifyDepositBtn.disabled = false;
    verifyDepositBtn.textContent = 'Verify Deposit';
  }
});

// ── WITHDRAW ───────────────────────────────────────────
withdrawBtn.addEventListener('click', async () => {
  const amount = parseInt(withdrawInput.value, 10);
  if (!amount || amount <= 0 || amount % 4 !== 0) {
    showMsg(withdrawMessage, 'Amount must be a multiple of 4.', true);
    return;
  }

  withdrawBtn.disabled = true;
  try {
    const res = await fetch('/api/withdraw', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tornId: currentUser.torn_id, moolaAmount: amount })
    });
    const data = await res.json();
    if (data.success) {
      showMsg(withdrawMessage, data.message, false);
      if (data.site_balance !== undefined) updateBalance(data.site_balance);
      withdrawInput.value = '';
    } else {
      showMsg(withdrawMessage, data.error || 'Withdrawal failed.', true);
    }
  } catch (err) {
    showMsg(withdrawMessage, 'Network error.', true);
  } finally {
    withdrawBtn.disabled = false;
  }
});

// ── ROOMS ──────────────────────────────────────────────
createRoomBtn.addEventListener('click', () => {
  const bet = parseInt(betInput.value, 10);
  if (!bet || bet < 4 || bet % 4 !== 0) {
    showMsg(lobbyMessage, 'Bet must be a multiple of 4.', true);
    return;
  }
  const rounds = parseInt(roundsSelect.value, 10) || 3;
  lastRoomConfig = {
    betAmount: bet,
    rounds,
    playWithBot: false,
    isPublic: publicRoomCheckbox.checked
  };
  sendWs({
    action: 'createRoom',
    tornId: currentUser.torn_id,
    username: currentUser.username,
    betAmount: bet,
    rounds: rounds,
    isPublic: publicRoomCheckbox.checked
  });
});

playBotBtn?.addEventListener('click', () => {
  const bet = parseInt(betInput.value, 10);
  if (!bet || bet < 4 || bet % 4 !== 0) {
    showMsg(lobbyMessage, 'Bet must be a multiple of 4.', true);
    return;
  }
  const rounds = parseInt(roundsSelect.value, 10) || 3;
  lastRoomConfig = {
    betAmount: bet,
    rounds,
    playWithBot: true,
    isPublic: false
  };
  sendWs({
    action: 'createRoom',
    tornId: currentUser.torn_id,
    username: currentUser.username,
    betAmount: bet,
    rounds: rounds,
    playWithBot: true,
    isPublic: false
  });
});

joinRoomBtn.addEventListener('click', () => {
  const code = roomCodeInput.value.trim();
  if (!code) {
    showMsg(lobbyMessage, 'Enter a room code.', true);
    return;
  }
  sendWs({
    action: 'joinRoom',
    tornId: currentUser.torn_id,
    username: currentUser.username,
    roomId: code
  });
});

refreshPublicRoomsBtn.addEventListener('click', () => requestPublicRooms());
refreshLeaderboardBtn.addEventListener('click', () => loadLeaderboard());

profileButton?.addEventListener('click', () => {
  updateStatsModal();
  profileModal?.classList.remove('hidden');
});

closeProfileModalBtn?.addEventListener('click', () => profileModal?.classList.add('hidden'));
profileModal?.addEventListener('click', (e) => {
  if (e.target === profileModal) profileModal.classList.add('hidden');
});

// ── CHAT ───────────────────────────────────────────────
function sendGlobalChat(message) {
  sendWs({
    action: 'globalChat',
    tornId: currentUser.torn_id,
    username: currentUser.username,
    message
  });
}

function sendRoomChat(message) {
  if (!currentRoomId) return;
  sendWs({
    action: 'roomChat',
    tornId: currentUser.torn_id,
    username: currentUser.username,
    roomId: currentRoomId,
    message
  });
}

globalChatForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const msg = globalChatInput.value.trim();
  if (!msg) return;
  sendGlobalChat(msg);
  globalChatInput.value = '';
});

roomChatForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const msg = roomChatInput.value.trim();
  if (!msg) return;
  sendRoomChat(msg);
  roomChatInput.value = '';
});

matchRoomChatForm.addEventListener('submit', (e) => {
  e.preventDefault();
  const msg = matchRoomChatInput.value.trim();
  if (!msg) return;
  sendRoomChat(msg);
  matchRoomChatInput.value = '';
});

// ── PLAYER PROFILE BUTTONS ─────────────────────────────
document.addEventListener('click', (e) => {
  const btn = e.target.closest('.player-profile-btn, .player-avatar-btn');
  if (!btn) return;
  const tornId = btn.dataset.tornId;
  if (!tornId || tornId === 'BOT_BAINING') {
    showBotProfile();
    return;
  }
  showPlayerProfile(tornId);
});

function formatPlayerName(name, tornId) {
  if (tornId === 'BOT_BAINING' || name === 'BaiNing') {
    return 'BaiNing';
  }
  return name;
}

function setPlayerLabel(el, name, tornId) {
  if (!el) return;
  el.textContent = formatPlayerName(name, tornId);
}

function syncDuelBanner(p1Name, p2Name, p1Id, p2Id) {
  const duelP1Name = $('#duel-p1-name');
  const duelP2Name = $('#duel-p2-name');
  const duelP1Btn = $('#duel-p1-btn');
  const duelP2Btn = $('#duel-p2-btn');
  if (duelP1Name) {
    setPlayerLabel(duelP1Name, p1Name, p1Id);
    duelP1Name.dataset.tornId = p1Id || '';
  }
  if (duelP2Name) {
    setPlayerLabel(duelP2Name, p2Name, p2Id);
    duelP2Name.dataset.tornId = p2Id || '';
  }
  if (duelP1Btn) duelP1Btn.dataset.tornId = p1Id || '';
  if (duelP2Btn) duelP2Btn.dataset.tornId = p2Id || '';
  $('#duel-p1-avatar').src = matchPlayers.p1.avatar || '';
  $('#duel-p2-avatar').src = matchPlayers.p2.avatar || '';
  $('#duel-p1-record').textContent = '';
  $('#duel-p2-record').textContent = '';
  $('#reveal-p1-avatar').src = matchPlayers.p1.avatar || '';
  $('#reveal-p2-avatar').src = matchPlayers.p2.avatar || '';
}

function setMatchAvatar(slot, url) {
  const key = slot === 'p1' ? 'p1' : 'p2';
  matchPlayers[key].avatar = url;
  const avatarEl = $(`#${slot}-avatar`);
  const duelAv = $(`#duel-${slot}-avatar`);
  const revealAv = $(`#reveal-${slot}-avatar`);
  if (avatarEl) avatarEl.src = url;
  if (duelAv) duelAv.src = url;
  if (revealAv) revealAv.src = url;
}

async function loadMatchPlayerProfile(tornId, slot) {
  if (!tornId) return;
  const recordEl = $(`#${slot}-record`);
  const duelRecord = $(`#duel-${slot}-record`);
  if (tornId === 'BOT_BAINING') {
    setMatchAvatar(slot, '/baining.jpg');
    const rec = '5200W / 10000';
    if (recordEl) recordEl.textContent = rec;
    if (duelRecord) duelRecord.textContent = rec;
    return;
  }
  try {
    const res = await fetch(`/api/user/${tornId}`);
    if (!res.ok) return;
    const data = await res.json();
    const url = data.profile_image_url || `https://images.torn.com/avatars/${tornId}.png`;
    setMatchAvatar(slot, url);
    const rec = `${data.total_matches_won}W / ${data.total_matches_played}`;
    if (recordEl) recordEl.textContent = rec;
    if (duelRecord) duelRecord.textContent = rec;
  } catch (_) {}
}

async function showPlayerProfile(tornId) {
  try {
    const res = await fetch(`/api/user/${tornId}`);
    if (!res.ok) return;
    const data = await res.json();
    openProfileModal(data.username, data);
  } catch (_) {}
}

function showBotProfile() {
  openProfileModal('BaiNing', {
    username: 'BaiNing',
    total_moola_betted: 1000000,
    total_moola_won: 1250000,
    total_moola_lost: 800000,
    total_matches_played: 10000,
    total_matches_won: 5200,
    win_rate: 52.0,
    net_profit_loss: 250000,
    profile_image_url: '/baining.jpg'
  });
}

function openProfileModal(username, data) {
  const winRate = data.total_matches_played === 0 ? 0
    : (data.total_matches_won * 100.0) / data.total_matches_played;
  const won = asNumber(data.total_moola_won);
  const lost = asNumber(data.total_moola_lost);
  const net = won - lost;

  if (statsUsername) statsUsername.textContent = `${username}`;
  const fmtStatVal = (el, val) => {
    if (!el) return;
    if (typeof MoolaIcon !== 'undefined') {
      el.innerHTML = MoolaIcon.amountHtml(val);
    } else {
      el.textContent = asNumber(val).toLocaleString();
    }
  };
  fmtStatVal(statBetted, data.total_moola_betted);
  fmtStatVal(statWon, won);
  fmtStatVal(statLost, lost);
  if (statNet) {
    statNet.textContent = net >= 0 ? `+${net.toLocaleString()}` : net.toLocaleString();
  }
  if (statWinrate) statWinrate.textContent = `${winRate.toFixed(2)}%`;
  profileModal?.classList.remove('hidden');
}

// ── CHOICES ────────────────────────────────────────────
const RPS_LABELS = { rock: 'Rock', paper: 'Paper', scissors: 'Scissors' };

function animateChoiceReveal(p1Name, p2Name, p1Choice, p2Choice, roundWinner) {
  return new Promise((resolve) => {
    const p1Slot = $('#reveal-p1-slot');
    const p2Slot = $('#reveal-p2-slot');
    const p1Player = p1Slot?.closest('.reveal-player');
    const p2Player = p2Slot?.closest('.reveal-player');
    const vsEl = choiceReveal?.querySelector('.reveal-vs');

    $('#reveal-p1-name').textContent = p1Name;
    $('#reveal-p2-name').textContent = p2Name;
    if (matchPlayers.p1.avatar) $('#reveal-p1-avatar').src = matchPlayers.p1.avatar;
    if (matchPlayers.p2.avatar) $('#reveal-p2-avatar').src = matchPlayers.p2.avatar;

    $('#reveal-p1-label').textContent = '';
    $('#reveal-p2-label').textContent = '';

    p1Slot.classList.remove('clash-hit');
    p2Slot.classList.remove('clash-hit');
    p1Player?.classList.remove('winner-glow');
    p2Player?.classList.remove('winner-glow');
    choiceReveal.classList.remove('hidden', 'reveal-active');
    void choiceReveal.offsetWidth;
    choiceReveal.classList.add('reveal-active');

    if (typeof MatchFx !== 'undefined') {
      MatchFx.playReveal();
    }

    const onFlipbooksDone = () => {
      p1Slot.classList.add('clash-hit');
      p2Slot.classList.add('clash-hit');
      if (vsEl) {
        vsEl.classList.remove('bursting');
        void vsEl.offsetWidth;
        vsEl.classList.add('bursting');
      }
      if (typeof MatchFx !== 'undefined') {
        MatchFx.screenShake(matchArena, 1.3);
      }
      if (roundWinner && roundWinner !== 'tie') {
        const p1Won = roundWinner === p1Name;
        if (p1Won) p1Player?.classList.add('winner-glow');
        else p2Player?.classList.add('winner-glow');
      }
      setTimeout(resolve, 400);
    };

    if (typeof RpsSketch !== 'undefined') {
      Promise.all([
        RpsSketch.runRevealFlipbook(p1Slot, p1Choice),
        RpsSketch.runRevealFlipbook(p2Slot, p2Choice)
      ]).then(onFlipbooksDone);
    } else {
      onFlipbooksDone();
    }
  });
}

function hideChoiceReveal() {
  choiceReveal.classList.add('hidden');
  choiceReveal.classList.remove('reveal-active');
  const p1Slot = $('#reveal-p1-slot');
  const p2Slot = $('#reveal-p2-slot');
  if (p1Slot) p1Slot.classList.remove('clash-hit');
  if (p2Slot) p2Slot.classList.remove('clash-hit');
  $$('.reveal-player').forEach((el) => el.classList.remove('winner-glow'));
}

function showMatchOverlay(isWin, winnerName, winnings, rake, p1Wins, p2Wins, isForfeit, betAmount) {
  const stake = asNumber(betAmount || currentMatchBet);
  const netProfit = Math.max(0, asNumber(winnings) - stake);
  const title = isWin ? 'Victory!' : 'Defeat';
  const subtitle = isForfeit
    ? `${escapeHtml(winnerName)} wins by forfeit.`
    : isWin
      ? 'Your sketch beat theirs — notebook fame unlocked.'
      : `${escapeHtml(winnerName)} won this duel. Shake it off.`;

  const trophySvg = isWin
    ? `<svg class="result-sketch-icon" viewBox="0 0 100 100" aria-hidden="true">
        <path d="M30 38 Q28 22 50 18 Q72 22 70 38 L68 48 Q66 58 50 62 Q34 58 32 48Z" fill="none" stroke="#227c41" stroke-width="2.5"/>
        <path d="M38 62 L36 78 L64 78 L62 62" fill="none" stroke="#163e79" stroke-width="2"/>
        <path d="M42 78 L40 88 L60 88 L58 78" fill="none" stroke="#163e79" stroke-width="2"/>
        <path d="M22 32 Q18 28 24 24 M78 32 Q82 28 76 24" fill="none" stroke="#3d69ad" stroke-width="1.8" stroke-linecap="round"/>
      </svg>`
    : `<svg class="result-sketch-icon" viewBox="0 0 100 100" aria-hidden="true">
        <circle cx="50" cy="48" r="28" fill="none" stroke="#b22d2d" stroke-width="2.5"/>
        <path d="M32 30 L68 66 M68 30 L32 66" fill="none" stroke="#b22d2d" stroke-width="3" stroke-linecap="round"/>
        <path d="M18 78 Q50 88 82 78" fill="none" stroke="#163e79" stroke-width="1.5" stroke-linecap="round"/>
      </svg>`;

  const moolaIcon = typeof MoolaIcon !== 'undefined' ? MoolaIcon.iconHtml('moola-icon--result') : '';
  const moolaBlock = isWin
    ? `<div class="result-moola win-moola">${moolaIcon}+${netProfit.toLocaleString()} <span class="moola-unit">profit</span></div>
       <div class="result-moola-detail">${moolaIcon}Payout ${asNumber(winnings).toLocaleString()} · Stake ${stake.toLocaleString()}</div>`
    : `<div class="result-moola lose-moola">${moolaIcon}-${stake.toLocaleString()} <span class="moola-unit">lost</span></div>`;

  matchOverlay.innerHTML = `
    <div class="sketch-result-card match-result-card ${isWin ? 'win' : 'lose'}">
      <div class="sketch-result-scribble" aria-hidden="true"></div>
      ${trophySvg}
      <div class="result-title">${title}</div>
      <div class="result-subtitle">${subtitle}</div>
      ${moolaBlock}
      ${rake > 0 ? `<div class="result-rake">${moolaIcon}House rake: ${rake.toLocaleString()}</div>` : ''}
      <div class="result-score">Final score · ${p1Wins} – ${p2Wins}</div>
      <button type="button" class="btn-accent btn-play-again" id="play-again-btn">Play Again</button>
    </div>
  `;
  matchOverlay.classList.remove('hidden');
  $('#play-again-btn')?.addEventListener('click', playAgain, { once: true });
  if (typeof MatchFx !== 'undefined') {
    if (isWin) {
      MatchFx.playWin();
      const overlayFx = document.createElement('div');
      overlayFx.className = 'match-confetti';
      overlayFx.setAttribute('aria-hidden', 'true');
      matchOverlay.prepend(overlayFx);
      MatchFx.spawnConfetti(overlayFx, 140, true);
      MatchFx.spawnConfetti(matchConfetti, 60, true);
      MatchFx.screenShake(matchArena, 1.5);
    } else {
      MatchFx.playLose();
    }
  }
}

function resetMatchUI() {
  matchOverlay.classList.add('hidden');
  matchOverlay.innerHTML = '';
  matchRoomChat.classList.add('hidden');
  if (matchConfetti) matchConfetti.innerHTML = '';
  currentRoomId = null;
  pendingMatchEnd = null;
  hideChoiceReveal();
  roundResult?.classList.add('hidden');
}

function closeMatchOverlay() {
  resetMatchUI();
  gameSectionPanel.classList.add('hidden');
  syncNotebookDoodles();
}

function playAgain() {
  if (!lastRoomConfig || !ws || ws.readyState !== WebSocket.OPEN) {
    closeMatchOverlay();
    showMsg(lobbyMessage, 'Reconnect to play again.', true);
    return;
  }
  resetMatchUI();
  gameSectionPanel.classList.add('hidden');
  syncNotebookDoodles();
  refreshBalance();

  const { betAmount, rounds, playWithBot, isPublic } = lastRoomConfig;
  if (betInput) betInput.value = betAmount;
  if (roundsSelect) roundsSelect.value = String(rounds);
  if (publicRoomCheckbox) publicRoomCheckbox.checked = !!isPublic && !playWithBot;

  showMsg(lobbyMessage, playWithBot ? 'Starting another bot match...' : 'Creating rematch room...', false);

  sendWs({
    action: 'createRoom',
    tornId: currentUser.torn_id,
    username: currentUser.username,
    betAmount,
    rounds,
    playWithBot: !!playWithBot,
    isPublic: !!isPublic && !playWithBot
  });
}

window.playAgain = playAgain;
window.closeMatchOverlay = closeMatchOverlay;

function showPendingMatchEnd() {
  if (!pendingMatchEnd) return;
  const data = pendingMatchEnd;
  pendingMatchEnd = null;
  enableChoices(false);
  refreshBalance();
  hideChoiceReveal();
  roundResult.classList.add('hidden');
  setTimeout(() => {
    const isWin = data.winner === currentUser.username;
    if (data.betAmount) currentMatchBet = asNumber(data.betAmount);
    showMatchOverlay(
      isWin, data.winner, data.winnings, data.rake,
      data.player1Wins, data.player2Wins, false, currentMatchBet
    );
  }, 300);
}

choiceBtns.forEach(btn => {
  btn.addEventListener('click', () => {
    choiceBtns.forEach(b => {
      b.classList.remove('selected', 'choice-slam');
    });
    btn.classList.add('selected', 'choice-slam');
    if (typeof MatchFx !== 'undefined') {
      MatchFx.playSelect();
    }
    sendWs({ action: 'submitChoice', tornId: currentUser.torn_id, roomId: currentRoomId, choice: btn.dataset.choice });
    enableChoices(false);
  });
});

// ── REFRESH BALANCE ────────────────────────────────────
async function refreshBalance() {
  try {
    const res = await fetch(`/api/user/${currentUser.torn_id}`);
    const data = await res.json();
    if (data.site_balance !== undefined) {
      updateBalance(data.site_balance);
      currentUser.total_moola_betted = asNumber(data.total_moola_betted);
      currentUser.total_moola_won = asNumber(data.total_moola_won);
      currentUser.total_moola_lost = asNumber(data.total_moola_lost);
      currentUser.total_matches_played = asNumber(data.total_matches_played);
      currentUser.total_matches_won = asNumber(data.total_matches_won);
      currentUser.win_rate = asNumber(data.win_rate);
      currentUser.net_profit_loss = asNumber(data.net_profit_loss);
      updateStatsModal();
    }
  } catch (_) {}
}

// ── INIT ───────────────────────────────────────────────
(async () => {
  try {
    const res = await fetch('/api/auth/me', { credentials: 'same-origin' });
    if (res.ok) {
      const data = await res.json();
      if (data.success && data.user) {
        setLoggedIn(data.user);
        connectWS();
        return;
      }
    }
  } catch (_) {}

  const storedKey = localStorage.getItem('rpsApiKey');
  const storedPin = localStorage.getItem('rpsPin');
  if (storedKey && storedPin) {
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ api_key: storedKey, pin: storedPin })
      });
      const data = await res.json();
      if (res.ok && data.success && data.user) {
        setLoggedIn(data.user);
        localStorage.setItem('tornId', data.user.torn_id);
        connectWS();
      } else {
        localStorage.removeItem('rpsApiKey');
        localStorage.removeItem('rpsPin');
      }
    } catch (_) {
      localStorage.removeItem('rpsApiKey');
      localStorage.removeItem('rpsPin');
    }
  }
  mountGameUiDecorations();
})();
