# API Builder Builds

GitHub Actions repository for building Java JAR files from API definitions stored in Cloudflare R2.

## Overview

This repository contains a GitHub Actions workflow that:
1. Downloads source code ZIP files from Cloudflare R2
2. Builds Java projects using Gradle
3. Uploads compiled JAR files back to R2
4. Optionally notifies Cloudflare Workers via webhook callback

## Workflow

The `build-jar.yml` workflow is triggered via GitHub API (workflow_dispatch) with the following inputs:

- `jobId`: Build job ID
- `definitionId`: API Definition ID
- `sourceR2Key`: R2 key for source ZIP file
- `callbackUrl`: Cloudflare callback URL (optional)
- `callbackToken`: Callback authentication token (optional)

## Setup

### Required GitHub Secrets

Configure these secrets in the repository settings:

- `R2_ACCOUNT_ID`: Cloudflare R2 Account ID
- `R2_ACCESS_KEY_ID`: R2 Access Key
- `R2_SECRET_ACCESS_KEY`: R2 Secret Key
- `R2_ENDPOINT`: R2 Endpoint URL (e.g., `https://<account-id>.r2.cloudflarestorage.com`)
- `R2_BUCKET_NAME`: R2 Bucket name
- `CLOUDFLARE_CALLBACK_TOKEN`: Token for webhook callbacks (optional)
- `CLOUDFLARE_CALLBACK_URL`: Webhook URL (optional)

### How It Works

1. **Source Download**: Workflow downloads source ZIP from R2 using S3-compatible API
2. **Build**: Extracts ZIP, sets up Java/Gradle environment, and builds the JAR
3. **Upload**: Uploads compiled JAR back to R2
4. **Logs**: Uploads build metadata to R2 for reference
5. **Callback**: Optionally notifies Cloudflare Workers when build completes

## Usage

This workflow is triggered automatically by Cloudflare Workers via GitHub API. It is not intended to be run manually, though it can be triggered manually from the GitHub Actions UI for testing.

## Notes

- This repository is a **build runner only** - it does not store source code
- Source code comes from R2 ZIP files
- Each build runs in an isolated `/tmp/build` directory
- Multiple builds can run concurrently without conflicts
- Builds are automatically cleaned up after completion
