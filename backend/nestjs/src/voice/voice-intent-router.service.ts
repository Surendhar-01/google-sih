import { Injectable, Logger } from '@nestjs/common';
import axios from 'axios';
import { RoleType } from '../auth/dto/auth.dto';

export enum SemanticIntent {
  OPEN_LOGIN = 'OPEN_LOGIN',
  NAVIGATE_DASHBOARD = 'NAVIGATE_DASHBOARD',
  QUERY_MATERIAL_PRICE = 'QUERY_MATERIAL_PRICE',
  QUERY_HAZARD_SAFETY = 'QUERY_HAZARD_SAFETY',
  CREATE_MATERIAL_LOT = 'CREATE_MATERIAL_LOT',
  SUBMIT_CONFIRMATION = 'SUBMIT_CONFIRMATION',
  CANCEL_ACTION = 'CANCEL_ACTION',
  SIGN_OUT = 'SIGN_OUT',
  CHANGE_LANGUAGE = 'CHANGE_LANGUAGE',
  HELP_EXPLANATION = 'HELP_EXPLANATION',
  UNKNOWN = 'UNKNOWN',
}

export interface NavigationContext {
  currentScreen: string;
  currentRole?: RoleType;
  isAuthenticated: boolean;
  activeLanguage: 'en' | 'hi' | 'mr';
}

export interface VoiceRouteDecision {
  intent: SemanticIntent;
  confidence: number;
  targetRole?: RoleType;
  navigationTarget?: string;
  isAuthorized: boolean;
  authorizationReason?: string;
  requiresConfirmation: boolean;
  spokenFeedback: {
    en: string;
    hi: string;
    mr: string;
  };
  extractedParams?: Record<string, any>;
}

@Injectable()
export class VoiceIntentRouterService {
  private readonly logger = new Logger(VoiceIntentRouterService.name);
  private readonly fastApiVoiceUrl: string;

  constructor() {
    this.fastApiVoiceUrl = process.env.FASTAPI_VOICE_URL || 'http://localhost:8000';
    this.logger.log(`VoiceIntentRouter initialized with FastAPI bridge at: ${this.fastApiVoiceUrl}`);
  }

  /**
   * Centralized Entry Point:
   * Maps natural language input -> calls FastAPI NLP layer -> applies role authorization -> determines context-aware navigation
   */
  async routeVoiceIntent(
    transcript: string,
    context: NavigationContext,
    userRole?: RoleType
  ): Promise<VoiceRouteDecision> {
    const cleanText = transcript.trim();
    this.logger.log(`Routing voice transcript: "${cleanText}" in context: ${context.currentScreen}`);

    // 1. Attempt to consult the FastAPI NLP layer
    let nlpResult: any = null;
    try {
      const response = await axios.post(
        `${this.fastApiVoiceUrl}/api/v1/voice/semantic-intent`,
        {
          transcript: cleanText,
          language: context.activeLanguage,
          currentScreen: context.currentScreen,
          currentRole: userRole || context.currentRole,
        },
        { timeout: 3500 }
      );
      nlpResult = response.data;
    } catch (err: any) {
      this.logger.warn(`FastAPI NLP service unavailable (${err.message}). Using native resilient semantic parser.`);
      nlpResult = this.nativeSemanticIntentParser(cleanText, context);
    }

    // 2. Perform Role-Based Authorization on Extracted Intent
    const authorizationCheck = this.verifyRoleAuthorization(nlpResult.intent, nlpResult.targetRole, userRole || context.currentRole);

    // 3. Resolve Context-Aware Navigation Target
    const navigationTarget = this.resolveNavigationTarget(nlpResult.intent, nlpResult.targetRole, context);

    return {
      intent: nlpResult.intent,
      confidence: nlpResult.confidence || 0.95,
      targetRole: nlpResult.targetRole,
      navigationTarget,
      isAuthorized: authorizationCheck.isAuthorized,
      authorizationReason: authorizationCheck.reason,
      requiresConfirmation: nlpResult.requiresConfirmation || false,
      spokenFeedback: nlpResult.spokenFeedback || this.generateDefaultSpokenFeedback(nlpResult.intent, nlpResult.targetRole),
      extractedParams: nlpResult.params || {},
    };
  }

