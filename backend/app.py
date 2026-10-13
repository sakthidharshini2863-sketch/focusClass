#!/usr/bin/env python3
"""
FocusClass Backend Server
Flask + SQLite REST API and Teacher Web Dashboard
Provides class-wide focus session management, student enrollment,
and Android device restriction synchronization.
"""

import os
import json
import sqlite3
import datetime
import hashlib
from flask import Flask, request, jsonify, render_template_string

app = Flask(__name__)

# Allow CORS for external API clients
@app.after_request
def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Headers'] = 'Content-Type,Authorization'
    response.headers['Access-Control-Allow-Methods'] = 'GET,POST,PUT,DELETE,OPTIONS'
    return response

DB_PATH = os.path.join(os.path.dirname(__file__), 'focusclass.db')

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def hash_password(password: str) -> str:
    return hashlib.sha256(password.strip().encode('utf-8')).hexdigest()

def init_db():
    conn = get_db()
    cursor = conn.cursor()

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        email TEXT UNIQUE NOT NULL,
        role TEXT NOT NULL CHECK(role IN ('teacher', 'student')),
        register_no TEXT,
        department TEXT,
        year TEXT,
        class_name TEXT,
        password_hash TEXT NOT NULL,
        created_at TEXT NOT NULL
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        class_name TEXT NOT NULL UNIQUE,
        teacher_id INTEGER,
        teacher_name TEXT,
        status TEXT NOT NULL CHECK(status IN ('ACTIVE', 'BREAK', 'ENDED')),
        start_time TEXT,
        end_time TEXT,
        restricted_apps TEXT, -- JSON array of app packages
        updated_at TEXT NOT NULL
    )
    """)

    cursor.execute("""
    CREATE TABLE IF NOT EXISTS device_status (
        device_id TEXT PRIMARY KEY,
        student_id INTEGER,
        student_name TEXT,
        register_no TEXT,
        class_name TEXT NOT NULL,
        is_accessibility_enabled INTEGER DEFAULT 0,
        is_overlay_enabled INTEGER DEFAULT 0,
        is_restricted INTEGER DEFAULT 0,
        last_ping TEXT NOT NULL
    )
    """)

    # Seed pre-authorized teachers if not exist
    teachers = [
        ("Prof. John Smith", "prof.smith@college.edu", "Computer Science & Engineering", hash_password("admin123")),
        ("Dr. S. Raj", "dr.raj@college.edu", "Computer Science & Engineering", hash_password("focus2026"))
    ]
    for name, email, dept, p_hash in teachers:
        cursor.execute("SELECT id FROM users WHERE email = ?", (email,))
        if not cursor.fetchone():
            cursor.execute("""
                INSERT INTO users (name, email, role, department, password_hash, created_at)
                VALUES (?, ?, 'teacher', ?, ?, ?)
            """, (name, email, dept, p_hash, datetime.datetime.now().isoformat()))

    # Seed default sample students: two in CSE-A, one in CSE-B
    students = [
        ("Alex Kumar", "alex.cse@college.edu", "21CS001", "Computer Science", "3rd Year", "CSE-A", hash_password("student123")),
        ("Priya Sharma", "priya.cse@college.edu", "21CS002", "Computer Science", "3rd Year", "CSE-A", hash_password("student123")),
        ("Rahul Verma", "rahul.cse@college.edu", "21CS045", "Computer Science", "3rd Year", "CSE-B", hash_password("student123"))
    ]
    for name, email, reg, dept, yr, cls, p_hash in students:
        cursor.execute("SELECT id FROM users WHERE email = ?", (email,))
        if not cursor.fetchone():
            cursor.execute("""
                INSERT INTO users (name, email, role, register_no, department, year, class_name, password_hash, created_at)
                VALUES (?, ?, 'student', ?, ?, ?, ?, ?, ?)
            """, (name, email, reg, dept, yr, cls, p_hash, datetime.datetime.now().isoformat()))

    conn.commit()
    conn.close()

# ----------------- AUTH APIS -----------------

@app.route('/api/auth/login', methods=['POST'])
def login():
    data = request.get_json(force=True) or {}
    email = data.get('email', '').strip().lower()
    password = data.get('password', '').strip()

    if not email or not password:
        return jsonify({"success": False, "message": "Email and password are required"}), 400

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM users WHERE LOWER(email) = ?", (email,))
    user = cursor.fetchone()
    conn.close()

    if not user:
        return jsonify({"success": False, "message": "Account not found with this email"}), 404

    if user['password_hash'] != hash_password(password):
        return jsonify({"success": False, "message": "Invalid password"}), 401

    user_dict = {
        "id": user['id'],
        "name": user['name'],
        "email": user['email'],
        "role": user['role'],
        "register_no": user['register_no'] or "",
        "department": user['department'] or "",
        "year": user['year'] or "",
        "class_name": user['class_name'] or ""
    }

    return jsonify({
        "success": True,
        "user": user_dict,
        "token": f"token_{user['id']}_{int(datetime.datetime.now().timestamp())}"
    })

@app.route('/api/auth/register-student', methods=['POST'])
def register_student():
    data = request.get_json(force=True) or {}
    name = data.get('name', '').strip()
    email = data.get('email', '').strip().lower()
    reg_no = data.get('register_no', '').strip()
    dept = data.get('department', '').strip()
    year = data.get('year', '').strip()
    class_name = data.get('class_name', '').strip().upper()
    password = data.get('password', '').strip()

    # Rule: Students cannot register as teachers
    role = data.get('role', 'student').lower()
    if role == 'teacher':
        return jsonify({"success": False, "message": "Unauthorized. Teacher accounts cannot be registered self-service."}), 403

    if not all([name, email, reg_no, dept, year, class_name, password]):
        return jsonify({"success": False, "message": "All fields are required"}), 400

    conn = get_db()
    cursor = conn.cursor()
    try:
        cursor.execute("""
            INSERT INTO users (name, email, role, register_no, department, year, class_name, password_hash, created_at)
            VALUES (?, ?, 'student', ?, ?, ?, ?, ?, ?)
        """, (name, email, reg_no, dept, year, class_name, hash_password(password), datetime.datetime.now().isoformat()))
        user_id = cursor.lastrowid
        conn.commit()
    except sqlite3.IntegrityError:
        conn.close()
        return jsonify({"success": False, "message": "An account with this college email already exists"}), 409

    conn.close()
    return jsonify({
        "success": True,
        "message": "Student registered successfully",
        "user": {
            "id": user_id,
            "name": name,
            "email": email,
            "role": "student",
            "register_no": reg_no,
            "department": dept,
            "year": year,
            "class_name": class_name
        }
    }), 201

# ----------------- TEACHER APIS -----------------

@app.route('/api/teacher/classes', methods=['GET'])
def get_classes():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
        SELECT class_name, COUNT(*) as enrolled_count
        FROM users
        WHERE role = 'student' AND class_name IS NOT NULL AND class_name != ''
        GROUP BY class_name
        ORDER BY class_name ASC
    """)
    rows = cursor.fetchall()
    conn.close()

    classes = []
    default_classes = ["CSE-A", "CSE-B", "ECE-A", "IT-A"]
    found_classes = {r['class_name'] for r in rows}

    for r in rows:
        classes.append({"class_name": r['class_name'], "enrolled_count": r['enrolled_count']})

    for c in default_classes:
        if c not in found_classes:
            classes.append({"class_name": c, "enrolled_count": 0})

    return jsonify({"classes": classes})

