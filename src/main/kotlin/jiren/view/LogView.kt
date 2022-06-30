package jiren.view

import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import jiren.data.entity.Log
import jiren.service.controller.LogController
import javax.annotation.PostConstruct

@PageTitle("Logs")
@Route(value = "/log", layout = MainLayout::class)
@SpringComponent
@UIScope
class LogView(
    private var logController: LogController
) : VerticalLayout() {
    private val table = Grid(Log::class.java)
    private var modal = Dialog()
    private var log = Log()

    @PostConstruct
    fun init() {
        add(createSearch(), createTable(), createModal())
        setSizeFull()
        isSpacing = false
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        style["text-align"] = "center"
    }

    private fun createTable(): Scroller {
        table.addItemClickListener {
            log = it.item
            modal.open()
        }
        table.setColumns("user", "instant", "elapsedTime", "code", "value", "sqlScript", "sqlType", "task")
        table.columns[0].setHeader("Usuário")
        table.columns[1].setHeader("Data")
        table.columns[2].setHeader("Tempo (ms)")
        table.columns[3].setHeader("Código")
        table.columns[4].setHeader("Valor")
        table.columns[5].setHeader("SQL")
        table.columns[6].setHeader("Tipo")
        table.columns[7].setHeader("Task")
        table.setSelectionMode(Grid.SelectionMode.SINGLE)
        table.isRowsDraggable = true
        table.isColumnReorderingAllowed = true
        table.isVerticalScrollingEnabled = true
        table.columns.forEach { it.isResizable = true }
        table.setWidthFull()
        table.setHeight(95F, Unit.PERCENTAGE)
        val tableScroller = Scroller(table)
        tableScroller.setSizeFull()
        return tableScroller
    }

    private fun createSearch(): HorizontalLayout {
        val searchField = TextField()
        val searchButton = Button("Buscar", Icon("search"))
        val searchPanel = HorizontalLayout()
        searchPanel.add(searchField, searchButton)
        searchPanel.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        searchPanel.defaultVerticalComponentAlignment = FlexComponent.Alignment.BASELINE
        searchButton.addClickListener {
            table.setItems(logController.search(searchField.value)?.toList())
        }
        return searchPanel
    }

    private fun createModal(): Dialog {

        val code = TextField("Code")
        code.isReadOnly = true

        val tempo = TextField("Tempo")
        tempo.isReadOnly = true

        val data = TextField("Data")
        data.isReadOnly = true

        val cmd = TextField("Comando")
        cmd.isReadOnly = true

        val sql = TextField("SQL")
        sql.isReadOnly = true

        val task = TextField("Task")
        task.isReadOnly = true

        val user = TextField("Usuário")
        user.isReadOnly = true

        val value = TextField("Valor")
        value.isReadOnly = true


        modal.addOpenedChangeListener {
            user.value = log.user.toString()
            value.value = log.value.toString()
            code.value = log.code.toString()
            tempo.value = log.elapsedTime.toString()
            data.value = log.instant.toString()
            cmd.value = log.sqlType.toString()
            sql.value = log.sqlScript.toString()
            task.value = log.task.toString()
        }

        val form = FormLayout(code, tempo, data, cmd, sql, task, user, value)
        modal.add(Scroller(form))
        modal.isResizable = true
        modal.isModal = true
        modal.isDraggable = true
        return modal

    }

}

