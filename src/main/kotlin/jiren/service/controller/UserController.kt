package jiren.service.controller

import jiren.data.entity.Log
import jiren.data.entity.Permission
import jiren.data.entity.User
import jiren.data.enum.Parameters
import jiren.data.enum.PermissionType
import jiren.data.repository.parameter.ParameterRepository
import jiren.data.repository.user.PermissionRepository
import jiren.data.repository.user.UserRepository
import jiren.data.repository.user.UserSpecification
import jiren.service.security.SecurityService
import jiren.service.rocketchat.ChatAPI
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant.now
import java.util.*
import java.util.stream.Collectors

@Service
class UserController(
    private var userRepository: UserRepository,
    private var permissionRepository: PermissionRepository,
    private var parameterRepository: ParameterRepository,
    private var chatAPI: ChatAPI
) {
    private val specification = UserSpecification()
    fun search(param: String, boolean: Boolean): Page<User> {
        return userRepository.findAll(
            Specification.where(
                specification.name(param).or(
                    specification.login(param)
                )
                    .or(specification.email(param))
                    .or(specification.document(param))
            )
                .and(specification.inactive(boolean)),
            Pageable.ofSize(50)
        )
    }

    fun userAlreadyExists(name: String, username: String, email: String, document: String): Boolean {
        return userRepository.findByNameOrUsernameOrEmailOrDocument(name, username, email, document) != null
    }

    fun findById(id: Long): Optional<User> {
        return userRepository.findById(id)
    }

    fun findMenuListByRole(role: String): List<Permission> {
        return permissionRepository.listRoleMenu(role, PermissionType.MENU)
    }

    fun findByUsername(name: String): User? {
        return userRepository.findByUsername(name)
    }

    fun loggedUserHasPermission(permissionCode: String): Boolean {
        return userRepository.findByUsername(
            SecurityService().authenticatedUser?.username ?: ""
        )?.role?.permissions?.find { it.code == permissionCode } != null
    }

    fun updateStatus(user: User) {
        userRepository.save(user)
        try {
            val roomId = "${parameterRepository.findByCode("${Parameters.ROCKETCHAT_TEAM_ROOM}")}"
            chatAPI.sendMessage("${user.name} está ${user.status.toString().lowercase()}", roomId)
        } catch (e: Exception) {
            val log = Log()
            log.value = e.message
            log.user = userRepository.findByUsername(user.username)
            log.code = ChatAPI::class.simpleName
            log.instant = Timestamp.from(now())
        }
    }

    fun save(user: User): User {
        userRepository.save(user)
        return user
    }

    fun needChangePassword(username: String): Boolean {
        return userRepository.findByUsername(username)?.changePassword ?: false
    }

    fun changePassword(username: String, password: String) {
        val user = userRepository.findByUsername(username)
        if (user != null) {
            user.password = password
            user.changePassword = false
            userRepository.save(user)
        }
    }

    fun generatePassword(): String {
        val upperCaseLetters = RandomStringUtils.random(2, 65, 90, true, true)
        val lowerCaseLetters = RandomStringUtils.random(2, 97, 122, true, true)
        val numbers = RandomStringUtils.randomNumeric(2)
        val specialChar = RandomStringUtils.random(2, 33, 47, false, false)
        val totalChars = RandomStringUtils.randomAlphanumeric(2)
        val combinedChars = upperCaseLetters + lowerCaseLetters + numbers + specialChar + totalChars
        val pwdChars = combinedChars.chars()
            .mapToObj { c: Int -> c.toChar() }
            .collect(Collectors.toList())
        pwdChars.shuffle()
        return pwdChars.stream().map(Char::toString).collect(Collectors.joining())
    }

}