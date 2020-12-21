package de.fester.client;

import de.fester.clock.ClockCommands;
import de.fester.grpc.Command;
import de.fester.grpc.RemoteClockGrpc;
import de.fester.grpc.Response;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ClockStub implements ClockCommands {

    private final StreamObserver<Command> streamObserver;

    public ClockStub(ClockStubListener clockStubListener) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8980").usePlaintext().build();
        RemoteClockGrpc.RemoteClockStub asyncStub = RemoteClockGrpc.newStub(channel);

        streamObserver = asyncStub.executeCommand(new StreamObserver<>() {
            @Override
            public void onNext(Response value) {
                if (clockStubListener != null)
                    clockStubListener.receiveResponse(value.getContent());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                streamObserver.onCompleted();
            }
        });
    }

    public void sendCommand(Command command) {
        streamObserver.onNext(command);

        if (command.getCmd() == ClockCommands.CMD_EXIT)
            streamObserver.onCompleted();
    }

    public Command buildCommand(int cmd) {
        return buildCommand(cmd, 0);
    }

    public Command buildCommand(int cmd, long parameter) {
        return Command.newBuilder().setCmd(cmd).setParameter(parameter).build();
    }

    @Override
    public void start() {
        sendCommand(buildCommand(ClockCommands.CMD_START));
    }

    @Override
    public void reset() {
        sendCommand(buildCommand(ClockCommands.CMD_RESET));
    }

    @Override
    public long getTime() {
        sendCommand(buildCommand(ClockCommands.CMD_GETTIME));
        return 0;
    }

    @Override
    public void waitTime(long time) {
        sendCommand(buildCommand(ClockCommands.CMD_WAIT, time));
    }

    @Override
    public long halt() {
        sendCommand(buildCommand(ClockCommands.CMD_HALT));
        return 0;
    }

    @Override
    public void conTinue() {
        sendCommand(buildCommand(ClockCommands.CMD_CONTINUE));
    }

    @Override
    public void exit() {
        sendCommand(buildCommand(ClockCommands.CMD_EXIT));
    }

    public interface ClockStubListener {
        void receiveResponse(String response);
    }
}
