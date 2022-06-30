package jiren.view

import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.validator.BeanValidator
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import jiren.data.entity.Monitoring
import jiren.data.enum.HttpAllowedMethods
import jiren.data.enum.MonitoringType
import jiren.data.enum.Privileges
import jiren.data.enum.StatusMonitoring
import jiren.service.controller.DatabaseController
import jiren.service.controller.MonitoringController
import jiren.service.controller.UserController
import jiren.view.component.ConfirmationDialog
import java.util.*
import javax.annotation.PostConstruct

@PageTitle("Monitoramento")
@Route(value = "/monitor", layout = MainLayout::class)
@SpringComponent
@UIScope
class MonitoringView(
    private val monitoringController: MonitoringController,
    private val userController: UserController,
    databaseController: DatabaseController
) : VerticalLayout() {
    private val table = Grid(Monitoring::class.java, true)
    private val modal = Dialog()
    private val form = FormLayout()
    private var binder = Binder(Monitoring::class.java)
    private var monitoring = Monitoring()
    private val databaseOptions = databaseController.monitoringDatabaseOptions()
    private val notificationPosition = Notification.Position.TOP_END

    @PostConstruct
    private fun init() {
        this.add(
            createSearch(), createMenu(), createTable(), createModal()
        )
        this.setSizeFull()
        this.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        this.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        this.style["text-align"] = "center"
    }

    private fun createTable(): Scroller {
        table.addItemClickListener {
            monitoring = it.item
            modal.open()
            binder.readBean(monitoring)
        }
        table.setColumns(
            "name",
            "databaseOptionOne",
            "type",
            "executionInterval",
            "scheduleAt",
            "ranAt",
            "status",
            "firstReport",
            "lastReport",
            "errorCount",
            "documentURL"
        )
        table.columns[0].setHeader("Nome")
        table.columns[1].setHeader("Banco de Dados")
        table.columns[2].setHeader("Tipo")
        table.columns[3].setHeader("Intervalo de Execução")
        table.columns[4].setHeader("Próxima Execução")
        table.columns[5].setHeader("Última execução")
        table.columns[6].setHeader("Status")
        table.columns[7].setHeader("Primeiro Report")
        table.columns[8].setHeader("Último Report")
        table.columns[9].setHeader("Reports")
        table.columns[10].setHeader("Ajuda")
        table.setSelectionMode(Grid.SelectionMode.SINGLE)
        table.isRowsDraggable = true
        table.isColumnReorderingAllowed = true
        table.isVerticalScrollingEnabled = true
        table.setWidthFull()
        table.setHeight(95F, Unit.PERCENTAGE)
        table.columns.forEach { it.isResizable = true }
        val tableScroller = Scroller(table)
        tableScroller.setSizeFull()
        return tableScroller
    }

    private fun createMenu(): HorizontalLayout {
        val newMonitoring = Button("Novo", Icon("plus"))
        newMonitoring.addClickListener {
            monitoring = Monitoring()
            binder.readBean(monitoring)
            modal.open()
        }
        val btnLayout = HorizontalLayout()
        btnLayout.setWidthFull()
        btnLayout.add(newMonitoring)
        btnLayout.justifyContentMode = FlexComponent.JustifyContentMode.END
        btnLayout.defaultVerticalComponentAlignment = FlexComponent.Alignment.END
        return btnLayout
    }

    private fun createSearch(): VerticalLayout {
        val searchField = TextField()
        searchField.placeholder = "Digite para buscar"
        val btnSearch = Button("Buscar", Icon("search"))
        val inactiveFilter = Checkbox("Inativo")
        val btnGroup = FormLayout()
        val optionsGroup = HorizontalLayout(btnSearch, HorizontalLayout(inactiveFilter))
        optionsGroup.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        btnGroup.add(searchField, optionsGroup)

        val statusBox = ComboBox("Status", EnumSet.allOf(StatusMonitoring::class.java))
        val sysBox = ComboBox("Banco de Dados", databaseOptions)
        val typeBox = ComboBox("Tipo", EnumSet.allOf(MonitoringType::class.java))
        val boxGroup = FormLayout()
        boxGroup.setResponsiveSteps(
            FormLayout.ResponsiveStep("0px", 3), FormLayout.ResponsiveStep("600px", 3)
        )
        boxGroup.add(statusBox, sysBox, typeBox)

        val searchPanel = VerticalLayout()
        searchPanel.add(btnGroup, boxGroup)
        searchPanel.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        searchPanel.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        searchPanel.isSpacing = false

        btnSearch.addClickListener {
            table.setItems(
                monitoringController.search(
                    searchField.value, sysBox.value, typeBox.value, statusBox.value, inactiveFilter.value
                )?.toList()
            )
        }
        return searchPanel
    }

    private fun createModal(): Dialog {
        val nameField = TextField("Nome")
        nameField.isRequiredIndicatorVisible = true
        binder.forField(nameField).withValidator(BeanValidator(Monitoring::class.java, "name"))
            .bind(Monitoring::name, Monitoring::name.setter).validate(true)
        form.add(nameField)

        val titleField = TextField("Título")
        titleField.isRequiredIndicatorVisible = true
        binder.forField(titleField).withValidator(BeanValidator(Monitoring::class.java, "title"))
            .bind(Monitoring::title, Monitoring::title.setter).validate(true)
        form.add(titleField)

        val documentUrlField = TextField("Confluence")
        binder.forField(documentUrlField).withValidator(BeanValidator(Monitoring::class.java, "documentURL"))
            .bind(Monitoring::documentURL, Monitoring::documentURL.setter).validate(true)
        form.add(documentUrlField)

        val sqlSystemField = ComboBox("Banco de Dados", databaseOptions)
        sqlSystemField.isRequiredIndicatorVisible = true
        sqlSystemField.isVisible = false
        binder.forField(sqlSystemField).withValidator(BeanValidator(Monitoring::class.java, "databaseOptionOne"))
            .bind(Monitoring::databaseOptionOne, Monitoring::databaseOptionOne.setter).validate(true)
        form.add(sqlSystemField)

        val cmdField = TextArea("SQL")
        cmdField.isRequiredIndicatorVisible = true
        cmdField.isVisible = false
        binder.forField(cmdField).withValidator(BeanValidator(Monitoring::class.java, "databaseOneSql"))
            .bind(Monitoring::databaseOneSql, Monitoring::databaseOneSql.setter).validate(true)
        form.add(cmdField)

        val sqlSystemField2 = ComboBox("Banco de Dados 2", databaseOptions)
        sqlSystemField2.isRequiredIndicatorVisible = true
        sqlSystemField2.isVisible = false
        binder.forField(sqlSystemField2).withValidator(BeanValidator(Monitoring::class.java, "databaseOptionTwo"))
            .bind(Monitoring::databaseOptionTwo, Monitoring::databaseOptionTwo.setter).validate(true)
        form.add(sqlSystemField2)

        val cmdField2 = TextArea("SQL 2")
        cmdField2.isRequiredIndicatorVisible = true
        cmdField2.isVisible = false
        binder.forField(cmdField2).withValidator(BeanValidator(Monitoring::class.java, "databaseTwoSql"))
            .bind(Monitoring::databaseTwoSql, Monitoring::databaseTwoSql.setter).validate(true)
        form.add(cmdField2)

        val httpBody = TextArea("Body")
        httpBody.isRequiredIndicatorVisible = true
        httpBody.isVisible = false
        binder.forField(httpBody).withValidator(BeanValidator(Monitoring::class.java, "body"))
            .bind(Monitoring::body, Monitoring::body.setter).validate(true)
        form.add(httpBody)

        val typeField = ComboBox("Tipo", EnumSet.allOf(MonitoringType::class.java))
        typeField.isRequiredIndicatorVisible = true
        binder.forField(typeField).withValidator(BeanValidator(Monitoring::class.java, "type"))
            .bind(Monitoring::type, Monitoring::type.setter).validate(true)
        form.add(typeField)

        val scheduleConfigField = IntegerField("Intervalo em Minutos")
        scheduleConfigField.isRequiredIndicatorVisible = true
        binder.forField(scheduleConfigField).withValidator(BeanValidator(Monitoring::class.java, "executionInterval"))
            .bind(Monitoring::executionInterval, Monitoring::executionInterval.setter).validate(true)
        form.add(scheduleConfigField)

        val scheduleField = DateTimePicker("Agendamento")
        scheduleField.isRequiredIndicatorVisible = true
        binder.forField(scheduleField).withValidator(BeanValidator(Monitoring::class.java, "scheduleAt"))
            .bind(Monitoring::getScheduleAt, Monitoring::setScheduleAt).validate()
        form.add(scheduleField)

        val showInPanelField = Checkbox("Painel")
        binder.forField(showInPanelField).withValidator(BeanValidator(Monitoring::class.java, "showInPanel"))
            .bind(Monitoring::showInPanel, Monitoring::showInPanel.setter).validate(true)

        val sendMailField = Checkbox("E-Mail", false)
        binder.forField(sendMailField).withValidator(BeanValidator(Monitoring::class.java, "emailNotification"))
            .bind(Monitoring::emailNotification, Monitoring::emailNotification.setter).validate(true)

        val rocketChatField = Checkbox("RocketChat", false)
        rocketChatField.value = true
        binder.forField(rocketChatField).withValidator(BeanValidator(Monitoring::class.java, "rocketNotification"))
            .bind(Monitoring::rocketNotification, Monitoring::rocketNotification.setter).validate(true)

        val whatsappNotificationField = Checkbox("Whatsapp", false)
        whatsappNotificationField.value = true
        binder.forField(whatsappNotificationField)
            .withValidator(BeanValidator(Monitoring::class.java, "whatsappNotification"))
            .bind(Monitoring::whatsappNotification, Monitoring::whatsappNotification.setter).validate(true)

        val jiraNotificationField = Checkbox("Jira", false)
        jiraNotificationField.value = true
        binder.forField(jiraNotificationField).withValidator(BeanValidator(Monitoring::class.java, "jiraNotification"))
            .bind(Monitoring::jiraNotification, Monitoring::jiraNotification.setter).validate(true)

        val activeField = Checkbox("Ativo", false)
        binder.forField(activeField).withValidator(BeanValidator(Monitoring::class.java, "enabled"))
            .bind(Monitoring::enabled, Monitoring::enabled.setter).validate(true)

        val checkBoxGroup = FormLayout(
            showInPanelField,
            sendMailField,
            rocketChatField,
            whatsappNotificationField,
            jiraNotificationField,
            activeField
        )
        checkBoxGroup.setResponsiveSteps(
            FormLayout.ResponsiveStep("0px", 2), FormLayout.ResponsiveStep("600px", 4)
        )
        form.add(checkBoxGroup)

        val mailToField = TextField("Destinatários", "email@email,email2@email")
        mailToField.isVisible = sendMailField.value
        binder.forField(mailToField).bind(Monitoring::mailTo, Monitoring::mailTo.setter)
        form.add(mailToField)

        val rocketChatRoomField = TextField("Rocketchat RoomID")
        binder.forField(rocketChatRoomField).bind(Monitoring::rocketchatRoom, Monitoring::rocketchatRoom.setter)
        form.add(rocketChatRoomField)

        val requestTypeField = ComboBox("Tipo de Requisição", EnumSet.allOf(HttpAllowedMethods::class.java))
        requestTypeField.isRequiredIndicatorVisible = true
        requestTypeField.isVisible = false
        binder.forField(requestTypeField).bind(Monitoring::httpType, Monitoring::httpType.setter)
        form.add(requestTypeField)

        val responseCodeField = TextField("Resposta Esperada [StatusCode]", "200")
        responseCodeField.isRequiredIndicatorVisible = true
        responseCodeField.isVisible = false
        binder.forField(responseCodeField).bind(Monitoring::httpResponseCode, Monitoring::httpResponseCode.setter)
        form.add(responseCodeField)

        val requestUrlField = TextField("URL")
        requestUrlField.isRequiredIndicatorVisible = true
        requestUrlField.isVisible = false
        binder.forField(requestUrlField).bind(Monitoring::httpRequestUrl, Monitoring::httpRequestUrl.setter)
        form.add(requestUrlField)

        val requestTimeOutField = TextField("Timeout (Segundos) ")
        requestTimeOutField.isRequiredIndicatorVisible = true
        requestTimeOutField.isVisible = false
        binder.forField(requestTimeOutField).bind(Monitoring::httpTimeout, Monitoring::httpTimeout.setter)
        form.add(requestTimeOutField)

        val contentTypeField = TextField("ContentType")
        contentTypeField.isRequiredIndicatorVisible = true
        contentTypeField.isVisible = false
        binder.forField(contentTypeField).bind(Monitoring::httpContentType, Monitoring::httpContentType.setter)
        form.add(contentTypeField)
        // ------------
        typeField.addValueChangeListener {
            val isComparison = (typeField.value == MonitoringType.DATABASE_COMPARE)

            if (isComparison) {
                sqlSystemField.isVisible = true
                sqlSystemField2.isVisible = true
                cmdField.isVisible = true
                cmdField2.isVisible = true
                binder.forField(sqlSystemField).asRequired("Campo obrigatório")
                    .bind(Monitoring::databaseOptionOne, Monitoring::databaseOptionOne.setter)
                binder.forField(cmdField).asRequired("Campo obrigatório")
                    .bind(Monitoring::databaseOneSql, Monitoring::databaseOneSql.setter)
                binder.forField(sqlSystemField2).asRequired("Campo obrigatório")
                    .bind(Monitoring::databaseOptionTwo, Monitoring::databaseOptionTwo.setter)
                binder.forField(cmdField2).asRequired("Campo obrigatório")
                    .bind(Monitoring::databaseTwoSql, Monitoring::databaseTwoSql.setter)
                cmdField.isRequiredIndicatorVisible = true
                cmdField2.isRequiredIndicatorVisible = true
                sqlSystemField.isRequiredIndicatorVisible = true
                sqlSystemField2.isRequiredIndicatorVisible = true
            } else {
                sqlSystemField.isVisible = false
                sqlSystemField2.isVisible = false
                cmdField.isVisible = false
                cmdField2.isVisible = false
                binder.forField(sqlSystemField)
                    .bind(Monitoring::databaseOptionOne, Monitoring::databaseOptionOne.setter)
                binder.forField(cmdField).bind(Monitoring::databaseOneSql, Monitoring::databaseOneSql.setter)
                binder.forField(sqlSystemField2)
                    .bind(Monitoring::databaseOptionTwo, Monitoring::databaseOptionTwo.setter)
                binder.forField(cmdField2).bind(Monitoring::databaseTwoSql, Monitoring::databaseTwoSql.setter)
                cmdField.isRequiredIndicatorVisible = false
                cmdField2.isRequiredIndicatorVisible = false
                sqlSystemField.isRequiredIndicatorVisible = false
                sqlSystemField2.isRequiredIndicatorVisible = false
            }

            val isDatabase = (typeField.value == MonitoringType.DATABASE)

            if (isDatabase) {
                sqlSystemField.isVisible = true
                cmdField.isVisible = true
                binder.forField(sqlSystemField).asRequired("Campo obrigatório")
                    .bind(Monitoring::databaseOptionOne, Monitoring::databaseOptionOne.setter)
                binder.forField(cmdField).asRequired("Campo obrigatório")
                    .bind(Monitoring::databaseOneSql, Monitoring::databaseOneSql.setter)
                cmdField.isRequiredIndicatorVisible = false
            } else if (!isComparison) {
                sqlSystemField.isVisible = false
                cmdField.isVisible = false
                binder.forField(sqlSystemField)
                    .bind(Monitoring::databaseOptionOne, Monitoring::databaseOptionOne.setter)
                binder.forField(cmdField).bind(Monitoring::databaseOneSql, Monitoring::databaseOneSql.setter)
                cmdField.isRequiredIndicatorVisible = true
            }

            val isHttp = (typeField.value == MonitoringType.HTTP)

            if (isHttp) {
                httpBody.isVisible = true
                requestUrlField.isVisible = true
                requestTypeField.isVisible = true
                responseCodeField.isVisible = true
                requestTimeOutField.isVisible = true
                contentTypeField.isVisible = true
                binder.forField(requestTypeField).asRequired("Campo obrigatório")
                    .bind(Monitoring::httpType, Monitoring::httpType.setter).validate(true)
                binder.forField(httpBody).asRequired("Campo obrigatório")
                    .bind(Monitoring::body, Monitoring::body.setter).validate(true)
                binder.forField(responseCodeField).asRequired("Campo obrigatório")
                    .bind(Monitoring::httpResponseCode, Monitoring::httpResponseCode.setter).validate(true)
                binder.forField(requestUrlField).asRequired("Campo obrigatório")
                    .bind(Monitoring::httpRequestUrl, Monitoring::httpRequestUrl.setter).validate(true)
                binder.forField(requestTimeOutField).asRequired("Campo obrigatório")
                    .bind(Monitoring::httpTimeout, Monitoring::httpTimeout.setter).validate(true)
                binder.forField(contentTypeField).asRequired("Campo obrigatório")
                    .bind(Monitoring::httpContentType, Monitoring::httpContentType.setter).validate(true)
            } else {
                httpBody.isVisible = false
                requestUrlField.isVisible = false
                requestTypeField.isVisible = false
                responseCodeField.isVisible = false
                requestTimeOutField.isVisible = false
                contentTypeField.isVisible = false
                binder.forField(requestTypeField).bind(Monitoring::httpType, Monitoring::httpType.setter)
                binder.forField(responseCodeField)
                    .bind(Monitoring::httpResponseCode, Monitoring::httpResponseCode.setter)
                binder.forField(httpBody).bind(Monitoring::body, Monitoring::body.setter)
                binder.forField(requestUrlField).bind(Monitoring::httpRequestUrl, Monitoring::httpRequestUrl.setter)
                binder.forField(requestTimeOutField).bind(Monitoring::httpTimeout, Monitoring::httpTimeout.setter)
                binder.forField(contentTypeField).bind(Monitoring::httpContentType, Monitoring::httpContentType.setter)
            }

        }

        sendMailField.addValueChangeListener {
            val isMailActive = sendMailField.value
            mailToField.isVisible = isMailActive
            mailToField.isRequired = isMailActive
            if (isMailActive) {
                binder.forField(mailToField).asRequired("Campo obrigatório")
                    .bind(Monitoring::mailTo, Monitoring::mailTo.setter).validate(true)
            } else {
                binder.forField(mailToField).bind(Monitoring::mailTo, Monitoring::mailTo.setter)
            }
        }

        rocketChatField.addValueChangeListener {
            val isRocketActive = rocketChatField.value
            rocketChatRoomField.isVisible = isRocketActive
            rocketChatRoomField.isRequired = isRocketActive
            if (isRocketActive) {
                binder.forField(rocketChatRoomField).asRequired("Campo obrigatório")
                    .bind(Monitoring::rocketchatRoom, Monitoring::rocketchatRoom.setter).validate(true)
            } else {
                binder.forField(rocketChatRoomField)
                    .bind(Monitoring::rocketchatRoom, Monitoring::rocketchatRoom.setter)
            }
        }

        val save = Button("Salvar", Icon("check-circle")) {
            try {
                if (!userController.loggedUserHasPermission("${Privileges.WRITE_PRIVILEGE}")) throw Exception("Você não tem permissão")
                if (!binder.validate().hasErrors()) {
                    binder.writeBean(monitoring)
                    monitoringController.save(monitoring)
                    table.setItems(monitoringController.findByName(monitoring.name))
                    modal.close()
                    Notification.show("Sucesso", 5000, notificationPosition)
                }
            } catch (e: Exception) {
                Notification.show(e.message ?: "", 5000, notificationPosition)
            }
        }
        val cancel = Button("Fechar", Icon("close-circle")) { modal.close() }
        val exclusionConfirmDialog = ConfirmationDialog("Confirma a exclusão ?")
        val delete = Button("Excluir", Icon("trash")) { exclusionConfirmDialog.open() }
        exclusionConfirmDialog.yes.addClickListener {
            if (!userController.loggedUserHasPermission("${Privileges.DELETE_PRIVILEGE}")) {
                Notification.show("Você não tem permissão", 5000, notificationPosition)
            } else {
                monitoringController.delete(monitoring)
                modal.close()
                exclusionConfirmDialog.close()
                Notification.show("Sucesso", 5000, notificationPosition)
            }
        }

        val executeConfirmationDialog = ConfirmationDialog("Confirma a execução ?")
        executeConfirmationDialog.yes.isDisableOnClick = true
        val execute = Button("Executar", Icon("play"))
        execute.isDisableOnClick = true

        execute.addClickListener {
            executeConfirmationDialog.open()
        }

        executeConfirmationDialog.addOpenedChangeListener {
            execute.isEnabled = (monitoring.id > 0 && !executeConfirmationDialog.isOpened)
            executeConfirmationDialog.yes.isEnabled = executeConfirmationDialog.isOpened
        }

        executeConfirmationDialog.yes.addClickListener {
            if (!userController.loggedUserHasPermission("${Privileges.EXECUTE_PRIVILEGE}")) {
                Notification.show("Você não tem permissão", 5000, notificationPosition)
            } else {
                monitoringController.execute(monitoring).let { response ->
                    if (response != null) {
                        Notification.show("${monitoring.name} executado - OK", 5000, notificationPosition)
                    } else {
                        Notification.show("${monitoring.name} executado - NOK", 5000, notificationPosition)
                    }
                }
            }
            executeConfirmationDialog.close()
        }

        modal.addOpenedChangeListener {
            execute.isEnabled = (monitoring.id > 0 && !executeConfirmationDialog.isOpened)
            delete.isEnabled = (monitoring.id > 0 && !exclusionConfirmDialog.isOpened)
        }

        val header = HorizontalLayout()
        header.justifyContentMode = FlexComponent.JustifyContentMode.START
        header.defaultVerticalComponentAlignment = FlexComponent.Alignment.START
        header.add(H3("Monitoramento"))
        header.setWidthFull()
        header.isSpacing = false

        val btnGrp = FormLayout(cancel, delete, save, execute, exclusionConfirmDialog)
        btnGrp.setResponsiveSteps(
            FormLayout.ResponsiveStep("0px", 2), FormLayout.ResponsiveStep("600px", 4)
        )

        this.modal.add(header, Scroller(form), btnGrp)
        this.modal.isResizable = true
        this.modal.isModal = true
        this.modal.isDraggable = true
        return this.modal
    }

}