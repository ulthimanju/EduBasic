import React from 'react';
import { Database, ShieldCheck, UserRound } from 'lucide-react';
import useAuthStore from '../features/auth/store/authStore';

/**
 * Dashboard — the main authenticated landing page.
 * Shows a welcome card with the user's profile.
 */
export default function Dashboard() {
  const { user } = useAuthStore();

  if (!user) {
    return (
      <section className="empty-state page-enter">
        <div className="empty-state__icon" aria-hidden="true">
          <UserRound size={48} strokeWidth={1.5} />
        </div>
        <h2 className="empty-state__title">No account loaded</h2>
        <p className="empty-state__text">
          Your profile will appear here after authentication completes.
        </p>
      </section>
    );
  }

  return (
    <section className="dashboard page-enter">
      <section className="dashboard-hero panel">
        <div className="dashboard-hero__content">
          <h1 className="dashboard-hero__greeting">Welcome back, {user.name?.split(' ')[0] ?? 'there'}</h1>
          <p className="dashboard-hero__subtitle">
            Signed in as <span className="dashboard-hero__email">{user.email}</span>
          </p>
        </div>
      </section>

      <section className="dashboard-cards">
        <article className="dashboard-card panel">
          <div className="dashboard-card__icon" aria-hidden="true">
            <UserRound size={18} strokeWidth={1.5} />
          </div>
          <h2 className="dashboard-card__title">Your Profile</h2>
          <dl className="profile-list">
            <dt>Name</dt>
            <dd id="profile-name">{user.name}</dd>
            <dt>Email</dt>
            <dd id="profile-email">{user.email}</dd>
            <dt>User ID</dt>
            <dd id="profile-id" className="profile-id">{user.id}</dd>
          </dl>
        </article>

        <article className="dashboard-card panel">
          <div className="dashboard-card__icon" aria-hidden="true">
            <ShieldCheck size={18} strokeWidth={1.5} />
          </div>
          <h2 className="dashboard-card__title">Session</h2>
          <p className="dashboard-card__body">
            Session validity is backed by Neo4j and accelerated through Redis. JWT is
            delivered via secure HttpOnly cookie and checked on each request.
          </p>
        </article>

        <article className="dashboard-card panel">
          <div className="dashboard-card__icon" aria-hidden="true">
            <Database size={18} strokeWidth={1.5} />
          </div>
          <h2 className="dashboard-card__title">Stack</h2>
          <ul className="stack-list">
            <li>Spring Boot 3 · Spring Security 6</li>
            <li>Neo4j 5 for user and session relationships</li>
            <li>Redis 7 for JWT and profile caching</li>
            <li>React 18 + Vite + Zustand</li>
          </ul>
        </article>
      </section>
    </section>
  );
}
