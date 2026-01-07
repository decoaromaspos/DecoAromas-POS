package com.decoaromas.decoaromaspos.utils;

public final class SecurityConstants {

    // Previene que alguien instancie esta clase de utilidad
    private SecurityConstants() {}

    // Expresiones para roles individuales
    public static final String IS_ADMIN = "hasRole('ADMIN')";
    public static final String IS_SUPER_ADMIN = "hasRole('SUPER_ADMIN')";
    public static final String IS_VENDEDOR = "hasRole('VENDEDOR')";

    // Expresiones combinadas para roles
    public static final String IS_ADMIN_OR_SUPER_ADMIN = "hasAnyRole('ADMIN', 'SUPER_ADMIN')";
    public static final String IS_VENDEDOR_OR_ADMIN_OR_SUPER_ADMIN = "hasAnyRole('VENDEDOR', 'ADMIN', 'SUPER_ADMIN')";
    public static final String IS_VENDEDOR_OR_ADMIN = "hasAnyRole('VENDEDOR', 'ADMIN')";
    public static final String IS_AUTHENTICATED = "isAuthenticated()";

}