import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
    <div className="container mx-auto p-4">
      <h1 className="text-3xl font-bold mb-6">Select a Course</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {courses.map((course) => (
          <div key={course.id} className="bg-white p-6 rounded-lg shadow-md border hover:border-blue-500 transition-colors">
            <h2 className="text-xl font-semibold mb-2">{course.name}</h2>
            <div className="flex flex-wrap gap-2 mb-4">
              {course.topics.map((topic, index) => (
                <span key={index} className="bg-blue-100 text-blue-800 text-xs font-medium px-2.5 py-0.5 rounded">
                  {topic}
                </span>
              ))}
            </div>
            <button
              onClick={() => handleStartExam(course.id)}
              className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700"
            >
              Start Exam
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default CourseSelectPage;
