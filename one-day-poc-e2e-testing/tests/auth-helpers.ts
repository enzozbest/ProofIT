// auth-helpers.ts
import {expect, Page} from '@playwright/test';

export async function loginWithCognitoViaFlow(page: Page): Promise<void> {
    // Navigate to your app's login page
    await page.goto('http://localhost:5173/');
    const buttonSign = page.locator("xpath=//button[contains(@class, 'py-2')]")
    await buttonSign.click()
    const username = page.locator("xpath=//*[@id='formField:R36sn55:']")


    // Fill in credentials from environment variables
    await username.fill(process.env.COGNITO_TEST_USERNAME || '');

    const buttonNext = page.locator("xpath=//button")
    await buttonNext.click()
    
    const password = page.locator("xpath=//*[@id='formField:r0:']")

    await password.fill(process.env.COGNITO_TEST_PASSWORD || '');

    const buttonContinue = page.locator("xpath=//button[@type='submit']")

    // Submit login form
    await buttonContinue.click()

    // Wait for authentication to complete
    await page.waitForURL("http://localhost:5173")
}
