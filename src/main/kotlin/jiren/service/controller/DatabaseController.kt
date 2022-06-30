package jiren.service.controller

import jiren.data.entity.Database
import jiren.data.entity.Permission
import jiren.data.enum.PermissionType
import jiren.data.repository.database.DatabaseRepository
import jiren.data.repository.user.PermissionRepository
import jiren.service.security.Credentials
import jiren.service.security.EncryptionService
import org.springframework.stereotype.Controller

@Controller
class DatabaseController(
    private var databaseRepository: DatabaseRepository,
    private var permissionRepository: PermissionRepository,
    private var credentials: Credentials
) {

    fun scriptingDatabaseOptions(): List<Database> {
        return databaseRepository.findByScriptsEnabledTrue()
    }

    fun automationDatabaseOptions(): List<Database> {
        return databaseRepository.findByAutomationEnabledTrue()
    }

    fun monitoringDatabaseOptions(): List<Database> {
        return databaseRepository.findByMonitoringEnabledTrue()
    }

    fun findAll(): MutableList<Database> {
        return databaseRepository.findAll()
    }

    fun save(db: Database) {
        db.secret = EncryptionService.encrypt(db.secret, credentials.encryptionKey)!!
        databaseRepository.save(db)

        if (db.scriptsEnabled) {
            createPermissionIfNotFound("${db.name}_INSERT")
            createPermissionIfNotFound("${db.name}_UPDATE")
            createPermissionIfNotFound("${db.name}_DELETE")
            createPermissionIfNotFound("${db.name}_CALL")
        }
    }

    fun createPermissionIfNotFound(code: String): Permission {
        var permission: Permission? = permissionRepository.findByCode(code)
        if (permission == null) {
            permission = Permission()
            permission.code = code
            permission.active = 1
            permission.type = PermissionType.ACTION
            permissionRepository.save(permission)
        }
        return permission
    }
}