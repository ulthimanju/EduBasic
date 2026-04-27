import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { ArrowRight, GraduationCap } from 'lucide-react';
import { API_BASE_URL } from '../../../config/runtimeConfig';
import ErrorBanner from '../../../components/common/ErrorBanner/ErrorBanner';
import styles from './LoginPage.module.css';

export default function LoginPage() {
  const [searchParams] = useSearchParams();
  const errorParam = searchParams.get('error');

  const googleAuthUrl = `${API_BASE_URL}/oauth2/authorization/google`;

  const getErrorBanner = (code) => {
    switch (code) {
      case 'email_conflict':
        return 'This email is already associated with a different Google account.';
      case 'server_error':
        return 'An unexpected error occurred during login. Please try again.';
      default:
        return 'Login failed. Please try again.';
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <div className={styles.brand}>
          <GraduationCap size={48} color="var(--color-primary)" />
          <h1 className={styles.title}>EduBasic</h1>
        </div>

        <p className={styles.subtitle}>
          Sign in to your account to continue your learning journey.
        </p>

        {errorParam && (
          <div className={styles.error}>
            <ErrorBanner message={getErrorBanner(errorParam)} />
          </div>
        )}

        <a href={googleAuthUrl} className={styles.googleBtn}>
          <svg className={styles.googleIcon} viewBox="0 0 18 18">
            <path fill="#4285F4" d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z"/>
            <path fill="#34A853" d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332C2.438 15.983 5.482 18 9 18z"/>
            <path fill="#FBBC05" d="M3.964 10.706c-.18-.54-.282-1.117-.282-1.706s.102-1.166.282-1.706V4.962H.957C.347 6.175 0 7.55 0 9s.348 2.825.957 4.038l3.007-2.332z"/>
            <path fill="#EA4335" d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0 5.482 0 2.438 2.017.957 4.962L3.964 7.294C4.672 5.163 6.656 3.58 9 3.58z"/>
          </svg>
          <span>Sign in with Google</span>
          <ArrowRight size={18} />
        </a>

        <div className={styles.footer}>
          <p>By signing in, you agree to our Terms and Privacy Policy.</p>
          <div className={styles.divider} />
          <p className={styles.footnote}>OAuth 2.0 Secure Authentication</p>
        </div>
      </div>
    </div>
  );
}
