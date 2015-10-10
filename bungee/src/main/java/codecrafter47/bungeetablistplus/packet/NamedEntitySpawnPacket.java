package codecrafter47.bungeetablistplus.packet;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
public class NamedEntitySpawnPacket extends DefinedPacket{
	int id;
	private UUID uuid;
	private ByteBuf data = null;
	public NamedEntitySpawnPacket() {
		
	}
	public NamedEntitySpawnPacket(UUID uuid,int id) {
		this.id = Short.MIN_VALUE+id;
		this.uuid = uuid;
	}

	@Override
	public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
	{
		data = buf;
	}

	@Override
	public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion)
	{
		if (data != null) {
			buf.writeBytes(data);
			return;
		}
		writeVarInt(id,buf);
		writeUUID(uuid,buf);
		buf.writeInt(0);
		buf.writeInt(0);
		buf.writeInt(0);
		buf.writeByte(0);
		buf.writeByte(0);
		buf.writeShort(0);
		buf.writeByte(0);
		buf.writeByte(0x20);
		buf.writeByte(10);
		buf.writeByte(127);
		buf.writeByte(127);
	}

	@Override
	public void handle(AbstractPacketHandler handler) throws Exception {

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		result = prime * result + id;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NamedEntitySpawnPacket other = (NamedEntitySpawnPacket) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		if (id != other.id)
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NamedEntitySpawnPacket [id=" + id + ", uuid=" + uuid + ", data=" + data + "]";
	}

}
