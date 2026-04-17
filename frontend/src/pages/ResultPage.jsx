import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';

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

  const getLevelColor = (level) => {
    switch (level) {
      case 'Expert': return 'text-purple-600';
      case 'Advanced': return 'text-blue-600';
      case 'Intermediate': return 'text-green-600';
      default: return 'text-gray-600';
    }
  };

  return (
    <div className="container mx-auto p-4 max-w-4xl">
      <div className="bg-white rounded-2xl shadow-xl overflow-hidden">
        <div className="bg-blue-600 p-8 text-white text-center">
          <h1 className="text-3xl font-bold mb-2">Exam Completed!</h1>
          <p className="opacity-90">Here is your proficiency analysis</p>
        </div>

        <div className="p-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12">
            <div className="text-center p-8 bg-gray-50 rounded-2xl border">
              <p className="text-sm uppercase tracking-wider text-gray-500 font-bold mb-2">Your Level</p>
              <h2 className={`text-5xl font-black ${getLevelColor(result.level)}`}>
                {result.level}
              </h2>
            </div>
            <div className="text-center p-8 bg-gray-50 rounded-2xl border">
              <p className="text-sm uppercase tracking-wider text-gray-500 font-bold mb-2">Normalized Score</p>
              <h2 className="text-5xl font-black text-gray-800">
                {Math.round(result.normalizedScore)}%
              </h2>
            </div>
          </div>

          <div className="flex justify-center space-x-4">
            <Link
              to="/courses"
              className="bg-blue-600 text-white px-8 py-3 rounded-lg font-bold hover:bg-blue-700 transition-colors"
            >
              Take Another Exam
            </Link>
            <Link
              to="/dashboard"
              className="bg-gray-100 text-gray-700 px-8 py-3 rounded-lg font-bold hover:bg-gray-200 transition-colors"
            >
              Go to Dashboard
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ResultPage;
