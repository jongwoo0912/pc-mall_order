// 공용 인증 도우미: 로그인 회원 조회, 상단 로그인 메뉴 그리기
const Auth = (() => {
	/** 로그인한 회원 { id, name, mail } 또는 null */
	async function me() {
		const res = await fetch('/api/customers/me', { cache: 'no-store' });
		return res.ok ? res.json() : null;
	}

	/** <div id="authNav"></div> 안에 로그인/회원가입 또는 회원명/로그아웃 표시 */
	async function renderNav(el) {
		const user = await me().catch(() => null);
		el.innerHTML = '';
		if (!user) {
			el.append(link('/login', '로그인', 'btn'), link('/signup', '회원가입', 'btn'));
			return null;
		}
		const who = document.createElement('span');
		who.className = 'who';
		who.textContent = `${user.name}님`;

		const btn = document.createElement('button');
		btn.type = 'button';
		btn.className = 'btn';
		btn.textContent = '로그아웃';
		btn.addEventListener('click', async () => {
			btn.disabled = true;
			await logout().catch(() => {});
			location.href = '/';
		});
		el.append(who, link('/cart', '장바구니', 'btn'), link('/orders', '주문 내역', 'btn'), btn);
		return user;
	}

	/** 비로그인 상태로 로그인이 필요한 동작을 하면 로그인 후 현재 화면으로 돌아오도록 이동 */
	function goLogin() {
		location.href = '/login?next=' + encodeURIComponent(location.pathname + location.search);
	}

	/** POST /api/customers/logout — 세션 종료 */
	async function logout() {
		await fetch('/api/customers/logout', { method: 'POST' });
	}

	function link(href, text, className) {
		const a = document.createElement('a');
		a.href = href;
		a.textContent = text;
		a.className = className;
		return a;
	}

	/** 관리자 여부 (me() 결과의 roles 에 'ADMIN' 포함) */
	function isAdmin(user) {
		return !!user && Array.isArray(user.roles) && user.roles.includes('ADMIN');
	}

	return { me, logout, isAdmin, renderNav, goLogin };
})();
