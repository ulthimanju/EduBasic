import { useState, useCallback, useRef, useEffect } from 'react';

/**
 * Encapsulates answer tracking and periodic sync logic for exams.
 */
export function useAnswerSync(attemptId, syncAttempt, initialVersion = 0) {
  const [answers, setAnswers] = useState({});
  const [dirtyQuestionIds, setDirtyQuestionIds] = useState(new Set());
  const [version, setVersion] = useState(initialVersion);
  
  const syncIntervalRef = useRef(null);

  const serializeAnswers = useCallback((onlyDirty = false) => {
    const serialized = {};
    const idsToSerialize = onlyDirty ? Array.from(dirtyQuestionIds) : Object.keys(answers);
    
    idsToSerialize.forEach(qid => {
      const ans = answers[qid];
      serialized[qid] = typeof ans === 'object' ? JSON.stringify(ans) : String(ans);
    });
    return serialized;
  }, [answers, dirtyQuestionIds]);

  const handleSync = useCallback(async (isFullSync = false) => {
    if (!isFullSync && dirtyQuestionIds.size === 0) return;
    
    try {
      const deltaAnswers = serializeAnswers(!isFullSync);
      const updatedAttempt = await syncAttempt(attemptId, { version, answers: deltaAnswers });
      setVersion(updatedAttempt.version);
      if (!isFullSync) {
        setDirtyQuestionIds(new Set());
      }
    } catch (err) {
      console.error('Autosave failed', err);
    }
  }, [attemptId, version, syncAttempt, serializeAnswers, dirtyQuestionIds]);

  const onAnswerChange = useCallback((questionId, val) => {
    setAnswers(prev => ({ ...prev, [questionId]: val }));
    setDirtyQuestionIds(prev => {
      const next = new Set(prev);
      next.add(questionId);
      return next;
    });
  }, []);

  useEffect(() => {
    syncIntervalRef.current = setInterval(() => handleSync(false), 30000);
    return () => {
      if (syncIntervalRef.current) clearInterval(syncIntervalRef.current);
    };
  }, [handleSync]);

  return {
    answers,
    onAnswerChange,
    handleSync,
    version
  };
}
