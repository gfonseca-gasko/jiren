package jiren.service.controller

import jiren.service.database.DatabasePicker
import jiren.service.database.sql.DataAcessObject
import jiren.data.entity.Automation
import jiren.data.entity.Database
import jiren.data.enum.SGBD
import jiren.data.enum.StatusAutomation
import jiren.data.repository.automation.AutomationRepository
import jiren.data.repository.automation.AutomationSpecification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Controller
import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant.now

@Controller
class AutomationController(
    private var automationRepository: AutomationRepository,
    private var logController: LogController,
    private var dataAcessObject: DataAcessObject,
    private var databasePicker: DatabasePicker
) {

    private var specification = AutomationSpecification()

    fun search(
        text: String,
        db: Database?,
        status: StatusAutomation?,
        inactive: Boolean
    ): Page<Automation>? {
        if (db == null && status == null) {
            return automationRepository.findAll(
                Specification.where(
                    specification.inactive(inactive)
                        .and(specification.name(text).or(specification.query(text)))
                ), Pageable.ofSize(50)
            )
        } else if (db != null && status != null) {
            return automationRepository.findAll(
                Specification.where(
                    specification.inactive(inactive)
                        .and(specification.name(text).or(specification.query(text)))
                        .and(specification.sistema(db).and(specification.status(status)))
                ), Pageable.ofSize(50)
            )
        } else if (db != null) {
            return automationRepository.findAll(
                Specification.where(
                    specification.inactive(inactive)
                        .and(specification.name(text).or(specification.query(text))).and(specification.sistema(db))
                ), Pageable.ofSize(50)
            )
        } else {
            return automationRepository.findAll(
                Specification.where(
                    specification.inactive(inactive)
                        .and(specification.name(text).or(specification.query(text))).and(specification.status(status))
                ), Pageable.ofSize(50)
            )
        }
    }

    fun findAutomationToRun(): List<Automation>? {
        return automationRepository.findByScheduleAndActiveAndStatus(
            Timestamp.from(now()),
            true,
            StatusAutomation.WAITING
        )
    }

    fun saveAll(automations: List<Automation>) {
        automationRepository.saveAll(automations)
    }

    fun findByName(name: String): Automation? {
        return automationRepository.findByName(name)
    }

    fun delete(automation: Automation) {
        return automationRepository.deleteById(automation.id)
    }

    fun save(automation: Automation): Automation {
        automationRepository.save(automation)
        return automation
    }

    fun execute(automation: Automation) {
        when (automation.database!!.sgbd) {
            SGBD.MongoDb -> {}
            else -> {
                try {
                    dataAcessObject.executeAutomation(
                        automation.query!!,
                        databasePicker.getConnection(automation.database!!) as Connection,
                        automation.name!!
                    )
                    automation.reschedule()
                    automationRepository.save(automation)
                } catch (e: Exception) {
                    logController.automationLog(automation.name, e.stackTraceToString(), null)
                    automation.reschedule()
                    automationRepository.save(automation)
                    throw (e)
                }
            }
        }
    }
}
