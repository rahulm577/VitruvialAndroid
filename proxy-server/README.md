# Vitruvial Proxy Server

This server acts as a secure middleware between your Vitruvial mobile app and the Anthropic Claude API, keeping the API key secure.

## Why a Proxy Server?

1. **Security** - Your Claude API key never exists in the mobile app, preventing it from being extracted
2. **Control** - Allows you to monitor and limit API usage
3. **Flexibility** - Can easily swap out APIs or implement caching without changing the mobile app

## Setup Instructions

1. Install dependencies:
   ```
   npm install express axios dotenv
   ```

2. Create `.env` file with your actual values:
   ```
   # Your Anthropic Claude API key (HIPAA-compliant with BAA)
   CLAUDE_API_KEY=sk-ant-your-actual-api-key-here
   
   # App token for basic authentication between your mobile app and this server
   # This should match the APP_TOKEN in your mobile app's ProxyConfig
   APP_TOKEN=app_token_123456
   
   # Server port
   PORT=3000
   ```

3. Start the server:
   ```
   node server.js
   ```

4. Deploy to a secure hosting service (AWS, Google Cloud, etc.)

5. Update your mobile app's `ProxyConfig.kt` file with your deployed server URL and matching app token

## Security Considerations

1. Use HTTPS for all communication
2. Implement rate limiting
3. Consider using a more robust authentication system in production
4. Store the `.env` file securely and never commit it to version control 