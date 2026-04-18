import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Trophy, Target, Award, LineChart, ChevronRight, LayoutDashboard, RotateCcw } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';
import { ROUTES } from '../constants/appConstants';

const ResultPage = () => {
  const { sessionId } = useParams();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchResult = async () => {
      try {
        const response = await examApi.getResult(sessionId);
        setResult(response.data);
      } catch (err) {
        setError('Failed to load result');
      } finally {
        setLoading(false);
      }
    };
    fetchResult();
  }, [sessionId]);

  if (loading) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;
  if (!result) return null;

  const getLevelColorClass = (level) => {
    switch (level) {
      case 'Expert': return 'text-purple-500';
      case 'Advanced': return 'text-blue-500';
      case 'Intermediate': return 'text-green-500';
      default: return 'text-accent';
    }
  };

  return (
    <div className="max-w-4xl mx-auto page-enter">
      <header className="dashboard-hero panel mb-8">
        <div className="dashboard-hero__content flex flex-col items-center text-center py-4">
          <div className="login-logo w-16 h-16 mb-4">
            <Trophy size={32} />
          </div>
          <h1 className="dashboard-hero__greeting text-3xl">Assessment Complete</h1>
          <p className="dashboard-hero__subtitle text-base mt-2">
            We've analyzed your performance. Here is your proficiency profile.
          </p>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        <article className="panel flex flex-col items-center justify-center p-10 text-center">
          <div className="dashboard-card__icon mb-4" aria-hidden="true">
            <Award size={24} className="text-text-secondary" />
          </div>
          <p className="text-xs font-bold text-text-muted uppercase tracking-widest mb-2">Proficiency Level</p>
          <h2 className={`text-4xl font-bold ${getLevelColorClass(result.level)}`}>
            {result.level}
          </h2>
        </article>

        <article className="panel flex flex-col items-center justify-center p-10 text-center">
          <div className="dashboard-card__icon mb-4" aria-hidden="true">
            <Target size={24} className="text-text-secondary" />
          </div>
          <p className="text-xs font-bold text-text-muted uppercase tracking-widest mb-2">Adaptive Score</p>
          <h2 className="text-4xl font-bold text-text-primary">
            {Math.round(result.normalizedScore)}%
          </h2>
        </article>
      </div>

      <div className="panel p-8 mb-8">
        <div className="flex items-center gap-3 mb-6">
          <div className="dashboard-card__icon">
            <LineChart size={18} />
          </div>
          <h3 className="text-lg font-semibold">Performance Insights</h3>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div>
            <h4 className="text-xs font-bold text-text-muted uppercase tracking-widest mb-4">Strong Topics</h4>
            <div className="flex flex-wrap gap-2">
              {result.topicsStrong?.length > 0 ? result.topicsStrong.map((topic, i) => (
                <span key={i} className="px-3 py-1 bg-green-500/10 text-green-500 border border-green-500/20 rounded-lg text-sm font-medium">
                  {topic}
                </span>
              )) : <span className="text-text-muted text-sm italic">Not enough data to determine</span>}
            </div>
          </div>
          <div>
            <h4 className="text-xs font-bold text-text-muted uppercase tracking-widest mb-4">Focus Areas</h4>
            <div className="flex flex-wrap gap-2">
              {result.topicsWeak?.length > 0 ? result.topicsWeak.map((topic, i) => (
                <span key={i} className="px-3 py-1 bg-accent/10 text-accent border border-accent/20 rounded-lg text-sm font-medium">
                  {topic}
                </span>
              )) : <span className="text-text-muted text-sm italic">You handled all topics well</span>}
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mt-12">
        <Link
          to={ROUTES.COURSES}
          className="btn btn-primary px-8 py-3 text-base w-full sm:w-auto flex items-center gap-2"
        >
          <RotateCcw size={18} />
          Try Another Course
        </Link>
        <Link
          to={ROUTES.DASHBOARD}
          className="btn btn-secondary px-8 py-3 text-base w-full sm:w-auto flex items-center gap-2"
        >
          <LayoutDashboard size={18} />
          Return to Dashboard
        </Link>
      </div>
    </div>
  );
};

export default ResultPage;