  /**
   * Role-Based Authorization Enforcement:
   * Ensures users cannot access unauthorized features or role actions.
   */
  private verifyRoleAuthorization(
    intent: SemanticIntent,
    targetRole?: RoleType,
    currentRole?: RoleType
  ): { isAuthorized: boolean; reason?: string } {
    // Open login is accessible to everyone
    if (intent === SemanticIntent.OPEN_LOGIN || intent === SemanticIntent.HELP_EXPLANATION || intent === SemanticIntent.CHANGE_LANGUAGE) {
      return { isAuthorized: true };
    }

    // Role-specific action restrictions
    if (intent === SemanticIntent.CREATE_MATERIAL_LOT && currentRole !== RoleType.INFORMAL_COLLECTOR) {
      return {
        isAuthorized: false,
        reason: 'Creating material lots is restricted to registered Informal Collectors.',
      };
    }

    if (intent === SemanticIntent.NAVIGATE_DASHBOARD && targetRole && currentRole && targetRole !== currentRole) {
      return {
        isAuthorized: false,
        reason: `Your current credentials (${currentRole}) do not have clearance for ${targetRole} dashboard.`,
      };
    }

    return { isAuthorized: true };
  }

  /**
   * Context-Aware Navigation Logic:
   * Dynamically determines destination route based on current screen, intent, and role.
   */
  private resolveNavigationTarget(
    intent: SemanticIntent,
    targetRole?: RoleType,
    context?: NavigationContext
  ): string {
    switch (intent) {
      case SemanticIntent.OPEN_LOGIN:
        switch (targetRole) {
          case RoleType.INFORMAL_COLLECTOR:
            return '/auth/collector';
          case RoleType.FORMAL_RECYCLER:
            return '/auth/recycler';
          case RoleType.GOVERNMENT_ADMIN:
            return '/auth/government';
          default:
            return '/auth/selection';
        }

      case SemanticIntent.NAVIGATE_DASHBOARD:
        switch (targetRole || context?.currentRole) {
          case RoleType.INFORMAL_COLLECTOR:
            return '/collector/dashboard';
          case RoleType.FORMAL_RECYCLER:
            return '/recycler/portal';
          case RoleType.GOVERNMENT_ADMIN:
            return '/government/oversight';
          default:
            return '/';
        }

      case SemanticIntent.QUERY_MATERIAL_PRICE:
        return '/collector/dashboard?tab=rates';

      case SemanticIntent.QUERY_HAZARD_SAFETY:
        return '/collector/dashboard?tab=safety';

      case SemanticIntent.SIGN_OUT:
        return '/';

      default:
        return context?.currentScreen || '/';
    }
  }

