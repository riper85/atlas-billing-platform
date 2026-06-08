# Java Preview Feature Policy

## Decision

Preview and incubator features are disabled by default.

## Rule

A module may use preview features only when an ADR explicitly approves it.

The ADR must explain:

- why the preview feature is needed
- which module may use it
- how runtime/test/compiler flags are configured
- how the code will be removed or migrated if the feature changes

## Current Phase

Phase 0 uses no preview features and does not configure `--enable-preview`.
