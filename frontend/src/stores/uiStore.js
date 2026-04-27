import { create } from 'zustand';

const useUiStore = create((set) => ({
  // Lesson player
  activeLessonId: null,
  setActiveLessonId: (id) => set({ activeLessonId: id }),

  // Sidebar
  sidebarOpen: true,
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),

  // Confirm modal
  confirmModal: null,   // { title, message, onConfirm, isDangerous }
  openConfirmModal: (config) => set({ confirmModal: config }),
  closeConfirmModal: () => set({ confirmModal: null }),
}));

export default useUiStore;
