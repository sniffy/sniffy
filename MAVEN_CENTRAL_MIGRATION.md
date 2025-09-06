# Maven Central Repository Migration

This document describes the migration from the deprecated OSSRH (OSS Repository Hosting) system to the new Maven Central infrastructure.

## Overview

On June 30, 2025, OSSRH reached end of life and was shut down. All OSSRH namespaces have been migrated to Central Publisher Portal. This project has been updated to use the new infrastructure.

## Changes Made

### Maven Configuration Updates

1. **Repository URLs**: Updated from `oss.sonatype.org` to `s01.oss.sonatype.org`
   - Snapshot repository: `https://s01.oss.sonatype.org/content/repositories/snapshots`
   - Staging repository: `https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/`

2. **Plugin Updates**: 
   - Updated nexus-staging-maven-plugin from version 1.6.8 to 1.7.0
   - Updated server IDs from `sonatype-nexus-snapshots` to `central`

### GitHub Actions Updates

1. **Server Configuration**: Updated `server-id` in GitHub Actions workflow to `central`
2. **Secret Names**: Updated secret references for compatibility with Central Portal:
   - `OSSRH_USERNAME` → `CENTRAL_TOKEN_USERNAME`
   - `OSSRH_TOKEN` → `CENTRAL_TOKEN_PASSWORD`

## Required Secrets Configuration

The following GitHub Secrets need to be configured in the repository settings:

- `CENTRAL_TOKEN_USERNAME`: Your Central Portal username
- `CENTRAL_TOKEN_PASSWORD`: Your Central Portal token (generated from the Portal)
- `SIGNING_KEY`: GPG private key for artifact signing
- `PASSPHRASE`: GPG passphrase

## Migration Strategy

This migration uses the transitional approach of moving to the newer OSSRH instance (`s01.oss.sonatype.org`) which provides compatibility with existing workflows while preparing for the full Central Portal migration.

## Compatibility

- ✅ Maintains existing Maven build process
- ✅ Compatible with existing CI/CD workflows
- ✅ Supports both snapshot and release deployments
- ✅ Maintains GPG signing for security

## Future Considerations

Once the Central Portal APIs are fully stabilized, consider migrating to the new `central-publishing-maven-plugin` for a more streamlined deployment process.