# TCP vs UDP: Análisis y Cambios Necesarios

---

## PARTE 1: DIFERENCIAS FUNDAMENTALES

### TCP (Lo que usamos ahora)

```
CARACTERÍSTICA      | DESCRIPCIÓN
--------------------|------------------------------------
Conexión            | ORIENTADO A CONEXIÓN (3-way handshake)
Confiabilidad       | GARANTIZADO - llegan todos los datos
Orden               | GARANTIZADO - llegan en orden
Retransmisión       | AUTOMÁTICA si se pierden paquetes
Velocidad           | Más lento (por las garantías)
Overhead            | Mayor (headers más grandes)
Uso                 | Web (HTTP), Email, Transferencias
```

**En nuestro caso (TCP):**
```
Cliente conecta → Conexión establecida → Comunicación bidireccional
Si se pierde 1 byte → Se retransmite automáticamente
Si llega fuera de orden → TCP reordena
```

---

### UDP (Lo que pediría el profesor)

```
CARACTERÍSTICA      | DESCRIPCIÓN
--------------------|------------------------------------
Conexión            | SIN CONEXIÓN (envía y olvida)
Confiabilidad       | NO GARANTIZADO - pueden perderse paquetes
Orden               | NO GARANTIZADO - pueden llegar desordenados
Retransmisión       | NO AUTOMÁTICA - el programador debe hacerlo
Velocidad           | Más rápido (sin overhead)
Overhead            | Menor (headers más pequeños)
Uso                 | Gaming, Streaming, VoIP, DNS
```

**En nuestro caso (UDP):**
```
Cliente envía paquete → Servidor lo recibe (o no)
Si se pierde → No se retransmite automáticamente
Si llega fuera de orden → Cliente debe reordenar
```

---

## PARTE 2: CAMBIOS EN EL CÓDIGO

### CON TCP (Actual)

```java
// ========== SERVER ==========
ServerSocket serverSocket = new ServerSocket(5000);  // Escucha

while(true) {
    Socket clientSocket = serverSocket.accept();      // Espera conexión
    new Thread(() -> handleClient(clientSocket)).start();
}

public void handleClient(Socket socket) {
    // Conexión YA ESTABLECIDA
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
    );
    String line = reader.readLine();  // Espera a leer algo
    // ... procesar ...
}
```

**Características:**
- Conexión previa
- Lectura/escritura como streams
- Sockets individual por cliente
- Una línea = un mensaje

---

### CON UDP (Cambios necesarios)

```java
// ========== SERVER ==========
DatagramSocket serverSocket = new DatagramSocket(5000);  // Escucha UDP

while(true) {
    // 1. Preparar buffer para recibir
    byte[] receiveData = new byte[1024];
    DatagramPacket receivePacket = 
        new DatagramPacket(receiveData, receiveData.length);
    
    // 2. Recibir paquete (BLOQUEANTE)
    serverSocket.receive(receivePacket);
    
    // 3. Extraer información del cliente
    InetAddress clientAddress = receivePacket.getAddress();
    int clientPort = receivePacket.getPort();
    
    // 4. Procesar en thread separado
    new Thread(() -> handleClient(
        serverSocket, 
        clientAddress, 
        clientPort, 
        receiveData
    )).start();
}

public void handleClient(DatagramSocket socket, 
                        InetAddress clientAddr, 
                        int clientPort, 
                        byte[] receiveData) {
    
    try {
        // 1. Convertir datos recibidos a String
        String line = new String(receiveData, 0, receiveData.length).trim();
        
        // 2. Procesar request
        Request request = gson.fromJson(line, Request.class);
        Response response = handleRequest(request);
        
        // 3. Convertir respuesta a bytes
        byte[] sendData = gson.toJson(response).getBytes();
        
        // 4. Crear paquete de respuesta
        DatagramPacket sendPacket = new DatagramPacket(
            sendData, 
            sendData.length, 
            clientAddr,      // ← IMPORTANTE: dirección del cliente
            clientPort       // ← IMPORTANTE: puerto del cliente
        );
        
        // 5. Enviar respuesta
        socket.send(sendPacket);
        
    } catch(Exception e) {
        e.printStackTrace();
    }
    // NO hay socket.close() porque UDP no tiene conexión
}
```

---

## PARTE 3: TABLA COMPARATIVA DE CAMBIOS

| Aspecto | TCP | UDP |
|--------|-----|-----|
| **Clase Principal** | `ServerSocket` | `DatagramSocket` |
| **Objeto Cliente** | `Socket` | `DatagramPacket` + `InetAddress` |
| **Crear servidor** | `new ServerSocket(5000)` | `new DatagramSocket(5000)` |
| **Recibir datos** | `socket.accept()` + `reader.readLine()` | `socket.receive(packet)` |
| **Enviar datos** | `writer.write()` + `writer.flush()` | `socket.send(packet)` |
| **Obtener cliente** | Implícito en `Socket` | `packet.getAddress()` + `packet.getPort()` |
| **Buffer datos** | Stream continuo | Bytes fijos `byte[1024]` |
| **Cerrar conexión** | `socket.close()` | NO APLICA (sin conexión) |
| **Conexión previo** | SÍ (handshake) | NO (envía directo) |
| **Confiabilidad** | Garantizada | No garantizada |

