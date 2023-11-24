package gqserver.api.packets.data;

import gqserver.api.Packet;

public record DataRequestPacket(String networkCode, String stationCode, boolean cancel) implements Packet {
}