import React from 'react';
import { Navigate } from 'react-router-dom';
import { UserRound } from 'lucide-react';
import useAuthStore from '../features/auth/store/authStore';
import { ROUTES } from '../constants/appConstants';

/**
 * Dashboard — the main authenticated landing page.
 * Shows a welcome card with the user's profile.
 */
export default function Dashboard() {
  const { user } = useAuthStore();

  if (!user) {
    return <Navigate to={ROUTES.LOGIN} replace />;
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
      </section>
    </section>
  );
}
