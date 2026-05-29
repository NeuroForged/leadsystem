package com.neuroforged.leadsystem.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * SSRF guard for client-supplied outbound URLs (e.g. {@code Client.webhookUrl}).
 *
 * <p>A URL is considered safe only if its scheme is {@code https} and every IP its
 * host resolves to is publicly routable — i.e. not loopback, link-local, site-local
 * (RFC 1918), the CGNAT range (100.64.0.0/10), the cloud metadata address
 * (169.254.169.254), nor any wildcard/multicast/unspecified address. Resolution is
 * performed at validation time; callers that fire the request immediately after a
 * passing check keep the DNS-rebinding window small.
 */
public final class SafeUrl {

    private SafeUrl() {}

    /** Returns true if {@code rawUrl} is an https URL whose host resolves only to public IPs. */
    public static boolean isPublicHttps(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }

        final URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }

        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return false;
        }
        if (addresses.length == 0) {
            return false;
        }

        for (InetAddress addr : addresses) {
            if (!isPublicAddress(addr)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPublicAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            return false;
        }

        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int o0 = b[0] & 0xFF;
            int o1 = b[1] & 0xFF;
            // 169.254.169.254 (cloud metadata) is link-local and already blocked above,
            // but guard the whole 169.254/16 explicitly for clarity.
            if (o0 == 169 && o1 == 254) {
                return false;
            }
            // CGNAT shared address space 100.64.0.0/10 (RFC 6598) — not isSiteLocal.
            if (o0 == 100 && o1 >= 64 && o1 <= 127) {
                return false;
            }
        }
        return true;
    }
}