---

## PARTE 4: CÓDIGO COMPLETO UDP

```java
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

import com.google.gson.Gson;

import dtos.Request;
import dtos.Response;
import model.MainAccount;
import model.Pocket;
import services.PocketService;

/**
 * Server UDP: Sin conexión, basado en paquetes
 * 
 * FLUJO:
 * 1. Recibir paquete UDP (contiene cliente + datos)
 * 2. Procesar en thread
 * 3. Enviar respuesta AL CLIENTE (sin conexión previa)
 * 
 * DIFERENCIAS:
 * - No hay "conexión" previa
 * - Cada interacción es independiente
 * - Puede haber pérdida de paquetes
 * - Más rápido, menos overhead
 */
public class ServerUDP {
    
    private PocketService pocketService;
    private Gson gson;
    
    // Tamaño máximo de paquete UDP
    private static final int BUFFER_SIZE = 1024;
    
    public static void main(String[] args) throws Exception {
        ServerUDP server = new ServerUDP();
        server.init(5000.0);
    }
    
    /**
     * init: Inicializar servidor UDP
     * 
     * DIFERENCIAS CON TCP:
     * - NO hay ServerSocket, sino DatagramSocket
     * - NO hay accept(), sino receive()
     * - Cada receive() bloquea hasta recibir un paquete
     */
    public void init(Double initialAmount) throws Exception {
        // === INICIALIZAR ESTADO ===
        pocketService = new PocketService(initialAmount);
        gson = new Gson();
        
        // === CREAR SOCKET UDP ===
        DatagramSocket serverSocket = new DatagramSocket(5000);
        System.out.println("UDP Server listening on port 5000...");
        
        // === LOOP INFINITO ===
        while (true) {
            try {
                // === PREPARAR BUFFER PARA RECIBIR ===
                byte[] receiveData = new byte[BUFFER_SIZE];
                
                // === CREAR PAQUETE PARA RECIBIR ===
                // El paquete se llena con los datos recibidos
                DatagramPacket receivePacket = new DatagramPacket(
                    receiveData,        // Buffer donde guardar datos
                    receiveData.length  // Tamaño máximo
                );
                
                // === RECIBIR PAQUETE (BLOQUEANTE) ===
                // Se queda esperando hasta que llegue un paquete
                serverSocket.receive(receivePacket);
                
                // === EXTRAER INFORMACIÓN DEL CLIENTE ===
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                
                System.out.println("Request from: " + clientAddress.getHostAddress() 
                    + ":" + clientPort);
                
                // === PROCESAR EN THREAD SEPARADO ===
                // NO bloqueamos el servidor mientras procesamos
                new Thread(() -> handleClient(
                    serverSocket,      // Socket compartido
                    clientAddress,     // Dirección del cliente
                    clientPort,        // Puerto del cliente
                    receiveData        // Datos recibidos
                )).start();
                
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * handleClient: Procesar UN cliente
     * 
     * DIFERENCIAS CON TCP:
     * - No tenemos un Socket específico del cliente
     * - Tenemos su dirección (IP) y puerto
     * - Debemos saber a dónde responder
     * - No hay "conexión" que cerrar
     */
    public void handleClient(DatagramSocket socket,
                            InetAddress clientAddr,
                            int clientPort,
                            byte[] receiveData) {
        try {
            // === CONVERTIR BYTES A STRING ===
            // UDP recibe datos como bytes puros
            String line = new String(receiveData, 0, receiveData.length).trim();
            
            System.out.println("Raw data: " + line);
            
            // === PARSEAR REQUEST ===
            Request request = gson.fromJson(line, Request.class);
            
            // === PROCESAR ===
            Response response = handleRequest(request);
            
            // === CONVERTIR RESPUESTA A BYTES ===
            String responseJson = gson.toJson(response);
            byte[] sendData = responseJson.getBytes();
            
            // === CREAR PAQUETE DE RESPUESTA ===
            DatagramPacket sendPacket = new DatagramPacket(
                sendData,           // Datos a enviar
                sendData.length,    // Longitud de datos
                clientAddr,         // ← IMPORTANTE: IP del cliente
                clientPort          // ← IMPORTANTE: Puerto del cliente
            );
            
            // === ENVIAR RESPUESTA ===
            socket.send(sendPacket);
            
            System.out.println("Response sent to: " + clientAddr.getHostAddress() 
                + ":" + clientPort);
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        // NO cerrar socket porque es compartido
        // NO cerrar conexión porque UDP no tiene conexión
    }
    
    /**
     * handleRequest: Procesar una request
     * (EXACTAMENTE IGUAL QUE CON TCP)
     */
    public Response handleRequest(Request request) throws Exception {
        Response response = new Response();
        
        try {
            switch (request.action) {
                case "ADD_POCKET": {
                    String name = request.data.get("name");
                    double initialAmount = Double.parseDouble(
                        request.data.get("initialAmount")
                    );
                    Pocket pocket = pocketService.addPocket(name, initialAmount);
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
                // ... resto de casos igual que TCP ...
                default:
                    response.status = "error";
                    response.data = gson.toJsonTree(
                        Map.of("message", "Unknown action")
                    ).getAsJsonObject();
            }
        } catch (Exception e) {
            response.status = "error";
            response.data = gson.toJsonTree(
                Map.of("message", e.getMessage())
            ).getAsJsonObject();
        }
        
        return response;
    }
}
```

