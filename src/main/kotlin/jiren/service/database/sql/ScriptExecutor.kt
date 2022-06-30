package jiren.service.database.sql

import jiren.service.controller.UserController
import jiren.data.entity.Database
import jiren.data.enum.SGBD
import jiren.service.database.DatabasePicker
import org.springframework.stereotype.Component
import java.sql.Connection

@Component
class ScriptExecutor(var dao: DataAcessObject, var userController: UserController, var databasePicker: DatabasePicker) {
    fun execute(sql: String, db: Database, task: String): String {
        val batchList: MutableList<String> = sql.split(";").toMutableList()
        validate(batchList, db).let { if (it !== null) return it }
        try {
            if (db.sgbd == SGBD.MongoDb) throw Exception("Banco de dados NoSQL não suportado")
            dao.executeScript(batchList, databasePicker.getConnection(db) as Connection, task)
                .let { return "SUCCESS!#!$it" }
        } catch (e: Exception) {
            return "ERROR!#!${e.message}"
        }
    }

    private fun validate(batchList: MutableList<String>, db: Database): String? {
        if (batchList.size > 1 && (batchList.last().trim() === ";") || batchList.last().trim().isEmpty()) {
            batchList.removeAt(batchList.lastIndex)
        }
        batchList.forEach { query ->
            val commandType = query.trimStart().substringBefore(" ", "").uppercase()
            if ((!userController.loggedUserHasPermission("${db}_$commandType")) && commandType != "SET") {
                return "Você não tem permissão para executar $commandType"
            }

            when (commandType) {
                "INSERT" -> {
                    if ((query.contains("SELECT ", ignoreCase = true)) || (query.contains(
                            "SELECT\n",
                            ignoreCase = true
                        ))
                    ) {
                        if (!(query.contains("WHERE ", ignoreCase = true)) && !(query.contains(
                                "WHERE\n",
                                ignoreCase = true
                            ))
                        ) {
                            return "Cannot execute select without WHERE closure!"
                        }
                    }
                }
                "UPDATE" -> {
                    if (!(query.contains("WHERE ", ignoreCase = true)) && !(query.contains(
                            "WHERE\n",
                            ignoreCase = true
                        ))
                    ) {
                        return "Cannot execute update without WHERE closure!"
                    }
                    if (!(query.contains("SET ", ignoreCase = true)) && !(query.contains("SET\n", ignoreCase = true))) {
                        return "Cannot execute update without SET values"
                    }
                    if ((query.contains("LIMIT ", ignoreCase = true) || (query.contains(
                            "LIMIT\n",
                            ignoreCase = true
                        )))
                    ) {
                        return "LIMIT is defined by system default, cannot define new limit"
                    }
                }
                "DELETE" -> {
                    if (!(query.contains("WHERE ", ignoreCase = true)) && !(query.contains(
                            "WHERE\n",
                            ignoreCase = true
                        ))
                    ) {
                        return "Cannot execute update without WHERE closure!"
                    }
                    if ((query.contains("LIMIT ", ignoreCase = true)) || (query.contains(
                            "LIMIT\n",
                            ignoreCase = true
                        ))
                    ) {
                        return "LIMIT is defined by system default, cannot define new limit"
                    }
                }
                "SET" -> {}
                "CALL" -> {}
                else -> return "Command not found"
            }
        }
        return null
    }

}