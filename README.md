# SkillBridge SA — Professional Career Development Platform

SkillBridge SA is a comprehensive Android application designed to bridge the gap between education and employment for South African youth. It provides a tailored experience for students, graduates, mentors, and employers through a professional, Figma-style interface.

## 🚀 Tech Stack & Backend (API)

- **UI Framework**: Kotlin + Jetpack Compose (Modern Single-Activity Architecture).
- **Layout System**: XML-based layouts integrated via `AndroidView` for high-fidelity "Figma-style" design consistency.
- **Authentication (API)**: **Firebase Authentication** handles the secure registration and login logic, providing a cloud-based identity API.
- **Single Sign-On (SSO)**: Trusted authentication through **Google Account** integration.
- **Local Database**: Raw **SQLite** (`SQLiteOpenHelper`) for high-performance, offline-first data persistence of profiles, messages, and opportunities.
- **Notifications**: Firebase Cloud Messaging (FCM) integration for real-time alerts.
- **Localization**: Full support for English, isiXhosa (`values-xh`), and isiZulu (`values-zu`).

---

## 📁 File Structure Diagram

```text
SkillBridgeSA/
├── app/
│   ├── src/main/java/com/skillbridge/sa/
│   │   ├── data/
│   │   │   ├── local/          # SQLite DB Helpers & Entity Store classes
│   │   │   ├── model/          # Shared Data Models (User, Job, Message, etc.)
│   │   │   ├── repository/     # Repository Pattern (Auth & Data management)
│   │   │   └── notifications/  # FCM Messaging Service logic
│   │   ├── navigation/         # NavGraph, Routes, and Bottom Navigation wiring
│   │   ├── ui/
│   │   │   ├── components/     # Reusable Custom View components
│   │   │   ├── screens/        # Feature-based Screen Implementations
│   │   │   │   ├── admin/      # Admin User Management
│   │   │   │   ├── auth/       # Login, Register, Google SSO
│   │   │   │   ├── dashboard/  # Role-based Home Dashboards
│   │   │   │   ├── explore/    # Job & Talent Search
│   │   │   │   ├── learning/   # Career Pathways & Learning Centre
│   │   │   │   ├── mentorship/ # Mentorship Request/Approval flow
│   │   │   │   ├── messages/   # Chat Bubble Messaging System
│   │   │   │   ├── notifications/# User Alerts History
│   │   │   │   ├── opportunity/# Job Details & Applications
│   │   │   │   └── profile/    # Professional Profile Management
│   │   │   └── theme/          # Custom Color Palette & M3 Theme
│   │   ├── MainActivity.kt     # App Entry Point & Google Sign-In Host
│   │   └── SkillBridgeApp.kt   # Application Singleton & Service Locator
│   └── src/main/res/
│       ├── layout/             # High-Fidelity XML Layouts (Figma Design)
│       ├── drawable/           # Modern Icons (incl. Google Logo) & Backgrounds
│       └── values/             # Professional Colors, Themes, and Multi-language Strings
└── build.gradle.kts            # Project dependencies & build configuration
```

---

## 👥 User Roles & Features

### 🎓 Student
*Primary focus: Career discovery and foundational growth.*
- **Dashboard**: Career overview stats (Applications, Mentors, Opportunities).
- **Explore**: Search for Jobs and Student Internships with keyword filtering.
- **Mentorship**: Find industry mentors and submit guidance requests.
- **Learning Centre**: Access foundational resources (CV Writing, Interview Prep).
- **Career Pathways**: Visual roadmaps for Software Engineering, Data Analysis, etc.
- **Messaging**: Direct communication with Mentors and Employers.

### 📜 Graduate
*Primary focus: Professional entry and skill specialization.*
- **Dashboard**: Professional progress stats.
- **Explore**: Specialized "Graduate Programme" search and entry-level roles.
- **Mentorship**: Connect with Career Coaches for transition guidance.
- **Learning Centre**: Advanced technical resources (Python, Git, Workplace Readiness).
- **Profile**: Detailed sections for Qualification, Skills, and Expertise.

### 🤝 Mentor
*Primary focus: Guidance and talent development.*
- **Dashboard**: Management overview (Pending Requests, Active Mentees).
- **Mentorship Requests**: Review incoming requests from Students/Graduates.
- **Approval Flow**: **Approve** or **Reject** requests based on student profiles.
- **Active Mentees**: Manage ongoing mentorship sessions.
- **Professional Profile**: Showcase Skills, Expertise, Experience, and Availability.

### 🏢 Employer
*Primary focus: Recruitment and talent sourcing.*
- **Dashboard**: Recruitment metrics (Active Posts, Total Applications).
- **Manage Jobs**: Post new opportunities and track existing listings.
- **Talent Search**: Search for qualified Students and Graduates by skills.
- **Review Applications**: Vet candidates and track hiring status.
- **Direct Messaging**: Chat directly with potential hires.

---

## 💎 Key Features
- **Figma-Style UI**: Modern aesthetics with consistent 12dp-20dp rounded corners and professional neutral tones.
- **Bubble Chat**: Real-time feel messaging with distinct incoming/outgoing message styles.
- **Offline-First**: All professional data is accessible without internet after the initial sync.
- **Strict Security**: Role-based access control and strong password validation (Upper/Lower/Numbers/Symbols).
- **Multi-Language**: Instant switching between English, isiXhosa, and isiZulu.

---

## 🛠️ Installation & Setup
1.  Open the project in **Android Studio (Koala/Ladybug)**.
2.  Allow Gradle to sync (Internet required).
3.  **Firebase Setup**: Replace the placeholder `google-services.json` with your real Firebase config file to enable Auth and SSO.
4.  Run on any device with **API 24 (Android 7.0)** or higher.
