const Auth = (() => {
    const SESSION_KEY = 'rc_sess';
    const API_URL = 'http://localhost:9090/api/auth/login';

    async function login(username, password) {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (!data.success) {
            return false;
        }

        sessionStorage.setItem(SESSION_KEY, JSON.stringify({
            name: data.name,
            role: data.role,
            at: Date.now()
        }));

        return true;
    }

    function logout() {
        sessionStorage.removeItem(SESSION_KEY);
        window.location.href = '/'; // o '/login'
    }

    function getSession() {
        try { 
            return JSON.parse(sessionStorage.getItem(SESSION_KEY)); 
        } catch { 
            return null; 
        }
    }

    function isLoggedIn() { 
        return !!getSession(); 
    }

    function requireLogin() {
        if (!isLoggedIn()) {
            const inPages = window.location.pathname.replace(/\\/g, '/').includes('/pages/');
            window.location.href = inPages ? '../index.html' : 'index.html';
        }
    }

    return { login, logout, getSession, isLoggedIn, requireLogin };
})();
