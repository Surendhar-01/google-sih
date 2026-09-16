import { Body, Controller, HttpCode, HttpStatus, Post, UseGuards } from '@nestjs/common';
import { VoiceIntentRouterService, NavigationContext } from './voice-intent-router.service';
import { RoleType } from '../auth/dto/auth.dto';

export class ProcessVoiceDto {
  transcript: string;
  context: NavigationContext;
  userRole?: RoleType;
}

@Controller('api/voice')
export class VoiceController {
  constructor(private readonly voiceIntentRouter: VoiceIntentRouterService) {}

  @Post('intent')
  @HttpCode(HttpStatus.OK)
  async processVoiceIntent(@Body() dto: ProcessVoiceDto) {
    return this.voiceIntentRouter.routeVoiceIntent(dto.transcript, dto.context, dto.userRole);
  }
}
