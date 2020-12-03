package de.fester.client;

import de.fester.clock.ClockCommands;
import de.fester.grpc.Command;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class ClockClient {
    // displays a message on the screen
    void display(String msg) {
        System.out.println(" " + msg);
    }

    // sends a prompt message for user (command) input
    void prompt(String msg) {
        System.out.print(msg);
    }

    // read syntactically correct command from keyboard
    Command getCommand() {
        // allowed commands as regular expression
        Pattern commandSyntax = Pattern.compile("s|c|h|r|e|g|w +[1-9][0-9]*");
        String cmdText = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        // loop until correct command has been entered
        while (true) {
            try {
                prompt("command [s|c|h|r|e|g|w]: ");
                cmdText = in.readLine();
                if (!commandSyntax.matcher(cmdText).matches())
                    throw new Exception();
                break; // leave loop here if correct command detected
            } catch (Exception e) {
                display("command syntax error");
            }
        }

        //   decode command and return with Command object
        StringTokenizer st = new StringTokenizer(cmdText);
        // first token=command
        // second token = parameter
        // case 'e' and any other char

        Command.Builder command = Command.newBuilder();
        switch (st.nextToken().charAt(0)) {
            case 'c' -> command.setCmd(ClockCommands.CMD_CONTINUE);
            case 'g' -> command.setCmd(ClockCommands.CMD_GETTIME);
            case 's' -> command.setCmd(ClockCommands.CMD_START);
            case 'w' -> command.setCmd(ClockCommands.CMD_WAIT).setParameter(Long.parseLong(st.nextToken()));
            case 'h' -> command.setCmd(ClockCommands.CMD_HALT);
            case 'r' -> command.setCmd(ClockCommands.CMD_RESET);
            default -> command.setCmd(ClockCommands.CMD_EXIT);
        }
        return command.build();
    }

    void run() {
        final String target = "localhost:8980";

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        ClockStub clockStub = new ClockStub(channel);

        display("accepted commands:");
        display("s[tart] h[old] c[ontinue] r[eset])");
        display("g[et time] e[xit] w[ait]\n");

        Command command;
        do {
            // Read command from console
            command = getCommand();
            switch (command.getCmd()) {
                case ClockCommands.CMD_CONTINUE -> clockStub.conTinue();
                case ClockCommands.CMD_GETTIME -> clockStub.getTime();
                case ClockCommands.CMD_START -> clockStub.start();
                case ClockCommands.CMD_WAIT -> clockStub.waitTime(command.getParameter());
                case ClockCommands.CMD_HALT -> clockStub.halt();
                case ClockCommands.CMD_RESET -> clockStub.reset();
                case ClockCommands.CMD_EXIT -> clockStub.exit();
                default -> display("Illegal command");
            }

            // Print response received from server
            display(clockStub.getLastResponse());
        } while (command.getCmd() != ClockCommands.CMD_EXIT);
    }

    public static void main(String[] args) {
        (new ClockClient()).run();
    }
}
