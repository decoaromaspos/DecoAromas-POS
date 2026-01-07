package com.decoaromas.decoaromaspos.controller;

import com.decoaromas.decoaromaspos.dto.other.response.AuthSuccessResponse;
import com.decoaromas.decoaromaspos.dto.other.response.GeneralErrorResponse;
import com.decoaromas.decoaromaspos.dto.other.response.ValidationErrorResponse;
import com.decoaromas.decoaromaspos.dto.usuario.UsuarioLoginRequest;
import com.decoaromas.decoaromaspos.dto.usuario.UsuarioResponse;
import com.decoaromas.decoaromaspos.exception.AuthException;
import com.decoaromas.decoaromaspos.model.Usuario;
import com.decoaromas.decoaromaspos.repository.UsuarioRepository;
import com.decoaromas.decoaromaspos.config.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "API para autenticación de usuarios y obtención de tokens JWT.")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Iniciar sesión", description = "Verifica las credenciales del usuario. Si son correctas, devuelve la información del usuario y un token JWT.")
    @ApiResponses(value = {
            // 200 OK: Usamos la clase dummy AuthSuccessResponse para que Swagger sepa la estructura
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthSuccessResponse.class))),

            // 400 Bad Request: Validaciones (@Valid)
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (ej. campos vacíos)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class))),

            // 401 Unauthorized: Credenciales incorrectas
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o usuario no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GeneralErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Login Fallido",
                                    summary = "Credenciales inválidas",
                                    value = """
                                            {
                                                "error": "Credenciales inválidas",
                                                "timestamp": "2025-11-29T17:49:40.18159098",
                                                "status": 401
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Credenciales de acceso")
            @Valid @RequestBody UsuarioLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(request.getUsername())
                    .orElseGet(() -> usuarioRepository.findByCorreo(request.getUsername()).orElse(null));

            if (usuario == null) {
                return ResponseEntity.status(401).body("Usuario no encontrado");
            }

            String token = jwtUtil.createToken(usuario.getCorreo(), usuario.getRol().name());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("usuario", UsuarioResponse.builder()
                    .usuarioId(usuario.getUsuarioId())
                    .nombre(usuario.getNombre())
                    .apellido(usuario.getApellido())
                    .correo(usuario.getCorreo())
                    .username(usuario.getUsername())
                    .rol(usuario.getRol())
                    .activo(usuario.getActivo())
                    .nombreCompleto(usuario.getNombre() + " " + usuario.getApellido())
                    .build()
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            throw new AuthException("Credenciales inválidas");
        }
    }
}