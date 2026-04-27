import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { 
  BookOpen, 
  LayoutDashboard, 
  LogOut, 
  GraduationCap, 
  Database, 
  ClipboardList,
  PlusCircle
} from 'lucide-react';
import useAuthStore from '../../../stores/authStore';
import styles from './Navbar.module.css';

export default function Navbar() {
  const { isInstructor, clearAuth } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <nav className={styles.navbar}>
      <div className={styles.brand}>
        <GraduationCap size={24} color="var(--color-primary)" />
        <span className={styles.brandName}>EduBasic</span>
      </div>

      <div className={styles.navLinks}>
        <div className={styles.section}>
          <span className={styles.sectionLabel}>Student</span>
          <NavLink to="/courses" className={({ isActive }) => isActive ? styles.active : styles.link}>
            <BookOpen size={18} />
            <span>Courses</span>
          </NavLink>
          <NavLink to="/my-courses" className={({ isActive }) => isActive ? styles.active : styles.link}>
            <LayoutDashboard size={18} />
            <span>My Learning</span>
          </NavLink>
          <NavLink to="/assessments" className={({ isActive }) => isActive ? styles.active : styles.link}>
            <ClipboardList size={18} />
            <span>Assessments</span>
          </NavLink>
        </div>

        {isInstructor() && (
          <div className={styles.section}>
            <span className={styles.sectionLabel}>Instructor</span>
            <NavLink to="/instructor/courses" className={({ isActive }) => isActive ? styles.active : styles.link}>
              <PlusCircle size={18} />
              <span>My Courses</span>
            </NavLink>
            <NavLink to="/instructor/question-bank" className={({ isActive }) => isActive ? styles.active : styles.link}>
              <Database size={18} />
              <span>Question Bank</span>
            </NavLink>
            <NavLink to="/instructor/exams" className={({ isActive }) => isActive ? styles.active : styles.link}>
              <ClipboardList size={18} />
              <span>Exams</span>
            </NavLink>
          </div>
        )}
      </div>

      <div className={styles.footer}>
        <button onClick={handleLogout} className={styles.logoutBtn}>
          <LogOut size={18} />
          <span>Logout</span>
        </button>
      </div>
    </nav>
  );
}
