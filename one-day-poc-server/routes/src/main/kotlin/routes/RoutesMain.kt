package routes

import io.ktor.server.application.Application
import routes.AuthenticationRoutes.configureAuthenticationRoutes
import routes.ChatRoutes.configureChatRoutes

/**
 * Main function to configure all routes in the application.
 * This function is called automatically on Ktor startup and should be used to configure all routes in the application.
 * This function should not be called manually, save for testing purposes.
 * */
fun Application.configureApplicationRoutes() {
    configureAuthenticationRoutes()
    configureChatRoutes()
}
