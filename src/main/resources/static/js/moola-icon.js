/**
 * Moola — hand-drawn dollar sign in site notebook ink (matches RPS sketches).
 */
const MoolaIcon = (() => {
  const INK = '#163e79';
  const INK2 = '#3d69ad';

  function moolaSvg() {
    return `<svg class="moola-sketch" viewBox="0 0 32 42" aria-hidden="true">
      <path d="M8 2 Q7 4 8 6 L8.5 36 Q7.5 38 8.5 40" fill="none" stroke="${INK}" stroke-width="1.9" stroke-linecap="round"/>
      <path d="M24 2 Q25 4 24 6 L23.5 36 Q24.5 38 23.5 40" fill="none" stroke="${INK}" stroke-width="1.9" stroke-linecap="round"/>
      <path d="M22 6 C10 4 9 12 12 15 C15 17 20 16 11 18 C9 20 10 28 21 31" fill="none" stroke="${INK}" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M21 7 C11 5 10 13 13 16 C16 18 19 17 12 19 C10 21 11 27 20 30" fill="none" stroke="${INK2}" stroke-width="1.15" stroke-linecap="round" stroke-linejoin="round" opacity="0.7"/>
      <path d="M11 11 Q13 10 15 11" fill="none" stroke="${INK2}" stroke-width="0.9" stroke-linecap="round" opacity="0.5"/>
      <path d="M10 25 Q12 24 14 25" fill="none" stroke="${INK2}" stroke-width="0.9" stroke-linecap="round" opacity="0.5"/>
    </svg>`;
  }

  function iconHtml(extraClass = '') {
    return `<span class="moola-icon-slot ${extraClass}" title="Moola">${moolaSvg()}</span>`;
  }

  function amountHtml(value, extraClass = '') {
    const n = Number(value);
    const text = Number.isFinite(n) ? n.toLocaleString() : '0';
    return `<span class="moola-amount-wrap ${extraClass}">${iconHtml('moola-icon--amt')}${text}</span>`;
  }

  function iconElement(size = 'md') {
    const slot = document.createElement('span');
    slot.className = `moola-icon-slot moola-icon--${size}`;
    slot.setAttribute('title', 'Moola');
    slot.innerHTML = moolaSvg();
    return slot;
  }

  function setBalanceElement(el, amount) {
    if (!el) return;
    el.innerHTML = '';
    const wrap = document.createElement('span');
    wrap.className = 'moola-balance-display';
    wrap.appendChild(iconElement('lg'));
    const num = document.createElement('span');
    num.className = 'moola-balance-num';
    num.textContent = Number(amount).toLocaleString();
    wrap.appendChild(num);
    el.appendChild(wrap);
  }

  function mountIcons() {
    document.querySelectorAll('[data-moola-icon], [data-moola-badge]').forEach((slot) => {
      slot.querySelectorAll('.moola-img, .moola-icon-graphic').forEach((el) => el.remove());
      if (slot.querySelector('.moola-sketch')) return;
      slot.appendChild(iconElement('sm'));
    });
  }

  return {
    iconHtml,
    amountHtml,
    setBalanceElement,
    mountIcons,
    mountBadges: mountIcons
  };
})();
