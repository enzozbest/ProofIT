interface User {
  name: string;
  email: string;
  dob: string;
  role: string;
  id?: string;
}

class UserService {
  private static instance: UserService;
  private currentUser: User | null = null;
  private loading: boolean = false;
  private error: string | null = null;

  private constructor() {}

  public static getInstance(): UserService {
    if (!UserService.instance) {
      UserService.instance = new UserService();
    }
    return UserService.instance;
  }

  public async fetchUserData(): Promise<User | null> {
    if (this.currentUser) {
      return this.currentUser;
    }

    this.loading = true;
    this.error = null;
    
    try {
      const response = await fetch('http://localhost:8000/api/auth/me', {
        method: 'GET',
        credentials: 'include'
      });
      
      if (!response.ok) {
        throw new Error('Failed to load user data');
      }
      
      const userData = await response.json();
      this.currentUser = userData;
      this.loading = false;
      return userData;
    } catch (err) {
      this.error = err instanceof Error ? err.message : 'Unknown error';
      this.loading = false;
      console.error('Error fetching user data:', err);
      return null;
    }
  }

  public setUser(user: User): void {
    this.currentUser = user;
  }

  public getUser(): User | null {
    return this.currentUser;
  }

  public getUserId(): string {
    return this.currentUser?.id || this.currentUser?.email || "anonymous";
  }
  
  public isLoading(): boolean {
    return this.loading;
  }
  
  public getError(): string | null {
    return this.error;
  }
  
  public clearUser(): void {
    this.currentUser = null;
  }
}

export default UserService.getInstance();