package de.fester.client;

import de.fester.clock.ClockCommands;
import de.fester.grpc.Command;
import de.fester.grpc.RemoteClockGrpc;
import de.fester.grpc.Response;
import io.grpc.Channel;

public class ClockStub implements ClockCommands {

    private final RemoteClockGrpc.RemoteClockBlockingStub blockingStub;

    private String lastResponse;

    public ClockStub(Channel channel) {
        blockingStub = RemoteClockGrpc.newBlockingStub(channel);
    }

    public void sendCommand(Command command) {
        Response response = blockingStub.executeCommand(command);

        lastResponse = response.getContent();
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

    public String getLastResponse() {
        return lastResponse;
    }
}
