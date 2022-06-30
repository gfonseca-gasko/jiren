package jiren.view

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.validator.BeanValidator
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import jiren.data.entity.Database
import jiren.data.enum.SGBD
import jiren.data.repository.parameter.ParameterRepository
import jiren.service.controller.DatabaseController
import java.util.*
import javax.annotation.PostConstruct

@PageTitle("Configurações")
@Route(value = "admin/config", layout = MainLayout::class)
@SpringComponent
@UIScope
class ConfigurationView(
    private val parameterRepository: ParameterRepository,
    private val databaseController: DatabaseController
) : VerticalLayout() {

    private val parameterView = Tab(Icon("notebook"), Span("Parametros"))
    private val databaseView = Tab(Icon("database"), Span("Bancos de dados"))
    private val tabs = Tabs(parameterView, databaseView)
    private val content = VerticalLayout()
    private val dbBinder = Binder(Database::class.java)
    private var db = Database()

    @PostConstruct
    fun init() {
        this.add(createTabLayout(), content)
        this.setContent(tabs.selectedTab)
        this.setSizeFull()
        this.isSpacing = false
        this.justifyContentMode = FlexComponent.JustifyContentMode.START
        this.defaultHorizontalComponentAlignment = FlexComponent.Alignment.START
        this.style["text-align"] = "center"
    }

    private fun createTabLayout(): Tabs {
        this.tabs.orientation = Tabs.Orientation.HORIZONTAL
        this.tabs.addSelectedChangeListener { event -> setContent(event.selectedTab) }
        return this.tabs
    }

    private fun setContent(tab: Tab) {
        this.content.removeAll()
        when (tab) {
            this.parameterView -> {
                this.content.add(createParameterView())
            }
            this.databaseView -> {
                this.content.add(createDatabaseView())
            }
        }
    }

    private fun createParameterView(): FormLayout {
        val parameterContent = FormLayout()
        parameterRepository.findAll().forEach { parameter ->
            val parameterValue = TextField(parameter.code)
            parameterValue.value = parameter.value ?: ""
            parameterValue.addValueChangeListener {
                parameter?.value = parameterValue.value
                if (parameter != null) parameterRepository.save(parameter)
            }
            parameterContent.add(parameterValue)
        }
        return parameterContent
    }

    private fun createDatabaseView(): VerticalLayout {

        val databaseSelector = ComboBox("Bancos", databaseController.findAll())
        val newDatabaseButton = Button("Novo", Icon("plus"))
        val menu = HorizontalLayout(databaseSelector, newDatabaseButton)
        menu.defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE

        val form = FormLayout()

        val sgbdBox = ComboBox("SGBD", EnumSet.allOf(SGBD::class.java))
        sgbdBox.isRequiredIndicatorVisible = true
        dbBinder.forField(sgbdBox).withValidator(BeanValidator(Database::class.java, "sgbd"))
            .bind(Database::sgbd, Database::sgbd.setter).validate(true)
        form.add(sgbdBox)

        val nameField = TextField("Nome")
        nameField.isRequiredIndicatorVisible = true
        dbBinder.forField(nameField).withValidator(BeanValidator(Database::class.java, "name"))
            .bind(Database::name, Database::name.setter).validate(true)
        form.add(nameField)

        val hostField = TextField("Endereço")
        hostField.isRequiredIndicatorVisible = true
        dbBinder.forField(hostField).withValidator(BeanValidator(Database::class.java, "host"))
            .bind(Database::host, Database::host.setter).validate(true)
        form.add(hostField)

        val sidField = TextField("SID")
        dbBinder.forField(sidField).withValidator(BeanValidator(Database::class.java, "sid"))
            .bind(Database::sid, Database::sid.setter).validate(true)
        form.add(sidField)

        val schemaField = TextField("Schema")
        dbBinder.forField(schemaField).withValidator(BeanValidator(Database::class.java, "schemaName"))
            .bind(Database::schemaName, Database::schemaName.setter).validate(true)
        form.add(schemaField)

        val portField = IntegerField("Porta")
        portField.isRequiredIndicatorVisible = true
        dbBinder.forField(portField).withValidator(BeanValidator(Database::class.java, "port"))
            .bind(Database::port, Database::port.setter).validate(true)

        val timeoutField = IntegerField("Timeout")
        timeoutField.isRequiredIndicatorVisible = true
        dbBinder.forField(timeoutField).withValidator(BeanValidator(Database::class.java, "timeout"))
            .bind(Database::timeout, Database::timeout.setter).validate(true)

        val integerBox = HorizontalLayout(portField, timeoutField)
        integerBox.defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
        form.add(integerBox)

        val userField = TextField("Usuário")
        userField.isRequiredIndicatorVisible = true
        dbBinder.forField(userField).withValidator(BeanValidator(Database::class.java, "user"))
            .bind(Database::user, Database::user.setter).validate(true)
        form.add(userField)

        val passwordField = PasswordField("Senha")
        passwordField.isRequiredIndicatorVisible = true
        dbBinder.forField(passwordField).withValidator(BeanValidator(Database::class.java, "secret"))
            .bind({ "" }, Database::secret.setter).validate(true)
        form.add(passwordField)

        val isAutomationEnabled = Checkbox("Automações")
        dbBinder.forField(isAutomationEnabled).withValidator(BeanValidator(Database::class.java, "automationEnabled"))
            .bind(Database::automationEnabled, Database::automationEnabled.setter).validate(true)

        val isMonitoringEnabled = Checkbox("Monitoramentos")
        dbBinder.forField(isMonitoringEnabled).withValidator(BeanValidator(Database::class.java, "monitoringEnabled"))
            .bind(Database::monitoringEnabled, Database::monitoringEnabled.setter).validate(true)

        val isScriptingEnabled = Checkbox("Execução de Scripts")
        dbBinder.forField(isScriptingEnabled).withValidator(BeanValidator(Database::class.java, "scriptsEnabled"))
            .bind(Database::scriptsEnabled, Database::scriptsEnabled.setter).validate(true)

        val checkboxFields = FormLayout(isAutomationEnabled, isMonitoringEnabled, isScriptingEnabled)
        checkboxFields.setResponsiveSteps(
            FormLayout.ResponsiveStep("0px", 1),
            FormLayout.ResponsiveStep("600px", 3)
        )
        form.add(checkboxFields)

        val saveButton = Button("Salvar", Icon("check-circle")) {
            try {
                if (!dbBinder.validate().hasErrors()) {
                    dbBinder.writeBean(db)
                    databaseController.save(db)
                    Notification.show(
                        "Sucesso", 5000, Notification.Position.TOP_END
                    )
                }
            } catch (v: Exception) {
                Notification.show(v.message ?: "", 5000, Notification.Position.TOP_END)
            }
        }

        newDatabaseButton.addClickListener {
            db = Database()
            databaseSelector.value = null
            dbBinder.readBean(db)
        }

        databaseSelector.addValueChangeListener {
            db = it.value
            dbBinder.readBean(db)
        }

        newDatabaseButton.clickInClient()

        val databaseContent = VerticalLayout(menu, form, saveButton)
        databaseContent.setSizeFull()
        databaseContent.justifyContentMode = FlexComponent.JustifyContentMode.START
        databaseContent.defaultHorizontalComponentAlignment = FlexComponent.Alignment.START
        return databaseContent
    }

}