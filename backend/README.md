# FocusClass Backend & Android Restriction System

A classroom distraction management platform built with **Flask + SQLite** and **Android (Kotlin + Jetpack Compose)**.

---

## 1. Backend Architecture & Setup

### Prerequisites
- Python 3.9+
- pip

### Installation & Run
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install Python dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Run the Flask server:
   ```bash
   python3 app.py
   ```
   The backend will automatically initialize `focusclass.db` SQLite database, pre-seed teacher and sample student accounts, and start listening on `http://0.0.0.0:5000`.

4. **Teacher Web Dashboard:**
   Open `http://localhost:5000` in any web browser to access the Teacher Control Console.

---

## 2. Pre-Configured Accounts

### Pre-Authorized Teachers
| Name | College Email | Password | Department |
| :--- | :--- | :--- | :--- |
| **Prof. John Smith** | `prof.smith@college.edu` | `admin123` | Computer Science |
| **Dr. S. Raj** | `dr.raj@college.edu` | `focus2026` | Computer Science |

> *Note: Self-service registration as a Teacher is strictly prevented by API authorization rules.*

### Pre-Enrolled Sample Students
| Name | College Email | Reg No | Class | Password |
| :--- | :--- | :--- | :--- | :--- |
| **Alex Kumar** | `alex.cse@college.edu` | `21CS001` | **CSE-A** | `student123` |
| **Priya Sharma** | `priya.cse@college.edu` | `21CS002` | **CSE-A** | `student123` |
| **Rahul Verma** | `rahul.cse@college.edu` | `21CS045` | **CSE-B** | `student123` |

---

## 3. Class-Wide Restriction Testing

Run the automated flow test:
```bash
python3 backend/test_flow.py
```
This tests:
1. Teacher starts Focus for **CSE-A** with Instagram, YouTube, and Snapchat.
2. Two **CSE-A** students receive active policy with all 3 restricted apps.
3. One **CSE-B** student remains completely **unaffected** (`INACTIVE` policy).
4. Break, Resume, and End actions propagate only to the target class.

---

## 4. Android Restriction Mechanism & OS Limitations

### How Restriction Works on Android
- **Accessibility Service (`FocusAccessibilityService`)**:
  Monitors `TYPE_WINDOW_STATE_CHANGED` events. When a student attempts to open any restricted package (e.g. `com.instagram.android`), the service immediately intercepts the launch, triggers `GLOBAL_ACTION_HOME`, and displays the `FocusBlockActivity` shield.
- **Draw Over Other Apps (`SYSTEM_ALERT_WINDOW`)**:
  Allows the app to display a full-screen blocking overlay above other apps.
- **Why Genuine Device-Level App Killing Is Not Possible in Standard Android**:
  In standard non-rooted consumer Android, third-party apps cannot kill or disable another app's process (`killBackgroundProcesses` is restricted by Android OS sandboxing). The official production approach used by wellbeing apps (AppBlock, StayFree, Forest) is foreground detection via `AccessibilityService` or `UsageStatsManager` with an immediate blocking shield. For institutional/MDM devices, `DevicePolicyManager.setPackagesSuspended()` can suspend packages completely. FocusClass provides the Accessibility Service and overlay mechanism with real permission auditing and clear status reporting.
