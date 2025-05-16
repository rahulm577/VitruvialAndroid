# Vitruvial - HIPAA Compliant Healthcare OCR

A mobile application for securely extracting and processing patient information from medical documents using OCR and Claude AI.

## HIPAA Compliance

This application is designed with HIPAA compliance in mind when used with Anthropic's enterprise tier with a BAA (Business Associate Agreement) in place.

## Setting Up the API Key (Prototype Version)

For this prototype version, the Claude API key is stored directly in the app:

1. Obtain a HIPAA-compliant API key from Anthropic's enterprise services with a BAA in place
2. Open the `app/build.gradle.kts` file
3. Find the line with `buildConfigField("String", "CLAUDE_API_KEY", "\"sk-ant-replace-with-your-actual-key\"")`
4. Replace `sk-ant-replace-with-your-actual-key` with your actual Claude API key
5. Rebuild the application

Example:
```kotlin
buildConfigField("String", "CLAUDE_API_KEY", "\"sk-ant-api1234567890abcdefghijklmnopqrstuvwxyz\"")
```

> **Note:** For production use, a server-based approach that doesn't store the API key in the app would be recommended for better security.

## Features

- Camera and gallery integration for capturing medical documents
- OCR processing using ML Kit
- Extraction of patient information using Claude AI
- Fallback to regex-based extraction if AI processing fails
- HIPAA-compliant when used with appropriate Claude enterprise tier 