  /**
   * Resilient Fallback Semantic Parser:
   * Understands natural speech in English, Hindi, Marathi, and Hinglish.
   */
  private nativeSemanticIntentParser(text: string, context: NavigationContext): any {
    const lower = text.toLowerCase();

    // 1. Login intents (English, Hindi, Marathi, Hinglish)
    const isLogin =
      lower.includes('login') ||
      lower.includes('open') ||
      lower.includes('access') ||
      lower.includes('signin') ||
      lower.includes('khola') ||
      lower.includes('kholo') ||
      lower.includes('jaana hai') ||
      lower.includes('jayche ahe') ||
      lower.includes('उघडा') ||
      lower.includes('खोलो');

    // Role detection
    let detectedRole: RoleType | undefined;
    if (lower.includes('collector') || lower.includes('scrap') || lower.includes('kabadi') || lower.includes('कलेक्टर') || lower.includes('संकलन')) {
      detectedRole = RoleType.INFORMAL_COLLECTOR;
    } else if (lower.includes('recycler') || lower.includes('factory') || lower.includes('facility') || lower.includes('रीसायकलर') || lower.includes('पुनर्प्रक्रिया')) {
      detectedRole = RoleType.FORMAL_RECYCLER;
    } else if (lower.includes('government') || lower.includes('admin') || lower.includes('officer') || lower.includes('cpcb') || lower.includes('सरकारी') || lower.includes('प्रशासन')) {
      detectedRole = RoleType.GOVERNMENT_ADMIN;
    }

    if (isLogin) {
      return {
        intent: SemanticIntent.OPEN_LOGIN,
        confidence: 0.96,
        targetRole: detectedRole || RoleType.INFORMAL_COLLECTOR,
        spokenFeedback: {
          en: `Opening ${detectedRole || 'role'} login portal.`,
          hi: `${detectedRole || 'रोल'} लॉगिन पोर्टल खोला जा रहा है।`,
          mr: `${detectedRole || 'भूमिका'} लॉगिन पोर्टल उघडत आहे.`,
        },
      };
    }

    // 2. Price Queries
    if (lower.includes('price') || lower.includes('rate') || lower.includes('bhav') || lower.includes('kimat') || lower.includes('भाव') || lower.includes('किंमत')) {
      return {
        intent: SemanticIntent.QUERY_MATERIAL_PRICE,
        confidence: 0.94,
        targetRole: RoleType.INFORMAL_COLLECTOR,
        spokenFeedback: {
          en: 'Displaying latest CPCB authorized material scrap rates.',
          hi: 'नवीनतम सीपीसीबी अधिकृत सामग्री दर दिखाए जा रहे हैं।',
          mr: 'नवीनतम सीपीसीबी अधिकृत साहित्य दर दाखवत आहे.',
        },
      };
    }

    // 3. Safety Queries
    if (lower.includes('burn') || lower.includes('safety') || lower.includes('acid') || lower.includes('hazard') || lower.includes('धुआं') || lower.includes('सुरक्षा')) {
      return {
        intent: SemanticIntent.QUERY_HAZARD_SAFETY,
        confidence: 0.95,
        spokenFeedback: {
          en: 'Never burn cables or leach with acid. Always route to authorized formal recyclers.',
          hi: 'केबल्स को कभी न जलाएं और एसिड लीचिंग न करें। हमेशा अधिकृत रीसायकलर को दें।',
          mr: 'केबल्स कधीही जाळू नका किंवा ऍसिड वापरू नका. अधिकृत पुनर्प्रक्रिया केंद्राकडे द्या.',
        },
      };
    }

    // 4. Sign out
    if (lower.includes('logout') || lower.includes('sign out') || lower.includes('exit') || lower.includes('बाहेर पडा')) {
      return {
        intent: SemanticIntent.SIGN_OUT,
        confidence: 0.98,
        requiresConfirmation: true,
        spokenFeedback: {
          en: 'Are you sure you want to sign out of your account?',
          hi: 'क्या आप वाकई अपने खाते से साइन आउट करना चाहते हैं?',
          mr: 'तुम्हाला तुमच्या खात्यातून बाहेर पडायचे आहे का?',
        },
      };
    }

    return {
      intent: SemanticIntent.UNKNOWN,
      confidence: 0.5,
      spokenFeedback: {
        en: "I understood your words. Say 'Open collector login', 'Check rates', or tap a button.",
        hi: 'बोलने के लिए धन्यवाद। आप "कलेक्टर लॉगिन खोलो" या "दर देखें" कह सकते हैं।',
        mr: 'धन्यवाद. तुम्ही "कलेक्टर लॉगिन उघडा" किंवा "दर तपासा" म्हणू शकता.',
      },
    };
  }

  private generateDefaultSpokenFeedback(intent: SemanticIntent, role?: RoleType): any {
    return {
      en: `Action ${intent} processed.`,
      hi: `कार्रवाई ${intent} पूरी हुई।`,
      mr: `कृती ${intent} पूर्ण झाली.`,
    };
  }
}
