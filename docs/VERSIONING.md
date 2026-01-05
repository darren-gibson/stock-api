# Versioning Guide

This project uses automatic version numbering managed through CI/CD pipelines.

## How Versioning Works

The project version is calculated dynamically based on Git state:

### Local Development
- **With Git tag**: Uses the tag version (e.g., `v1.2.3` → `1.2.3`)
- **Without tag**: Uses base version + commit hash (e.g., `1.0.0-dev-abc1234`)
- **Dirty working tree**: Appends `-dirty` suffix (e.g., `1.0.0-dev-abc1234-dirty`)

### CI/CD Releases
- Set via `RELEASE_VERSION` environment variable
- Creates a Git tag automatically
- Builds artifacts with the release version

## Creating a Release

### Manual Release via GitHub Actions

1. Go to **Actions** → **Release** workflow
2. Click **Run workflow**
3. Enter the version (e.g., `1.2.3`) following semantic versioning
4. Select release type: `patch`, `minor`, or `major`
5. Click **Run workflow**

The workflow will:
- Validate the version format
- Build the project with the release version
- Create a Git tag (`v1.2.3`)
- Create a GitHub Release with artifacts
- Upload distribution files

### Semantic Versioning

Follow [semantic versioning](https://semver.org/):
- **Major** (1.0.0 → 2.0.0): Breaking changes
- **Minor** (1.0.0 → 1.1.0): New features, backward compatible
- **Patch** (1.0.0 → 1.0.1): Bug fixes, backward compatible

## Checking Current Version

### Via API Endpoint
```bash
curl http://localhost:8080/info
```

Response:
```json
{
  "service": "Stock API",
  "version": "1.2.3",
  "buildTime": "2026-01-05T10:30:00Z",
  "status": "running"
}
```

### Via Gradle
```bash
./gradlew properties | grep version
```

### In Application Logs
The version is logged at startup:
```
INFO  o.d.s.k.ApplicationKt - Starting Stock API version 1.2.3
```

## Version in JAR Manifest

The version is embedded in the JAR manifest and can be accessed via:
```kotlin
val version = javaClass.getPackage()?.implementationVersion
```

## Building with Specific Version

For local testing with a specific version:
```bash
./gradlew build -PreleaseVersion=1.2.3
```

## First Release

To create your first release:

1. Ensure the code is ready for release
2. Run the GitHub Actions release workflow with version `1.0.0`
3. Future releases increment from there

## Troubleshooting

### Version shows as "unknown" or "dev"
- Check Git repository is initialized
- Ensure commits exist
- For releases, verify `RELEASE_VERSION` env var is set

### Release workflow fails
- Verify version format (must be X.Y.Z)
- Check that the Git tag doesn't already exist
- Ensure GitHub Actions has write permissions

## Configuration

Version calculation is in [build.gradle.kts](../build.gradle.kts):
- `calculateVersion()` function
- Base version: `1.0.0`
- Git commands for tag/commit detection
