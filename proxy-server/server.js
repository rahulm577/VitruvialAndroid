// This is a simple Node.js Express server that serves as a proxy between
// your mobile app and the Anthropic Claude API.
// The actual Claude API key is stored securely on the server side, never in the client.

const express = require('express');
const axios = require('axios');
const dotenv = require('dotenv');
const app = express();

// Load environment variables from .env file
dotenv.config();

// Your Claude API key stored securely on server-side (environment variable)
const CLAUDE_API_KEY = process.env.CLAUDE_API_KEY;
const APP_TOKEN = process.env.APP_TOKEN; // The token your app uses to authenticate

// Make sure API key is available
if (!CLAUDE_API_KEY) {
  console.error('ERROR: Missing CLAUDE_API_KEY environment variable');
  process.exit(1);
}

// Parse JSON request bodies
app.use(express.json());

// Authentication middleware
function authenticate(req, res, next) {
  const authHeader = req.headers.authorization;
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized - Missing or malformed authorization header' });
  }
  
  const token = authHeader.split(' ')[1];
  
  // Validate app token
  if (token !== APP_TOKEN) {
    return res.status(401).json({ error: 'Unauthorized - Invalid token' });
  }
  
  next();
}

// Endpoint to extract patient information using Claude
app.post('/api/claude/extract-patient-info', authenticate, async (req, res) => {
  try {
    const { text } = req.body;
    
    if (!text) {
      return res.status(400).json({ error: 'Missing text field in request body' });
    }

    // Prepare the Claude API request
    const prompt = `
      Extract patient information from the following text that was obtained through OCR from a medical document or patient sticker.
      
      Here's the OCR text:
      ${text}
      
      Please extract and return ONLY the following information in JSON format:
      {
        "firstName": "First name of the patient", 
        "lastName": "Last name of the patient",
        "dateOfBirth": "Date of birth (any format found)",
        "address": "Full address if available",
        "phoneNumber": "Patient's phone number if available",
        "medicareNumber": "Medicare number if available",
        "healthcareFund": "Name of healthcare fund if available",
        "healthcareFundNumber": "Healthcare fund number if available"
      }
      
      If a field is not found, leave it as an empty string. Analyze the text carefully for these details, looking for patterns or labels like "Name:", "DOB:", "Address:", etc.
      Return ONLY the JSON, no other text.
    `;

    // Make the request to Claude API with the secure API key
    const claudeResponse = await axios.post(
      'https://api.anthropic.com/v1/messages',
      {
        model: 'claude-3-sonnet-20240229',
        max_tokens: 1000,
        messages: [
          {
            role: 'system',
            content: 'You are a helpful assistant that extracts structured patient information from OCR text. Only respond with the JSON data, nothing else.'
          },
          {
            role: 'user',
            content: prompt
          }
        ]
      },
      {
        headers: {
          'x-api-key': CLAUDE_API_KEY,
          'anthropic-version': '2023-06-01',
          'Content-Type': 'application/json'
        }
      }
    );

    // Extract the content from Claude response
    const content = claudeResponse.data.content.find(c => c.type === 'text')?.text || '';
    
    // Extract JSON from the response
    const jsonRegex = /\{[\s\S]*\}/;
    const jsonMatch = content.match(jsonRegex);
    
    if (!jsonMatch) {
      return res.status(500).json({ error: 'Failed to extract JSON from Claude response' });
    }
    
    // Parse the JSON
    const patientInfo = JSON.parse(jsonMatch[0]);
    
    // Return the patient info to the mobile app
    return res.json(patientInfo);
    
  } catch (error) {
    console.error('Error processing request:', error);
    
    // Return a clean error message without exposing sensitive information
    return res.status(500).json({ 
      error: 'Failed to process patient information',
      message: error.message
    });
  }
});

// Start the server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Proxy server running on port ${PORT}`);
}); 