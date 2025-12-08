import React, { useState, useEffect } from 'react';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import UploadContentPage from './pages/UploadContentPage';
import ViewContentPage from './pages/ViewContentPage';
import FeedPage from './pages/FeedPage';
import AdminPage from './pages/AdminPage';
import ProblemPage from './pages/ProblemPage';
import apiFetch, { apiEndpoints } from './utils/apiClient';
import { getStoredAuth, persistAuth, clearStoredAuth } from './utils/authStorage';

function App() {
  const [currentPage, setCurrentPage] = useState('login');
  const [currentUser, setCurrentUser] = useState(null);
  const [token, setToken] = useState(getStoredAuth().token);
  const [isLoading, setIsLoading] = useState(true);
  const [practiceProblemData, setPracticeProblemData] = useState(null);

  // Initialize auth state from stored token on mount
  useEffect(() => {
    // Cleanup any legacy persisted user object
    localStorage.removeItem('currentUser');

    const savedToken = getStoredAuth().token;
    const savedPage = localStorage.getItem('currentPage');

    const fetchCurrentUser = async () => {
      try {
        const response = await apiFetch(apiEndpoints.auth.me, { method: 'GET' });
        if (response.ok) {
          const data = await response.json();
          setCurrentUser({
            username: data.username,
            email: data.email,
            role: data.role,
            userId: data.userId,
            avatar: data.avatar,
            profileVisibility: data.profileVisibility,
            emailNotifications: data.emailNotifications,
          });
          setCurrentPage(savedPage || 'dashboard');
        } else {
          // Token invalid/expired
          handleLogout();
        }
      } catch (error) {
        console.error('Failed to fetch current user:', error);
        handleLogout();
      } finally {
        setIsLoading(false);
      }
    };

    if (savedToken) {
      setToken(savedToken);
      fetchCurrentUser();
    } else {
      setIsLoading(false);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Persist currentPage to localStorage whenever it changes
  useEffect(() => {
    if (currentUser && currentPage !== 'login' && currentPage !== 'register') {
      localStorage.setItem('currentPage', currentPage);
    }
  }, [currentPage, currentUser]);

  const handleLogin = (user, token) => {
    setCurrentUser(user);
    setToken(token);
    persistAuth(token);
    localStorage.setItem('currentPage', 'dashboard');
    setCurrentPage('dashboard');
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setToken(null);
    clearStoredAuth();
    localStorage.removeItem('currentPage');
    localStorage.removeItem('practiceProblemData');
    setCurrentPage('login');
  };

  const openProblemPage = (data) => {
    setPracticeProblemData(data);
    try {
      localStorage.setItem('practiceProblemData', JSON.stringify(data));
    } catch (err) {
      console.warn('Failed to persist practice problem', err);
    }
    setCurrentPage('problem');
  };

  if (isLoading) {
    return null;
  }

  if (currentPage === 'login') {
    return <LoginPage onNavigate={setCurrentPage} onLogin={handleLogin} />;
  } else if (currentPage === 'register') {
    return <RegisterPage onNavigate={setCurrentPage} onLogin={handleLogin} />;
  } else if (currentPage === 'dashboard') {
    return <DashboardPage user={currentUser} onLogout={handleLogout} onNavigate={setCurrentPage} />;
  } else if (currentPage === 'upload') {
    return <UploadContentPage onNavigate={setCurrentPage} onLogout={handleLogout} user={currentUser} />;
  } else if (currentPage === 'view') {
    return <ViewContentPage onLogout={handleLogout} onNavigate={setCurrentPage} user={currentUser}
      onOpenPracticeProblem={openProblemPage} />;
  } else if (currentPage === 'feed') {
    return <FeedPage onLogout={handleLogout} onNavigate={setCurrentPage} user={currentUser} />;
  } else if (currentPage === 'admin') {
    return <AdminPage onLogout={handleLogout} onNavigate={setCurrentPage} user={currentUser} />;
  } else if (currentPage === 'problem') {
    return <ProblemPage onLogout={handleLogout} onNavigate={setCurrentPage} user={currentUser}
      data={practiceProblemData} />;
  }

  return null;
}

export default App;
