package jiren.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import jiren.service.controller.UserController
import jiren.service.security.SecurityService
import javax.annotation.PostConstruct

@PageTitle("Trocar Senha")
@Route(value = "/changepassword", layout = MainLayout::class)
@SpringComponent
@UIScope
class ChangePasswordView(
    private val userController: UserController
) : VerticalLayout() {

    @PostConstruct
    fun init() {
        add(createPasswordDialog())
        setSizeFull()
        isSpacing = false
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        style["text-align"] = "center"
    }

    private fun createPasswordDialog(): HorizontalLayout {
        val password = TextField("Trocar Senha (Obrigatório)")
        password.setSizeFull()
        password.isRequiredIndicatorVisible = true
        password.isRequired = true
        password.isReadOnly = false
        password.minLength = 8
        val passGen = Button("Gerar")
        val save = Button("", Icon("check-circle"))
        val passwordDiv = HorizontalLayout(password, passGen, save)
        passwordDiv.justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        passwordDiv.defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE

        passGen.addClickListener { password.value = userController.generatePassword() }

        save.addClickListener {
            if (!password.value.isNullOrEmpty()) {
                userController.changePassword(SecurityService().authenticatedUser?.username.toString(), password.value)
                UI.getCurrent().navigate("/home")
            }
        }
        return passwordDiv
    }

}

