"""
ECOBRIDGES Voice & Speech Processing Engine (FastAPI)
Provides high-performance speech-to-text (Whisper/faster-whisper) and
multilingual NLP intent understanding (Hindi, Marathi, English, Hinglish).
"""

import os
import base64
import logging
from fastapi import FastAPI, HTTPException, UploadFile, File, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any

from whisper_service import WhisperSpeechService
from nlp_intent_service import NlpIntentService

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("FastAPIVoice")

app = FastAPI(
    title="ECOBRIDGES Multilingual Voice & Intent Service",
    version="1.0.0",
    description="FastAPI service configuring Whisper and NLP layers for Hindi, Marathi, English, and Hinglish speech processing."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

whisper_service = WhisperSpeechService()
nlp_service = NlpIntentService()

class SemanticIntentRequest(BaseModel):
    transcript: str
    language: Optional[str] = "en"
    currentScreen: Optional[str] = "INTRO"
    currentRole: Optional[str] = None

class Base64AudioRequest(BaseModel):
    audioBase64: str
    language: Optional[str] = None
    currentScreen: Optional[str] = "INTRO"
    currentRole: Optional[str] = None

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "ECOBRIDGES FastAPI Voice Layer",
        "whisperReady": whisper_service.model is not None,
        "supportedLanguages": ["hi", "mr", "en", "hinglish"]
    }

@app.post("/api/v1/voice/transcribe")
async def transcribe_audio(
    file: UploadFile = File(...),
    language: Optional[str] = Form(None)
):
    """
    Transcribes uploaded audio files using Whisper STT layer configured for native
    Hindi, Marathi, English, and Hinglish code-switching.
    """
    try:
        audio_content = await file.read()
        result = whisper_service.transcribe_audio(audio_content, preferred_language=language)
        return result
    except Exception as e:
        logger.error(f"Failed to transcribe audio file: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/v1/voice/semantic-intent")
def analyze_semantic_intent(request: SemanticIntentRequest):
    """
    NLP layer that maps natural language transcripts to precise semantic intents,
    role targets, and localized spoken responses.
    """
    result = nlp_service.parse_intent(
        transcript=request.transcript,
        current_screen=request.currentScreen or "INTRO",
        current_role=request.currentRole,
        language=request.language or "en"
    )
    return result

@app.post("/api/v1/voice/process-audio")
async def process_full_voice_pipeline(request: Base64AudioRequest):
    """
    Complete end-to-end voice pipeline:
    Base64 Audio -> Whisper STT (Hindi/Marathi/Hinglish/En) -> NLP Intent -> Action Decision
    """
    try:
        audio_bytes = base64.b64decode(request.audioBase64)
        transcription = whisper_service.transcribe_audio(audio_bytes, preferred_language=request.language)
        
        intent_result = nlp_service.parse_intent(
            transcript=transcription.get("text", ""),
            current_screen=request.currentScreen or "INTRO",
            current_role=request.currentRole,
            language=transcription.get("language", request.language or "en")
        )

        return {
            "transcription": transcription,
            "intentAnalysis": intent_result,
            "detectedLanguage": transcription.get("language"),
            "isHinglish": transcription.get("is_hinglish", False)
        }
    except Exception as e:
        logger.error(f"Error in full voice pipeline: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
