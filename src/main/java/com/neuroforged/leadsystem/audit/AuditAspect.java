package com.neuroforged.leadsystem.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroforged.leadsystem.entity.AuditEvent;
import com.neuroforged.leadsystem.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Around("within(@org.springframework.web.bind.annotation.RestController *) && " +
            "((@annotation(org.springframework.web.bind.annotation.PostMapping)) || " +
            "(@annotation(org.springframework.web.bind.annotation.PutMapping)) || " +
            "(@annotation(org.springframework.web.bind.annotation.PatchMapping)) || " +
            "(@annotation(org.springframework.web.bind.annotation.DeleteMapping)))")
    public Object auditMutatingRequest(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();

        try {
            String actorEmail = resolveActor();
            String httpMethod = resolveHttpMethod(pjp);
            String requestPath = resolveRequestPath();
            String action = httpMethod + " " + requestPath;
            String diffJson = serializeArgs(pjp.getArgs());

            AuditEvent event = AuditEvent.builder()
                    .actorEmail(actorEmail)
                    .action(action)
                    .entityType(resolveEntityType(requestPath))
                    .entityId(resolveEntityId(requestPath))
                    .requestPath(requestPath)
                    .httpMethod(httpMethod)
                    .diffJson(diffJson)
                    .build();

            auditEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Audit logging failed (non-fatal): {}", e.getMessage());
        }

        return result;
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "anonymous";
        return auth.getName();
    }

    private String resolveHttpMethod(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        if (method.isAnnotationPresent(PostMapping.class))   return "POST";
        if (method.isAnnotationPresent(PutMapping.class))    return "PUT";
        if (method.isAnnotationPresent(PatchMapping.class))  return "PATCH";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        return "UNKNOWN";
    }

    private String resolveRequestPath() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "";
        HttpServletRequest req = attrs.getRequest();
        return req.getRequestURI();
    }

    /** Best-effort: extract the first path segment after /api/ as entity type. */
    private String resolveEntityType(String path) {
        if (path == null || path.isBlank()) return null;
        String[] parts = path.split("/");
        // e.g. /api/leads/1 → "leads"; /api/clients/2/... → "clients"
        for (int i = 0; i < parts.length; i++) {
            if ("api".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    /** Best-effort: last numeric segment of the path as entity ID. */
    private String resolveEntityId(String path) {
        if (path == null || path.isBlank()) return null;
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+")) return parts[i];
        }
        return null;
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) return null;
        for (Object arg : args) {
            if (arg != null && !(arg instanceof HttpServletRequest)) {
                try {
                    return objectMapper.writeValueAsString(arg);
                } catch (JsonProcessingException e) {
                    return arg.toString();
                }
            }
        }
        return null;
    }
}
