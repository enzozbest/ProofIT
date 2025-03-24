// auth-helpers.ts
import {expect, Page} from '@playwright/test';

export async function loginWithCognitoViaFlow(page: Page): Promise<void> {
    // Navigate to your app's login page
    await page.goto('/');
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
    await page.waitForURL("http://localhost:5173/")
}

export async function logoutWithCognitoViaFlow(page: Page): Promise<void> {
    await page.goto('/');
    const buttonSignOut = page.locator("xpath=//button[contains(@class, 'py-2')]")
    await buttonSignOut.click()
    await clearAllStorageMechanisms(page.context(), page)
    await expect(page).toHaveURL('')
    await expect(buttonSignOut).toHaveText("Sign In")
}

async function clearAllStorageMechanisms(context, page) {
    // 1. Clear cookies
    await context.clearCookies();

    // 2. Clear localStorage (Cognito stores tokens here)
    await page.evaluate(() => {
        localStorage.clear();
    });

    // 3. Clear sessionStorage
    await page.evaluate(() => {
        sessionStorage.clear();
    });

    // 4. Clear IndexedDB (some Cognito implementations use this)
    await page.evaluate(() => {
        indexedDB.databases().then(databases => {
            databases.forEach(database => {
                indexedDB.deleteDatabase(database.name);
            });
        });
    });

    // 5. Clear the Authentication Cache specifically
    await page.evaluate(() => {
        // Check if the Credential Management API is available
        if (typeof navigator.credentials !== 'undefined' &&
            typeof navigator.credentials.preventSilentAccess === 'function') {
            navigator.credentials.preventSilentAccess().catch(err => console.error('Error preventing silent access:', err));
        }
    });

    // 6. Clear service workers that might be caching auth state
    await page.evaluate(() => {
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.getRegistrations().then(registrations => {
                for (let registration of registrations) {
                    registration.unregister();
                }
            });
        }
    });

    console.log('All storage mechanisms cleared');
}
