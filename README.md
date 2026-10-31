# OmniChat AI

> Android-only, local-first, autonomous AI assistant — chat, multi-model, agents, skills, voice and phone control in one app.

![Platform](https://img.shields.io/badge/platform-Android-green)
![Language](https://img.shields.io/badge/language-Kotlin-purple)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Status](https://img.shields.io/badge/status-planning-orange)

## Status

**v0.1 starter** — Android project skeleton with working chat core (streaming chat, history, API provider manager, secure key storage). Everything else in the blueprint is still to come. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Core Idea (Maximum Freedom)

- AI decides, plans and solves problems on its own
- AI uses agents, skills and tools by itself
- No unnecessary app-imposed limits
- User always stays in control (Emergency Stop, Execution Logs, Permissions)
- Must respect Android OS, API, hardware and legal limits

## Main Features

| Area | Highlights |
|---|---|
| AI Chat | Streaming, history, folders, Markdown, code highlight, LaTeX |
| Providers | OpenAI, Gemini, Claude, DeepSeek, Grok, Mistral, OpenRouter, Groq, Together, Custom / OpenAI-compatible |
| Multi-Model | Run 2–4 models in parallel, compare speed, tokens, cost |
| Skills | Built-in + custom skills, install/update/permissions |
| AI Harness | Agent runtime, tool calling, workflows, multi-agent |
| Autonomous Engine | Self-planning, self-correction, result verification |
| Phone Control | Accessibility-based app control, settings, notifications, files |
| Voice AI | Bangla + English STT/TTS, continuous conversation |
| Image / PDF | OCR, image understanding, PDF Q&A, Image Studio |
| Research | Web search, deep research, citations, PDF report |
| Coding / Data | Code agent, CSV/Excel/JSON analysis, charts |
| Privacy | Android Keystore, guest mode, local data, no mandatory login |

Full list: [docs/FEATURES.md](docs/FEATURES.md)

## Tech Stack

Kotlin · Jetpack Compose · Clean Architecture · Room · Android Keystore · WorkManager · Retrofit (HTTPS) · Kotlin Coroutines · GitHub Actions · APK + AAB

## Build

- Open in Android Studio (JDK 17) and run, or push to GitHub: the **Android CI** workflow builds a debug APK (see Actions → Artifacts).
- Android Studio will generate the Gradle wrapper on first sync; commit `gradlew` and `gradle/wrapper/` afterwards.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md).

## Decisions

- [x] Android only
- [x] Kotlin + Jetpack Compose
- [x] Local-only backend
- [x] Autonomous AI
- [x] Maximum phone control
- [ ] Logo & UI design
- [ ] Final technical specification
- [ ] Coding & APK build

## License

TBD
