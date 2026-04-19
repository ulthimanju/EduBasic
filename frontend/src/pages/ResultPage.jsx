import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Trophy, Target, Award, LineChart, LayoutDashboard, RotateCcw, ShieldAlert, AlertTriangle } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';
import { ROUTES } from '../constants/appConstants';
import { RESULT_CONTENT } from '../content/pageContent';
import { VISUAL_MAPPINGS } from '../config/pageConfig';
import { getLevelColorClass } from '../utils/viewModels';

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

  const isTerminated = result.status === 'TERMINATED';

  return (
    <div className="max-w-4xl mx-auto page-enter pb-20">
      {isTerminated && (
        <div className="mb-8 p-6 rounded-2xl bg-accent/5 border border-accent/20 flex flex-col md:flex-row items-center gap-6 animate-page-enter">
          <div className="w-12 h-12 rounded-full bg-accent/10 text-accent flex items-center justify-center shrink-0">
            <ShieldAlert size={24} />
          </div>
          <div className="flex-1 text-center md:text-left">
            <h3 className="text-lg font-bold text-text-primary mb-1">{RESULT_CONTENT.TERMINATED_NOTICE.TITLE}</h3>
            <p className="text-text-secondary text-sm leading-relaxed">
              {RESULT_CONTENT.TERMINATED_NOTICE.DESCRIPTION_PREFIX} {result.violationCount} {RESULT_CONTENT.TERMINATED_NOTICE.DESCRIPTION_SUFFIX}
            </p>
          </div>
          <div className="px-4 py-2 bg-accent/10 rounded-lg text-accent text-xs font-mono font-bold whitespace-nowrap">
            {RESULT_CONTENT.TERMINATED_NOTICE.CODE}
          </div>
        </div>
      )}

      <header className="dashboard-hero panel mb-8">
        <div className="dashboard-hero__content flex flex-col items-center text-center py-4">
          <div className={`login-logo w-16 h-16 mb-4 ${isTerminated ? 'bg-text-muted text-white' : ''}`}>
            {isTerminated ? <AlertTriangle size={32} /> : <Trophy size={32} />}
          </div>
          <h1 className="dashboard-hero__greeting text-3xl">
            {isTerminated ? RESULT_CONTENT.HERO.TERMINATED_TITLE : RESULT_CONTENT.HERO.COMPLETE_TITLE}
          </h1>
          <p className="dashboard-hero__subtitle text-base mt-2 max-w-lg mx-auto">
            {isTerminated 
              ? `${RESULT_CONTENT.HERO.TERMINATED_SUBTITLE} ${result.terminationReason || 'Integrity violation'}.`
              : RESULT_CONTENT.HERO.COMPLETE_SUBTITLE}
          </p>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        <article className="panel flex flex-col items-center justify-center p-10 text-center">
          <div className="dashboard-card__icon mb-4" aria-hidden="true">
            <Award size={24} className="text-text-secondary" />
          </div>
          <p className="text-xs font-bold text-text-muted uppercase tracking-widest mb-2">{RESULT_CONTENT.CARDS.LEVEL_LABEL}</p>
          <h2 className={`text-4xl font-bold ${getLevelColorClass(result.level, isTerminated)}`}>
            {result.level}
          </h2>
        </article>

        <article className="panel flex flex-col items-center justify-center p-10 text-center">
          <div className="dashboard-card__icon mb-4" aria-hidden="true">
            <Target size={24} className="text-text-secondary" />
          </div>
          <p className="text-xs font-bold text-text-muted uppercase tracking-widest mb-2">{RESULT_CONTENT.CARDS.SCORE_LABEL}</p>
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
          <h3 className="text-lg font-semibold">{RESULT_CONTENT.INSIGHTS.TITLE}</h3>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div>
            <h4 className="text-xs font-bold text-text-muted uppercase tracking-widest mb-4">{RESULT_CONTENT.INSIGHTS.STRENGTH_LABEL}</h4>
            <div className="flex flex-wrap gap-2">
              {result.topicsStrong && Object.keys(result.topicsStrong).length > 0 ? Object.keys(result.topicsStrong).map((topic, i) => (
                <span key={i} className="px-3 py-1 bg-green-500/10 text-green-500 border border-green-500/20 rounded-lg text-sm font-medium">
                  {topic}
                </span>
              )) : <span className="text-text-muted text-sm italic">{RESULT_CONTENT.INSIGHTS.STRENGTH_EMPTY}</span>}
            </div>
          </div>
          <div>
            <h4 className="text-xs font-bold text-text-muted uppercase tracking-widest mb-4">{RESULT_CONTENT.INSIGHTS.FOCUS_LABEL}</h4>
            <div className="flex flex-wrap gap-2">
              {result.topicsWeak && Object.keys(result.topicsWeak).length > 0 ? Object.keys(result.topicsWeak).map((topic, i) => (
                <span key={i} className="px-3 py-1 bg-accent/10 text-accent border border-accent/20 rounded-lg text-sm font-medium">
                  {topic}
                </span>
              )) : <span className="text-text-muted text-sm italic">
                {isTerminated ? RESULT_CONTENT.INSIGHTS.FOCUS_EMPTY_TERMINATED : RESULT_CONTENT.INSIGHTS.FOCUS_EMPTY_COMPLETE}
              </span>}
            </div>
          </div>
        </div>
      </div>

      <div className="panel p-8 border-border-subtle bg-surface-glass">
        <h4 className="text-xs font-bold text-text-muted uppercase tracking-widest mb-4 flex items-center gap-2">
          <ShieldAlert size={12} />
          {RESULT_CONTENT.INTEGRITY.TITLE}
        </h4>
        <div className="grid gap-3 text-sm text-text-secondary leading-relaxed">
          <p>
            This assessment was conducted using <b>Secure Exam Lifecycle</b> protocols. 
            {isTerminated 
              ? ` ${RESULT_CONTENT.INTEGRITY.DESCRIPTION_TERMINATED}` 
              : ` ${RESULT_CONTENT.INTEGRITY.DESCRIPTION_COMPLETE}`}
          </p>
          <div className="flex flex-wrap gap-4 mt-2">
             <div className="flex items-center gap-2 text-xs">
                <div className={`w-2 h-2 rounded-full ${isTerminated ? VISUAL_MAPPINGS.INTEGRITY_STATUS.FAILED.colorClass : VISUAL_MAPPINGS.INTEGRITY_STATUS.PASSED.colorClass}`} />
                <span>{RESULT_CONTENT.INTEGRITY.TRACKING_LABELS.FOCUS}: {isTerminated ? VISUAL_MAPPINGS.INTEGRITY_STATUS.FAILED.label : VISUAL_MAPPINGS.INTEGRITY_STATUS.PASSED.label}</span>
             </div>
             <div className="flex items-center gap-2 text-xs">
                <div className={`w-2 h-2 rounded-full ${isTerminated ? VISUAL_MAPPINGS.INTEGRITY_STATUS.FAILED.colorClass : VISUAL_MAPPINGS.INTEGRITY_STATUS.PASSED.colorClass}`} />
                <span>{RESULT_CONTENT.INTEGRITY.TRACKING_LABELS.NAVIGATION}: {isTerminated ? VISUAL_MAPPINGS.INTEGRITY_STATUS.FAILED.label : VISUAL_MAPPINGS.INTEGRITY_STATUS.PASSED.label}</span>
             </div>
             <div className="flex items-center gap-2 text-xs">
                <div className={`w-2 h-2 rounded-full ${VISUAL_MAPPINGS.INTEGRITY_STATUS.VERIFIED.colorClass}`} />
                <span>{RESULT_CONTENT.INTEGRITY.TRACKING_LABELS.SESSION}: {VISUAL_MAPPINGS.INTEGRITY_STATUS.VERIFIED.label}</span>
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
          {isTerminated ? RESULT_CONTENT.ACTIONS.RETRY : RESULT_CONTENT.ACTIONS.TRY_ANOTHER}
        </Link>
        <Link
          to={ROUTES.DASHBOARD}
          className="btn btn-secondary px-8 py-3 text-base w-full sm:w-auto flex items-center gap-2"
        >
          <LayoutDashboard size={18} />
          {RESULT_CONTENT.ACTIONS.DASHBOARD}
        </Link>
      </div>
    </div>
  );
};

export default ResultPage;
