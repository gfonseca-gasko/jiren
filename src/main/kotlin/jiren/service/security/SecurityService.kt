package jiren.service.security

import com.vaadin.flow.component.UI
import com.vaadin.flow.server.VaadinServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler

class SecurityService {
    val authenticatedUser: UserDetails?
        get() {
            val context = SecurityContextHolder.getContext()
            val principal = context.authentication?.principal
            return if (principal is UserDetails) {
                context.authentication.principal as UserDetails
            } else null
        }

    fun logout() {
        UI.getCurrent().page.setLocation(LOGOUT_SUCCESS_URL)
        val logoutHandler = SecurityContextLogoutHandler()
        logoutHandler.logout(
            VaadinServletRequest.getCurrent().httpServletRequest, null,
            null
        )
    }

    companion object {
        private const val LOGOUT_SUCCESS_URL = "/"
    }
}