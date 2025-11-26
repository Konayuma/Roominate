# Roominate — quick developer guide

Roominate is the Android client and server codebase for a room/rental listing and booking product. This README is a short, practical entry point for developers who want to run, test and contribute to the project locally.

## Project layout (quick)
- `app/` — Android application source and Gradle project
- `supabase/` — Supabase database + Edge Functions (TypeScript)
- `supabase-functions/` — other function utilities
- `docs/` — feature docs, testing guides, troubleshooting and implementation notes

## Prerequisites
- Java JDK 11+ and Android SDK (use Android Studio recommended)
- Git
- (Optional) Supabase CLI / Deno for local testing of edge functions

## Quick start (Windows PowerShell)
1. Clone and open the project in Android Studio or from terminal:

```powershell
git clone <repo-url>
cd Roominate
```

2. Build the Android app (debug):

```powershell
cd app
.\gradlew.bat assembleDebug
```

3. Run unit tests (Android JVM tests):

```powershell
cd app
.\gradlew.bat test
```

4. Run or test Supabase edge functions locally (recommended):

- Install and configure the Supabase CLI: https://supabase.com/docs/guides/cli
- Use `supabase start` and consult `supabase/functions/*/README.md` for function-specific env variables.

## Environment / secrets
- Local secrets (Supabase keys, API tokens) should never be committed — use `local.properties`, `.env` files and `supabase secrets` features for local dev. See `docs/` for environment examples.

## Tests & verification
- Automated tests live under `app/src/test` and `app/src/androidTest`. See `docs/TESTING_AND_VERIFICATION_GUIDE.md` for manual verification and test policies.

## Contributing
- Work on a feature branch, add tests for behavior you change, and open a pull request explaining the design and test plan.
- Keep changes focused and add small, descriptive commits.

## Next actions you will find in this repo
- `docs/` — start here for detailed feature guides, implementation notes and deployment checklists.

If you want a more opinionated developer guide (CI setup, local supabase+emulator scripts, or contribution checklist), tell me what you'd prefer and I can add it.
