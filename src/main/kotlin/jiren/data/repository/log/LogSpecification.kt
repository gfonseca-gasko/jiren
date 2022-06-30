package jiren.data.repository.log

import jiren.data.entity.Log
import org.springframework.data.jpa.domain.Specification

class LogSpecification {

    fun cmd(sqlType: String): Specification<Log> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("sqlType"),
                "%$sqlType%"
            )
        }
    }

    fun code(code: String): Specification<Log> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("code"),
                "%$code%"
            )
        }
    }

    fun sql(sql: String): Specification<Log> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("sqlScript"),
                "%$sql%"
            )
        }
    }

    fun task(task: String): Specification<Log> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("task"),
                "%$task%"
            )
        }
    }

    fun value(value: String): Specification<Log> {
        return Specification { root, _, criteriaBuilder ->
            criteriaBuilder.like(
                root.get("value"),
                "%$value%"
            )
        }
    }
}