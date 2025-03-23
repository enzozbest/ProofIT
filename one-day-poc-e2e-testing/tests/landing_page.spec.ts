import {test, expect, Locator} from '@playwright/test';

test('Landing page has the correct title', async ({ page }) => {
    await page.goto('http://localhost:5173');
    await expect(page).toHaveTitle('ProofIT');
});

test('Landing page has sign in button', async ({ page }) => {
    const buttonSign = page.locator("xpath=//button[contains(@class, 'py-2')]")
    await page.goto("http://localhost:5173")

    await expect(buttonSign).toHaveText("Sign In")
    await expect(buttonSign).toBeVisible()
    await expect(buttonSign).toBeEnabled()
    await expect(buttonSign).toHaveAttribute("class", "border-2 border-white bg-transparent px-6 py-2 rounded-full hover:bg-white hover:text-[#731ecb] transition")
})

test('Landing page has pre-set prompt buttons', async ({ page }) => {
    const buttonChatbotAssistantForCustomer = page.locator("xpath=/html/body/div/div/div/div[2]/div/button[1]")
    const buttonDashboardForFinancialReports = page.locator("xpath=/html/body/div/div/div/div[2]/div/button[2]")
    const buttonIntelligentDocumentProcessingTool = page.locator("xpath=/html/body/div/div/div/div[2]/div/button[3]")
    await page.goto("http://localhost:5173")
    await presetPromptAssertions(buttonChatbotAssistantForCustomer, "AI chatbot assistant for customer self-service →")
    await presetPromptAssertions(buttonDashboardForFinancialReports, "Dashboard for financial reports →")
    await presetPromptAssertions(buttonIntelligentDocumentProcessingTool, "Intelligent document processing tool →")
})

async function presetPromptAssertions(button: Locator, buttonText: string): Promise<void> {
    await expect(button).toHaveText(buttonText)
    await expect(button).toBeVisible()
    await expect(button).toBeEnabled()
    await expect(button).toHaveAttribute("class", "border-2 border-white px-6 py-4 rounded-lg bg-transparent  text-center whitespace-nowrap hover:bg-white/20 hover:border-white hover: transition duration-300")
}

test('Landing page has the correct h1', async ({ page }) => {
    const h1EnablingYouFrom = page.locator("xpath=//h1")
    await page.goto("http://localhost:5173")
    await expect(h1EnablingYouFrom).toHaveText("Enabling you from")
})

test('Landing page has the correct h2', async ({ page }) => {
    const dayOne = page.locator("xpath=//h2")
    await page.goto("http://localhost:5173")
    await expect(dayOne).toHaveText("day one")
})


test('Landing page has the correct logo', async ({ page }) => {
    const divLogo = page.locator("xpath=//div[contains(@class, 'text-xl')]")
    const spanProof = page.locator("xpath=//span[@class='font-normal']")
    const spanIT = page.locator("xpath=//span[@class='font-bold']")
    //const svgLucideBolt = page.locator("xpath=/html/body/div/nav/div[1]/svg") USE ONCE LOGO FINALISED

    await page.goto("http://localhost:5173")
    await expect(divLogo).toBeVisible()
    await expect(spanProof).toHaveText("PROOF - IT!")
    await expect(spanProof).toHaveAttribute("class", "font-normal")
    await expect(spanIT).toHaveText("IT!")
    await expect(spanIT).toHaveAttribute("class", "font-bold")
})

test('Landing page has the correct input box', async ({ page }) => {
    const inputBoxDiv = page.locator("xpath=//div[contains(@class, 'px-5')]")
    const textareaNoneWhite = page.locator("xpath=//textarea")
    const sendButton = page.locator("xpath=//button[contains(@class, 'ml-2')]")

    await page.goto("http://localhost:5173")
    await expect(inputBoxDiv).toBeVisible()
    await expect(inputBoxDiv).toHaveAttribute("class", "flex flex-col items-center w-full max-w-5xl border-black rounded-2xl bg-gray-500 bg-opacity-50 px-5 py-5 shadow-lg")
    await expect(textareaNoneWhite).toBeVisible()
    await expect(textareaNoneWhite).toHaveAttribute("class", "flex-1 bg-transparent px-4 py-3 outline-none placeholder-white resize-none overflow-y-auto")
    await expect(textareaNoneWhite).toHaveAttribute("placeholder", "Tell us what we can do for you?")
    await expect(sendButton).toBeVisible()
    await expect(sendButton).toBeEnabled()
    await expect(sendButton).toHaveAttribute("class", "p-3 flex items-center justify-center bg-transparent rounded-full hover:bg-gray-800 transition ml-2")
})

test('Clicking on the sign in button navigates to the sign in page', async ({ page }) => {
    const buttonSign = page.locator("xpath=//button[contains(@class, 'py-2')]")
    await page.goto("http://localhost:5173")
    await buttonSign.click()
    await expect(page).toHaveTitle('Sign-in');
})

test('Clicking on any of the pre-set prompt buttons unauthenticated navigates to the sign in page', async ({ page }) => {
    const buttonChatbotAssistantForCustomer = page.locator("xpath=/html/body/div/div/div/div[2]/div/button[1]")
    const buttonDashboardForFinancialReports = page.locator("xpath=/html/body/div/div/div/div[2]/div/button[2]")
    const buttonIntelligentDocumentProcessingTool = page.locator("xpath=/html/body/div/div/div/div[2]/div/button[3]")
    await page.goto("http://localhost:5173")
    await Promise.all([buttonChatbotAssistantForCustomer.click(), page.waitForNavigation()]);
    await expect(page).toHaveTitle('Sign-in');
    await page.goBack();
    await Promise.all([buttonDashboardForFinancialReports.click(), page.waitForNavigation()]);
    await expect(page).toHaveTitle('Sign-in');
    await page.goBack()
    await Promise.all([buttonIntelligentDocumentProcessingTool.click(), page.waitForNavigation()]);
    await expect(page).toHaveTitle('Sign-in');
})





