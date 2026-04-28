import React, { useEffect, useState, useRef, memo } from 'react';
import { Clock } from 'lucide-react';

const ExamTimer = memo(({ initialSeconds, onTimeUp }) => {
  const [timeLeft, setTimeLeft] = useState(initialSeconds);
  const timerRef = useRef(null);

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timerRef.current);
          onTimeUp();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [onTimeUp]);

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="exam-status-chip">
      <Clock size={16} />
      <div>
        <span className="exam-status-chip__label">Time Remaining</span>
        <strong>{formatTime(timeLeft)}</strong>
      </div>
    </div>
  );
});

export default ExamTimer;
