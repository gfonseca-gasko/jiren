package jiren.view

import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.combobox.ComboBox.ItemFilter
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
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.validator.BeanValidator
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import jiren.data.entity.Shift
import jiren.data.entity.User
import jiren.data.enum.Privileges
import jiren.service.controller.ShiftController
import jiren.service.controller.UserController
import java.io.IOException
import java.util.*
import javax.annotation.PostConstruct

@PageTitle("Plantões")
@Route(value = "/shifts", layout = MainLayout::class)
@SpringComponent
@UIScope
class ShiftView(
    private var shiftController: ShiftController, private val userController: UserController
) : VerticalLayout() {
    private val table = Grid(Shift::class.java)
    private var binder = Binder(Shift::class.java)
    private var shift = Shift()
    private var modal = Dialog()
    private val modalLayout = VerticalLayout()
    private val notificationPosition = Notification.Position.TOP_END

    @PostConstruct
    fun init() {
        add(createSearch(), createMenu(), createTable(), createModal())
        setSizeFull()
        isSpacing = false
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        style["text-align"] = "center"
    }

    private fun createTable(): Scroller {
        table.addItemClickListener {
            shift = it.item
            modal.open()
            binder.readBean(shift)
        }
        table.setColumns("plantonystName", "start", "end")
        table.columns[0].setHeader("Plantonista")
        table.columns[1].setHeader("Início")
        table.columns[2].setHeader("Fim")
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

    private fun createMenu(): HorizontalLayout {
        val newShift = Button("Novo", Icon("plus"))
        newShift.addClickListener {
            shift = Shift()
            binder.readBean(shift)
            modal.open()
        }
        val btnLayout = HorizontalLayout()
        btnLayout.add(newShift)
        btnLayout.setWidthFull()
        btnLayout.justifyContentMode = FlexComponent.JustifyContentMode.END
        btnLayout.defaultVerticalComponentAlignment = FlexComponent.Alignment.END
        return btnLayout
    }

    private fun createSearch(): FormLayout {
        val startDate = DateTimePicker("Do dia")
        val endDate = DateTimePicker("Até o dia")
        val searchButton = Button("Buscar", Icon("search"))
        searchButton.addClickListener {
            if (startDate.value == null || endDate.value == null) {
                startDate.helperText = "Campo obrigatório"
                endDate.helperText = "Campo obrigatório"
            } else {
                table.setItems(shiftController.search(startDate.value, endDate.value))
                startDate.helperText = ""
                endDate.helperText = ""
            }
        }
        val searchPanel = FormLayout()
        val optionsGroup = HorizontalLayout(searchButton)
        optionsGroup.defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
        searchPanel.add(startDate, endDate, optionsGroup)
        return searchPanel
    }

    private fun createModal(): Dialog {
        val shiftStart = DateTimePicker("Início")
        binder.forField(shiftStart)
            .withValidator(BeanValidator(Shift::class.java, "start"))
            .bind(Shift::getStart, Shift::setStart)
            .validate(true)

        val shiftEnd = DateTimePicker("Fim")
        binder.forField(shiftEnd)
            .withValidator(BeanValidator(Shift::class.java, "end"))
            .bind(Shift::getEnd, Shift::setEnd)
            .validate(true)

        val shiftPlantonyst = ComboBox<User>("Plantonista")
        shiftPlantonyst.isRequiredIndicatorVisible = true
        shiftPlantonyst.isRequired = true
        shiftPlantonyst.isAllowCustomValue = false
        shiftPlantonyst.placeholder = "Digite para filtrar"
        shiftPlantonyst.setWidthFull()

        val filter: ItemFilter<User> = ItemFilter<User> { plantonyst: User, filterString: String ->
            plantonyst.name.lowercase().startsWith(filterString.lowercase(Locale.getDefault()))
        }
        shiftPlantonyst.setItems(filter, shiftController.findPlantonyst())
        modal.addOpenedChangeListener { shiftPlantonyst.value = shift.plantonyst }

        val componentsLayout = FormLayout()
        componentsLayout.add(shiftStart, shiftEnd, shiftPlantonyst)
        componentsLayout.setResponsiveSteps(
            FormLayout.ResponsiveStep("0px",1)
        )

        val save = Button("Confirmar", Icon("check-circle")) {
            if (!userController.loggedUserHasPermission("${Privileges.WRITE_PRIVILEGE}")) {
                Notification.show("Você não tem permissão", 5000, notificationPosition)
            } else {
                try {
                    if (shiftPlantonyst.value == null) throw (IOException("Plantonista inválido"))
                    if (!binder.validate().hasErrors()) {
                        binder.writeBean(shift)
                        shift.plantonyst = shiftPlantonyst.value
                        table.setItems(
                            shiftController.save(shift)
                        )
                        modal.close()
                        Notification.show("Sucesso", 5000, notificationPosition)
                    }
                } catch (e: Exception) {
                    Notification.show(e.message, 5000, notificationPosition)
                }
            }
        }

        val cancel = Button("Fechar", Icon("close-circle")) {
            modal.close()
        }

        val delete = Button("Excluir", Icon("trash")) {
            if (!userController.loggedUserHasPermission("${Privileges.DELETE_PRIVILEGE}")) {
                Notification.show("Você não tem permissão", 5000, notificationPosition)
            } else {
                shiftController.delete(shift)
                Notification.show("Sucesso", 5000, notificationPosition)
                modal.close()
            }
        }

        val buttonGroup = FormLayout(cancel, delete, save)
        buttonGroup.setResponsiveSteps(
            FormLayout.ResponsiveStep("0px",3)
        )

        modalLayout.add(H3("Plantão"), componentsLayout, buttonGroup)
        modalLayout.setSizeFull()
        modalLayout.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        modalLayout.defaultHorizontalComponentAlignment = FlexComponent.Alignment.CENTER
        modalLayout.isPadding = false
        modalLayout.isSpacing = false
        modalLayout.isMargin = false

        modal.add(Scroller(modalLayout))
        modal.isResizable = true
        modal.isModal = true
        modal.isDraggable = true
        return modal
    }

}

