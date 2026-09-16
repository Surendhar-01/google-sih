import { Module } from '@nestjs/common';
import { VoiceController } from './voice.controller';
import { VoiceIntentRouterService } from './voice-intent-router.service';

@Module({
  controllers: [VoiceController],
  providers: [VoiceIntentRouterService],
  exports: [VoiceIntentRouterService],
})
export class VoiceModule {}
