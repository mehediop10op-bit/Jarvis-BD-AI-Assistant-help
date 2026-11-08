# Jarvis – BD AI Assistant Backend Proxy

A lightweight, secure Node.js Express backend proxy that shields the Google Gemini API key from the Android client APK. The client app speaks directly to this backend over HTTPS, and this service securely invokes Gemini using server-side environment secrets.

---

## 📁 Project Structure

```
backend/
├── Dockerfile          # Production Docker configuration for Cloud Run
├── .dockerignore      # Files ignored during Docker build
├── .env.example       # Example environment secrets
├── .gitignore         # Git ignore rules
├── package.json       # Node.js dependencies and scripts
├── server.js          # Express app with /chat proxy endpoint
└── README.md          # Deployment & setup documentation
```

---

## 🚀 API Endpoints

### 1. Health Check
`GET /health`

**Response:**
```json
{
  "status": "ok",
  "service": "jarvis-backend-proxy",
  "uptime": 12.34,
  "timestamp": "2026-11-08T09:00:00.000Z"
}
```

### 2. Chat Proxy
`POST /chat`

**Headers:**
`Content-Type: application/json`

**Request Body:**
```json
{
  "query": "আজকের আবহাওয়া কেমন?",
  "language": "bn",
  "history": [
    { "sender": "USER", "text": "হাই জারভিস" },
    { "sender": "JARVIS", "text": "জি, বলুন! আমি শুনছি।" }
  ]
}
```

**Response (200 OK):**
```json
{
  "response": "আজকের আবহাওয়া মনোরম ও শান্ত রয়েছে।",
  "text": "আজকের আবহাওয়া মনোরম ও শান্ত রয়েছে।",
  "reply": "আজকের আবহাওয়া মনোরম ও শান্ত রয়েছে।"
}
```

---

## 🛠 Local Setup & Running

1. **Install dependencies:**
   ```bash
   cd backend
   npm install
   ```

2. **Configure environment:**
   Create a `.env` file from `.env.example`:
   ```bash
   cp .env.example .env
   ```
   Add your Gemini API Key in `.env`:
   ```env
   GEMINI_API_KEY=AIzaSy...your_gemini_key
   PORT=8080
   ```

3. **Start the server:**
   ```bash
   npm start
   ```

4. **Test the endpoint:**
   ```bash
   curl -X POST http://localhost:8080/chat \
     -H "Content-Type: application/json" \
     -d '{"query": "কেমন আছো?", "language": "bn"}'
   ```

---

## ☁️ Deployment Guide

### Option 1: Deploy to Google Cloud Run (Recommended)

Google Cloud Run provides serverless, auto-scaling HTTPS endpoints ideal for Android backends.

#### Step 1: Install Google Cloud SDK and Authenticate
```bash
gcloud auth login
gcloud config set project YOUR_GCP_PROJECT_ID
```

#### Step 2: Store Gemini API Key in Google Secret Manager
```bash
echo -n "YOUR_GEMINI_API_KEY" | gcloud secrets create gemini-api-key --data-file=-
```

#### Step 3: Deploy Directly from Source
Navigate to the `backend` folder and run:
```bash
cd backend
gcloud run deploy jarvis-proxy \
  --source . \
  --region asia-southeast1 \
  --allow-unauthenticated \
  --set-secrets GEMINI_API_KEY=gemini-api-key:latest \
  --min-instances 0 \
  --max-instances 10 \
  --memory 512Mi \
  --cpu 1
```

Once deployment finishes, Cloud Run will print your Service URL:
```
Service URL: https://jarvis-proxy-xxxx-as.a.run.app
```

---

### Option 2: Deploy to Render / Railway

1. Push this repository to GitHub.
2. In **Render** or **Railway**, create a new **Web Service** connected to your repo.
3. Set the **Root Directory** to `backend`.
4. Build command: `npm install`
5. Start command: `npm start`
6. Add Environment Variable:
   - Key: `GEMINI_API_KEY`
   - Value: `AIzaSy...`
7. Copy the generated HTTPS URL (e.g., `https://jarvis-proxy.onrender.com`).

---

## 📱 Connecting with Jarvis Android App

1. Open **Jarvis – BD AI Assistant** on your Android phone.
2. Navigate to **সেটিংস (Settings)**.
3. Scroll down to **AI ব্যাকএন্ড আর্কিটেকচার ও প্রক্সি (AI Backend Architecture & Proxy)**.
4. Paste your deployed backend endpoint into the URL field:
   ```
   https://jarvis-proxy-xxxx-as.a.run.app/chat
   ```
5. All future voice and text queries will now route through your private cloud backend with **zero exposure** of your Google Gemini API key inside the client APK.
