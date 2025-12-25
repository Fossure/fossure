# Fossure FastAPI Backend

This directory hosts the new FastAPI backend, separated from the existing Java implementation under `src/main/java/io/github/fossure`.

## Auth & RLS integration

The FastAPI backend validates Supabase GoTrue bearer tokens via `GET /user` on the auth container. Configure the
endpoint through `GOTRUE_URL` (defaults to `http://fossure-auth:9999`). The example `GET /me` route shows how to
attach the dependency.

For PostgREST and FastAPI to share Row Level Security policies, both must provide the same JWT claims:

- `sub`: the user UUID (used to confirm an authenticated request).
- `role`: GoTrue role (typically `authenticated` or `service_role`).

PostgREST automatically maps these to `request.jwt.claim.sub` and `request.jwt.claim.role`. For FastAPI, after
verifying the token, call `set_rls_claims` from `backend/app/db/session.py` so every database session sets these
values with `SET LOCAL`, allowing the RLS policies to apply consistently.
