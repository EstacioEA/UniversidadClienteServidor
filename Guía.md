# GUÍA COMPLETA

---

## PARTE 1: CÓDIGO COMENTADO Y EXPLICADO

### 1.1 ConnectionManager - Patrón Singleton para BD

```java
package DBConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionManager {
    
    // Variables de configuración de conexión
    private String url;        // Dirección de la BD (ej: jdbc:h2:mem:pocket_manager_db)
    private String user;       // Usuario de la BD
    private String password;   // Contraseña de la BD
    
    // Instancia única del manager (Singleton)
    private static ConnectionManager manager;
    
    /**
     * Patrón Singleton: Devuelve la ÚNICA instancia del ConnectionManager
     * Si no existe, la crea con parámetros por defecto (desde variables de entorno)
     */
    public static ConnectionManager getInstance() {
        if(manager == null) {
            manager = new ConnectionManager();
        }
        return manager;
    }
    
    /**
     * Patrón Singleton con parámetros: Se usa en los tests
     * Crea la instancia UNA SOLA VEZ con los parámetros dados
     */
    public static ConnectionManager getInstance(String url, String user, String password) {
        if(manager == null) {
            manager = new ConnectionManager(url, user, password);
        }
        return manager;
    }
    
    // Constructor privado con parámetros (se usa una sola vez)
    private ConnectionManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }
    
    // Constructor privado sin parámetros (se usa una sola vez)
    private ConnectionManager() {
        this.url = System.getenv("url");
        this.user = System.getenv("user");
        this.password = System.getenv("password");
    }
    
    /**
     * Método que TODOS usan para obtener conexiones
     * - Carga el driver de H2
     * - Abre una conexión nueva cada vez
     * - Esta conexión debe cerrarse después de usarla
     */
    public Connection getConnection() {
        try {
            // Cargar el driver H2
            Class.forName("org.h2.Driver");
            
            // Crear y devolver una nueva conexión
            Connection con = DriverManager.getConnection(url, user, password);
            return con;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

**Por qué Singleton**: Asegura que toda la app use la MISMA configuración de conexión. Si usas múltiples managers, cada uno podría tener URLs diferentes y causaría inconsistencia.

---

### 1.2 PocketDao - Acceso a Base de Datos

```java
package daos;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import DBConfig.ConnectionManager;
import model.Pocket;

/**
 * PocketDao: Data Access Object
 * Responsable de TODAS las operaciones en la tabla "pocket"
 * 
 * Principio: La BD es la "fuente de verdad"
 * Si algo no está en BD, no cuenta
 */
public class PocketDao implements Dao<Pocket, String> {
    
