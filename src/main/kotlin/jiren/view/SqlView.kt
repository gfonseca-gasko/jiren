package jiren.view

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.splitlayout.SplitLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import jiren.service.database.sql.ScriptExecutor
import jiren.service.controller.DatabaseController
import jiren.view.component.ConfirmationDialog

@PageTitle("SQL")
@Route(value = "team/sql", layout = MainLayout::class)
@SpringComponent
@UIScope
class SqlView(private val scriptExecutor: ScriptExecutor, databaseController: DatabaseController) : VerticalLayout() {

    private val scriptArea = TextArea()
    private val outputArea = TextArea()
    private val databasesOptions = databaseController.scriptingDatabaseOptions()
    private val notificationPosition = Notification.Position.TOP_END

    init {
        val splitView = SplitLayout(buildExecutionView(), buildOutputView())
        splitView.orientation = SplitLayout.Orientation.HORIZONTAL
        splitView.setSizeFull()
        this.add(createMenu(), splitView)
        this.setSizeFull()
        this.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        this.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        this.style["text-align"] = "center"
    }

    private fun createMenu(): FormLayout {

        val sysBox = ComboBox("Banco de Dados", databasesOptions)
        val taskField = TextField("Task", "ST-00000")
        val execBtn = Button("Executar", Icon("play"))
        execBtn.isDisableOnClick = true
        val execConfirmation = ConfirmationDialog("Confirma a execução do script ?")
        execConfirmation.yes.isDisableOnClick = true

        execBtn.addClickListener {
            execConfirmation.open()
        }

        execConfirmation.addOpenedChangeListener {
            execBtn.isEnabled = !execConfirmation.isOpened
            execConfirmation.yes.isEnabled = execConfirmation.isOpened
        }

        execConfirmation.yes.addClickListener {
            try {
                if (scriptArea.value.isNullOrEmpty() || sysBox.value == null) {
                    Notification.show("Script ou Banco de Dados inválido", 5000, notificationPosition)
                } else {
                    scriptExecutor.execute(scriptArea.value, sysBox.value, taskField.value).let { response ->
                        Notification.show(response.split("!#!").first(), 5000, notificationPosition)
                        outputArea.value = response.split("!#!").last()
                    }
                }
            } catch (e: Exception) {
                Notification.show(e.message, 5000, notificationPosition)
            } finally {
                execConfirmation.close()
            }
        }

        val menuLayout = FormLayout()
        menuLayout.add(sysBox, taskField, HorizontalLayout(execBtn), execConfirmation)
        return menuLayout
    }

    private fun buildExecutionView(): VerticalLayout {
        scriptArea.maxLength = 900000
        scriptArea.label = "SQL"
        scriptArea.setSizeFull()
        val executionView = VerticalLayout()
        executionView.add(scriptArea)
        executionView.setSizeFull()
        return executionView
    }

    private fun buildOutputView(): VerticalLayout {
        outputArea.isReadOnly = true
        outputArea.label = "OUTPUT"
        outputArea.setSizeFull()
        val outputView = VerticalLayout()
        outputView.add(outputArea)
        outputView.setSizeFull()
        return outputView
    }

}