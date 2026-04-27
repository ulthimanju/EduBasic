/**
 * Maps API errors to user-friendly messages.
 */
export const getErrorMessage = (error) => {
  if (!error.response) {
    if (error.code === 'ECONNABORTED') return 'Request timed out. Please try again.';
    return 'Network error. Please check your connection.';
  }

  const { status, data } = error.response;
  const message = data?.message || data?.error;

  switch (status) {
    case 401:
      return 'Session expired. Please log in again.';
    case 403:
      return 'You do not have permission to perform this action.';
    case 409:
      return message || 'This action conflicts with an existing record (e.g., already enrolled).';
    case 422:
      return message || 'Validation failed. Please check your input.';
    case 500:
      return 'Internal server error. Please try again later.';
    default:
      return message || 'An unexpected error occurred.';
  }
};

/**
 * Common onError handler for mutations.
 */
export const handleMutationError = (error) => {
  const message = getErrorMessage(error);
  // Using alert for now as no toast system is present. 
  // In a real app, this would trigger a toast notification.
  alert(message);
};
