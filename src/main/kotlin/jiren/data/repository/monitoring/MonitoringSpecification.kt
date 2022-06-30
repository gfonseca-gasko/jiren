package jiren.data.repository.monitoring

import jiren.data.entity.Database
import jiren.data.entity.Monitoring
import jiren.data.enum.StatusMonitoring
import jiren.data.enum.MonitoringType
import org.springframework.data.jpa.domain.Specification

class MonitoringSpecification {

    fun name(name: String): Specification<Monitoring> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("name"),
                "%$name%"
            )
        }
    }

    fun command1(command1: String): Specification<Monitoring> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("databaseOneSql"),
                "%$command1%"
            )
        }
    }

    fun inactive(active: Boolean): Specification<Monitoring> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.notEqual(root.get<Boolean>("enabled"), active)
        }
    }

    fun system(db: Database?): Specification<Monitoring> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(
                root.get<Database>("databaseOptionOne"),
                db
            )
        }
    }

    fun status(status: StatusMonitoring?): Specification<Monitoring> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(
                root.get<StatusMonitoring>("status"),
                status
            )
        }
    }

    fun type(type: MonitoringType?): Specification<Monitoring> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.equal(
                root.get<MonitoringType>("type"),
                type
            )
        }
    }
}