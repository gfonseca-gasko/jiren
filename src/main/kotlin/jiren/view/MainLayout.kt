package jiren.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.html.*
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.AfterNavigationEvent
import com.vaadin.flow.router.AfterNavigationObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import com.vaadin.flow.theme.lumo.Lumo
import jiren.service.controller.UserController
import jiren.data.entity.Permission
import jiren.data.entity.User
import jiren.data.enum.StatusUser
import jiren.service.security.SecurityService
import jiren.service.security.SecurityUtils
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*
import javax.annotation.PostConstruct

@SpringComponent
@UIScope
@CssImport(value = "./styles/default.css")
class MainLayout(private var userController: UserController) : AppLayout(), AfterNavigationObserver {

    private var permissions: MutableList<Permission> = ArrayList()
    private var binder = Binder(User::class.java)
    private val toggle = DrawerToggle()
    private val header = Header()
    private val menuBar = HorizontalLayout()
    private val nav = Nav()
    private val menuList = VerticalLayout()
    private val pageTitle = H5()
    private val ui = UI.getCurrent()

    @PostConstruct
    private fun init() {
        this.primarySection = Section.NAVBAR
        this.isDrawerOpened = false
        this.addToNavbar(true, createHeaderContent())
        this.addToDrawer(createDrawerContent())
    }

    init {
        UI.getCurrent().page.addStyleSheet("https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css")
        this.toggle.isVisible = SecurityUtils.isUserLoggedIn
        this.toggle.addThemeVariants(ButtonVariant.MATERIAL_CONTAINED)
        this.toggle.element.setAttribute("aria-label", "Menu toggle")
    }

    private fun createHeaderContent(): Component {
        createTopMenuBar()
        this.menuBar.alignItems = FlexComponent.Alignment.CENTER
        this.menuBar.setWidthFull()
        this.header.setWidthFull()
        this.header.add(menuBar)
        return header
    }

    private fun createTopMenuBar() {

        val headerLayout = HorizontalLayout(toggle)
        headerLayout.setWidthFull()

        val logoutBtn = Button("Sair", Icon("exit"))
        logoutBtn.isVisible = SecurityUtils.isUserLoggedIn
        logoutBtn.addClickListener { SecurityService().logout() }

        val changePasswordBtn = Button("", Icon("key")) {
            ui.page.open("/changepassword", "_self")
        }
        changePasswordBtn.isVisible = SecurityUtils.isUserLoggedIn
        changePasswordBtn.isEnabled = SecurityUtils.isUserLoggedIn

        val loginBtn = Button("Entrar", Icon("sign-in")) { UI.getCurrent().page.open("/login", "_self") }
        loginBtn.isVisible = !SecurityUtils.isUserLoggedIn

        val login = HorizontalLayout(changePasswordBtn, logoutBtn, loginBtn)
        login.justifyContentMode = FlexComponent.JustifyContentMode.END

        val homeBtn = Button("", Icon("home")) { UI.getCurrent().page.open("/home", "_self") }
        homeBtn.isIconAfterText = true

        val colorBtn = Button("", Icon("adjust")) {
            val themeList = UI.getCurrent().element.themeList
            if (themeList.contains(Lumo.DARK)) {
                themeList.remove(Lumo.DARK)
            } else {
                themeList.add(Lumo.DARK)
            }
        }

        val titleDiv = HorizontalLayout(pageTitle)
        titleDiv.setWidthFull()
        titleDiv.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        headerLayout.add(HorizontalLayout(colorBtn, homeBtn), titleDiv, login)
        headerLayout.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        headerLayout.isSpacing = false
        headerLayout.setWidthFull()
        this.menuBar.add(headerLayout)
    }

    private fun createDrawerContent(): Component {
        return Section(createNavigation(), createFooter())
    }

    private fun createNavigation(): Nav {

        nav.element.setAttribute("aria-labelledby", "views")
        menuList.alignItems = FlexComponent.Alignment.CENTER
        menuList.setWidthFull()
        menuList.add(Image("img/logo-minor.png", "support"))
        nav.add(menuList)
        createLinks()

        val user = userController.findByUsername(SecurityService().authenticatedUser?.username.toString()) ?: return nav

        val statusLabel = Span(user.status.toString().lowercase(Locale.getDefault()))
        statusLabel.addClassNames("badge", getUserBadge(user.status ?: StatusUser.DESCONECTADO))

        val statusPicker = Select<StatusUser>()
        statusPicker.setItems(StatusUser.values().toMutableList())
        statusPicker.placeholder = "Trocar Status"
        statusPicker.value = user.status
        statusPicker.isVisible = false
        this.binder.forField(statusPicker).bind(User::status, User::status.setter)

        statusLabel.addClickListener {
            statusPicker.isVisible = !statusPicker.isVisible
        }

        statusPicker.addValueChangeListener {
            user.status = statusPicker.value as StatusUser
            userController.updateStatus(user)
            ui.access {
                statusLabel.removeClassNames(
                    "badge-success",
                    "badge-danger",
                    "badge-warning",
                    "badge-warning",
                    "badge-secondary"
                )
                statusLabel.addClassNames(getUserBadge(user.status ?: StatusUser.DESCONECTADO))
                statusLabel.text = user.status.toString().lowercase(Locale.getDefault())
                statusPicker.isVisible = false
            }
            ui.push()
        }

        val info = VerticalLayout()
        info.add(Span(user.name), statusLabel, statusPicker)
        info.isPadding = false
        info.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER

        menuList.add(info)

        return nav

    }

    private fun getUserBadge(status: StatusUser): String {

        return when (status) {

            StatusUser.CONECTADO -> "badge-success"

            StatusUser.DESCONECTADO -> "badge-danger"

            StatusUser.ALMOCANDO -> "badge-warning"

            StatusUser.AUSENTE -> "badge-warning"

            StatusUser.FERIAS -> "badge-secondary"

        }

    }

    private fun createLinks() {
        if (SecurityContextHolder.getContext().authentication.isAuthenticated) {

            permissions =
                userController.findMenuListByRole(SecurityService().authenticatedUser?.authorities?.first().toString())
                    .toMutableList()

            if (permissions.isNotEmpty()) permissions.forEach { i ->
                @Suppress("UNCHECKED_CAST") val div = Div(
                    Icon(i.icon ?: ""), createLink(
                        MenuItemInfo(
                            i.description ?: "", (Class.forName("jiren.view.${i.code}") as Class<out Component>)
                        )
                    )
                )
                div.setSizeFull()
                div.addClassNames("d-flex", "justify-content-between")
                menuList.add(div)
            }

        }
    }

    private fun createLink(menuItemInfo: MenuItemInfo): RouterLink {
        val link = RouterLink()
        link.setRoute(menuItemInfo.view)
        val label = Label(menuItemInfo.text)
        link.add(label)
        return link
    }

    private fun createFooter(): Footer {
        val div = HorizontalLayout()
        div.setSizeFull()
        div.add(Div(), Div())
        div.alignItems = FlexComponent.Alignment.CENTER
        val layout = Footer()
        layout.addClassNames("flex", "items-center", "my-s", "px-m", "py-xs")
        return layout
    }

    private fun getCurrentPageTitle(): String {
        val title = try {
            content.javaClass.getAnnotation(PageTitle::class.java).value
        } catch (_: Exception) {
            ""
        }
        return title
    }

    override fun afterNavigation(p0: AfterNavigationEvent?) {
        val title = getCurrentPageTitle()
        pageTitle.removeAll()
        pageTitle.add(title)
    }

    private class MenuItemInfo(val text: String, val view: Class<out Component>)

}