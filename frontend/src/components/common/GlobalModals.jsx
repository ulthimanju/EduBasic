import React from 'react';
import ConfirmModal from './ConfirmModal/ConfirmModal';
import useUiStore from '../../stores/uiStore';

/**
 * Orchestrates all globally-triggered modals.
 * Decoupled from router and specific pages.
 */
export default function GlobalModals() {
  const { confirmModal, closeConfirmModal } = useUiStore();

  if (!confirmModal) return null;

  return (
    <ConfirmModal 
      {...confirmModal} 
      onCancel={confirmModal.onCancel || closeConfirmModal}
    />
  );
}
