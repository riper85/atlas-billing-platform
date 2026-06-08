# Runbook — Local Build Failure

## Java version failure

Run:

```bash
java -version
/usr/libexec/java_home -V
cat ~/.m2/toolchains.xml
```

Expected: Java 25.

## Maven version failure

Run:

```bash
mvn -version
```

Expected: Maven 3.9.x or newer.

## Formatting failure

Run:

```bash
make format
```

Then retry:

```bash
make verify
```

## Dependency Check is slow

The first run can be slow because the vulnerability database must be downloaded. Retry once before changing configuration.

## Gitleaks failure

Inspect the reported file. If it is a real secret, rotate it and remove it from Git history before continuing. If it is a false positive, add the smallest possible allowlist entry in `.gitleaks.toml`.