    /**
     * Bloque estático: Se ejecuta UNA SOLA VEZ cuando se carga la clase
     * Crea la tabla si no existe
     */
    static {
        try {
            System.out.println("Initializing database...");
            Connection conn = ConnectionManager.getInstance().getConnection();
            
            // Crear tabla si no existe
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS pocket (" +
                "  name VARCHAR(255) PRIMARY KEY," +  // Nombre único del bolsillo
                "  balance DOUBLE" +                   // Saldo en el bolsillo
                ")"
            );
            
            // IMPORTANTE: Cerrar conexión después de usarla
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * findAll: Obtiene TODOS los bolsillos de la BD
     * SELECT * FROM pocket
     * Devuelve una Lista vacía si no hay bolsillos
     */
    @Override
    public List<Pocket> findAll() {
        List<Pocket> pockets = new ArrayList<>();
        try {
            // 1. Obtener conexión
            Connection conn = ConnectionManager.getInstance().getConnection();
            
            // 2. Ejecutar consulta SELECT
            var rs = conn.createStatement().executeQuery("SELECT * FROM pocket");
            
            // 3. Procesar resultados fila por fila
            while (rs.next()) {
                Pocket pocket = new Pocket();
                pocket.setName(rs.getString("name"));      // Leer columna "name"
                pocket.setBalance(rs.getDouble("balance")); // Leer columna "balance"
                pockets.add(pocket);
            }
            
            // 4. Cerrar conexión
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pockets;
    }
    
    /**
     * finById (buscar por ID): Obtiene un bolsillo por nombre
     * SELECT * FROM pocket WHERE name = ?
     * El ? es un parámetro para evitar SQL Injection
     */
    @Override
    public Pocket finById(String id) {
        Pocket pocket = null;
        try {
            Connection conn = ConnectionManager.getInstance().getConnection();
            
            // PreparedStatement protege contra SQL Injection
            var ps = conn.prepareStatement("SELECT * FROM pocket WHERE name = ?");
            ps.setString(1, id);  // Reemplazar ? con el ID (parámetro 1)
            var rs = ps.executeQuery();
            
            // Si existe una fila, extraer datos
            if (rs.next()) {
                pocket = new Pocket();
                pocket.setName(rs.getString("name"));
                pocket.setBalance(rs.getDouble("balance"));
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pocket;  // Devuelve null si no existe
    }
    
    /**
     * update: Modifica un bolsillo existente en la BD
     * UPDATE pocket SET balance = ? WHERE name = ?
     */
    @Override
    public Pocket update(Pocket newEntity) {
        try {
            Connection conn = ConnectionManager.getInstance().getConnection();
            
            // Actualizar el saldo del bolsillo
            var ps = conn.prepareStatement("UPDATE pocket SET balance = ? WHERE name = ?");
            ps.setDouble(1, newEntity.getBalance());  // Parámetro 1: nuevo balance
            ps.setString(2, newEntity.getName());     // Parámetro 2: nombre (WHERE)
            ps.executeUpdate();  // Ejecutar UPDATE (no retorna ResultSet)
            
            conn.close();
            return newEntity;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * delete: Elimina un bolsillo de la BD
     * DELETE FROM pocket WHERE name = ?
     */
    @Override
    public void delete(Pocket entity) {
        try {
            Connection conn = ConnectionManager.getInstance().getConnection();
            
            var ps = conn.prepareStatement("DELETE FROM pocket WHERE name = ?");
            ps.setString(1, entity.getName());
            ps.executeUpdate();
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * save: Crea un NUEVO bolsillo en la BD
     * INSERT INTO pocket VALUES(?, ?)
     * 
     * IMPORTANTE: Diferencia con update()
     * - save(): Inserta un registro NUEVO
     * - update(): Modifica un registro EXISTENTE
     */
    @Override
    public void save(Pocket entity) {
        try {
            Connection conn = ConnectionManager.getInstance().getConnection();
            
            var ps = conn.prepareStatement("INSERT INTO pocket (name, balance) VALUES(?, ?)");
            ps.setString(1, entity.getName());
            ps.setDouble(2, entity.getBalance());
            ps.executeUpdate();
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Patrón DAO**: Toda la lógica SQL está aquí. Si quieres cambiar de BD o cambiar queries, solo modificas esta clase.

---

### 1.3 PocketService - Lógica de Negocio

```java
package services;

import java.util.ArrayList;
import java.util.List;
import model.MainAccount;
import model.Pocket;
import daos.PocketDao;

/**
 * PocketService: Cerebro del sistema
 * 
 * RESPONSABILIDADES:
 * 1. Validar que las operaciones sean válidas
 * 2. Actualizar el MainAccount (estado en memoria)
 * 3. Persistir cambios en BD (via PocketDao)
 * 4. Sincronizar accesos concurrentes con synchronized
 * 
 * INVARIANTES (reglas que siempre deben cumplirse):
 * - totalBalance NO cambia (dinero total es fijo)
 * - availableBalance + suma(bolsillos) = totalBalance
 * - availableBalance nunca es negativo
 * - no puede haber dos bolsillos con el mismo nombre
 */
public class PocketService {
    
    // ESTADO ÚNICO DE LA APLICACIÓN
    private MainAccount mainAccount;  // Lo que TODOS ven y modifican
    private PocketDao pocketDao;      // Para leer/escribir en BD
    
    /**
     * Constructor: Inicializa la aplicación
     * Crea un MainAccount vacío con el dinero inicial
     */
    public PocketService(double initialAmount) {
        mainAccount = new MainAccount();
        mainAccount.setTotalBalance(initialAmount);
        mainAccount.setAvailableBalance(initialAmount);
        mainAccount.setPockets(new ArrayList<>());  // Sin bolsillos al inicio
        pocketDao = new PocketDao();
    }
    
    /**
     * addPocket: Crear un NUEVO bolsillo
     * 
     * FLUJO:
     * 1. Validar que el nombre no esté vacío
     * 2. Validar que la cantidad inicial > 0
     * 3. Validar que hay dinero disponible
     * 4. Validar que no existe bolsillo con ese nombre
     * 5. Crear bolsillo
     * 6. Guardar en BD
     * 7. Actualizar estado en memoria
     * 8. Devolver respuesta sin ciclos
     * 
     * SINCRONIZADO: Evita que dos threads creen bolsillos al mismo tiempo
     * y causen inconsistencia en los saldos
     */
    public synchronized Pocket addPocket(String name, double initialAmount) throws Exception {
        
        // === VALIDACIONES ===
        if (name == null || name.isEmpty()) {
            throw new Exception("Pocket name cannot be empty");
        }
        if (initialAmount <= 0) {
            throw new Exception("Initial amount must be greater than 0");
        }
        // VALIDACIÓN CRÍTICA: Hay suficiente dinero disponible
        if (initialAmount > mainAccount.getAvailableBalance()) {
            throw new Exception("Insufficient funds in main account");
        }
        // VALIDACIÓN CRÍTICA: No existe bolsillo con ese nombre
        if (pocketDao.finById(name) != null) {
            throw new Exception("Pocket with name " + name + " already exists");
        }
        
        // === CREAR BOLSILLO ===
        Pocket pocket = new Pocket();
        pocket.setName(name);
        pocket.setBalance(initialAmount);
        
        // === ACTUALIZAR ESTADO EN MEMORIA ===
        // El dinero se "transfiere" del disponible al bolsillo
        mainAccount.setAvailableBalance(
            mainAccount.getAvailableBalance() - initialAmount
        );
        
        // === PERSISTIR EN BD ===
        pocketDao.save(pocket);
        
        // === ACTUALIZAR LISTA EN MEMORIA ===
        mainAccount.getPockets().add(pocket);
        
        // === PREPARAR RESPUESTA (sin ciclos de referencias) ===
        Pocket responseData = new Pocket();
        responseData.setName(pocket.getName());
        responseData.setBalance(pocket.getBalance());
        responseData.setMainAccount(buildMainAccountResponse());
        
        return responseData;
    }
    
    /**
     * depositInPocket: Meter dinero en un bolsillo existente
     * 
     * FLUJO:
     * 1. Validar que la cantidad > 0
     * 2. Validar que hay dinero disponible
     * 3. Buscar bolsillo
     * 4. Sumar dinero al bolsillo
     * 5. Actualizar en BD
     * 6. Actualizar saldo disponible (restar)
     * 7. Actualizar lista en memoria
     */
    public synchronized Pocket depositInPocket(String name, double amount) throws Exception {
        
        // === VALIDACIONES ===
        if (amount <= 0) {
            throw new Exception("Amount must be greater than 0");
        }
        if (amount > mainAccount.getAvailableBalance()) {
            throw new Exception("Insufficient funds in main account");
        }
        
        // Buscar bolsillo en BD
        Pocket pocket = pocketDao.finById(name);
        if (pocket == null) {
            throw new Exception("Pocket not found");
        }
        
        // === ACTUALIZAR BOLSILLO ===
        double newBalance = pocket.getBalance() + amount;
        pocket.setBalance(newBalance);
        pocketDao.update(pocket);  // Guardar en BD
        
        // === ACTUALIZAR MAINACCOUNT ===
        // El dinero disponible disminuye
        mainAccount.setAvailableBalance(
            mainAccount.getAvailableBalance() - amount
        );
        
        // === ACTUALIZAR LISTA EN MEMORIA ===
        // Buscar el bolsillo en la lista y actualizar su saldo
        mainAccount.getPockets().stream()
            .filter(p -> p.getName().equals(name))
            .forEach(p -> p.setBalance(newBalance));
        
        // === PREPARAR RESPUESTA ===
        Pocket responseData = new Pocket();
        responseData.setName(name);
        responseData.setBalance(newBalance);
        responseData.setMainAccount(buildMainAccountResponse());
        return responseData;
    }
    
    /**
     * withdrawFromPocket: Sacar dinero de un bolsillo
     * Es lo inverso a depositInPocket
     */
    public synchronized Pocket withdrawFromPocket(String name, double amount) throws Exception {
        
        // === VALIDACIONES ===
        if (amount <= 0) {
            throw new Exception("Amount must be greater than 0");
        }
        
        Pocket pocket = pocketDao.finById(name);
        if (pocket == null) {
            throw new Exception("Pocket not found");
        }
        // VALIDACIÓN CRÍTICA: El bolsillo tiene suficiente dinero
        if (amount > pocket.getBalance()) {
            throw new Exception("Insufficient funds in pocket");
        }
        
        // === ACTUALIZAR BOLSILLO ===
        double newBalance = pocket.getBalance() - amount;
        pocket.setBalance(newBalance);
        pocketDao.update(pocket);
        
        // === ACTUALIZAR MAINACCOUNT ===
        // El dinero disponible AUMENTA (se devuelve)
        mainAccount.setAvailableBalance(
            mainAccount.getAvailableBalance() + amount
        );
        
        // === ACTUALIZAR LISTA EN MEMORIA ===
        mainAccount.getPockets().stream()
            .filter(p -> p.getName().equals(name))
            .forEach(p -> p.setBalance(newBalance));
        
        // === PREPARAR RESPUESTA ===
        Pocket responseData = new Pocket();
        responseData.setName(name);
        responseData.setBalance(newBalance);
        responseData.setMainAccount(buildMainAccountResponse());
        return responseData;
    }
    
    /**
     * depositInAccount: Meter dinero a la cuenta principal
     * Esto AUMENTA tanto el disponible como el total
     */
    public synchronized MainAccount depositInAccount(double amount) throws Exception {
        
        if (amount <= 0) {
            throw new Exception("Amount must be greater than 0");
        }
        
        // Aumentar ambos saldos
        mainAccount.setAvailableBalance(
            mainAccount.getAvailableBalance() + amount
        );
        mainAccount.setTotalBalance(
            mainAccount.getTotalBalance() + amount
        );
        
        return buildMainAccountResponse();
    }
    
    /**
     * getMainAccount: Obtener el estado actual de la cuenta
     * Solo lectura (aunque sea synchronized por consistencia)
     */
    public synchronized MainAccount getMainAccount() {
        return buildMainAccountResponse();
    }
    
    /**
     * buildMainAccountResponse: Método CRÍTICO
     * 
     * Crea una copia "limpia" del MainAccount para la respuesta JSON
     * 
     * ¿Por qué es necesario?
     * - El Pocket tiene referencia a MainAccount
     * - MainAccount tiene lista de Pockets
     * - Si mandamos los objetos tal como están, Gson ve un CICLO
     * - Gson trata de serializar Pocket → MainAccount → lista de Pockets → Pocket...
     * - ¡StackOverflowError!
     * 
     * Solución: Copiar solo los datos, sin referencias circulares
     */
    private MainAccount buildMainAccountResponse() {
        MainAccount copy = new MainAccount();
        copy.setAvailableBalance(mainAccount.getAvailableBalance());
        copy.setTotalBalance(mainAccount.getTotalBalance());
        
        // Crear copia de pockets SIN la referencia a MainAccount
        List<Pocket> pocketsCopy = new ArrayList<>();
        for (Pocket p : mainAccount.getPockets()) {
            Pocket pCopy = new Pocket();
            pCopy.setName(p.getName());
            pCopy.setBalance(p.getBalance());
            // IMPORTANTE: No setear mainAccount aquí
            pocketsCopy.add(pCopy);
        }
        copy.setPockets(pocketsCopy);
        
        return copy;
    }
}
```

**Regla de oro**: PocketService es el ÚNICO lugar donde se modifican los saldos. Todos los cambios pasan por aquí.

---

### 1.4 Server - Servidor TCP con Hilos

```java
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import com.google.gson.Gson;

import dtos.Request;
import dtos.Response;
import model.MainAccount;
import model.Pocket;
import services.PocketService;

/**
 * Server: Servidor TCP que escucha conexiones de clientes
 * 
 * ARQUITECTURA:
 * - Main thread: Escucha conexiones en puerto 5000
 * - Cada cliente: En su propio thread
 * - Comunicación: JSON por sockets
 * 
 * VENTAJA: Un cliente no bloquea a otros
 * Si Cliente A está lento, Cliente B se atiende al instante
 */
public class Server {
    
    // Estado compartido entre TODOS los threads
    private PocketService pocketService;  // Singleton: todos los threads usan el mismo
    private Gson gson;                    // Convertidor JSON
    
    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.init(5000.0);  // Dinero inicial: 5000
    }
    
    /**
     * init: Inicializar el servidor
     * 
     * FLUJO:
     * 1. Crear PocketService (estado compartido)
     * 2. Crear ServerSocket (escuchar en puerto 5000)
     * 3. Loop infinito:
     *    - Esperar conexión de cliente
     *    - Crear thread para ese cliente
     *    - Continuar escuchando (NO BLOQUEA)
     */
    public void init(Double initialAmount) throws Exception {
        // === INICIALIZAR ESTADO ===
        pocketService = new PocketService(initialAmount);
        gson = new Gson();
        
        // === CREAR SOCKET SERVIDOR ===
        // Escucha en puerto 5000
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Server listening on port 5000...");
        
        // === LOOP INFINITO DE ACEPTACIÓN ===
        while (true) {
            // Esperar una conexión (BLOQUEANTE)
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            
            // CLAVE: Crear thread para este cliente
            // El main thread NO se bloquea, sigue esperando más clientes
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }
    
    /**
     * handleClient: Procesar UN cliente
     * Se ejecuta en su propio thread
     * 
     * FLUJO:
     * 1. Abrir readers/writers del socket
     * 2. Leer línea JSON (request)
     * 3. Procesar request
     * 4. Enviar respuesta JSON
     * 5. Cerrar socket
     */
    public void handleClient(Socket socket) {
        try {
            // === PREPARAR COMUNICACIÓN ===
            // InputStreamReader: Bytes → String
            // BufferedReader: Lee líneas completas (newLine)
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            
            // OutputStreamWriter: String → Bytes
            // BufferedWriter: Escribe líneas
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())
            );
            
            // === LEER REQUEST DEL CLIENTE ===
            String line = reader.readLine();
            if (line != null) {
                // Convertir JSON string a objeto Request
                Request request = gson.fromJson(line, Request.class);
                
                // Procesar request (puede throw Exception)
                Response response = handleRequest(request);
                
                // === ENVIAR RESPUESTA ===
                writer.write(gson.toJson(response));  // Convertir a JSON
                writer.newLine();                      // Agregar salto de línea
                writer.flush();                        // Enviar (IMPORTANTE)
            }
            
            // === LIMPIAR RECURSOS ===
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * handleRequest: Procesar una request
     * 
     * FLUJO:
     * 1. Crear respuesta vacía
     * 2. Usar switch para diferentes acciones
     * 3. Llamar al PocketService correspondiente
     * 4. Si hay error, capturar y devolver status "error"
     * 5. Siempre devolver respuesta con status + data
     */
    public Response handleRequest(Request request) throws Exception {
        Response response = new Response();
        
        try {
            // === PROCESAR POR TIPO DE ACCIÓN ===
            switch (request.action) {
                
                case "ADD_POCKET": {
                    String name = request.data.get("name");
                    double initialAmount = Double.parseDouble(request.data.get("initialAmount"));
                    
                    // Llamar al servicio
                    Pocket pocket = pocketService.addPocket(name, initialAmount);
                    
                    // Respuesta exitosa
                    response.status = "ok";
                    response.data = gson.toJsonTree(pocket).getAsJsonObject();
                    break;
                }
                
                case "DEPOSIT_POCKET": {
                    String name = request.data.get("name");
                    double amount = Double.parseDouble(request.data.get("amount"));
                    
                    Pocket pocket = pocketService.depositInPocket(name, amount);
                    response.status = "ok";
                    response.data = gson.toJsonTree(pocket).getAsJsonObject();
                    break;
                }
                
                case "WITHDRAW_POCKET": {
                    String name = request.data.get("name");
                    double amount = Double.parseDouble(request.data.get("amount"));
                    
                    Pocket pocket = pocketService.withdrawFromPocket(name, amount);
                    response.status = "ok";
                    response.data = gson.toJsonTree(pocket).getAsJsonObject();
                    break;
                }
                
                case "DEPOSIT_ACCOUNT": {
                    double amount = Double.parseDouble(request.data.get("amount"));
                    
                    MainAccount account = pocketService.depositInAccount(amount);
                    response.status = "ok";
                    response.data = gson.toJsonTree(account).getAsJsonObject();
                    break;
                }
                
                case "GET_ACCOUNT": {
                    MainAccount account = pocketService.getMainAccount();
                    response.status = "ok";
                    response.data = gson.toJsonTree(account).getAsJsonObject();
                    break;
                }
                
                default:
                    response.status = "error";
                    response.data = gson.toJsonTree(
                        Map.of("message", "Unknown action")
                    ).getAsJsonObject();
            }
        } catch (Exception e) {
            // === MANEJO DE ERRORES ===
            // Si algo falla, devolver status "error" con mensaje
            response.status = "error";
            response.data = gson.toJsonTree(
                Map.of("message", e.getMessage())
            ).getAsJsonObject();
        }
        
        return response;
    }
}
```

**El corazón**: `new Thread(() -> handleClient(clientSocket)).start()` → Sin esto, un cliente lento bloquea a todos.

---

## PARTE 2: CONCEPTOS GENERALES

### Iniciando un Servidor TCP

**Paso 1: Crear ServerSocket**
```java
ServerSocket ss = new ServerSocket(5000);  // Escuchar en puerto 5000
```

**Paso 2: Loop infinito aceptando clientes**
```java
while(true) {
    Socket client = ss.accept();  // Bloqueante: espera conexión
    // Procesar cliente
}
```

**Paso 3: Problema sin threads**
```
Cliente A conecta → accept() retorna
Procesar Cliente A (lento, 10 segundos)
Cliente B intenta conectar → BLOQUEADO, espera
Cliente C intenta conectar → BLOQUEADO, espera
...después de 10 segundos, finalmente procesa B
```

**Solución: Threads**
```java
while(true) {
    Socket client = ss.accept();
    new Thread(() -> handleClient(client)).start();  // ← CLAVE
    // Vuelve a accept() inmediatamente
}
```

Ahora:
```
Cliente A conecta → Thread A comienza
Cliente B conecta inmediatamente → Thread B comienza
Ambos threads se ejecutan EN PARALELO
```

---

### Concurrencia Efectiva y Estable

**Problema: Race Condition**
```
Escenario inicial: availableBalance = 1000

Thread A:
  1. Lee availableBalance = 1000
  2. Calcula: 1000 - 500 = 500
  [PAUSA, otro thread toma control]

Thread B:
  1. Lee availableBalance = 1000  ← ¡Aún es 1000!
  2. Calcula: 1000 - 600 = 400
  3. Escribe: availableBalance = 400

Thread A reanuda:
  3. Escribe: availableBalance = 500  ← ¡SOBRESCRIBE!

Resultado: availableBalance = 500
Esperado: availableBalance = -100 (error) o 400, NO 500
```

**Solución: synchronized**
```java
public synchronized void restarSaldo(double amount) {
    // Solo UN thread puede estar aquí a la vez
    // Otros threads ESPERAN su turno
    
    double newBalance = availableBalance - amount;
    availableBalance = newBalance;
}
```

Con synchronized:
```
Thread A entra en método → acquire lock
Thread B intenta entrar → BLOQUEADO, espera
Thread A sale del método → libera lock
Thread B finalmente entra → acquire lock
Thread B sale → libera lock
```

Resultado: SIEMPRE correcto, sin importar el orden.

---

### Peticiones SQL en Java

**Patrón básico: Conexión → Statement → Ejecutar → Cerrar**

```java
// 1. OBTENER CONEXIÓN
Connection conn = ConnectionManager.getInstance().getConnection();

// 2. CREAR STATEMENT (dos tipos)

// Tipo 1: Statement simple (evitar si tienes parámetros)
String query = "SELECT * FROM pocket WHERE balance > 500";
ResultSet rs = conn.createStatement().executeQuery(query);

// Tipo 2: PreparedStatement (SEGURO contra SQL Injection)
String query = "SELECT * FROM pocket WHERE balance > ?";
PreparedStatement ps = conn.prepareStatement(query);
ps.setDouble(1, 500);  // Reemplazar ? con parámetro
ResultSet rs = ps.executeQuery();

// 3. PROCESAR RESULTADOS
while(rs.next()) {
    String name = rs.getString("name");
    double balance = rs.getDouble("balance");
}

// 4. CERRAR CONEXIÓN
conn.close();
```

**Operaciones CRUD**

| Operación | SQL | Método | Retorna |
|-----------|-----|--------|---------|
| **CREATE** | INSERT INTO pocket VALUES(?, ?) | executeUpdate() | int (filas afectadas) |
| **READ** | SELECT * FROM pocket | executeQuery() | ResultSet |
| **UPDATE** | UPDATE pocket SET balance = ? WHERE name = ? | executeUpdate() | int |
| **DELETE** | DELETE FROM pocket WHERE name = ? | executeUpdate() | int |

```java
// INSERT
var ps = conn.prepareStatement("INSERT INTO pocket (name, balance) VALUES(?, ?)");
ps.setString(1, "Vacaciones");
ps.setDouble(2, 1000);
int rowsAffected = ps.executeUpdate();  // Retorna 1 (una fila insertada)

// UPDATE
var ps = conn.prepareStatement("UPDATE pocket SET balance = ? WHERE name = ?");
ps.setDouble(1, 2000);
ps.setString(2, "Vacaciones");
ps.executeUpdate();

// DELETE
var ps = conn.prepareStatement("DELETE FROM pocket WHERE name = ?");
ps.setString(1, "Vacaciones");
ps.executeUpdate();
```

**Tipos de datos SQL ↔ Java**

```java
// Escribir en BD
ps.setString(1, "nombre");        // VARCHAR → String
ps.setDouble(1, 1000.5);          // DOUBLE → double
ps.setInt(1, 42);                 // INTEGER → int
ps.setBoolean(1, true);           // BOOLEAN → boolean

// Leer desde BD
String name = rs.getString("name");           // VARCHAR
double balance = rs.getDouble("balance");     // DOUBLE
int count = rs.getInt("count");               // INTEGER
boolean active = rs.getBoolean("is_active");  // BOOLEAN
```

**SQL Injection: El problema**

```java
// ❌ PELIGROSO
String name = "admin' OR '1'='1";
String query = "SELECT * FROM users WHERE name = '" + name + "'";
// Query resultante: SELECT * FROM users WHERE name = 'admin' OR '1'='1'
// ¡Devuelve TODOS los usuarios!

// ✅ SEGURO con PreparedStatement
String query = "SELECT * FROM users WHERE name = ?";
PreparedStatement ps = conn.prepareStatement(query);
ps.setString(1, name);  // El parámetro se escapa automáticamente
// El ' se convierte en \' internamente
```

---

## PARTE 3: TIPS Y CHECKLIST PARA EL PARCIAL

### ✅ ANTES DE EMPEZAR

- [ ] Lee bien el README y entiende qué datos manejar
- [ ] Identifica qué acciones el servidor debe soportar (ADD, DEPOSIT, etc)
- [ ] Dibuja el flujo: Cliente → JSON → Servidor → Lógica → BD → Respuesta JSON
- [ ] Ten claro: ¿Qué es estado en memoria? ¿Qué es en BD?

---

### 🏗️ ARQUITECTURA: Cómo organizar las clases

**SIEMPRE sigue este patrón:**

```
1. Modelos (model/)
   - MainEntity (la entidad principal)
   - OtherEntity (entidades relacionadas)
   
2. BD (DBConfig/, daos/)
   - ConnectionManager (Singleton)
   - Dao interface
   - ConcreteDao (TODAS las queries aquí)
   
3. Lógica (services/)
   - Service (VALIDACIONES + actualizaciones)
   - Usa synchronized en métodos que modifiquen estado
   
4. Comunicación (dtos/, Server.java)
   - Request, Response (DTOs)
   - Server con threads
   - handleRequest() para mapear acciones
   
5. Tests
   - PruebanyTest
```

**¿Por qué este orden?**
- Las clases de arriba NO dependen de las de abajo
- Puedes cambiar la BD sin tocar Service
- Puedes cambiar Service sin tocar el protocolo
- Cada clase tiene UNA responsabilidad

---

### 🎯 CHECKLIST DE IMPLEMENTACIÓN

#### Paso 1: Modelos
- [ ] Crear todas las clases Model con getters/setters
- [ ] Decidir: ¿Qué datos van en cada modelo?

#### Paso 2: Base de Datos
- [ ] Crear ConnectionManager como Singleton
- [ ] En PocketDao:
  - [ ] Bloque static para crear tabla
  - [ ] Implementar findAll()
  - [ ] Implementar finById()
  - [ ] Implementar save() ← INSERTA nuevo
  - [ ] Implementar update() ← MODIFICA existente
  - [ ] Implementar delete() si es necesario
- [ ] SIEMPRE cerrar conexiones: `conn.close()`

#### Paso 3: Lógica de Negocio
- [ ] En Service:
  - [ ] Constructor inicializa estado
  - [ ] CADA método que modifica estado: `synchronized`
  - [ ] VALIDAR todo (montos > 0, existencia, suficiencia)
  - [ ] Actualizar estado en MEMORIA
  - [ ] PERSISTIR en BD
  - [ ] Devolver respuesta limpia (sin ciclos)

#### Paso 4: Servidor
- [ ] `init()`: Crear ServerSocket + loop
- [ ] Cada cliente en su propio thread: `new Thread(() -> ...).start()`
- [ ] `handleClient()`: Read → Process → Write → Close
- [ ] `handleRequest()`: Switch por acción, manejo de errores
- [ ] Siempre cerrar sockets

#### Paso 5: Pruebas
- [ ] Ejecutar cada test individualmente
- [ ] Probar con números que hagan sentido
- [ ] Verificar que BD persiste (no se pierden datos)
- [ ] Test de concurrencia: verificar que múltiples threads no causan inconsistencia

---

### ⚠️ ERRORES COMUNES (Y cómo evitarlos)

| Error | Causa | Solución |
|-------|-------|----------|
| StackOverflowError en JSON | Referencias circulares | Copiar solo datos necesarios, sin referencias |
| Saldos inconsistentes | Race condition | `synchronized` en TODOS los métodos que modifiquen estado |
| Datos desaparecen | No persistir en BD | Llamar a `pocketDao.save/update()` SIEMPRE |
| Cliente se queda esperando | No cerrar socket | `socket.close()` al final de handleClient |
| No llegan mensajes | No hacer flush | `writer.flush()` después de `writer.write()` |
| "Already exists" error | PreparedStatement mal usado | Verificar con finById() ANTES de save() |
| Múltiples MainAccounts | Crear varias instancias | Singleton: `pocketService` es GLOBAL |
| Null pointer en bolsillo | finById() retorna null | Siempre validar con `if(pocket == null)` |

---

### 💡 TIPS PARA PASAR LOS TESTS

**1. Entiende qué espera el test**
```java
// Test espera respuesta como:
{
  "status": "ok",
  "data": {
    "name": "Vacaciones",
    "balance": 1000.0,
    "mainAccount": {
      "availableBalance": 4000.0,
      "totalBalance": 5000.0
    }
  }
}

// Si retornas estructura diferente → Falla
```

**2. Respeta el flujo de balances**
```
totalBalance = NUNCA CAMBIA (dinero total fijo)
availableBalance = totalBalance - suma(bolsillos)

Así siempre cumples la invariante
```

**3. Sincronización es TODO**
```java
// ❌ MALO: Solo algunos métodos synchronized
public synchronized void addPocket() { }
public void depositInPocket() { }  // ← Problema

// ✅ BUENO: Todos los que tocan estado
public synchronized void addPocket() { }
public synchronized void depositInPocket() { }
public synchronized void withdrawFromPocket() { }
```

**4. Cierra siempre las conexiones**
```java
try {
    Connection conn = ConnectionManager.getInstance().getConnection();
    // ... operaciones ...
} finally {
    conn.close();  // Hacerlo en finally o al final del try
}
```

**5. No reutilices conexiones**
```java
// ❌ MALO: Guardar conexión global
private static Connection conn;

// ✅ BUENO: Nueva conexión cada vez
public void save(Pocket p) {
    Connection conn = ConnectionManager.getInstance().getConnection();
    // ... use ...
    conn.close();
}
```

---

### 🧪 DEBUGGING: Cómo saber qué está pasando

**Agrega prints estratégicos:**

```java
// En PocketService.addPocket()
System.out.println("ADD_POCKET: " + name + ", amount: " + initialAmount);
System.out.println("Available before: " + mainAccount.getAvailableBalance());
// ... hacer cambios ...
System.out.println("Available after: " + mainAccount.getAvailableBalance());
System.out.println("Saved to BD");
```

**En Server.handleRequest():**
```java
System.out.println("Request received: " + request.action);
System.out.println("Data: " + request.data);
System.out.println("Response: " + response.status + " " + response.data);
```

**Así ves exactamente dónde se quiebra**

---

### 📊 FLUJO COMPLETO: De lado a lado

```
1. CLIENTE CONECTA
   Socket socket = new Socket("localhost", 5000);

2. CLIENTE ENVÍA REQUEST
   Request req = { "action": "ADD_POCKET", "data": { "name": "Vac", "initialAmount": "1000" } }
   writer.write(gson.toJson(req));
   writer.flush();

3. SERVIDOR RECIBE
   String line = reader.readLine();
   Request req = gson.fromJson(line, Request.class);

4. PROCESAR EN HANDLER
   handleRequest(req)
   → case "ADD_POCKET": pocketService.addPocket("Vac", 1000);

5. EN SERVICE
   synchronized void addPocket(...) {
     // Validar
     if(1000 > mainAccount.getAvailableBalance()) throw error;
     
     // Actualizar estado
     mainAccount.setAvailableBalance(4000);
     
     // Persistir en BD
     pocketDao.save(pocket);
     
     // Devolver respuesta limpia
     return pocket;  // SIN ciclos
   }

6. SERVIDOR CONVIERTE A JSON
   response.data = gson.toJsonTree(pocket);

7. SERVIDOR ENVÍA RESPUESTA
   writer.write(gson.toJson(response));
   writer.flush();

8. CLIENTE RECIBE Y PARSEA
   String line = reader.readLine();
   Response resp = gson.fromJson(line, Response.class);

9. CLIENTE VERIFICA
   assert resp.status.equals("ok");
   assert pocket.getBalance() == 1000.0;
```

---

### 🚀 OPTIMIZACIONES (Opcional pero impresiona)

**1. Connection pooling**
```java
// En lugar de crear conexión nueva cada vez:
// Tener un pool de conexiones reutilizables
// (HikariCP, C3P0, etc)
```

**2. Caché de datos**
```java
// En lugar de ir a BD cada vez:
// Guardar en memoria y sincronizar
// (Lo hacemos con mainAccount)
```

**3. Transacciones ACID**
```java
Connection conn = ConnectionManager.getInstance().getConnection();
conn.setAutoCommit(false);  // Comenzar transacción
try {
    // Múltiples operaciones
    pocketDao.update(p1);
    pocketDao.update(p2);
    conn.commit();  // Todo o nada
} catch(Exception e) {
    conn.rollback();  // Deshacer cambios
}
```

---

## PARTE 4: RESPUESTAS RÁPIDAS

### P: ¿Por qué synchronized y no lock()?
**R:** `synchronized` es más simple para principiantes. `Lock` da más control pero es más complejo.

### P: ¿Puedo usar un ArrayList global en lugar de MainAccount?
**R:** Técnicamente sí, pero no es buena práctica. MainAccount encapsula el estado de forma clara.

### P: ¿Qué pasa si dos clientes hacen ADD_POCKET al mismo tiempo?
**R:** Con `synchronized`, uno espera a que el otro termine. Sin `synchronized`, ambos leen el mismo saldo y ambos restan, causando inconsistencia.

### P: ¿Necesito cerrar la conexión en cada operación?
**R:** Sí. Cada `getConnection()` abre una conexión nueva. Cada una debe cerrarse.

### P: ¿Puedo hacer SELECT dentro de un while?
**R:** No, porque necesitas nueva conexión. Haz SELECT, guarda en lista, luego procesa.

### P: ¿El puerto 5000 es especial?
**R:** No, es solo un número > 1024 (para no necesitar permisos especiales). Podrías usar 8080, 3000, etc.

### P: ¿Qué pasa si un cliente se desconecta sin cerrar?
**R:** El try-catch de handleClient() atrapa la excepción. El socket se cierra y el thread termina.

### P: ¿Debo serializar el MainAccount completo?
**R:** No, eso causa ciclos. Copia solo los datos que necesitas (sin referencias).

---

## PARTE 5: FÓRMULA PARA RESOLVER EL PARCIAL

### Hora 0-5 minutos: Lee y entiende
- [ ] Lee README 2 veces
- [ ] Identifica acciones que debe soportar
- [ ] Entiende estructura de Request/Response esperada

### Hora 5-20 minutos: Modelos
- [ ] Crea clases Model (getters/setters)
- [ ] No pierdas tiempo en validaciones, solo datos

### Hora 20-40 minutos: Base de Datos
- [ ] ConnectionManager Singleton
- [ ] PocketDao con TODAS las queries
- [ ] PRUEBA con print que guarda/lee correctamente

### Hora 40-60 minutos: Lógica de Negocio
- [ ] PocketService con synchronized
- [ ] Validaciones AQUÍ
- [ ] Actualizar estado + persistir en BD

### Hora 60-75 minutos: Servidor
- [ ] Init con ServerSocket + loop
- [ ] handleClient con thread
- [ ] handleRequest con switch
- [ ] Manejo de errores

### Hora 75-100 minutos: Tests
- [ ] Ejecuta tests
- [ ] Corrige errores
- [ ] Verifica que BD persiste
- [ ] Prueba concurrencia

---

## PARTE 6: RESUMEN EN UNA IMAGEN MENTAL

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENTE                              │
│  Socket → JSON → Servidor:5000                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       SERVIDOR                              │
│  ServerSocket.accept()                                      │
│  → new Thread(handleClient).start()  ← CLAVE               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    handleRequest()                          │
│  switch(request.action) {                                  │
│    case ADD: pocketService.addPocket()                     │
│    case DEPOSIT: pocketService.depositInPocket()           │
│    ...                                                      │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  PocketService (sincronizado)               │
│  synchronized void addPocket() {                            │
│    // 1. VALIDAR                                            │
│    // 2. ACTUALIZAR en memoria (mainAccount)               │
│    // 3. PERSISTIR en BD (pocketDao)                       │
│    // 4. DEVOLVER respuesta limpia                         │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                       PocketDao                             │
│  - save(pocket)    → INSERT                                 │
│  - finById(name)   → SELECT WHERE                           │
│  - update(pocket)  → UPDATE WHERE                           │
│  - findAll()       → SELECT *                               │
│  - delete(pocket)  → DELETE WHERE                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      BASE DE DATOS                          │
│  H2 en memoria (o MySQL, PostgreSQL, etc)                  │
└─────────────────────────────────────────────────────────────┘

FLUJO DE DATOS:
Client JSON → String → Request object → Service → DAO → DB
     ↓                                                        ↑
Response JSON ← String ← Response object ← Service output ← DB state
```

---

## FINAL: La Regla de Oro

**"Un valor debe estar en UN SOLO LUGAR, y todos deben ir a buscarlo allí"**

- MainAccount: está en PocketService
- Todos los métodos de Service: synchronized
- BD: fuente de verdad para persistencia
- Respuestas JSON: copias limpias

Si violás esto → inconsistencias, bugs, race conditions.

¡Buena suerte en el parcial! 🚀
