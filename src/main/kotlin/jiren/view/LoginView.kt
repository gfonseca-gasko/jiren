package jiren.view

import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.login.LoginI18n
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope

@PageTitle("Login")
@Route(value = "login", layout = MainLayout::class)
@UIScope
@SpringComponent
class LoginView : VerticalLayout(), BeforeEnterObserver {

    private var loginForm = LoginForm()

    init {
        val teamLogo = Image("img/logo-minor.png", "sti")
        loginForm.setI18n(createLogin())
        loginForm.action = "login"
        loginForm.isForgotPasswordButtonVisible = false
        style["text-align"] = "center"

        this.add(teamLogo, loginForm)
        this.setSizeFull()
        this.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        this.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
    }

    private fun createLogin(): LoginI18n? {
        val i18n = LoginI18n.createDefault()
        i18n.form.title = "Entrar"
        i18n.errorMessage.title = "Usuário ou Senha inválidos"
        i18n.errorMessage.message = "Tente novamente ou entre em contato com um administrador"
        return i18n
    }

    override fun beforeEnter(bee: BeforeEnterEvent?) {
        if (bee?.location?.queryParameters?.parameters!!.containsKey("error")) loginForm.isError = true
    }

}