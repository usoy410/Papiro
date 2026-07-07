# Papiro

Papiro is an expert multi-disciplinary academic companion and smart notebook application for Android. Designed for students, researchers, and professionals, it provides an advanced rich-text markdown engine, robust local storage, and powerful AI-driven capabilities to elevate the note-taking and learning experience.

## Features

### 🧠 AI Coprocessor (Powered by Gemini)
- **AI Note Generation:** Instantly generate highly structured, detailed academic notes on any topic.
- **Smart Note Enhancer:** Improve clarity, grammar, and depth of your existing notes.
- **Interactive Quiz Generator:** Automatically generate interactive Flashcards or Multiple Choice quizzes from your notes. Includes a virtual AI Tutor to answer questions.
- **Document OCR:** Extract text from images using ML Kit (fast, local) or Gemini Vision (cloud, handles complex layouts).
- **Auto Title Generation:** Let AI suggest concise and accurate titles based on the note's content.

### 📝 Advanced Markdown Engine
Papiro utilizes a custom Markdown engine built for rich scientific and academic notes.
- **Rich Text Formatting:** Standard markdown features with hierarchical nested bullets.
- **Mermaid Diagrams:** Render dynamic flowcharts, state diagrams, and architectural graphs directly from markdown blocks (` ```mermaid `).
- **Native Illustrations:** Support for custom JSON coordinate-based drawings (` ```drawing `) for line-art and free-form figures.
- **Mathematical Equations:** Full LaTeX/KaTeX math formula support (`$$E = mc^2$$`).
- **Code Highlighting:** Syntax highlighting for code blocks across various programming languages.
- **Custom Styled Tables:** Create tables with customizable header column background colors.

### 💾 Robust Local Storage & Reliability
- **Room Database:** All notes, metadata, and history are saved securely on your device for offline access.
- **Version History:** Automatically keeps snapshots of your notes when saving, allowing you to easily browse and restore previous versions.
- **Undo / Redo Manager:** Sophisticated explicit and temporal undo/redo tracking within the editor.

### 🎨 Beautiful, Adaptive UI
- **Material Design 3:** Adheres to Android's modern UI guidelines with dynamic theming.
- **Paper Designs:** Choose between different editor backgrounds (Grid, Ruled, Dot, or Blank).
- **Dark & Light Modes:** Customizable app theme settings.

## Getting Started

### Prerequisites
- Android Studio or Google AI Studio Build environment.
- Android SDK.

### Setting up the AI Features
To enable the AI capabilities (Note Generation, AI Refine, Quiz Generator, Gemini Vision OCR):
1. Obtain a **Google Gemini API Key** from [Google AI Studio](https://aistudio.google.com/).
2. Open the **Settings** screen inside the Papiro app.
3. Paste your API Key under the **AI Coprocessor Settings** section.
4. (Optional) You can change the chosen Gemini Model (e.g., `gemini-3.1-flash-lite`) from the Settings page.

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Local Storage:** Room Database
- **Networking:** Retrofit
- **AI Integration:** Google Gemini API
- **Concurrency:** Kotlin Coroutines & Flow
- **Image Loading:** Coil
- **OCR:** ML Kit Text Recognition

## License
This project is for educational and personal use.
