import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BookOpen, GraduationCap } from 'lucide-react';
import examApi from '../services/examApi';
import Spinner from '../components/ui/Spinner/Spinner';
import ErrorMessage from '../components/ui/ErrorMessage/ErrorMessage';

const CourseSelectPage = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchCourses = async () => {
      try {
        const response = await examApi.getCourses();
        setCourses(response.data);
      } catch (err) {
        setError('Failed to load courses');
      } finally {
        setLoading(false);
      }
    };
    fetchCourses();
  }, []);

  const handleStartExam = async (courseId) => {
    try {
      const response = await examApi.startExam(courseId);
      navigate(`/exam/${response.data.id}`);
    } catch (err) {
      setError('Failed to start exam');
    }
  };

  if (loading) return <Spinner />;
  if (error) return <ErrorMessage message={error} />;

  return (
    <section className="dashboard page-enter">
      <header className="dashboard-hero panel mb-8">
        <div className="dashboard-hero__content">
          <div className="flex items-center gap-3 mb-2">
            <div className="login-logo">
              <GraduationCap size={20} />
            </div>
            <h1 className="dashboard-hero__greeting">Select Your Path</h1>
          </div>
          <p className="dashboard-hero__subtitle">
            Choose a course to begin your adaptive proficiency assessment.
          </p>
        </div>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {courses.map((course) => (
          <article key={course.id} className="dashboard-card panel flex flex-col h-full">
            <div className="dashboard-card__icon" aria-hidden="true">
              <BookOpen size={18} strokeWidth={1.5} />
            </div>
            
            <div className="flex-1">
              <h2 className="dashboard-card__title mb-2">{course.name}</h2>
              <div className="flex flex-wrap gap-2 mb-6">
                {course.topics.map((topic, index) => (
                  <span key={index} className="bg-surface-glass border border-border-subtle text-text-secondary text-xs font-medium px-2 py-0.5 rounded-md">
                    {topic}
                  </span>
                ))}
              </div>
            </div>

            <button
              onClick={() => handleStartExam(course.id)}
              className="btn btn-primary w-full mt-auto"
            >
              Start Assessment
            </button>
          </article>
        ))}
      </div>
    </section>
  );
};

export default CourseSelectPage;
