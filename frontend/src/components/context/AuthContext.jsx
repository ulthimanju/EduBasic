import React, { createContext, useMemo, useState } from 'react';
import { getStoredAuth, persistAuth, clearStoredAuth } from '../../utils/authStorage';

const AuthContext = createContext(null);

const initialAuth = getStoredAuth();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(initialAuth.token);
  const [isReady] = useState(true);

  const login = (nextUser, nextToken) => {
    setUser(nextUser);
    setToken(nextToken);
    persistAuth(nextToken);
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    clearStoredAuth();
  };

  const value = useMemo(() => ({ user, token, isReady, login, logout }), [user, token, isReady]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export default AuthContext;
