package jiren.service.security

import jiren.data.entity.Parameter
import jiren.data.entity.Permission
import jiren.data.entity.Role
import jiren.data.entity.User
import jiren.data.enum.Parameters
import jiren.data.enum.PermissionType
import jiren.data.repository.parameter.ParameterRepository
import jiren.data.repository.user.PermissionRepository
import jiren.data.repository.user.RoleRepository
import jiren.data.repository.user.UserRepository
import jiren.view.*
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SetupDataLoader(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val privilegeRepository: PermissionRepository,
    private val parameterRepository: ParameterRepository
) : ApplicationListener<ContextRefreshedEvent?> {
    private var alreadySetup = false

    @Transactional
    override fun onApplicationEvent(event: ContextRefreshedEvent) {

        if (alreadySetup) return
        val views = listOf(
            SqlView::class.java.simpleName,
            AutomationView::class.java.simpleName,
            MonitoringView::class.java.simpleName,
            ShiftView::class.java.simpleName,
            UserView::class.java.simpleName,
            ConfigurationView::class.java.simpleName,
            LogView::class.java.simpleName
        )
        val writePrivilege: Permission = createPrivilegeIfNotFound("WRITE_PRIVILEGE")
        val deletePrivilege: Permission = createPrivilegeIfNotFound("DELETE_PRIVILEGE")
        val executePrivilege: Permission = createPrivilegeIfNotFound("EXECUTE_PRIVILEGE")

        val adminPrivileges: MutableList<Permission> =
            listOf(
                writePrivilege,
                deletePrivilege,
                executePrivilege
            ).toMutableList()

        views.forEach {
            adminPrivileges.add(createViewsIfNotFound(it, views.indexOf(it)))
        }

        createRoleIfNotFound("ADMIN", adminPrivileges)
        createRoleIfNotFound("TEAM", listOf(executePrivilege))
        createRoleIfNotFound("USER", listOf(executePrivilege))

        val adminRole: Role? = roleRepository.findByName("ADMIN")
        var defaultUser = parameterRepository.findByCode("${Parameters.SYSTEM_USERNAME}")?.value
        if (defaultUser.isNullOrEmpty()) defaultUser = "jiren"
        if (userRepository.findByUsername(defaultUser) == null) {
            val user = User()
            user.name = defaultUser
            user.username = defaultUser
            user.password = defaultUser
            user.email = "jiren@mobly.com.br"
            user.role = adminRole
            user.enabled = true
            user.document = "000.000.000-00"
            userRepository.save<User>(user)
        }
        createParametersIfNotFound()
        alreadySetup = true
    }

    @Transactional
    fun createPrivilegeIfNotFound(name: String?): Permission {
        var privilege: Permission? = privilegeRepository.findByCode(name.toString())
        if (privilege == null) {
            privilege = Permission()
            privilege.code = name!!
            privilege.type = PermissionType.AUTHORITY
            privilege.active = 1
            privilegeRepository.save(privilege)
        }
        return privilege
    }

    @Transactional
    fun createRoleIfNotFound(
        name: String, privileges: List<Permission>
    ): Role? {
        var role: Role? = name.let { roleRepository.findByName(it) }
        if (role == null) {
            role = Role()
            role.name = name
            role.permissions = privileges.toMutableList()
            roleRepository.save(role)
        }
        return role
    }

    @Transactional
    fun createParametersIfNotFound() {
        Parameters.values().forEach {
            if (parameterRepository.findByCode(it.toString()) == null) {
                val p = Parameter()
                p.code = it.toString()
                parameterRepository.save(p)
            }
        }
    }

    @Transactional
    fun createViewsIfNotFound(code: String, position: Int): Permission {
        var view: Permission? = privilegeRepository.findByCode(code)
        if (view == null) {
            view = Permission()
            view.code = code
            view.position = position
            view.description = code.substringBefore("View")
            view.active = 1
            view.type = PermissionType.MENU
            privilegeRepository.save(view)
        }
        return view
    }

}