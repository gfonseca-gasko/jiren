package jiren.service.security

import jiren.service.security.SecurityUtils.isFrameworkInternalRequest
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal class CustomRequestCache : HttpSessionRequestCache() {
    override fun saveRequest(request: HttpServletRequest, response: HttpServletResponse) {
        if (!isFrameworkInternalRequest(request) || !request.requestURL.contains("/login")) {
            super.saveRequest(request, response)
        }
    }
}