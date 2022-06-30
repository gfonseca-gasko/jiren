package jiren.service.security

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.UIInitEvent
import com.vaadin.flow.server.VaadinServiceInitListener
import jiren.service.controller.UserController
import jiren.data.repository.user.UserRepository
import jiren.service.security.SecurityUtils.isUserLoggedIn
import jiren.view.*
import org.springframework.stereotype.Component

@Component
class ConfigureUIServiceInitListener(var userRepository: UserRepository, var userController: UserController) :
    VaadinServiceInitListener {
    override fun serviceInit(event: ServiceInitEvent) {
        event.source.addUIInitListener { uiEvent: UIInitEvent ->
            val ui = uiEvent.ui
            ui.addBeforeEnterListener { event: BeforeEnterEvent -> beforeEnter(event) }
        }
    }

    private fun beforeEnter(event: BeforeEnterEvent) {

        val username = userRepository.findByUsername(SecurityService().authenticatedUser?.username ?: "")

        if (LoginView::class.java != event.navigationTarget && !isUserLoggedIn && HomeView::class.java != event.navigationTarget) {
            event.rerouteTo(LoginView::class.java)
        }
        if (LoginView::class.java != event.navigationTarget && isUserLoggedIn && userController.needChangePassword(SecurityService().authenticatedUser?.username ?: "")) {
            event.rerouteTo(ChangePasswordView::class.java)
        }
        if (LoginView::class.java == event.navigationTarget && isUserLoggedIn) {
            event.rerouteTo(HomeView::class.java)
        }
        if (UserView::class.java == event.navigationTarget && (username)?.role?.permissions?.find { p -> p.code == UserView::class.simpleName } == null) {
            event.rerouteTo(HomeView::class.java)
        }
        if (SqlView::class.java == event.navigationTarget && (username)?.role?.permissions?.find { p -> p.code == SqlView::class.simpleName } == null) {
            event.rerouteTo(HomeView::class.java)
        }
        if (AutomationView::class.java == event.navigationTarget && (username)?.role?.permissions?.find { p -> p.code == AutomationView::class.simpleName } == null) {
            event.rerouteTo(HomeView::class.java)
        }
        if (MonitoringView::class.java == event.navigationTarget && (username)?.role?.permissions?.find { p -> p.code == MonitoringView::class.simpleName } == null) {
            event.rerouteTo(HomeView::class.java)
        }
        if (ShiftView::class.java == event.navigationTarget && (username)?.role?.permissions?.find { p -> p.code == ShiftView::class.simpleName } == null) {
            event.rerouteTo(HomeView::class.java)
        }
        if (ConfigurationView::class.java == event.navigationTarget && (username)?.role?.permissions?.find { p -> p.code == ConfigurationView::class.simpleName } == null) {
            event.rerouteTo(HomeView::class.java)
        }
        if (LogView::class.java == event.navigationTarget && (username)?.role?.permissions?.find { p -> p.code == LogView::class.simpleName } == null) {
            event.rerouteTo(HomeView::class.java)
        }
    }
}