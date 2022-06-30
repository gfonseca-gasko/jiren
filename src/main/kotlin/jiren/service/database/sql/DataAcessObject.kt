package jiren.service.database.sql

import jiren.data.enum.MonitoringType
import jiren.data.enum.Parameters
import jiren.data.repository.parameter.ParameterRepository
import jiren.service.controller.LogController
import org.springframework.stereotype.Component
import java.io.IOException
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import java.time.Instant.now
import java.util.*
import javax.annotation.PostConstruct

@Component
class DataAcessObject(private val logController: LogController, private val parameterRepository: ParameterRepository) {
    private lateinit var limit: String

    @PostConstruct
    fun setParameters() {
        try {
            this.limit = parameterRepository.findByCode(Parameters.SQL_UPDATE_LIMIT.toString()).toString()
        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun executeAutomation(sql: String, connection: Connection, name: String): List<Int> {
        val transact: Statement = connection.createStatement()
        val queryList = sql.split(";").toMutableList()
        queryList.removeAt(queryList.lastIndex)
        queryList.indices.forEach { transact.addBatch(queryList[it]) }
        val affectedRows: List<Int>
        val resultLog = StringBuilder()
        try {
            val start = now().toEpochMilli()
            affectedRows = transact.executeBatch().toList()
            val elapsedTime = (now().toEpochMilli() - start)
            affectedRows.indices.forEach { x -> resultLog.appendLine("Query ${x + 1} affected ${affectedRows[x]} lines") }
            logController.log(resultLog.toString(), elapsedTime, name, sql, "SQL_AUTOMATION", "SQL")
        } catch (e: Exception) {
            throw IOException(e.message)
        } finally {
            transact.close()
            connection.close()
        }
        return affectedRows
    }

    fun executeMonitoring(sql: String, connection: Connection, name: String): String {
        lateinit var rs: ResultSet
        val transact = connection.prepareStatement(sql)
        val objList: ArrayList<GenericDto> = ArrayList()

        try {
            val start = now().toEpochMilli()
            rs = transact.executeQuery()
            val elapsedTime = (now().toEpochMilli() - start)
            logController.log(
                "Fetch size: ${rs.fetchSize}",
                elapsedTime,
                name,
                sql,
                "SELECT",
                "Monitoring::${MonitoringType.DATABASE}"
            )
            while (rs.next()) {
                val obj = GenericDto()
                for (i in 1..rs.metaData.columnCount) {
                    try {
                        if (rs.getObject(i) != null && rs.getObject(i).toString().isNotEmpty()) {
                            obj.props.setProperty(rs.metaData.getColumnLabel(i), rs.getObject(i).toString())
                        } else {
                            obj.props.setProperty(rs.metaData.getColumnLabel(i), " ")
                        }
                    } catch (e: Exception) {
                        obj.props.setProperty(rs.metaData.getColumnLabel(i), " ")
                        throw (e)
                    }
                }
                objList.add(obj)
            }
        } catch (e: Exception) {
            throw (e)
        } finally {
            connection.close()
            transact.close()
            rs.close()
        }

        val csv = StringBuilder()
        if (objList.isNotEmpty()) {
            csv.appendLine(objList.first().props.stringPropertyNames().toString())
            objList.indices.forEach { csv.appendLine(objList[it].props.values) }
            csv.chars().forEach { _ ->
                if (csv.contains("[")) csv.deleteAt(csv.indexOf("["))
                if (csv.contains("]")) csv.deleteAt(csv.indexOf("]"))
            }
        }
        return csv.toString()
    }

    fun executeMonitoringToList(sql: String, connection: Connection, name: String): MutableList<String> {
        lateinit var rs: ResultSet
        val transact = connection.prepareStatement(sql)
        val objList: MutableList<String> = ArrayList()

        try {
            val start = now().toEpochMilli()
            rs = transact.executeQuery()
            val elapsedTime = (now().toEpochMilli() - start)
            logController.log(
                "Fetch size: ${rs.fetchSize}",
                elapsedTime,
                name,
                sql,
                "SELECT",
                "Monitoring::${MonitoringType.DATABASE_COMPARE}"
            )
            while (rs.next()) {
                objList.add(rs.getObject(1).toString())
            }
        } catch (e: Exception) {
            throw (e)
        } finally {
            connection.close()
            transact.close()
            rs.close()
        }
        return objList
    }

    fun executeScript(batchList: List<String>, connection: Connection, task: String?): String {
        val response = StringBuilder()
        val transact: Statement = connection.createStatement()
        var sql: String
        try {
            batchList.indices.forEach { i ->
                sql = batchList[i]
                val commandType = sql.trimStart().substringBefore(" ", "").uppercase()
                if (commandType == "UPDATE" || commandType == "DELETE") {
                    sql.plus(" LIMIT $limit")
                }
                val start = now().toEpochMilli()
                try {
                    transact.executeUpdate(sql).let { rowCount ->
                        val logValue = "Query ${(batchList.indices.indexOf(i) + 1)} affected $rowCount rows"
                        response.appendLine(logValue)
                        val elapsedTime = (now().toEpochMilli() - start)
                        logController.log(logValue, elapsedTime, task, sql, commandType, "SQLLogger")
                    }
                } catch (e: Exception) {
                    val error = "Query ${(batchList.indices.indexOf(i) + 1)} -> ${e.message.toString()}"
                    response.appendLine(error)
                    logController.log(error, null, task, sql, commandType, "SQLLogger")
                    throw (e)
                }
            }
        } catch (e: Exception) {
            return response.toString()
        } finally {
            transact.close()
            connection.close()
        }
        return response.toString()
    }

    private class GenericDto {
        var props = Properties()
    }

}