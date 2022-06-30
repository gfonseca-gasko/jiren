package jiren.view.component

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

class ConfirmationDialog(title: String) : Dialog() {

    private val dialogTitle = H3(title)
    val yes = Button("Sim", Icon("check-circle"))
    private val no = Button("Não", Icon("close-circle"))

    init {
        add(dialogTitle, HorizontalLayout(yes, no))
        no.addClickListener { this.close() }
        this.isModal = true
    }
}