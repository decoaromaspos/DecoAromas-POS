package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.request.ActivateIdRequest;
import com.decoaromas.decoaromaspos.dto.other.response.*;
import com.decoaromas.decoaromaspos.dto.other.request.EmailRequest;
import com.decoaromas.decoaromaspos.dto.other.response.AvailabilityResponse;
import com.decoaromas.decoaromaspos.dto.usuario.*;
import com.decoaromas.decoaromaspos.exception.ResourceNotFoundException;
import com.decoaromas.decoaromaspos.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.decoaromas.decoaromaspos.utils.SecurityConstants.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Gestión de Usuarios", description = "API para administración de usuarios, roles, contraseñas y validaciones de cuenta.")
@ApiResponses(value = {
        @ApiResponse(responseCode = "401", description = "Usuario no autenticado",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = UnauthorizedResponse.class),
                        examples = @ExampleObject(value = "{\"path\": \"/api/usuarios\", \"error\": \"No autorizado\", \"status\": 401}")
                )
        )
})
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Listar usuarios (sin super admin)", description = "Obtiene una lista de usuarios excluyendo al rol SUPER_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class))))
    @GetMapping
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<UsuarioResponse>> listarUsuariosNoSuperAdmin(){
        return ResponseEntity.ok(usuarioService.listarUsuariosNoSuperAdmin());
    }

    @Operation(summary = "Listar todos los usuarios", description = "Obtiene una lista completa de usuarios incluyendo super administradores.")
    @ApiResponse(responseCode = "200", description = "Lista completa obtenida",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class))))
    @GetMapping("/super")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(){
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @Operation(summary = "Listar usuarios activos", description = "Devuelve solo los usuarios que están habilitados para operar.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class))))
    @GetMapping("/activos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<UsuarioResponse>> listarUsuariosActivos(){
        return ResponseEntity.ok(usuarioService.listarUsuariosActivos());
    }

    @Operation(summary = "Listar usuarios inactivos", description = "Devuelve solo los usuarios que están deshabilitados.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class))))
    @GetMapping("/inactivos")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<UsuarioResponse>> listarUsuariosInactivos(){
        return ResponseEntity.ok(usuarioService.listarUsuariosInactivos());
    }

    @Operation(summary = "Obtener usuario por ID", description = "Devuelve el detalle de un usuario específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Username no existe",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"No existe usuario con id 1\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )
                    ))
    })
    @GetMapping("/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorId(@PathVariable Long id){
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    @Operation(summary = "Obtener usuario por username", description = "Busca un usuario exacto por su nombre de usuario (login).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Username no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    summary = "Username no disponible",
                                    value = "{\"error\": \"Usuario 'username' no encontrado\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )))
    })
    @GetMapping("/username/{username}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorUsername(@PathVariable String username) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorUsername(username));
    }

    @Operation(summary = "Obtener usuario por correo", description = "Busca un usuario exacto por su email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "Correo no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples = @ExampleObject(
                                    value = "{\"error\": \"Usuario con correo correo@mail.com no encontrado\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )))
    })
    @GetMapping("/correo")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorCorreo(@Valid @RequestBody EmailRequest request) {
        return  ResponseEntity.ok(usuarioService.obtenerUsuarioPorCorreo(request));
    }

    @Operation(summary = "Buscar usuarios por nombre parcial", description = "Devuelve usuarios que coincidan parcialmente con el nombre.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios buscados por nombre parcial",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class))))
    @GetMapping("/buscar/nombre/{nombre}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<UsuarioResponse>> buscarUsuariosPorNombre(@PathVariable String nombre) {
        return ResponseEntity.ok(usuarioService.buscarUsuariosPorNombreParcial(nombre));
    }

    @Operation(summary = "Buscar usuarios por nombre y apellido parcial", description = "Devuelve usuarios que coincidan parcialmente con el nombre y apellido.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios buscados por nombre y apellido parcial",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class))))
    @GetMapping("/buscar/nombre/{nombre}/apellido/{apellido}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<List<UsuarioResponse>> buscarUsuariosPorNombreYApellido(@PathVariable String nombre, @PathVariable String apellido) {
        return ResponseEntity.ok(usuarioService.buscarUsuariosPorNombreYApellidoParcial(nombre, apellido));
    }

    @Operation(summary = "Listar usuarios paginados y filtrados", description = "Búsqueda avanzada de usuarios mediante filtros en el cuerpo de la petición.")
    @ApiResponse(responseCode = "200", description = "Resultados paginados",
            content = @Content(schema = @Schema(implementation = PaginacionResponse.class)))
    @PostMapping("/filtros/paginas")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<PaginacionResponse<UsuarioResponse>> getUsuariosFiltrados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "usuarioId") String sortBy,
            @Valid @RequestBody UsuarioFilterDTO dto) {
        return ResponseEntity.ok(usuarioService.getUsuariosFiltradosPaginados(page, size, sortBy, dto));
    }

    @Operation(summary = "Crear usuario", description = "Registra un nuevo usuario en el sistema. Valida unicidad de username y correo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class),
                    examples = @ExampleObject(value = "{\"error\": \"El correo no puede estar vacío.\", \"status\": 400}"))),
            @ApiResponse(responseCode = "409", description = "Conflicto: Username o Correo ya existen",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(summary = "Datos duplicados", value = "{\"error\": \"El correo admin@mail.com ya está registrado.\", \"status\": 409}")))
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearUsuario(@Valid @RequestBody UsuarioRequest request) {
        return new ResponseEntity<>(usuarioService.registrarUsuario(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar contraseña", description = "Permite a un usuario cambiar su propia contraseña validando la anterior.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Contraseña actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error: Contraseña actual incorrecta o nueva muy corta",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(value = "{\"error\": \"La contraseña actual es incorrecta\", \"status\": 400}"))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"Usuario no encontrado\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )))
    })
    @PostMapping("/{id}/password")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<Void> actualizarPassword(@PathVariable Long id, @Valid @RequestBody UsuarioPasswordRequest request) {
        usuarioService.actualizarMiPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Actualizar usuario", description = "Modifica datos básicos (nombre, username, correo). No modifica el Rol.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Actualizado exitosamente", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Permisos insuficientes para modificar a este usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"error\": \"El usuario SUPER_ADMIN no puede ser modificado\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 403}"
                            ))),
            @ApiResponse(responseCode = "404", description = "Username no existe",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"No existe usuario con id 1\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            ))),
            @ApiResponse(responseCode = "409", description = "Conflicto: Nuevo username/correo ya en uso por otro usuario")
    })
    @PutMapping("/update/{id}")
    @PreAuthorize(IS_AUTHENTICATED)
    public ResponseEntity<UsuarioResponse> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarUsuarioNoRol(id,request));
    }

    @Operation(summary = "Actualizar rol de usuario", description = "Modifica el rol de un usuario aplicando reglas jerárquicas estrictas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "403", description = "Operación no permitida por reglas de negocio",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "Auto-modificación", summary = "Intenta cambiar su propio rol", value = "{\"error\": \"No puedes cambiar tu propio rol.\", \"status\": 403}"),
                                    @ExampleObject(name = "Jerarquía", summary = "Admin intenta editar a Admin", value = "{\"error\": \"Como Administrador, solo puedes gestionar a usuarios con el rol de Vendedor.\", \"status\": 403}")
                            })),
            @ApiResponse(responseCode = "404", description = "Username no existe",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"No existe usuario con id 1\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )))
    })
    @PutMapping("/update/rol")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<UsuarioResponse> actualizarRolDeUsuario(@Valid @RequestBody UsuarioUpdateRol request) {
        return ResponseEntity.ok(usuarioService.actualizarRolDeUsuario(request));
    }

    @Operation(summary = "Cambiar estado activo", description = "Habilitar o deshabilitar acceso al sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para modificar a este usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"error\": \"No puedes cambiar tu propio rol.\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 403}"
                            ))),
            @ApiResponse(responseCode = "404", description = "Username no existe",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"No existe usuario con id 1\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            )))
    })
    @PutMapping("/cambiar/estado")
    @PreAuthorize(IS_ADMIN_OR_SUPER_ADMIN)
    public ResponseEntity<UsuarioResponse> cambiarEstadoActivo(@Valid @RequestBody ActivateIdRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarEstadoActivo(request.getId(), request.getActivo()));
    }

    @Operation(summary = "Eliminar usuario", description = "Eliminación física del usuario. Requiere rol SUPER_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "403", description = "Requiere rol SUPER_ADMIN"),
            @ApiResponse(responseCode = "404", description = "Username no existe",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResourceNotFoundException.class),
                            examples =  @ExampleObject(
                                    value = "{\"error\": \"No existe usuario con id 1\", \"timestamp\": \"2025-12-20T23:28:39.364143806\", \"status\": 404}"
                            ))),
            @ApiResponse(responseCode = "500", description = "Error de integridad (ej. usuario tiene ventas asociadas)")
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize(IS_SUPER_ADMIN)
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verificar disponibilidad de correo", description = "Comprueba si un correo electrónico está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Correo disponible", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"Correo disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "Correo ya existe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"Correo ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-correo")
    public ResponseEntity<AvailabilityResponse> checkCorreoAvailability(@Valid @RequestParam String correo) {
        AvailabilityResponse response = usuarioService.checkCorreoAvailability(correo);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
        }

        return new ResponseEntity<>(response, HttpStatus.OK); // 200 OK
    }

    @Operation(summary = "Verificar disponibilidad de username", description = "Comprueba si un username está disponible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Username disponible", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": true, \"message\": \"Username disponible.\"}"))),
            @ApiResponse(responseCode = "409", description = "Username ya existe", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AvailabilityResponse.class),
                    examples = @ExampleObject(value = "{\"available\": false, \"message\": \"Username ya en uso. Ingrese otro.\"}")))
    })
    @GetMapping("/check-username")
    public ResponseEntity<AvailabilityResponse> checkUsernameAvailability(@Valid @RequestParam String username) {
        AvailabilityResponse response = usuarioService.checkUsernameAvailability(username);

        if (!response.isAvailable()) {
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
