export function getStoredAuth() {
  try {
    const token = localStorage.getItem('authToken');
    return { token: token || null };
  } catch (err) {
    console.warn('authStorage: failed to read auth', err);
    return { token: null };
  }
}

export function persistAuth(token) {
  try {
    localStorage.setItem('authToken', token);
  } catch (err) {
    console.warn('authStorage: failed to persist auth', err);
  }
}

export function clearStoredAuth() {
  localStorage.removeItem('authToken');
}
