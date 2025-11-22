# Project documentation — quick index

This folder is the single place to find design notes, operational checklists, testing instructions, and implementation details used by the engineering team.

How to use this index

- If you need to understand or change behavior in the app, search here first for an implementation guide (eg. geocoding, payments, notifications).
- If you need to test or verify a change, open `TESTING_AND_VERIFICATION_GUIDE.md`.
- If you're onboarding or triaging work items, start at `IMPLEMENTATION_STATUS.md` or `FEATURE_STATUS.md`.

Top links (quick)

- `TESTING_AND_VERIFICATION_GUIDE.md` — test plans, manual verification, and acceptance criteria
- `IMPLEMENTATION_STATUS.md` — which features are complete / in-progress / blocked
- `FEATURE_STATUS.md` — per-feature implementation notes and owners
- `QUICK_REFERENCE.md` — fast dev commands, useful environment tips and local-run shortcuts

Useful areas

- Implementation guides: GEOCODING_*, LENCO_PAYMENT_*, OTP_PASSWORD_RESET_IMPLEMENTATION.md, NOTIFICATIONS_*
- Fixes & retrospectives: BUG_FIXES_SUMMARY.md, ROLE_ASSIGNMENT_BUG_FIX.md, CONFIRMED_BOOKING_FIX.md
- UX and testing: PROFILE_UX_FIXES.md, UI_UX_FIXES_SUMMARY.md, UX_UI_IMPROVEMENTS_SUMMARY.md

Contributing docs

- Keep docs focused and link to supporting code files or commits. Add a short acceptance/test checklist.
- New docs should live under `docs/` and use an explanatory filename. When updating a doc that changes behavior add a brief `## Changes` section with links to the PR/commit.

Search tips

- Use your editor to search across `docs/` for keywords (eg. "geocoding", "Resend", "OTP"). Many guides are small and purpose-driven.

Next actions (for maintainers)

- If you’re standardizing the docs further I can help: add front-matter metadata, convert to a small mkdocs site, or add a single consolidated changelog.

