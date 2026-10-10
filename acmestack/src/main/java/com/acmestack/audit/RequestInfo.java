package com.acmestack.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

record RequestInfo(String ip, String location, String device, String userAgent) {

    static RequestInfo current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return new RequestInfo(null, null, null, null);
        }
        HttpServletRequest req = attrs.getRequest();
        String ip = req.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) ip = "127.0.0.1";
        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 180) ua = ua.substring(0, 180);
        return new RequestInfo(ip, location(ip), device(ua), ua);
    }

    private static String location(String ip) {
        if (ip == null) return null;
        boolean local = ip.equals("127.0.0.1") || ip.startsWith("10.") || ip.startsWith("192.168.")
                || ip.matches("^172\\.(1[6-9]|2\\d|3[01])\\..*");
        return local ? "Local network" : "Unknown";
    }

    private static String device(String ua) {
        if (ua == null) return null;
        String browser = ua.contains("Edg/") ? "Edge"
                : ua.contains("OPR/") ? "Opera"
                : ua.contains("Firefox/") ? "Firefox"
                : ua.contains("Chrome/") ? "Chrome"
                : ua.contains("Safari/") ? "Safari" : "Browser";
        String os = ua.contains("Windows") ? "Windows"
                : ua.contains("Android") ? "Android"
                : (ua.contains("iPhone") || ua.contains("iPad")) ? "iOS"
                : ua.contains("Mac OS X") ? "macOS"
                : ua.contains("Linux") ? "Linux" : "unknown OS";
        return browser + " on " + os;
    }
}