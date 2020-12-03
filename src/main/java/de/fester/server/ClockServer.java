package de.fester.server;

import de.fester.clock.ClockCommands;
import de.fester.clock.IllegalCmdException;
import de.fester.grpc.Command;
import de.fester.grpc.RemoteClockGrpc;
import de.fester.grpc.Response;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class ClockServer implements ClockCommands {

    private static final Logger logger = Logger.getLogger(ClockServer.class.getName());

    private final Clock clock = new Clock();

    private final int port;
    private final Server server;

    public ClockServer(int port) {
        this(ServerBuilder.forPort(port), port);
    }

    /**
     * Create a RouteGuide server using serverBuilder as a base and features as data.
     */
    public ClockServer(ServerBuilder<?> serverBuilder, int port) {
        this.port = port;
        this.server = serverBuilder.addService(new ClockService()).build();
    }

    /**
     * RPC service to handle the forwarding
     */
    private class ClockService extends RemoteClockGrpc.RemoteClockImplBase {
        @Override
        public void executeCommand(Command request, StreamObserver<Response> responseObserver) {
            // Process the command
            responseObserver.onNext(Response.newBuilder().setContent(processCommand(request)).build());
            responseObserver.onCompleted();

            // Client told us to exit.
            if (request.getCmd() == ClockCommands.CMD_EXIT) {
                try {
                    stopServer();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Start serving requests.
     */
    public void startServer() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                ClockServer.this.stopServer();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stopServer() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    // region ClockCommands

    private String lastResponse = null;

    @Override
    public void start() throws IllegalCmdException {
        clock.start();
        setResponse("Clock started");
    }

    @Override
    public void reset() throws IllegalCmdException {
        clock.reset();
        setResponse("Clock resetted");
    }

    @Override
    public long getTime() throws IllegalCmdException {
        long elapsedTime = clock.getTime();
        setResponse("elapsed time = " + elapsedTime + "ms");
        return elapsedTime;
    }

    @Override
    public void waitTime(long time) throws IllegalCmdException {
        clock.waitTime(time);
        setResponse("Wait finished");
    }

    @Override
    public long halt() throws IllegalCmdException {
        long haltedAtTime = clock.halt();
        setResponse("clock halted, elapsed time = " + haltedAtTime + "ms");
        return haltedAtTime;
    }

    @Override
    public void conTinue() throws IllegalCmdException {
        clock.conTinue();
        setResponse("Clock continued");
    }

    @Override
    public void exit() throws IllegalCmdException {
        clock.exit();
        setResponse("Programm stop");
    }

    private void setResponse(String msg) {
        lastResponse = msg;
    }

    public String getLastResponse() {
        return lastResponse;
    }

    public String processCommand(Command command) {
        if (command == null) {
            command = Command.newBuilder().setCmd(-1).build();
        }

        logger.info("Processing command: " + command.getCmd() + "(" + command.getParameter() + ")");

        // Execute the command and get it's response
        String response = null;
        try {
            switch (command.getCmd()) {
                case ClockCommands.CMD_CONTINUE -> conTinue();
                case ClockCommands.CMD_GETTIME -> getTime();
                case ClockCommands.CMD_START -> start();
                case ClockCommands.CMD_WAIT -> waitTime(command.getParameter());
                case ClockCommands.CMD_HALT -> halt();
                case ClockCommands.CMD_RESET -> reset();
                case ClockCommands.CMD_EXIT -> response = "Closing connection..";
                default -> response = "Invalid command received";
            }

            if (response == null)
                response = getLastResponse();
        } catch (IllegalCmdException ex) {
            response = ex.getMessage();
        }

        logger.info("Processed command, Result: " + response);

        return response;
    }

    // endregion

    public static void main(String[] args) throws Exception {
        ClockServer server = new ClockServer(8980);
        server.startServer();
        server.blockUntilShutdown();
    }
}
