// Basic i18n loader and theme preference persistence

// Supported languages
export const SUPPORTED_LANGUAGES = [
  { code: 'en', name: 'English' },
  { code: 'es', name: 'Español' },
  { code: 'fr', name: 'Français' },
];

// Default language
export const DEFAULT_LANGUAGE = 'en';

// Theme types
export type Theme = 'light' | 'dark';

class I18nClient {
  private currentLanguage: string;
  private currentTheme: Theme;

  constructor() {
    this.currentLanguage = DEFAULT_LANGUAGE;
    this.currentTheme = 'light';
  }

  // Get current language
  public getCurrentLanguage(): string {
    return this.currentLanguage;
  }

  // Set current language
  public setCurrentLanguage(language: string): void {
    if (SUPPORTED_LANGUAGES.some(lang => lang.code === language)) {
      this.currentLanguage = language;
      // In a real implementation, we would persist this to localStorage/cookies
      console.log(`Language set to: ${language}`);
    }
  }

  // Get available languages
  public getSupportedLanguages(): typeof SUPPORTED_LANGUAGES {
    return SUPPORTED_LANGUAGES;
  }

  // Get current theme
  public getCurrentTheme(): Theme {
    return this.currentTheme;
  }

  // Set current theme
  public setCurrentTheme(theme: Theme): void {
    this.currentTheme = theme;
    // In a real implementation, we would persist this to localStorage/cookies
    console.log(`Theme set to: ${theme}`);
  }

  // Toggle theme
  public toggleTheme(): Theme {
    this.currentTheme = this.currentTheme === 'light' ? 'dark' : 'light';
    console.log(`Theme toggled to: ${this.currentTheme}`);
    return this.currentTheme;
  }

  // Get translation for a key (stub implementation)
  public t(key: string): string {
    // In a real implementation, this would load translations from files
    return key;
  }
}

export const i18nClient = new I18nClient();