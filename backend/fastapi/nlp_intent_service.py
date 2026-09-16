"""
ECOBRIDGES Semantic NLP Intent Understanding Layer
Processes natural language in English, Hindi, Marathi, and Hinglish across all application workflows.
Maps conversational utterances to precise statutory actions, role access, and entity parameters.
"""

import re
import logging
from typing import Dict, Any, Optional

logger = logging.getLogger("NlpIntentService")

# Material category synonyms in English, Hindi, Marathi, and Hinglish
MATERIAL_SYNONYMS = {
    "PCB_BOARDS": [
        "motherboard", "pcb", "circuit board", "motherboards", "green board",
        "मदरबोर्ड", "सर्किट बोर्ड", "पीसीबी", "circuit"
    ],
    "COPPER_WIRE": [
        "copper", "copper wire", "cable", "tamba", "taamba", "cables",
        "तांबा", "तांबे", "कॉपर वायर", "केबल", "तांब्याची तार"
    ],
    "BATTERIES": [
        "battery", "lithium", "li-ion", "cell", "batteries", "lead",
        "बैटरी", "लिथियम", "बॅटरी"
    ],
    "CRT_MONITORS": [
        "crt", "monitor", "tv tube", "screen", "glass monitor",
        "सीआरटी", "मॉनिटर", "टीव्ही स्क्रीन"
    ],
    "DISPLAY_PANELS": [
        "lcd", "led", "display", "screen panel", "mobile display",
        "एलसीडी", "एलईडी", "डिस्प्ले"
    ],
    "MIXED_PLASTICS": [
        "plastic", "casing", "body", "mixed plastic",
        "प्लास्टिक", "कचरा बॉडी"
    ],
    "FERROUS_METALS": [
        "iron", "steel", "loha", "metal frame", "casing metal",
        "लोहा", "लोखंड", "स्टील"
    ]
}

