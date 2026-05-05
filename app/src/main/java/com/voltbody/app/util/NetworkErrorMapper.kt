package com.voltbody.app.util

object NetworkErrorMapper {
    fun parse(error: Throwable?): String {
        val msg = error?.message ?: return "Error desconocido"
        val lowerMsg = msg.lowercase()
        
        return when {
            lowerMsg.contains("unable to resolve host") || 
            lowerMsg.contains("no address associated") ||
            lowerMsg.contains("network is unreachable") -> "Sin conexión a internet"
            
            lowerMsg.contains("timeout") || 
            lowerMsg.contains("connect") ||
            lowerMsg.contains("failed to connect") -> "No se puede conectar al servidor"
            
            lowerMsg.contains("401") || 
            lowerMsg.contains("invalid credentials") -> "Correo o contraseña incorrectos"
            
            lowerMsg.contains("409") || 
            lowerMsg.contains("already exists") -> "Este correo ya está registrado"
            
            else -> "Error: $msg"
        }
    }
}
