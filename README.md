# API Builder Builds

GitHub Actions repository for building Java JAR files from API definitions stored in Cloudflare R2.

## Overview

This repository contains a GitHub Actions workflow that:

1. **Checkout** – Uses the Gradle template in this repo (build.gradle, gradlew, lib/, common Java classes)
2. **Download** – Fetches generated package files from R2 (`packages/{definitionId}/`)
3. **Build** – Replaces placeholders, compiles with Gradle, produces JAR
4. **Upload** – Uploads JAR to R2 (`jars/{definitionId}/{runId}_{jarName}`)
5. **Callback** – Optionally notifies Cloudflare Workers when build completes

## Workflow

The `build-jar.yml` workflow is triggered via GitHub API (workflow_dispatch) with the following inputs:

- `jobId`: Build job ID
- `definitionId`: API Definition ID
- `usePreviewBucket`: Use preview R2 bucket (set automatically by API Builder when running locally)
- `callbackUrl`: Cloudflare callback URL (optional)
- `callbackToken`: Callback authentication token (optional)

## Setup

### Required GitHub Secrets

Configure these secrets in the repository settings:

- `R2_ACCESS_KEY_ID`: R2 Access Key (production bucket)
- `R2_SECRET_ACCESS_KEY`: R2 Secret Key (production bucket)
- `R2_ACCESS_KEY_ID_PREVIEW`: R2 Access Key for preview bucket (optional; when set, used when usePreviewBucket=true)
- `R2_SECRET_ACCESS_KEY_PREVIEW`: R2 Secret Key for preview bucket (optional; use with R2_ACCESS_KEY_ID_PREVIEW)
- `R2_ENDPOINT`: R2 Endpoint URL (e.g., `https://<account-id>.r2.cloudflarestorage.com`)
- **Bucket** (pick one; required):
  - `R2_BUCKET_NAME` (variable, recommended): Repo **Settings > Secrets and variables > Actions > Variables**. Add variable `R2_BUCKET_NAME` = `api-bldr-data-cache-r2` (prod) or `api-bldr-data-cache-r2-preview` (local).
  - `R2_BUCKET_BASE` (secret): Base name `api-bldr-data-cache-r2`; workflow adds `-preview` when usePreviewBucket=true
  - `R2_BUCKET_NAME` (secret): Full bucket name as fallback
- `CLOUDFLARE_CALLBACK_TOKEN`: Token for webhook callbacks (optional)
- `CLOUDFLARE_CALLBACK_URL`: Webhook URL (optional)

### R2 Package Layout

The workflow expects these files in R2 under `packages/{definitionId}/`:

- `field-mapping.json` – Contains `apiPack`, `version`, `handlers` array
- `servlet.java` – Main servlet class
- `FieldMapper.java` – Field mapper class
- `handlers/{HandlerName}.java` – One file per handler (e.g. `TriWorkTaskHandler.java`)
- `openapi.json` – OpenAPI spec; packaged into JAR for `/doc` and `/openapi.json` endpoints

## Usage

This workflow is triggered automatically by the API Builder when package generation completes. It can also be triggered manually from the GitHub Actions UI for testing.

## Notes

- This repository includes the Gradle template (build files, gradlew, lib/, common classes)
- Generated API-specific files (servlet, FieldMapper, handlers) are downloaded from R2
- Placeholders in build.gradle and settings.gradle are replaced at runtime
- Builds run in the repo root; output JAR is uploaded to R2
