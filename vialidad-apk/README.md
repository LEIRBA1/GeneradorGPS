# Vialidad Tecomán · Android

Contenedor Android para la app web existente.

Protecciones incluidas:
- R8/ProGuard en release.
- Capturas y grabación de pantalla bloqueadas mediante FLAG_SECURE.
- Depuración de WebView desactivada.
- HTTP sin cifrar bloqueado.
- Acceso local a archivos desactivado.
- Navegación restringida a HTTPS y dominios necesarios de Google.
- Copias de seguridad Android desactivadas.

Importante: ningún APK puede hacerse imposible de copiar o analizar. La seguridad de datos y permisos debe validarse también en el servidor, por usuario y rol.

El workflow genera un APK release para validación. La versión final de producción debe firmarse con un keystore maestro privado y conservar esa misma firma para todas las actualizaciones futuras.
