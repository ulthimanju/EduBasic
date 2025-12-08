import React, { useState } from 'react';
import apiFetch, { apiEndpoints } from '../utils/apiClient';
import {
  ThemeProvider,
  useTheme,
  ActionButton,
  InputField,
  PasswordField,
  CircularProgress,
} from '../components';
import logoSvg from '../assets/logo.svg';

function LoginPageContent({ onNavigate, onLogin }) {
  const colors = useTheme();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [captchaInput, setCaptchaInput] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Generate random captcha text
  const generateCaptcha = () => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789';
    let captcha = '';
    for (let i = 0; i < 6; i++) {
      captcha += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return captcha;
  };

  const [captchaText, setCaptchaText] = useState(() => generateCaptcha());

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.trim()) {
      setError('Please enter your email or username');
      return;
    }

    if (!password) {
      setError('Please enter your password');
      return;
    }

    if (captchaInput !== captchaText) {
      setError('Captcha does not match. Please try again.');
      setCaptchaText(generateCaptcha());
      setCaptchaInput('');
      return;
    }

    // Send login data to backend
    setIsLoading(true);
    try {
      const response = await apiFetch(apiEndpoints.auth.login, {
        method: 'POST',
        body: JSON.stringify({
          usernameOrEmail: email,
          password,
          captcha: captchaInput,
        }),
      });

      const data = await response.json();

      if (response.ok) {
        // Pass user data and token to parent and navigate to dashboard
        onLogin({
          username: data.username,
          email: data.email,
          role: data.role,
          userId: data.userId,
        }, data.token);
      } else {
        setError(data.message || 'Login failed');
        setCaptchaText(generateCaptcha());
        setCaptchaInput('');
        setIsLoading(false);
      }
    } catch (error) {
      console.error('Error:', error);
      setError('Failed to connect to server. Make sure backend is running on port 8080.');
      setIsLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen w-full flex items-center justify-center p-4 transition-colors duration-500 overflow-hidden"
      style={{ background: colors.bgApp, color: colors.textMain }}
    >
      <div
        className="w-full max-w-md rounded-[28px] p-6 backdrop-blur-xl border shadow-2xl overflow-y-auto max-h-[95vh]"
        style={{
          background: colors.bgPanel,
          borderColor: colors.border,
          boxShadow: colors.shadow,
        }}
      >
        {/* Logo and Header */}
        <div className="flex flex-col items-center mb-6">
          <img src={logoSvg} alt="edubas" className="w-16 h-16" />
          <h1 className="text-2xl font-bold mt-3">Welcome to edubas</h1>
        </div>

        {/* Error Message */}
        {error && (
          <div
            className="mb-4 p-3 rounded-xl text-sm font-medium"
            style={{
              background: colors.errorBg,
              color: colors.error,
            }}
          >
            {error}
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Email/Username Field */}
          <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 opacity-50 pointer-events-none" style={{ marginTop: '10px' }}>
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </div>
            <InputField
              label="Email or Username"
              placeholder="Enter your email or username"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="[&_input]:pl-12"
            />
          </div>

          {/* Password Field */}
          <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 opacity-50 z-10 pointer-events-none" style={{ marginTop: '10px' }}>
              <svg
                width="18"
                height="18"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </div>
            <PasswordField
              label="Password"
              value={password}
              visible={passwordVisible}
              onChange={setPassword}
              onToggle={() => setPasswordVisible(!passwordVisible)}
              className="[&_input]:pl-12"
            />
          </div>

          {/* Captcha */}
          <div>
            <span className="text-xs font-semibold ml-1 opacity-60 block mb-1.5">Enter Captcha</span>
            <div className="flex gap-3 items-center">
              {/* Captcha Display */}
              <div
                className="flex-shrink-0 px-6 py-3 rounded-xl font-bold text-lg tracking-wider select-none relative overflow-hidden flex items-center justify-center"
                style={{
                  background: colors.bgInput,
                  letterSpacing: '0.3em',
                  fontFamily: 'monospace',
                }}
              >
                <div
                  className="absolute inset-0 opacity-5"
                  style={{
                    background: `repeating-linear-gradient(
                      45deg,
                      ${colors.textMain},
                      ${colors.textMain} 2px,
                      transparent 2px,
                      transparent 8px
                    )`,
                  }}
                />
                <span className="relative">{captchaText}</span>
              </div>

              {/* Refresh Button */}
              <button
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  setCaptchaText(generateCaptcha());
                }}
                className="flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center transition-all active:scale-95 hover:opacity-80"
                style={{ background: colors.bgInput }}
                title="Generate new captcha"
              >
                <svg
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M21.5 2v6h-6M2.5 22v-6h6M2 11.5a10 10 0 0 1 18.8-4.3M22 12.5a10 10 0 0 1-18.8 4.2" />
                </svg>
              </button>
            </div>

            {/* Captcha Input */}
            <input
              className="mt-2 px-4 py-3 rounded-xl outline-none focus:ring-2 focus:ring-opacity-50 transition-all font-medium text-sm w-full"
              placeholder="Enter the text above"
              value={captchaInput}
              onChange={(e) => setCaptchaInput(e.target.value)}
              style={{
                background: colors.bgInput,
                color: colors.textMain,
                '--tw-ring-color': colors.accentSolid,
              }}
            />
          </div>

          {/* Submit Button */}
          <div className="pt-1 flex justify-center">
            {isLoading ? (
              <div className="flex items-center justify-center gap-3 py-2">
                <CircularProgress size={32} value={75} />
                <span style={{ color: colors.textMain }} className="text-sm font-medium">Signing in...</span>
              </div>
            ) : (
              <ActionButton label="Sign In" onClick={() => {}} className="px-12" />
            )}
          </div>
        </form>

        {/* Footer Links */}
        <div className="mt-4 text-center text-xs">
          <a
            href="#"
            className="text-sm font-medium hover:underline transition-opacity hover:opacity-70"
            style={{ color: colors.accentSolid }}
          >
            Forgot password?
          </a>
          <span className="opacity-50 mx-3">|</span>
          <span className="opacity-50">
            Don't have an account? <button
              type="button"
              onClick={() => onNavigate('register')}
              className="font-semibold hover:underline"
              style={{ color: colors.accentSolid }}
            >
              Sign up
            </button>
          </span>
        </div>
      </div>
    </div>
  );
}

function LoginPage({ onNavigate, onLogin }) {
  const [selectedTheme] = useState('Midnight Sunset');

  return (
    <ThemeProvider selectedTheme={selectedTheme}>
      <LoginPageContent onNavigate={onNavigate} onLogin={onLogin} />
    </ThemeProvider>
  );
}

export default LoginPage;
