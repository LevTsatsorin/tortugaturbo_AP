# Tortuga Turbo 🐢

*Lenta de nombre, turbo de corazón.*

App de logística para Android: los envíos avanzan solos por distintas ciudades de Argentina en segundo plano, se ven en vivo en dos teléfonos a la vez, avisan con notificaciones y se identifican con un código QR.

## Equipo

Lev Tsatsorin y Ainur Munasipov — Aplicaciones Móviles, TP2.

## Funcionalidades

- Login y registro con Firebase Auth, sin cuentas duplicadas.
- Roles Cliente y Operador, con permisos distintos.
- CRUD de usuarios: perfil propio, el operador gestiona cuentas y puede desactivarlas.
- CRUD de envíos: crear, editar, cancelar y eliminar.
- Simulación en segundo plano con WorkManager: el envío cambia de estado aunque cierres la app.
- Tiempo real con Firestore: lo que pasa en un teléfono se ve en el otro al instante.
- Notificaciones en cada novedad del envío.
- QR: etiqueta del envío y escáner para buscarlo.
- Validaciones en todos los formularios.
- Todos los textos de la interfaz en `strings.xml`.

## Tecnologías

- Java 11 y XML (Material Components 1.10.0, RecyclerView 1.4.0, ConstraintLayout 2.1.4)
- Firebase Auth + Firestore (BoM 35.2.0)
- WorkManager 2.12.0
- ZXing core 3.5.3 (generación de QR)
- Google code scanner 16.1.0 (lectura de QR)
- Android Gradle Plugin 9.4.0, minSdk 24, targetSdk 37

## Cómo ejecutarlo

1. Abrí Android Studio (versión reciente) y elegí la carpeta del proyecto.
2. Esperá a que termine el Gradle sync.
3. Corré la app en un emulador con imagen **Google Play** o en un celular real.

`google-services.json` ya está incluido y hace falta conexión a internet. El escáner QR anda mucho mejor en un celular real.

## Cuenta de demostración

Operador: `operador@tortugaturbo.com` / `Tortuga2026`

Los registros nuevos siempre son Cliente.

## Hilos secundarios

| Tarea | Dónde corre |
|---|---|
| Paso de la simulación (Firestore, notificación, próximo paso) | `SimulacionWorker`, hilo de WorkManager |
| Creación de un envío (tracking, tarifa, transacción) | `AppExecutors.background()` |
| Eliminación de la cuenta propia | `AppExecutors.background()` |
| Generación del QR | `AppExecutors.background()` |
| Login, registro, perfil y listas | API asíncrona de Firebase |

El resultado vuelve al hilo principal con `AppExecutors.main()` o con listeners de Firestore.

## Estructura

Paquete único `com.example.tortugaturbo`, una Activity por pantalla:

- `LoginActivity`: ingreso y entrada a la app.
- `RegistroActivity`: alta de cuenta de cliente.
- `HomeActivity`: tablero con la lista de envíos y el menú.
- `EnvioFormActivity`: alta y edición de un envío.
- `EnvioDetalleActivity`: estado en vivo, línea de tiempo, QR y acciones.
- `PerfilActivity`: perfil propio o, para el operador, el de otro usuario.
- `UsuariosActivity`: lista de usuarios (solo operador).
