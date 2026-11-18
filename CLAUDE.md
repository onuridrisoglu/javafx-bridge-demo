# WarehousePro Demo (JavaFX) — Context for Claude

## What this project is
The JavaFX edition of the SwingBridge WarehousePro demo: WarehousePro 3.5 reimplemented in JavaFX (`javafx-app`), and a Vaadin app (`vaadin-app`) that runs it in the browser through the JavaFX Extension for Vaadin SwingBridge. All four stages of the Swing demo's story; stages 2 and 3 use the bridge's interop (`JavaFXBridge.interop()`, `@ExposedMethod` on `fx.bridge.BridgeApp`, shared types in `warehouse-core` `com.example.warehouse.interop`). See README.md.

## Structure
- `warehouse-core/` — JPA entities, DAOs, services, seed data, `AppSession` (Hibernate 6.4, H2 file DB `./warehousedb`). No UI. Copied from the Swing demo; keep the two in step when changing domain behaviour.
- `javafx-app/` — `com.example.warehouse.fx`: `WarehouseApp` (whole app), `ui/LoginView`, `ui/MainView`, `ui/<module>/…Panel` + dialogs, `ui/util/Ui` (shared helpers), `ui/util/StatusBar`, `ui/util/TableExporter`; `bridge/BridgeApp` (bridge-mode main window; `@ExposedMethod show…()` and `@InstanceProvider current()` for interop, one running instance shared by all stage-2 routes). Styles in `src/main/resources/com/example/warehouse/fx/warehouse.css`. Shaded jar without JavaFX.
- `vaadin-app/` — Spring Boot 4.1, Vaadin 25.3, Aura theme, port 8889. `javafx/WarehouseView` (stage 1, route `""`), `javafx/AppLibsClassLoader` (child-first per-session loading), `views/*View` (stage 2), `views/LoginView`, `views/MainLayout`, `views/DashboardModernView`, `dashboard/*`.

## Build and run
```bash
./mvnw install -DskipTests        # all modules; vaadin-app copies the javafx-app jar into target/applibs
./start-javafx.sh                # desktop
./start-vaadin.sh                # browser, dev
./start-vaadin-prod.sh           # browser, production
```
The JavaFX extension `1.0.0-alpha2` comes from the Vaadin Directory's add-on repository (`vaadin-app/pom.xml`); it is a single jar that carries the patch and graphics jars, which the build unpacks. To try an unreleased extension build, `mvn install` it and set `swing-bridge-javafx.version` to `1.0-SNAPSHOT`.

## Rules for the JavaFX app
- Pure JavaFX: no `javax.swing` / `java.awt` in `javafx-app`.
- Keep everything in the primary stage (swap scenes); inside the bridge any other stage becomes a dialog.
- Never `System.exit` or `Platform.exit`: in the bridge the JavaFX runtime is shared by all sessions. Close the stage instead.
- Dialogs: `Dialog<T>`, `initOwner(Ui.windowOf(...))`, add `Ui.STYLESHEET`, resizable, `showAndWait()`.
- Charts: `setAnimated(false)` (animations only cost bandwidth in the bridge).
- Use `Ui` helpers and the CSS classes in `warehouse.css`; field labels from `Ui.fieldLabel` (they never shrink below their text).

## Rules for the Vaadin app
- The JavaFX app must stay out of the host class path (it is copied to `applibs`, not a dependency), and bridged views must use `AppLibsClassLoader.fromAppLibs()`.
- JVM flags live in `javafx.bridge.jvmArgs` in `vaadin-app/pom.xml`; the start scripts read them from there.
