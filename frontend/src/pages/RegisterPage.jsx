import React, { useState } from 'react';
import {
  ThemeProvider,
  useTheme,
  ActionButton,
  InputField,
  PasswordField,
  SuccessModal,
} from '../components';
import logoSvg from '../assets/logo.svg';
import apiFetch, { apiEndpoints } from '../utils/apiClient';

function RegisterPageContent({ onNavigate, onLogin }) {
  const colors = useTheme();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [captchaInput, setCaptchaInput] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [confirmPasswordVisible, setConfirmPasswordVisible] = useState(false);
  const [error, setError] = useState('');
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [registeredUser, setRegisteredUser] = useState(null);

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

  // Email validation regex
  const validateEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!username.trim()) {
      setError('Please enter a username');
      return;
    }

    if (username.length < 3) {
      setError('Username must be at least 3 characters long');
      return;
    }

    if (!email.trim()) {
      setError('Please enter your email');
      return;
    }

    if (!validateEmail(email)) {
      setError('Please enter a valid email address');
      return;
    }

    if (!password) {
      setError('Please enter a password');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters long');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (captchaInput !== captchaText) {
      setError('Captcha does not match. Please try again.');
      setCaptchaText(generateCaptcha());
      setCaptchaInput('');
      return;
    }

    // Send registration data to backend
    try {
      const response = await apiFetch(apiEndpoints.auth.register, {
        method: 'POST',
        body: JSON.stringify({
          username,
          email,
          password,
          confirmPassword,
          captcha: captchaInput,
        }),
      });

      const data = await response.json();

      if (response.ok) {
        setRegisteredUser(data);
        setShowSuccessModal(true);
        // Clear form
        setUsername('');
        setEmail('');
        setPassword('');
        setConfirmPassword('');
        setCaptchaInput('');
        setCaptchaText(generateCaptcha());
      } else {
        setError(data.message || 'Registration failed');
      }
    } catch (error) {
      console.error('Error:', error);
      setError('Failed to connect to server. Make sure backend is running on port 8080.');
    }
  };

  return (
    <div
      className="min-h-screen w-full flex items-center justify-center p-4 transition-colors duration-500 overflow-hidden"
      style={{ background: colors.bgApp, color: colors.textMain }}
    >
      {/* Success Modal */}
      <SuccessModal
        isOpen={showSuccessModal}
        onClose={() => {
          setShowSuccessModal(false);
          // Auto-login after registration
          if (registeredUser && registeredUser.token) {
            onLogin(registeredUser, registeredUser.token);
          } else {
            onNavigate('login');
          }
        }}
        title="Registration Successful!"
        username={registeredUser?.username}
        email={registeredUser?.email}
        role={registeredUser?.role}
        buttonText="Okay"
      />

      <div
        className="w-full max-w-2xl rounded-[28px] p-6 backdrop-blur-xl border shadow-2xl overflow-y-auto max-h-[95vh]"
        style={{
          background: colors.bgPanel,
          borderColor: colors.border,
          boxShadow: colors.shadow,
        }}
      >
        {/* Logo and Header */}
        <div className="flex flex-col items-center mb-6">
          <img src={logoSvg} alt="edubas" className="w-16 h-16" />
          <h1 className="text-2xl font-bold mt-3">Join edubas</h1>
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

        {/* Register Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Username Field */}
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
              label="Username"
              placeholder="Choose a username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="[&_input]:pl-12"
            />
          </div>

          {/* Email Field */}
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
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                <polyline points="22,6 12,13 2,6" />
              </svg>
            </div>
            <InputField
              label="Email"
              placeholder="your.email@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="[&_input]:pl-12"
            />
          </div>

          {/* Password and Confirm Password Fields */}
          <div className="flex gap-3">
            {/* Password Field */}
            <div className="relative flex-1">
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

            {/* Confirm Password Field */}
            <div className="relative flex-1">
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
                label="Confirm Password"
                value={confirmPassword}
                visible={confirmPasswordVisible}
                onChange={setConfirmPassword}
                onToggle={() => setConfirmPasswordVisible(!confirmPasswordVisible)}
                className="[&_input]:pl-12"
              />
            </div>
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
            <ActionButton label="Create Account" onClick={() => {}} className="px-12" />
          </div>
        </form>

        {/* Footer Links */}
        <div className="mt-4 text-center text-xs opacity-50">
          Already have an account?{' '}
          <button
            type="button"
            onClick={() => onNavigate('login')}
            className="font-semibold hover:underline"
            style={{ color: colors.accentSolid }}
          >
            Sign in
          </button>
        </div>
      </div>
    </div>
  );
}

function RegisterPage({ onNavigate, onLogin }) {
  const [selectedTheme] = useState('Midnight Sunset');

  return (
    <ThemeProvider selectedTheme={selectedTheme}>
      <RegisterPageContent onNavigate={onNavigate} onLogin={onLogin} />
    </ThemeProvider>
  );
}

export default RegisterPage;
