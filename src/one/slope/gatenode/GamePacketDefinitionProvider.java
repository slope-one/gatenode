package one.slope.gatenode;

import one.slope.slip.io.DataType;
import one.slope.slip.io.packet.PacketDefinition;
import one.slope.slip.io.packet.PacketDefinitionProvider;
import one.slope.slip.io.packet.PacketType;
import one.slope.slip.io.packet.field.NumericFieldCodec;
import one.slope.slip.io.packet.field.PacketField;

public class GamePacketDefinitionProvider implements PacketDefinitionProvider {
	private final PacketDefinition[] packets = new PacketDefinition[] {
		new PacketDefinition(PacketType.NEGOTIATION, 14, "service_select", new PacketField<?>[] {
			new PacketField<Integer>("name_part", 0, new NumericFieldCodec<Integer>(DataType.BYTE))
		}, 1)
	};
	
	@Override
	public PacketDefinition[] get() {
		return packets;
	}
}
