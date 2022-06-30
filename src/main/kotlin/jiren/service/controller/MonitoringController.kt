package jiren.service.controller

import com.mongodb.client.MongoDatabase
import jiren.service.database.DatabasePicker
import jiren.service.database.sql.DataAcessObject
import jiren.data.entity.Database
import jiren.data.entity.Monitoring
import jiren.data.enum.HttpAllowedMethods
import jiren.data.enum.MonitoringType
import jiren.data.enum.SGBD
import jiren.data.enum.StatusMonitoring
import jiren.data.repository.monitoring.MonitoringRepository
import jiren.data.repository.monitoring.MonitoringSpecification
import jiren.service.database.ResultComparator
import org.bson.BsonDocument
import org.bson.BsonInt64
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Controller
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.sql.Connection
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant.now
import java.util.*

@Controller
class MonitoringController(
    private var monitoringRepository: MonitoringRepository,
    private var dataAcessObject: DataAcessObject,
    private var databasePicker: DatabasePicker,
    private var logController: LogController
) {
    private var specification = MonitoringSpecification()

    fun search(
        text: String, db: Database?, type: MonitoringType?, status: StatusMonitoring?, inactive: Boolean
    ): Page<Monitoring>? {
        if (db == null && type == null && status == null) {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(specification.name(text).or(specification.command1(text)))
                ), Pageable.ofSize(50)
            )
        } else if (db != null && type != null && status != null) {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(specification.name(text).or(specification.command1(text))).and(
                        specification.status(status).and(
                            specification.type(type).and(specification.system(db))
                        )
                    )
                ), Pageable.ofSize(50)
            )
        } else if (db != null && type != null) {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(
                        specification.name(text).or(specification.command1(text)).and(
                            specification.type(type).and(specification.system(db))
                        )
                    )
                ), Pageable.ofSize(50)
            )
        } else if (db != null && status != null) {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(
                        specification.name(text).or(specification.command1(text)).and(
                            specification.status(status).and(specification.system(db))
                        )
                    )
                ), Pageable.ofSize(50)
            )
        } else if (type != null && status != null) {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(
                        specification.name(text).or(specification.command1(text)).and(
                            specification.status(status).and(specification.type(type))
                        )
                    )
                ), Pageable.ofSize(50)
            )
        } else if (db != null) {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(
                        specification.name(text).or(specification.command1(text)).and(specification.system(db))
                    )
                ), Pageable.ofSize(50)
            )
        } else if (type != null) {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(
                        specification.name(text).or(specification.command1(text)).and(
                            specification.type(type)
                        )
                    )
                ), Pageable.ofSize(50)
            )
        } else {
            return monitoringRepository.findAll(
                Specification.where(
                    specification.inactive(inactive).and(
                        specification.name(text).or(specification.command1(text)).and(
                            specification.status(status)
                        )
                    )
                ), Pageable.ofSize(50)
            )
        }
    }

    fun findMonitoringToRun(): List<Monitoring>? {
        return monitoringRepository.findByScheduleAtIsLessThanAndEnabledAndStatusIsNot(
                Timestamp.from(now()), true, StatusMonitoring.RUNNING
            )
    }

    fun findPanelItens(): List<Monitoring>? {
        return monitoringRepository.findByShowInPanelAndEnabled(inPanel = true, active = true)
    }

    fun save(monitoring: Monitoring) {
        monitoringRepository.save(monitoring)
    }

    fun saveAll(monitorings: List<Monitoring>) {
        monitoringRepository.saveAll(monitorings)
    }

    fun delete(monitoring: Monitoring) {
        monitoringRepository.delete(monitoring)
    }

    fun findById(id: Long): Optional<Monitoring> {
        return monitoringRepository.findById(id)
    }

    fun findByName(name: String): Monitoring? {
        return monitoringRepository.findByName(name)
    }

    fun execute(monitoring: Monitoring): String? {

        var result: String? = null

        when (monitoring.type) {

            MonitoringType.DATABASE -> {

                when (monitoring.databaseOptionOne!!.sgbd) {

                    SGBD.MongoDb -> {

                        try {
                            val mongodb = databasePicker.getConnection(monitoring.databaseOptionOne!!) as MongoDatabase
                            val command = BsonDocument(monitoring.databaseOneSql, BsonInt64(1))
                            val startTime = now()
                            result = mongodb.runCommand(command).toString()
                            logController.log(
                                result.toString(),
                                now().minusMillis(startTime.toEpochMilli()).toEpochMilli(),
                                monitoring.name,
                                command.toString(),
                                "NOSQL",
                                "Monitoring::${MonitoringType.DATABASE}"
                            )
                            if (result.isNotEmpty()) monitoring.onError()
                            else monitoring.onSuccess()
                            monitoring.reschedule()
                            monitoringRepository.save(monitoring)
                        } catch (e: Exception) {
                            logController.monitoringLog(monitoring.name, e.stackTraceToString())
                        }

                    }

                    else -> {
                        try {
                            result = dataAcessObject.executeMonitoring(
                                monitoring.databaseOneSql,
                                databasePicker.getConnection(monitoring.databaseOptionOne!!) as Connection,
                                monitoring.name
                            )
                            if (result.isNotEmpty()) monitoring.onError()
                            else monitoring.onSuccess()
                            monitoring.reschedule()
                            monitoringRepository.save(monitoring)
                        } catch (e: Exception) {
                            logController.monitoringLog(monitoring.name, e.stackTraceToString())
                        }
                    }

                }
            }

            MonitoringType.HTTP -> {

                val request = when (monitoring.httpType) {
                    HttpAllowedMethods.GET -> HttpRequest.newBuilder().uri(URI.create(monitoring.httpRequestUrl!!))
                        .timeout(Duration.ofMinutes(monitoring.httpTimeout?.toLong() ?: 5))
                        .header("Content-Type", monitoring.httpContentType).GET().build()
                    HttpAllowedMethods.POST -> {
                        HttpRequest.newBuilder().uri(URI.create(monitoring.httpRequestUrl!!))
                            .timeout(Duration.ofMinutes(monitoring.httpTimeout?.toLong() ?: 5))
                            .header("Content-Type", monitoring.httpContentType)
                            .POST(HttpRequest.BodyPublishers.ofString(monitoring.databaseOneSql)).build()
                    }
                    else -> {
                        null
                    }
                }

                try {
                    val response = HttpClient.newBuilder().build().send(request, BodyHandlers.ofString())
                    if (response.statusCode() != monitoring.httpResponseCode?.toInt()) {
                        monitoring.onError()
                    } else {
                        monitoring.onSuccess()
                    }
                    result = response.body()
                    logController.log(
                        "${response.statusCode()}",
                        null,
                        monitoring.name,
                        response.body(),
                        "${monitoring.httpType}",
                        "Monitoring::${MonitoringType.HTTP}"
                    )
                    monitoring.reschedule()
                    monitoringRepository.save(monitoring)
                } catch (e: Exception) {
                    logController.monitoringLog(monitoring.name, e.stackTraceToString())
                }

            }

            MonitoringType.DATABASE_COMPARE -> {

                val sourceResult: MutableList<String>? =
                    getComparingList(monitoring.name, monitoring.databaseOptionOne!!, monitoring.databaseOneSql)

                val targetResult: MutableList<String>? =
                    getComparingList(monitoring.name, monitoring.databaseOptionTwo!!, monitoring.databaseTwoSql)

                try {
                    ResultComparator(sourceResult!!, targetResult!!).compare().let { comparison ->
                        if (!comparison.isNullOrEmpty()) {
                            monitoring.onError()
                            result = comparison.toString()
                        } else {
                            monitoring.onSuccess()
                        }
                        monitoring.reschedule()
                        monitoringRepository.save(monitoring)
                    }
                } catch (e: Exception) {
                    logController.monitoringLog(monitoring.name, e.stackTraceToString())
                }

            }
            else -> {}
        }
        return result
    }

    private fun getComparingList(monitoring: String, database: Database, cmd: String): MutableList<String>? {
        when (database.sgbd) {
            SGBD.MongoDb -> {
                return try {
                    val resultList: MutableList<String> = ArrayList()
                    val mongodb = databasePicker.getConnection(database) as MongoDatabase
                    val command = BsonDocument(cmd, BsonInt64(1))
                    val startTime = now()
                    mongodb.runCommand(command).values.forEach { resultList.add("$it") }
                    logController.log(
                        resultList.toString(),
                        now().minusMillis(startTime.toEpochMilli()).toEpochMilli(),
                        monitoring,
                        command.toString(),
                        "NOSQL",
                        "Monitoring::${MonitoringType.DATABASE_COMPARE}"
                    )
                    resultList
                } catch (e: Exception) {
                    logController.monitoringLog(monitoring, e.stackTraceToString())
                    null
                }
            }
            else -> {
                return try {
                    dataAcessObject.executeMonitoringToList(cmd, databasePicker.getConnection(database) as Connection, monitoring)
                } catch (e: Exception) {
                    logController.monitoringLog(monitoring, e.stackTraceToString())
                    null
                }
            }

        }
    }

}
