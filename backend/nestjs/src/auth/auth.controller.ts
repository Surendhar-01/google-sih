import { Body, Controller, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AuthResponse, EmailLoginDto, GoogleSignInDto, RequestOtpDto, VerifyOtpDto } from './dto/auth.dto';

@Controller('api/auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('otp/send')
  @HttpCode(HttpStatus.OK)
  async sendOtp(@Body() dto: RequestOtpDto) {
    return this.authService.requestMobileOtp(dto);
  }

  @Post('otp/verify')
  @HttpCode(HttpStatus.OK)
  async verifyOtp(@Body() dto: VerifyOtpDto): Promise<AuthResponse> {
    return this.authService.verifyMobileOtp(dto);
  }

  @Post('email/login')
  @HttpCode(HttpStatus.OK)
  async loginWithEmail(@Body() dto: EmailLoginDto): Promise<AuthResponse> {
    return this.authService.loginWithEmail(dto);
  }

  @Post('google')
  @HttpCode(HttpStatus.OK)
  async loginWithGoogle(@Body() dto: GoogleSignInDto): Promise<AuthResponse> {
    return this.authService.loginWithGoogle(dto);
  }
}
