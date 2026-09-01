package net.client;

import com.google.gson.JsonObject;
import net.Envelope;
import net.JsonLine;
import net.Protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ServerConnection {

    private static final String POISON = " CLOSE";
    private static final int CONNECT_TIMEOUT_MILLIS = 4000;

    private final Socket socket = new Socket();
    private final BufferedReader in;
    private final BufferedWriter out;
    private final BlockingQueue<String> outbound = new LinkedBlockingQueue<>();
    private final Queue<Envelope> inbox = new ConcurrentLinkedQueue<>();
    private final AtomicLong sequence = new AtomicLong();

    private volatile boolean running = true;
    private volatile String failure;
    private volatile long lastPingSentMillis;
    private Thread writerThread;

    public ServerConnection(String host, int port) throws IOException {
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
        socket.setTcpNoDelay(true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

        Thread reader = new Thread(this::readLoop, "client-net-reader");
        reader.setDaemon(true);
        reader.start();

        Thread writer = new Thread(this::writeLoop, "client-net-writer");
        writer.setDaemon(true);
        writer.start();
        this.writerThread = writer;
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                Envelope envelope = JsonLine.decode(line);
                if (envelope != null) inbox.offer(envelope);
            }
            if (running) failure = "The server closed the connection.";
        } catch (IOException e) {
            if (running) failure = "Lost contact with the server.";
        } finally {
            running = false;
        }
    }

    private void writeLoop() {
        try {
            while (running) {
                String line = outbound.take();
                if (POISON.equals(line)) break;
                out.write(line);
                out.write('\n');
                out.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (running) failure = "Could not reach the server.";
        } finally {
            running = false;
        }
    }

    public long send(String type, JsonObject payload) {
        long id = sequence.incrementAndGet();
        if (!running) return id;
        outbound.offer(JsonLine.encode(new Envelope(type, id, null, payload)));
        return id;
    }

    public Envelope poll() {
        return inbox.poll();
    }

    public void maybePing() {
        long now = System.currentTimeMillis();
        if (now - lastPingSentMillis < Protocol.PING_INTERVAL_MILLIS) return;
        lastPingSentMillis = now;
        send(Protocol.PING, new JsonObject());
    }

    public boolean isRunning() {
        return running;
    }

    public String getFailure() {
        return failure;
    }

    public void close() {
        if (!running) return;
        running = false;
        outbound.offer(POISON);
        if (writerThread != null) {
            try {
                writerThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Nothing useful to do when the socket refuses to close.
        }
    }
}