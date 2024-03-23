package org.example;

import org.example.controller.GameController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        GameController controller = new GameController();

        if (args.length != 1) {
            System.err.println("No address in parameters!");
            System.exit(-1);
        }

        List<Byte> addrList = List.of();
        try {
            addrList = Arrays.stream(args[0].split("\\.")).map(s -> (byte) Integer.parseInt(s)).toList();
        } catch (Exception e) {
            System.err.println("Failed to parse address.");
            System.exit(-1);
        }

        if (addrList.size() != 4) {
            System.err.println("Wrong address size");
            System.exit(-1);
        }

        byte[] addr = new byte[] {
                addrList.get(0),
                addrList.get(1),
                addrList.get(2),
                addrList.get(3)
        };

//        InetAddress address = InetAddress.getByAddress(new byte[]{(byte) 172, 21, 47, (byte) 255});
        InetAddress address = InetAddress.getByAddress(addr);

        controller.runLab6(address);
    }
}