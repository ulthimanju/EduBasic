import uuid
import random
from locust import HttpUser, task, between, events

class ExamAutosaveUser(HttpUser):
    wait_time = between(25, 35) # Simulate 30s interval with some jitter
    
    def on_start(self):
        """
        Initialization: Create a fake attempt and setup headers
        In a real scenario, this would involve authenticating and starting an actual exam.
        """
        self.student_id = str(uuid.uuid4())
        self.exam_id = str(uuid.uuid4())
        self.attempt_id = str(uuid.uuid4())
        self.version = 0
        
        # Mocking auth token - in reality, you'd perform a login flow
        self.headers = {
            "Authorization": "Bearer mock-token",
            "Content-Type": "application/json"
        }
        
        # Pre-generate some question IDs for the session
        self.question_ids = [str(uuid.uuid4()) for _ in range(20)]

    @task
    def sync_answers(self):
        """
        Simulates the delta autosave call
        """
        # Pick 1-3 'dirty' questions to sync
        dirty_count = random.randint(1, 3)
        dirty_questions = random.sample(self.question_ids, dirty_count)
        
        payload = {
            "version": self.version,
            "answers": {qid: f"Answer text {random.random()}" for qid in dirty_questions}
        }
        
        with self.client.put(
            f"/api/v1/attempts/{self.attempt_id}/sync",
            json=payload,
            headers=self.headers,
            catch_response=True,
            name="/api/v1/attempts/[id]/sync"
        ) as response:
            if response.status_code == 200:
                self.version += 1
                response.success()
            else:
                response.failure(f"Sync failed with status {response.status_code}")

# Instructions for running:
# 1. Install locust: pip install locust
# 2. Run: locust -f load-test-autosave.py --host http://localhost:8081
# 3. Open browser at http://localhost:8089 to start the test