@app.route('/api/teacher/class/<class_name>/students', methods=['GET'])
def get_class_students(class_name):
    class_name = class_name.strip().upper()
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
        SELECT u.id, u.name, u.email, u.register_no, u.department, u.year, u.class_name,
               d.device_id, d.is_accessibility_enabled, d.is_overlay_enabled, d.is_restricted, d.last_ping
        FROM users u
        LEFT JOIN device_status d ON u.id = d.student_id
        WHERE u.role = 'student' AND UPPER(u.class_name) = ?
        ORDER BY u.register_no ASC
    """, (class_name,))
    students = []
    for r in cursor.fetchall():
        students.append({
            "id": r['id'],
            "name": r['name'],
            "email": r['email'],
            "register_no": r['register_no'],
            "department": r['department'],
            "year": r['year'],
            "class_name": r['class_name'],
            "device_id": r['device_id'] or "Not paired yet",
            "is_accessibility_enabled": bool(r['is_accessibility_enabled']),
            "is_overlay_enabled": bool(r['is_overlay_enabled']),
            "is_restricted": bool(r['is_restricted']),
            "last_ping": r['last_ping'] or "Never"
        })
    conn.close()
    return jsonify({"class_name": class_name, "students": students})

@app.route('/api/teacher/session', methods=['POST'])
def manage_session():
    data = request.get_json(force=True) or {}
    class_name = data.get('class_name', '').strip().upper()
    action = data.get('action', '').strip().upper()  # 'START', 'BREAK', 'RESUME', 'END'
    restricted_apps = data.get('restricted_apps', [])
    start_time = data.get('start_time', '')
    end_time = data.get('end_time', '')
    teacher_id = data.get('teacher_id', 1)
    teacher_name = data.get('teacher_name', 'Faculty Member')

    if not class_name or not action:
        return jsonify({"success": False, "message": "class_name and action required"}), 400

    now_iso = datetime.datetime.now().isoformat()
    conn = get_db()
    cursor = conn.cursor()

    if action == 'START':
        status = 'ACTIVE'
        if not start_time:
            start_time = datetime.datetime.now().strftime("%I:%M %p")
        if not end_time:
            # Default 50 minutes class
            end_time = (datetime.datetime.now() + datetime.timedelta(minutes=50)).strftime("%I:%M %p")

        cursor.execute("""
            INSERT INTO sessions (class_name, teacher_id, teacher_name, status, start_time, end_time, restricted_apps, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(class_name) DO UPDATE SET
                teacher_id = excluded.teacher_id,
                teacher_name = excluded.teacher_name,
                status = excluded.status,
                start_time = excluded.start_time,
                end_time = excluded.end_time,
                restricted_apps = excluded.restricted_apps,
                updated_at = excluded.updated_at
        """, (class_name, teacher_id, teacher_name, status, start_time, end_time, json.dumps(restricted_apps), now_iso))

    elif action == 'BREAK':
        status = 'BREAK'
        cursor.execute("""
            UPDATE sessions SET status = 'BREAK', updated_at = ? WHERE UPPER(class_name) = ?
        """, (now_iso, class_name))

    elif action == 'RESUME':
        status = 'ACTIVE'
        cursor.execute("""
            UPDATE sessions SET status = 'ACTIVE', updated_at = ? WHERE UPPER(class_name) = ?
        """, (now_iso, class_name))

    elif action == 'END':
        status = 'ENDED'
        cursor.execute("""
            UPDATE sessions SET status = 'ENDED', updated_at = ? WHERE UPPER(class_name) = ?
        """, (now_iso, class_name))
    else:
        conn.close()
        return jsonify({"success": False, "message": f"Unknown action: {action}"}), 400

    conn.commit()

    # Retrieve updated session
    cursor.execute("SELECT * FROM sessions WHERE UPPER(class_name) = ?", (class_name,))
    row = cursor.fetchone()
    conn.close()

    return jsonify({
        "success": True,
        "message": f"Focus session for {class_name} updated to {status}",
        "session": {
            "class_name": row['class_name'],
            "status": row['status'],
            "start_time": row['start_time'],
            "end_time": row['end_time'],
            "restricted_apps": json.loads(row['restricted_apps'] or '[]'),
            "teacher_name": row['teacher_name']
        }
    })

@app.route('/api/teacher/session/<class_name>', methods=['GET'])
def get_teacher_session(class_name):
    class_name = class_name.strip().upper()
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM sessions WHERE UPPER(class_name) = ?", (class_name,))
    row = cursor.fetchone()
    conn.close()

    if not row:
        return jsonify({
            "class_name": class_name,
            "status": "ENDED",
            "start_time": "",
            "end_time": "",
            "restricted_apps": [],
            "teacher_name": ""
        })

    return jsonify({
        "class_name": row['class_name'],
        "status": row['status'],
        "start_time": row['start_time'] or "",
        "end_time": row['end_time'] or "",
        "restricted_apps": json.loads(row['restricted_apps'] or '[]'),
        "teacher_name": row['teacher_name'] or ""
    })

# ----------------- STUDENT APIS -----------------

@app.route('/api/student/policy/<class_name>', methods=['GET'])
def get_student_policy(class_name):
    class_name = class_name.strip().upper()
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM sessions WHERE UPPER(class_name) = ?", (class_name,))
    row = cursor.fetchone()
    conn.close()

    if not row or row['status'] == 'ENDED':
        return jsonify({
            "class_name": class_name,
            "has_active_session": False,
            "status": "INACTIVE",
            "restricted_apps": [],
            "start_time": "",
            "end_time": "",
            "teacher_name": ""
        })

    is_active = (row['status'] == 'ACTIVE')
    return jsonify({
        "class_name": row['class_name'],
        "has_active_session": is_active,
        "status": row['status'],  # 'ACTIVE' or 'BREAK'
        "restricted_apps": json.loads(row['restricted_apps'] or '[]'),
        "start_time": row['start_time'] or "",
        "end_time": row['end_time'] or "",
        "teacher_name": row['teacher_name'] or "Class Teacher"
    })

@app.route('/api/student/device-status', methods=['POST'])
def update_device_status():
    data = request.get_json(force=True) or {}
    device_id = data.get('device_id', '').strip()
    student_id = data.get('student_id')
    student_name = data.get('student_name', '')
    register_no = data.get('register_no', '')
    class_name = data.get('class_name', '').strip().upper()
    is_accessibility = 1 if data.get('is_accessibility_enabled') else 0
    is_overlay = 1 if data.get('is_overlay_enabled') else 0
    is_restricted = 1 if data.get('is_restricted') else 0

    if not device_id or not class_name:
        return jsonify({"success": False, "message": "device_id and class_name required"}), 400

    now_iso = datetime.datetime.now().isoformat()
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("""
        INSERT INTO device_status (device_id, student_id, student_name, register_no, class_name,
                                   is_accessibility_enabled, is_overlay_enabled, is_restricted, last_ping)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(device_id) DO UPDATE SET
            student_id = excluded.student_id,
            student_name = excluded.student_name,
            register_no = excluded.register_no,
            class_name = excluded.class_name,
            is_accessibility_enabled = excluded.is_accessibility_enabled,
            is_overlay_enabled = excluded.is_overlay_enabled,
            is_restricted = excluded.is_restricted,
            last_ping = excluded.last_ping
    """, (device_id, student_id, student_name, register_no, class_name, is_accessibility, is_overlay, is_restricted, now_iso))
    conn.commit()
    conn.close()

    return jsonify({"success": True, "message": "Device status updated"})

# ----------------- TEACHER WEB DASHBOARD -----------------

HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FocusClass - Teacher Web Console</title>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --primary: #1E40AF;
            --primary-light: #3B82F6;
            --primary-bg: #EFF6FF;
            --bg: #F8FAFC;
            --surface: #FFFFFF;
            --text-main: #0F172A;
            --text-sub: #475569;
            --border: #E2E8F0;
            --success: #16A34A;
            --warning: #D97706;
            --danger: #DC2626;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', sans-serif; }
        body { background-color: var(--bg); color: var(--text-main); min-height: 100vh; padding-bottom: 40px; }
        header { background: var(--primary); color: white; padding: 20px 32px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
        .logo-area { display: flex; align-items: center; gap: 12px; }
        .logo-shield { width: 36px; height: 36px; background: white; color: var(--primary); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 20px; }
        .container { max-width: 1100px; margin: 28px auto; padding: 0 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
        @media(max-width: 860px) { .container { grid-template-columns: 1fr; } }
        .card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        h2 { font-size: 1.15rem; font-weight: 700; color: var(--text-main); margin-bottom: 16px; display: flex; align-items: center; gap: 8px; }
        label { display: block; font-size: 0.85rem; font-weight: 600; color: var(--text-sub); margin-bottom: 6px; }
        select, input[type="text"], input[type="time"] { width: 100%; padding: 10px 14px; border: 1.5px solid var(--border); border-radius: 8px; font-size: 0.95rem; margin-bottom: 16px; outline: none; transition: border-color 0.2s; }
        select:focus, input:focus { border-color: var(--primary); }
        .apps-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 20px; }
        .app-chip { border: 1.5px solid var(--border); padding: 10px 12px; border-radius: 8px; display: flex; align-items: center; gap: 10px; cursor: pointer; transition: all 0.2s; user-select: none; }
        .app-chip.selected { border-color: var(--primary); background: var(--primary-bg); font-weight: 600; color: var(--primary); }
        .app-chip input { margin-right: 4px; }
        .btn-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 12px; }
        button { border: none; padding: 12px 18px; border-radius: 8px; font-weight: 600; font-size: 0.95rem; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 8px; }
        .btn-primary { background: var(--primary); color: white; }
        .btn-primary:hover { background: #1D4ED8; }
        .btn-warning { background: #FEF3C7; color: #92400E; border: 1px solid #FDE68A; }
        .btn-warning:hover { background: #FDE68A; }
        .btn-resume { background: #DCFCE7; color: #166534; border: 1px solid #BBF7D0; }
        .btn-danger { background: #FEE2E2; color: #991B1B; border: 1px solid #FECACA; }
        .status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 6px 14px; border-radius: 20px; font-weight: 600; font-size: 0.85rem; }
        .status-active { background: #DCFCE7; color: #166534; }
        .status-break { background: #FEF3C7; color: #92400E; }
        .status-ended { background: #F1F5F9; color: #64748B; }
        .student-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #F1F5F9; }
        .student-item:last-child { border-bottom: none; }
        .student-info h4 { font-size: 0.95rem; font-weight: 600; }
        .student-info p { font-size: 0.8rem; color: var(--text-sub); }
        .dev-badge { font-size: 0.75rem; padding: 4px 8px; border-radius: 6px; font-weight: 600; }
        .dev-enforced { background: #DCFCE7; color: #166534; }
        .dev-pending { background: #FEF3C7; color: #92400E; }
        .dev-missing { background: #F1F5F9; color: #64748B; }
        .notice-banner { grid-column: 1 / -1; background: #EFF6FF; border: 1px solid #BFDBFE; border-radius: 10px; padding: 16px 20px; font-size: 0.9rem; color: #1E40AF; }
    </style>
</head>
<body>
    <header>
        <div class="logo-area">
            <div class="logo-shield">🛡️</div>
            <div>
                <h1>FocusClass</h1>
                <p style="font-size: 0.8rem; opacity: 0.9;">Teacher Classroom Focus Control System</p>
            </div>
        </div>
        <div>
            <span style="font-size: 0.9rem; background: rgba(255,255,255,0.2); padding: 6px 12px; border-radius: 6px;">Prof. John Smith (CSE Dept)</span>
        </div>
    </header>

    <main class="container">
        <div class="notice-banner">
            <strong>⚡ Class-Wide Enforcement Rule:</strong> When you start a focus session for <strong>CSE-A</strong>, policy applies to all registered CSE-A student devices automatically. Students of <strong>CSE-B</strong> or other sections remain unaffected.
        </div>

        <!-- Left Column: Session Controls -->
        <div class="card">
            <h2>🎯 Focus Session Controller</h2>
            <form id="sessionForm">
                <label>Target Classroom Section</label>
                <select id="classSelect" onchange="loadClassData()">
                    <option value="CSE-A" selected>CSE-A (Classroom 301)</option>
                    <option value="CSE-B">CSE-B (Classroom 302)</option>
                    <option value="ECE-A">ECE-A (Classroom 204)</option>
                    <option value="IT-A">IT-A (Classroom 405)</option>
                </select>

                <label>Select Distracting Apps to Restrict</label>
                <div class="apps-grid">
                    <label class="app-chip selected"><input type="checkbox" name="app" value="com.instagram.android" checked> Instagram</label>
                    <label class="app-chip selected"><input type="checkbox" name="app" value="com.google.android.youtube" checked> YouTube</label>
                    <label class="app-chip selected"><input type="checkbox" name="app" value="com.snapchat.android" checked> Snapchat</label>
                    <label class="app-chip"><input type="checkbox" name="app" value="com.zhiliaoapp.musically"> TikTok</label>
                    <label class="app-chip"><input type="checkbox" name="app" value="com.facebook.katana"> Facebook</label>
                    <label class="app-chip"><input type="checkbox" name="app" value="com.reddit.frontpage"> Reddit</label>
                </div>

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                    <div>
                        <label>Start Time</label>
                        <input type="text" id="startTime" value="09:00 AM">
                    </div>
                    <div>
                        <label>End Time</label>
                        <input type="text" id="endTime" value="10:00 AM">
                    </div>
                </div>

                <div class="btn-row">
                    <button type="button" class="btn-primary" onclick="triggerAction('START')">▶ Start Focus</button>
                    <button type="button" class="btn-warning" onclick="triggerAction('BREAK')">☕ Allow Break</button>
                </div>
                <div class="btn-row">
                    <button type="button" class="btn-resume" onclick="triggerAction('RESUME')">🔁 Resume Focus</button>
                    <button type="button" class="btn-danger" onclick="triggerAction('END')">⏹ End Focus</button>
                </div>
            </form>
        </div>

        <!-- Right Column: Status & Live Enrolled Devices -->
        <div class="card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                <h2>📱 Enrolled Student Devices</h2>
                <span id="sessionStatusBadge" class="status-badge status-ended">NO ACTIVE SESSION</span>
            </div>

            <p style="font-size: 0.85rem; color: var(--text-sub); margin-bottom: 16px;">
                Live restriction sync for students in <strong id="activeClassNameLabel">CSE-A</strong>:
            </p>

            <div id="studentList">
                <!-- Dynamically populated -->
            </div>
        </div>
    </main>

    <script>
        const appNames = {
            'com.instagram.android': 'Instagram',
            'com.google.android.youtube': 'YouTube',
            'com.snapchat.android': 'Snapchat',
            'com.zhiliaoapp.musically': 'TikTok',
            'com.facebook.katana': 'Facebook',
            'com.reddit.frontpage': 'Reddit'
        };

        async function loadClassData() {
            const cls = document.getElementById('classSelect').value;
            document.getElementById('activeClassNameLabel').innerText = cls;

            // Load session status
            try {
                const sRes = await fetch(`/api/teacher/session/${cls}`);
                const session = await sRes.json();
                const badge = document.getElementById('sessionStatusBadge');
                if (session.status === 'ACTIVE') {
                    badge.className = 'status-badge status-active';
                    badge.innerText = `● ACTIVE FOCUS (${session.restricted_apps.length} APPS RESTRICTED)`;
                } else if (session.status === 'BREAK') {
                    badge.className = 'status-badge status-break';
                    badge.innerText = '☕ BREAK IN PROGRESS';
                } else {
                    badge.className = 'status-badge status-ended';
                    badge.innerText = '⏹ SESSION ENDED';
                }
            } catch(e) { console.error(e); }

            // Load student list
            try {
                const stuRes = await fetch(`/api/teacher/class/${cls}/students`);
                const data = await stuRes.json();
                const list = document.getElementById('studentList');
                list.innerHTML = '';
                if (!data.students || data.students.length === 0) {
                    list.innerHTML = '<p style="color:#64748B; font-size:0.9rem; padding:16px 0;">No students enrolled in this section yet.</p>';
                    return;
                }

                data.students.forEach(s => {
                    const item = document.createElement('div');
                    item.className = 'student-item';
                    let statusLabel = 'Offline';
                    let statusClass = 'dev-missing';

                    if (s.is_restricted) {
                        statusLabel = '🛡️ Restriction Enforced';
                        statusClass = 'dev-enforced';
                    } else if (s.is_accessibility_enabled) {
                        statusLabel = 'Ready / Idle';
                        statusClass = 'dev-pending';
                    }

                    item.innerHTML = `
                        <div class="student-info">
                            <h4>${s.name} <span style="font-size:0.8rem; font-weight:normal; color:#64748B;">(${s.register_no})</span></h4>
                            <p>${s.email} • Device: ${s.device_id.substring(0, 14)}...</p>
                        </div>
                        <span class="dev-badge ${statusClass}">${statusLabel}</span>
                    `;
                    list.appendChild(item);
                });
            } catch(e) { console.error(e); }
        }

        async function triggerAction(action) {
            const cls = document.getElementById('classSelect').value;
            const checkboxes = document.querySelectorAll('input[name="app"]:checked');
            const apps = Array.from(checkboxes).map(c => c.value);
            const startTime = document.getElementById('startTime').value;
            const endTime = document.getElementById('endTime').value;

            const payload = {
                class_name: cls,
                action: action,
                restricted_apps: apps,
                start_time: startTime,
                end_time: endTime,
                teacher_id: 1,
                teacher_name: "Prof. John Smith"
            };

            const res = await fetch('/api/teacher/session', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            alert(data.message);
            loadClassData();
        }

        document.querySelectorAll('.app-chip input').forEach(input => {
            input.addEventListener('change', function() {
                this.parentElement.classList.toggle('selected', this.checked);
            });
        });

        loadClassData();
        setInterval(loadClassData, 4000);
    </script>
</body>
</html>
"""

@app.route('/')
@app.route('/teacher')
@app.route('/dashboard')
def teacher_dashboard():
    return render_template_string(HTML_TEMPLATE)

if __name__ == '__main__':
    init_db()
    port = int(os.environ.get('PORT', 5000))
    print(f"Starting FocusClass backend on http://0.0.0.0:{port}")
    app.run(host='0.0.0.0', port=port, debug=True)
