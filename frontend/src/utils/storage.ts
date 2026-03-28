export const TOKEN_KEY = 'fabric_iot_token';
export const USER_KEY = 'fabric_iot_user';

export interface UserStorage {
  username: string;
  role: string;
  permissions: string[];
}

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export function setUser(user: UserStorage): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function getUser(): UserStorage | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as Partial<UserStorage>;
    if (!parsed.username || !parsed.role) {
      return null;
    }
    return {
      username: parsed.username,
      role: parsed.role,
      permissions: Array.isArray(parsed.permissions) ? parsed.permissions : []
    };
  } catch {
    return null;
  }
}

export function clearUser(): void {
  localStorage.removeItem(USER_KEY);
}
