# WarehousePro Demo — JavaFX edition

The JavaFX edition of the SwingBridge WarehousePro demo: the same legacy
desktop app, WarehousePro 3.5, written in JavaFX instead of Swing, and a Vaadin
application that brings it to the browser through the
[JavaFX Extension for Vaadin SwingBridge](https://github.com/vaadin-component-factory/swing-bridge-javafx-addon).

## The story

1. **The desktop app, as it is.** `javafx-app` is WarehousePro 3.5 in JavaFX:
   login, a main window with menu, title and status bars, and five modules
   (Dashboard, Products, Suppliers, Orders, Stock Log) with their tables,
   filters, context menus and dialogs. It runs on the desktop with
   `./start-javafx.sh`.
2. **Stage 1 — the whole app in the browser, unchanged.** `/` in the Vaadin
   app runs the same application jar through `JavaFXBridge`, its own login
   screen included.
3. **Stage 2 — a Vaadin shell around JavaFX modules.** `/login` is a native
   Vaadin login; after it, an Aura `AppLayout` with a side navigation hosts
   the JavaFX modules (`/dashboard`, `/products`, `/orders`, `/stock`). Every
   route shows the same running application, `fx.bridge.BridgeApp` in
   *bridge mode* (no menu, title bar or tabs), and tells it which module to
   show through interop: `JavaFXBridge.interop(…).of(BridgeAppBridge.class)
   .onReady(BridgeAppBridge::showProducts)`. Moving between routes keeps the
   user's state in the JavaFX application.
4. **Stage 3 — interoperability (`/suppliers`).** A native Vaadin toolbar and
   dialogs drive the JavaFX suppliers table, both ways (below).
5. **Stage 4 — native Vaadin rebuild (`/dashboard-modern`).** The same
   dashboard rebuilt natively with Vaadin components and charts, reading the
   same database.

## Stage 3 — interoperability

`BridgeApp`'s methods marked `@ExposedMethod` are scanned at build time by
SwingBridge's `swing-bridge-codegen-maven-plugin`, which generates the typed
`BridgeAppBridge` interface (in `vaadin-app/target/generated-sources/swing-bridge`,
not checked in). `@InstanceProvider BridgeApp.current()` gives the bridge the
running instance, one per user.

**Vaadin → JavaFX (method calls)**

| Vaadin action | JavaFX method called |
|---|---|
| View opens | `showSuppliersTable()` (suppliers module, its own toolbar hidden) |
| Refresh button | `refreshSuppliers()` |
| Search field | `setSupplierFilter(String)` |
| After save / delete | `refreshSuppliers()` |

**Export CSV** runs entirely in Vaadin: `SuppliersView` reads the suppliers
through `SupplierService` and serves the file with a `DownloadHandler`.

**JavaFX → Vaadin (callbacks)**

`SuppliersView`'s `@VaadinCallback(observerFor = …Listener.class)` methods are
registered with the application in `onAttach` and removed in `onDetach`:

| User action in the JavaFX table | Event | Vaadin reaction |
|---|---|---|
| Row selected | `SupplierSelectedEvent` (with id) | Enable Edit + Delete, remember the selection |
| Row deselected | `SupplierSelectedEvent` (id = null) | Disable Edit + Delete |
| Double-click a row | `SupplierEditRequestEvent` | Open the Vaadin edit form, filled in |

The listener and event types live in `warehouse-core`
(`com.example.warehouse.interop`), which the host has on its class path:
both sides must see one class (see the class loader below).

**What to point out:** the Vaadin toolbar and the JavaFX table react to each
other in real time — selecting a row enables the Vaadin buttons, an edit saved
in the Vaadin dialog shows up in the JavaFX table, and the search field
filters the JavaFX table as you type.

## Modules

| Module | What |
|---|---|
| `warehouse-core` | Domain model (JPA entities), DAOs, services, seed data and `AppSession`. Hibernate 6.4 on an embedded H2 file database (`./warehousedb`). No UI. |
| `javafx-app` | The JavaFX desktop app. Shaded into one jar (without JavaFX) that serves both the desktop and the bridge's `applibs`. `com.example.warehouse.fx.WarehouseApp` is the whole app; `fx.bridge.BridgeApp` is the bridge-mode main window, with the `@ExposedMethod` methods stages 2 and 3 call. |
| `vaadin-app` | Spring Boot 4.1 + Vaadin 25.3 host with the Aura theme, on port 8889. |

## Running

```bash
./start-javafx.sh          # the desktop app
./start-vaadin.sh          # browser, dev mode:        http://localhost:8889
./start-vaadin-prod.sh     # browser, production mode: http://localhost:8889
```

Prerequisites: JDK 21+ (Maven is not needed: the project ships the Maven
wrapper) and a SwingBridge license or trial; sign in to Vaadin when the license
check asks. `vaadin-app` declares SwingBridge `1.4.0-alpha1` (from Vaadin's
pre-release repository) next to the JavaFX extension `1.0.0-alpha2`, without
SwingBridge's Log4j2 binding, which clashes with Spring Boot's logging. The
extension comes from the Vaadin Directory's add-on repository. OpenJFX comes from Maven Central:
21.x on JDK 21, or 25.x on JDK 23+ (`-Dopenjfx.version=…`).

Credentials: `admin / admin`, `manager / manager`, `viewer / viewer`.

## How the Vaadin app runs the JavaFX app

- JavaFX is on the JVM's module path, never the class path; the bridge's
  Glass patch is patched into `javafx.graphics`. `vaadin-app/pom.xml` copies
  the platform-specific OpenJFX jars into `target/`, unpacks the bridge's two
  JVM-level jars from the extension's jar next to them, and
  defines the JVM flags once (`javafx.bridge.jvmArgs`), which the start
  scripts reuse.
- The JavaFX app jar is copied into `vaadin-app/target/applibs`, where the
  bridge loads it per Vaadin session. It is deliberately **not** a
  dependency of the Vaadin app.
- `AppLibsClassLoader` loads the app's jars **child-first**. The host has
  `warehouse-core` on its class path too, for the native views; with the
  usual parent-first loading every session would share its classes, and so
  one `AppSession` (the signed-in user) and one Hibernate factory. With
  child-first loading two people can be signed in as different users at once.
  The exceptions are loaded from the host for both sides: the JDK, JavaFX, the
  bridge, SwingBridge's interop annotations, and the interop types in
  `com.example.warehouse.interop`.
- `swing-bridge-codegen-maven-plugin` runs on the app jar right after it is
  copied to `applibs`, and generates `BridgeAppBridge` for the views.
- Everything happens in the JavaFX app's primary stage: login and main window
  swap scenes rather than open new windows, because inside the bridge the
  primary stage is the page and any other stage becomes a dialog.

## Differences from the Swing app

- Charts are JavaFX charts instead of JFreeChart, and they refresh with the
  dashboard.
- Search fields match plain text rather than regular expressions.
- Dialogs are resizable, and their buttons use the platform's order.
- On an existing purchase order, added and removed line items are saved (the
  Swing dialog only saved quantity and price changes).
- "Exit" closes the window instead of `System.exit`, which would stop the
  JavaFX runtime for every browser session.
- CSV export uses the JavaFX file chooser; in the browser it asks for a file
  name and downloads the file.