---

## PARTE 5: CLIENTE UDP (Para probar)

```java
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.google.gson.Gson;
import dtos.Request;
import dtos.Response;

public class ClientUDP {
    
    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();
        
        // === CREAR SOCKET UDP ===
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 5000;
        
        // === CREAR REQUEST ===
        Request request = new Request();
        request.action = "ADD_POCKET";
        request.data = Map.of(
            "name", "Vacaciones",
            "initialAmount", "1000"
        );
        
        // === CONVERTIR A BYTES ===
        String requestJson = gson.toJson(request);
        byte[] sendData = requestJson.getBytes();
        
        // === CREAR PAQUETE ===
        DatagramPacket sendPacket = new DatagramPacket(
            sendData,
            sendData.length,
            serverAddress,
            serverPort
        );
        
        // === ENVIAR ===
        clientSocket.send(sendPacket);
        System.out.println("Request sent");
        
        // === RECIBIR RESPUESTA ===
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(
            receiveData,
            receiveData.length
        );
        clientSocket.receive(receivePacket);
        
        String responseJson = new String(receiveData, 0, receivePacket.getLength());
        Response response = gson.fromJson(responseJson, Response.class);
        
        System.out.println("Response: " + response.status);
        System.out.println("Data: " + response.data);
        
        clientSocket.close();
    }
}
```

---

## PARTE 6: VENTAJAS Y DESVENTAJAS

### UDP VENTAJAS
✅ **Más rápido** - sin overhead de conexión  
✅ **Menor latencia** - envía inmediatamente  
✅ **Broadcasting** - puedes enviar a múltiples destinos  
✅ **Mejor para datos en tiempo real** - streaming, gaming

### UDP DESVENTAJAS
❌ **Sin garantía de entrega** - pueden perderse paquetes  
❌ **Sin garantía de orden** - pueden llegar desordenados  
❌ **Sin garantía de duplicados** - puede recibir 2 veces lo mismo  
❌ **Tamaño máximo** - ~65KB por paquete  
❌ **Debo manejar errores yo** - no hay retransmisión automática

---

## PARTE 7: CUÁNDO USAR CADA UNO

### USA TCP SI:
- Necesitas garantía de entrega (banco, mensajería)
- Los datos son críticos (no puede perderse nada)
- Transferencias grandes de datos
- Orden importa

**Ejemplo: Sistema bancario REAL**

### USA UDP SI:
- Velocidad es más importante que perfección
- Algunos paquetes perdidos no es catástrofe
- Datos pequeños y frecuentes
- Tiempo real (streaming, gaming)

**Ejemplo: Videojuego en línea, VoIP**

---

## PARTE 8: RESUMEN DE CAMBIOS

| Cambio | TCP | UDP |
|--------|-----|-----|
| **Import** | `java.net.Socket` | `java.net.DatagramSocket` |
| **Crear servidor** | `ServerSocket ss = new ServerSocket(5000)` | `DatagramSocket ds = new DatagramSocket(5000)` |
| **Recibir datos** | `Socket s = ss.accept(); BufferedReader r = ...` | `DatagramPacket p = new DatagramPacket(...); ds.receive(p)` |
| **Obtener cliente** | Está en `Socket s` | Está en `p.getAddress()` y `p.getPort()` |
| **Leer mensaje** | `String line = reader.readLine()` | `String line = new String(data)` |
| **Enviar mensaje** | `writer.write(...); writer.flush()` | `DatagramPacket p = new DatagramPacket(...); socket.send(p)` |
| **Cerrar** | `socket.close()` | No es necesario (sin conexión) |
| **Threads** | Uno por cliente | Uno por paquete (opcional) |
| **Confiabilidad** | Garantizada | No garantizada |

---

## PARTE 9: REGLA DE ORO UDP

**"UDP es enviar un paquete a una dirección y esperar lo mejor"**

Con TCP: Hay una "relación" entre cliente y servidor
Con UDP: No hay relación, solo "envío de paquetes"

Por eso en UDP:
- No hay `accept()` (no hay conexión)
- No hay streams (son paquetes)
- Debo recordar dirección IP + Puerto (para responder)
- No hay garantía que llegue

---

## NOTA IMPORTANTE

**Si el profesor pide UDP, la LÓGICA de negocio (PocketService) es EXACTAMENTE IGUAL.**

Solo cambia:
- Cómo escuchas (`ServerSocket` → `DatagramSocket`)
- Cómo recibes datos (`accept()` → `receive()`)
- Cómo envías respuestas (`writer` → `send()`)
- Cómo identificas al cliente (Socket → IP+Puerto)

**PocketService, validaciones, BD, modelos: TODO IGUAL**
