import React from 'react';
import styles from './TerminatedOverlay.module.css';
import Spinner from '../../../../components/common/Spinner/Spinner';

const TerminatedOverlay = () => {
  return (
    <div className={styles.overlay}>
      <div className={styles.content}>
        <h1 className={styles.title}>Exam Terminated</h1>
        <p className={styles.message}>
          Your exam has been automatically submitted due to multiple security violations. 
          You are being redirected to the results page.
        </p>
        <Spinner />
      </div>
    </div>
  );
};

export default TerminatedOverlay;
