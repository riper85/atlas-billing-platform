# Java 25 Toolchain Governance

## Policy

Java 25 is the only supported runtime and compile target for this repository.

## Enforced by

- `.java-version`
- Maven Compiler Plugin `release=25`
- Maven Enforcer `requireJavaVersion [25,26)`
- Maven Toolchains Plugin requiring a Java 25 toolchain
- CI setup using Java 25

## Local setup

1. Install Java 25.
2. Copy `devops/maven-toolchains.xml.example` to `~/.m2/toolchains.xml`.
3. Replace `jdkHome` with your Java 25 path.
4. Run `make doctor`.
5. Run `make verify`.

## Why this is strict

Silent JDK drift creates false build confidence. A developer using Java 21 should fail immediately instead of discovering incompatibility later.
