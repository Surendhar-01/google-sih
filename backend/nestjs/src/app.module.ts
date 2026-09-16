import { Module } from '@nestjs/common';
import { AuthModule } from './auth/auth.module';
import { VoiceModule } from './voice/voice.module';

@Module({
  imports: [AuthModule, VoiceModule],
})
export class AppModule {}
