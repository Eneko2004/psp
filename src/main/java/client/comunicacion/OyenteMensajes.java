package client.comunicacion;

import modelo.MensajeChat;

/**
 * Interfaz de callback para la notificación asíncrona de eventos de red.
 * <p>
 * Define el contrato que debe implementar cualquier controlador o componente 
 * interesado en recibir las respuestas del servidor (mensajes entrantes, 
 * rechazos de login o desconexiones inesperadas). Permite desacoplar 
 * completamente la capa de red y sockets de la interfaz gráfica de usuario.
 * </p>
 * 
 * @author Eneko.Plaza
 * @version 1.0
 */
public interface OyenteMensajes {

    /**
     * Notifica la recepción exitosa de un paquete de mensaje enviado por el servidor.
     * <p>
     * Procesa eventos de tipo público, privado, mensajes del sistema (SERVER), 
     * errores devueltos o actualizaciones de la lista de usuarios (USERLIST).
     * </p>
     * 
     * @param mensaje Instancia de {@link MensajeChat} recibida a través del socket.
     */
    void mensajeRecibido(MensajeChat mensaje);

    /**
     * Notifica que el servidor ha denegado la solicitud de inicio de sesión (LOGIN).
     * <p>
     * Se activa ante situaciones como servidor con aforo completo o un nombre de 
     * usuario duplicado o no válido.
     * </p>
     * 
     * @param motivo Descripción textual con la causa del rechazo enviada por el servidor.
     */
    void conexionRechazada(String motivo);

    /**
     * Notifica la caída o cierre inesperado del canal de red TCP.
     * <p>
     * Se invoca cuando la conexión con el servidor se interrumpe de forma abrupta 
     * sin que el usuario haya solicitado explícitamente una desconexión (LOGOUT).
     * </p>
     */
    void conexionPerdida();
}