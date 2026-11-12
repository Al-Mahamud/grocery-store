package com.haat.store.customer;

import com.haat.store.shared.CurrentCustomer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/** Starts and ends server-side sessions (stored in PostgreSQL by Spring Session). */
@Component
class LoginSessions {

    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    LoginSessions(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    void start(long customerId, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            // A new session id at sign-in prevents session fixation.
            request.changeSessionId();
        }
        SecurityContext context = holder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                CurrentCustomer.PREFIX + customerId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        holder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        holder.clearContext();
    }

    /** Signs the customer out everywhere except this browser (after a password change). */
    void endOthers(long customerId, HttpServletRequest request) {
        HttpSession current = request.getSession(false);
        String keep = current == null ? "" : current.getId();
        sessions.findByPrincipalName(CurrentCustomer.PREFIX + customerId).keySet().stream()
                .filter(id -> !id.equals(keep))
                .forEach(sessions::deleteById);
    }
}
