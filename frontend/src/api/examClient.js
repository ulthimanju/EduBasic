import { createAuthenticatedClient } from './createClient';
import { EXAM_API_BASE_URL } from '../config/runtimeConfig';

const examClient = createAuthenticatedClient(EXAM_API_BASE_URL, {
  withCredentials: true,
  withRefreshQueue: true
});

export default examClient;
