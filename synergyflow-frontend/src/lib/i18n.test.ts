import { describe, it, expect } from 'vitest';
import { i18nClient, DEFAULT_LANGUAGE, SUPPORTED_LANGUAGES } from './i18n';

describe('I18n Client', () => {
  it('should have default language', () => {
    expect(i18nClient.getCurrentLanguage()).toBe(DEFAULT_LANGUAGE);
  });

  it('should have supported languages', () => {
    const languages = i18nClient.getSupportedLanguages();
    expect(languages).toEqual(SUPPORTED_LANGUAGES);
  });

  it('should be able to set language', () => {
    i18nClient.setCurrentLanguage('es');
    expect(i18nClient.getCurrentLanguage()).toBe('es');
  });

  it('should be able to toggle theme', () => {
    const initialTheme = i18nClient.getCurrentTheme();
    const newTheme = i18nClient.toggleTheme();
    expect(newTheme).not.toBe(initialTheme);
  });
});