class NlpIntentService:
    def __init__(self):
        logger.info("Initializing ECOBRIDGES Multilingual NLP Intent Service...")

    def parse_intent(
        self,
        transcript: str,
        current_screen: str = "INTRO",
        current_role: Optional[str] = None,
        language: str = "en"
    ) -> Dict[str, Any]:
        """
        Parses transcript and returns structured semantic intent, target role,
        entity parameters, and localized spoken feedback.
        """
        clean_text = transcript.strip()
        lower_text = clean_text.lower()

        # 1. Check for Login Navigation Intents (English / Hindi / Marathi / Hinglish)
        login_intent = self._check_login_intent(lower_text)
        if login_intent:
            return login_intent

        # 2. Check for Material Price Queries
        price_intent = self._check_price_query(lower_text)
        if price_intent:
            return price_intent

        # 3. Check for Hazard / Environmental Safety Queries
        safety_intent = self._check_safety_query(lower_text)
        if safety_intent:
            return safety_intent

        # 4. Check for Action Submissions / Verifications
        if any(w in lower_text for w in ["submit", "verify", "confirm", "pukka", "hota", "done", "होय", "हाँ", "पुष्टी"]):
            return {
                "intent": "SUBMIT_CONFIRMATION",
                "confidence": 0.95,
                "requiresConfirmation": False,
                "spokenFeedback": {
                    "en": "Submitting and verifying current transaction.",
                    "hi": "वर्तमान लेनदेन की पुष्टि और सत्यापन किया जा रहा है।",
                    "mr": "सध्याच्या व्यवहाराची पुष्टी आणि पडताळणी केली जात आहे."
                }
            }

        # 5. Check for Sign out / Destructive requests
        if any(w in lower_text for w in ["logout", "sign out", "exit", "band karo", "बाहेर पडा"]):
            return {
                "intent": "SIGN_OUT",
                "confidence": 0.98,
                "requiresConfirmation": True,
                "spokenFeedback": {
                    "en": "Are you sure you want to sign out of the ECOBRIDGES portal?",
                    "hi": "क्या आप वाकई इकोब्रिज पोर्टल से साइन आउट करना चाहते हैं?",
                    "mr": "तुम्हाला नक्की इकोब्रिज पोर्टलवरून बाहेर पडायचे आहे का?"
                }
            }

        # Default fallback
        return {
            "intent": "UNKNOWN",
            "confidence": 0.45,
            "params": {"raw": clean_text},
            "spokenFeedback": {
                "en": "I understood your request. You can say 'Open collector login', 'Check copper rates', or select an option.",
                "hi": "मैंने आपकी बात सुनी। आप 'कलेक्टर लॉगिन खोलो' या 'तांबे का भाव बताओ' कह सकते हैं।",
                "mr": "मी ऐकले. तुम्ही 'कलेक्टर लॉगिन उघडा' किंवा 'तांब्याचा भाव तपासा' म्हणू शकता."
            }
        }

    def _check_login_intent(self, text: str) -> Optional[Dict[str, Any]]:
        # Phrasings in English, Hindi, Marathi, Hinglish indicating an intent to open/access login
        login_triggers = [
            "login", "log in", "signin", "sign in", "open", "access", "go to",
            "kholo", "khola", "chalna", "jaana", "aana", "jayche", "ugada", "उघडा", "खोलो", "लॉगिन"
        ]

        is_login_phrase = any(trig in text for trig in login_triggers)
        if not is_login_phrase:
            return None

        # Determine target role
        target_role = "INFORMAL_COLLECTOR"
        role_label = "Informal Collector"

        if any(w in text for w in ["recycler", "factory", "facility", "plant", "रीसायकलर", "पुनर्प्रक्रिया"]):
            target_role = "FORMAL_RECYCLER"
            role_label = "Formal Recycler"
        elif any(w in text for w in ["government", "admin", "officer", "cpcb", "moefcc", "सरकारी", "प्रशासन"]):
            target_role = "GOVERNMENT_ADMIN"
            role_label = "Government Admin"
        elif any(w in text for w in ["collector", "scrap", "kabadi", "aggregator", "कलेक्टर", "संकलन"]):
            target_role = "INFORMAL_COLLECTOR"
            role_label = "Informal Collector"

        return {
            "intent": "OPEN_LOGIN",
            "targetRole": target_role,
            "confidence": 0.98,
            "requiresConfirmation": False,
            "params": {
                "targetRole": target_role,
                "authMethods": ["MOBILE_OTP", "EMAIL_PASSWORD", "GOOGLE_OAUTH"]
            },
            "spokenFeedback": {
                "en": f"Opening {role_label} login page. Please choose Mobile OTP, Email, or Google Sign-In.",
                "hi": f"{role_label} लॉगिन पेज खोल रहे हैं। कृपया मोबाइल ओटीपी, ईमेल या गूगल साइन-इन चुनें।",
                "mr": f"{role_label} लॉगिन पृष्ठ उघडत आहे. कृपया मोबाईल ओटीपी, ईमेल किंवा गुगल साइन-इन निवडा."
            }
        }

    def _check_price_query(self, text: str) -> Optional[Dict[str, Any]]:
        price_triggers = ["price", "rate", "bhav", "kimat", "cost", "how much", "kitna", "kiti", "भाव", "किंमत", "दर"]
        if not any(trig in text for trig in price_triggers):
            return None

        # Detect material category
        detected_category = "COPPER_WIRE"
        for cat, synonyms in MATERIAL_SYNONYMS.items():
            if any(syn in text for syn in synonyms):
                detected_category = cat
                break

        return {
            "intent": "QUERY_MATERIAL_PRICE",
            "targetRole": "INFORMAL_COLLECTOR",
            "confidence": 0.95,
            "params": {
                "materialCategory": detected_category
            },
            "spokenFeedback": {
                "en": f"The formal CPCB authorized rate for {detected_category.replace('_', ' ').lower()} is actively updated on your Price Board.",
                "hi": f"{detected_category} के लिए अधिकृत सीपीसीबी मूल्य आपके प्राइस बोर्ड पर प्रदर्शित है।",
                "mr": f"{detected_category} साठी अधिकृत सीपीसीबी दर आपल्या दर फलकावर उपलब्ध आहेत."
            }
        }

    def _check_safety_query(self, text: str) -> Optional[Dict[str, Any]]:
        safety_triggers = ["burn", "fire", "acid", "leach", "smoke", "toxic", "safe", "danger", "धुआं", "जलाना", "सुरक्षा", "धोकादायक"]
        if not any(trig in text for trig in safety_triggers):
            return None

        return {
            "intent": "QUERY_HAZARD_SAFETY",
            "confidence": 0.96,
            "spokenFeedback": {
                "en": "Statutory warning: Open cable burning and acid bath extraction are banned under Rule 13. Transfer all scrap to authorized formal recyclers.",
                "hi": "वैधानिक चेतावनी: खुले में तार जलाना और एसिड लीचिंग प्रतिबंधित है। सभी सामग्री अधिकृत रीसायकलर्स को ही सौंपें।",
                "mr": "वैधानिक चेतावणी: उघड्यावर केबल्स जाळणे आणि ऍसिड वापरणे प्रतिबंधित आहे. अधिकृत पुनर्प्रक्रिया केंद्राकडेच माल सुपूर्द करा."
            }
        }
