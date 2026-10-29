package com.acmestack.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(AccessDeniedException.class)
    public void accessDenied(AccessDeniedException e) { throw e; }

    @ExceptionHandler(AuthenticationException.class)
    public void authentication(AuthenticationException e) { throw e; }


    @ExceptionHandler(NotFoundException.class)
    public Object notFound(NotFoundException e, HttpServletRequest req) {
        return respond(req, HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    /** A rule was broken and no controller caught it. */
    @ExceptionHandler(BusinessException.class)
    public Object business(BusinessException e, HttpServletRequest req) {
        return respond(req, HttpStatus.UNPROCESSABLE_ENTITY, "That can't be done", e.getMessage());
    }

    /** repo.findById(id).orElseThrow() with no argument. */
    @ExceptionHandler(NoSuchElementException.class)
    public Object missing(NoSuchElementException e, HttpServletRequest req) {
        return respond(req, HttpStatus.NOT_FOUND, "Not found", "That page or item doesn't exist.");
    }

    /** ?status=abc, ?roleId=x ... The value itself is never echoed back. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object badParameter(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return respond(req, HttpStatus.BAD_REQUEST, "Invalid request",
                "The value of '" + e.getName() + "' isn't valid.");
    }

    /** Too long, or a duplicate that slipped past the checks. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object integrity(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Data integrity problem on {} {}: {}", req.getMethod(), req.getRequestURI(),
                e.getMostSpecificCause().getClass().getSimpleName());
        return respond(req, HttpStatus.CONFLICT, "Can't save that",
                "The data is too long or already exists. Check it and try again.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object tooLarge(MaxUploadSizeExceededException e, HttpServletRequest req) {
        return respond(req, HttpStatus.UNPROCESSABLE_ENTITY, "File too large",
                "The file is too large. The limit is 5 MB.");
    }


    @ExceptionHandler(Exception.class)
    public Object unexpected(Exception e, HttpServletRequest req) {
        if (e instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            HttpStatus s = HttpStatus.resolve(er.getStatusCode().value());
            if (s == null) s = HttpStatus.BAD_REQUEST;
            return switch (s) {
                case NOT_FOUND -> respond(req, s, "Not found", "That page or item doesn't exist.");
                case METHOD_NOT_ALLOWED -> respond(req, s, "Not allowed", "That action isn't available this way.");
                default -> respond(req, s, "Invalid request", "The request wasn't valid.");
            };
        }
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), e);
        return respond(req, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong",
                "An unexpected error occurred and was logged.");
    }

    /** htmx requests get plain text (the page shows it in an alert). Everyone else gets an error page. */
    private Object respond(HttpServletRequest req, HttpStatus status, String title, String message) {
        if ("true".equals(req.getHeader("HX-Request"))) {
            return ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(message);
        }
        ModelAndView mv = new ModelAndView("error/generic");
        mv.setStatus(status);
        mv.addObject("status", status.value());
        mv.addObject("title", title);
        mv.addObject("message", message);
        return mv;
    }
}