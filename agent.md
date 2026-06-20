# Agent Instructions

## Project Workflow

- After every code or documentation change, create a Git commit and push it to the remote branch.
- For each large feature module, create a dedicated feature branch before implementation.
- After implementing a large module, run the relevant verification commands.
- Merge a feature branch into `main` only after verification passes.
- Keep `main` as the stable integration branch.

## Local Verification

- Backend tests: run `mvn test` from `backend/`.
- Frontend build: run `npm run build` from `frontend/`.
- Runtime smoke check: verify the backend health endpoint at `http://localhost:8080/api/health` and the frontend dev server at `http://localhost:5173`.

## Runtime Notes

- The backend reads local environment values from the project root `.env`.
- The default local database configuration is MySQL on `localhost:3306`, database `ai4note`, user `root`, password `123456`.
- Do not commit local secrets, generated dependencies, build outputs, or runtime uploads.
