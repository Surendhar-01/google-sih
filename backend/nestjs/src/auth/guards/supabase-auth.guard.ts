import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';

@Injectable()
export class SupabaseAuthGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest();
    const authHeader = request.headers['authorization'];

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new UnauthorizedException('Missing or invalid Authorization header with Bearer token.');
    }

    const token = authHeader.split(' ')[1];
    try {
      const decodedJson = Buffer.from(token.replace('sb_bearer_', ''), 'base64').toString('utf-8');
      request.user = JSON.parse(decodedJson);
      return true;
    } catch {
      throw new UnauthorizedException('Invalid or expired Supabase authentication token.');
    }
  }
}
