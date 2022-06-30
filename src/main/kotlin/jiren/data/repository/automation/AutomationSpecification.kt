package jiren.data.repository.automation

import jiren.data.entity.Automation
import jiren.data.entity.Database
import jiren.data.enum.StatusAutomation
import org.springframework.data.jpa.domain.Specification

class AutomationSpecification {

    fun name(name: String): Specification<Automation> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("name"),
                "%$name%"
            )
        }
    }

    fun inactive(active: Boolean): Specification<Automation> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.notEqual(root.get<Boolean>("active"), active)
        }
    }

    fun query(query: String): Specification<Automation> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("query"),
                "%$query%"
            )
        }
    }

    fun sistema(db: Database): Specification<Automation> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(
                root.get<Database>("sistema"),
                db
            )
        }
    }

    fun status(statusAutomation: StatusAutomation?): Specification<Automation> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(
                root.get<StatusAutomation>("status"),
                statusAutomation
            )
        }
    }
}