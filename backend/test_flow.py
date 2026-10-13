#!/usr/bin/env python3
"""
Test script for verifying FocusClass Class-Wide Restriction flow:
- Enrolls two CSE-A students and one CSE-B student
- Teacher creates focus session for CSE-A restricting [Instagram, YouTube, Snapchat]
- Verifies that both CSE-A students receive the active restriction
- Verifies that the CSE-B student is NOT affected
- Verifies Break, Resume, and End Focus cycle
"""

import sys
import json
import unittest
from app import app, init_db, get_db

class FocusClassFlowTestCase(unittest.TestCase):

    def setUp(self):
        app.config['TESTING'] = True
        self.client = app.test_client()
        init_db()

    def test_class_wide_restriction_flow(self):
        print("\n--- Running Class-Wide Restriction Test Flow ---")

        # 1. Verify enrolled students: 2 in CSE-A, 1 in CSE-B
        res_a = self.client.get('/api/teacher/class/CSE-A/students')
        self.assertEqual(res_a.status_code, 200)
        csea_students = res_a.get_json()['students']
        self.assertGreaterEqual(len(csea_students), 2, "Should have at least 2 students in CSE-A")
        print(f"Verified CSE-A enrolled students: {[s['name'] for s in csea_students]}")

        res_b = self.client.get('/api/teacher/class/CSE-B/students')
        self.assertEqual(res_b.status_code, 200)
        cseb_students = res_b.get_json()['students']
        self.assertGreaterEqual(len(cseb_students), 1, "Should have at least 1 student in CSE-B")
        print(f"Verified CSE-B enrolled students: {[s['name'] for s in cseb_students]}")

        # 2. Teacher starts Focus Session for CSE-A
        restricted_apps = [
            "com.instagram.android",
            "com.google.android.youtube",
            "com.snapchat.android"
        ]
        start_payload = {
            "class_name": "CSE-A",
            "action": "START",
            "restricted_apps": restricted_apps,
            "start_time": "10:00 AM",
            "end_time": "11:00 AM",
            "teacher_id": 1,
            "teacher_name": "Prof. John Smith"
        }
        res_start = self.client.post('/api/teacher/session',
                                     data=json.dumps(start_payload),
                                     content_type='application/json')
        self.assertEqual(res_start.status_code, 200)
        print("Teacher started Focus Session for CSE-A with apps: Instagram, YouTube, Snapchat")

        # 3. Query policy for CSE-A students
        policy_a = self.client.get('/api/student/policy/CSE-A').get_json()
        self.assertTrue(policy_a['has_active_session'], "CSE-A should have an active session")
        self.assertEqual(policy_a['status'], "ACTIVE")
        self.assertEqual(policy_a['restricted_apps'], restricted_apps)
        print("-> CSE-A Student 1 & 2: Policy confirmed ACTIVE with 3 restricted apps! [PASSED]")

        # 4. Query policy for CSE-B student (MUST NOT BE AFFECTED)
        policy_b = self.client.get('/api/student/policy/CSE-B').get_json()
        self.assertFalse(policy_b['has_active_session'], "CSE-B should NOT have an active session")
        self.assertEqual(policy_b['status'], "INACTIVE")
        self.assertEqual(len(policy_b['restricted_apps']), 0)
        print("-> CSE-B Student 3: Policy confirmed INACTIVE with 0 restricted apps (Unchanged)! [PASSED]")

        # 5. Teacher triggers "Allow Break" for CSE-A
        break_payload = {"class_name": "CSE-A", "action": "BREAK"}
        self.client.post('/api/teacher/session', data=json.dumps(break_payload), content_type='application/json')
        policy_break = self.client.get('/api/student/policy/CSE-A').get_json()
        self.assertEqual(policy_break['status'], "BREAK")
        print("-> CSE-A: Break allowed successfully. [PASSED]")

        # 6. Teacher triggers "Resume Focus" for CSE-A
        resume_payload = {"class_name": "CSE-A", "action": "RESUME"}
        self.client.post('/api/teacher/session', data=json.dumps(resume_payload), content_type='application/json')
        policy_resume = self.client.get('/api/student/policy/CSE-A').get_json()
        self.assertEqual(policy_resume['status'], "ACTIVE")
        print("-> CSE-A: Session resumed successfully. [PASSED]")

        # 7. Teacher ends focus session
        end_payload = {"class_name": "CSE-A", "action": "END"}
        self.client.post('/api/teacher/session', data=json.dumps(end_payload), content_type='application/json')
        policy_end = self.client.get('/api/student/policy/CSE-A').get_json()
        self.assertEqual(policy_end['status'], "INACTIVE")
        print("-> CSE-A: Session ended. All devices unrestricted. [PASSED]")
        print("--- All Class-Wide Restriction Tests Passed Successfully! ---\n")

if __name__ == '__main__':
    unittest.main()
