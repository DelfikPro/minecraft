package net.minecraft.network;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.logging.Log;
import net.minecraft.network.protocol.minecraft.handshake.NetHandlerHandshakeMemory;
import net.minecraft.network.protocol.minecraft_47.play.server.S40PacketDisconnect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.protocol.minecraft.handshake.NetHandlerHandshakeTCP;
import net.minecraft.util.*;
import net.minecraft.util.chat.ChatComponentText;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NetworkSystem {

	private static final Log logger = Log.MAIN;
	public static final LazySupplier<NioEventLoopGroup> NIO_SERVER = new LazySupplier<NioEventLoopGroup>() {
		protected NioEventLoopGroup load() {
			return new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Server IO #%d").setDaemon(true).build());
		}
	};
	public static final LazySupplier<EpollEventLoopGroup> EPOLL_SERVER = new LazySupplier<EpollEventLoopGroup>() {
		protected EpollEventLoopGroup load() {
			return new EpollEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
		}
	};

	/**
	 * Reference to the MinecraftServer object.
	 */
	private final MinecraftServer mcServer;

	/**
	 * True if this NetworkSystem has never had his endpoints terminated
	 */
	public volatile boolean isAlive;
	private final List<ChannelFuture> endpoints = Collections.synchronizedList(new ArrayList<>());
	private final List<NetworkManager> networkManagers = Collections.synchronizedList(new ArrayList<>());

	public NetworkSystem(MinecraftServer server) {
		this.mcServer = server;
		this.isAlive = true;
	}

	/**
	 * Adds a channel that listens on publicly accessible network ports
	 */
	public void addLanEndpoint(InetAddress address, int port) throws IOException {
		synchronized (this.endpoints) {
			Class<? extends ServerSocketChannel> oclass;
			LazySupplier<? extends EventLoopGroup> lazyloadbase;

			if (Epoll.isAvailable() && this.mcServer.useEpoll()) {
				oclass = EpollServerSocketChannel.class;
				lazyloadbase = EPOLL_SERVER;
				logger.info("Тип системы подключений - Epoll");
			} else {
				oclass = NioServerSocketChannel.class;
				lazyloadbase = NIO_SERVER;
				logger.info("Тип системы подключений - NIO");
			}

			this.endpoints.add(new ServerBootstrap().channel(oclass).childHandler(new ChannelInitializer<Channel>() {
				protected void initChannel(Channel channel) {
					try {
						channel.config().setOption(ChannelOption.TCP_NODELAY, Boolean.TRUE);
					} catch (ChannelException ignored) {}

					channel.pipeline()
							.addLast("timeout", new ReadTimeoutHandler(30))
							.addLast("legacy_query", new PingResponseHandler(NetworkSystem.this))
							.addLast("splitter", new NettyCommunication.Splitter())
							.addLast("decoder", new NettyCommunication.Decoder(true))
							.addLast("prepender", new NettyCommunication.Prepender())
							.addLast("encoder", new NettyCommunication.Encoder(false));

					NetworkManager networkmanager = new NetworkManager(true);
					NetworkSystem.this.networkManagers.add(networkmanager);
					channel.pipeline().addLast("packet_handler", networkmanager);
					networkmanager.setNetHandler(new NetHandlerHandshakeTCP(NetworkSystem.this.mcServer, networkmanager));
				}
			}).group(lazyloadbase.getValue()).localAddress(address, port).bind().syncUninterruptibly());
		}
	}

	/**
	 * Adds a channel that listens locally
	 */
	public SocketAddress addLocalEndpoint() {
		ChannelFuture channelfuture;

		synchronized (this.endpoints) {
			channelfuture = new ServerBootstrap().channel(LocalServerChannel.class).childHandler(new ChannelInitializer<Channel>() {
				protected void initChannel(Channel channel) {
					NetworkManager networkmanager = new NetworkManager(true);
					networkmanager.setNetHandler(new NetHandlerHandshakeMemory(NetworkSystem.this.mcServer, networkmanager));
					networkManagers.add(networkmanager);
					channel.pipeline().addLast("packet_handler", networkmanager);
				}
			}).group(NIO_SERVER.getValue()).localAddress(LocalAddress.ANY).bind().syncUninterruptibly();
			this.endpoints.add(channelfuture);
		}

		return channelfuture.channel().localAddress();
	}

	/**
	 * Shuts down all open endpoints (with immediate effect?)
	 */
	public void terminateEndpoints() {
		this.isAlive = false;

		for (ChannelFuture channelfuture : this.endpoints) {
			try {
				channelfuture.channel().close().sync();
			} catch (InterruptedException var4) {
				logger.error("Interrupted whilst closing channel");
			}
		}
	}

	/**
	 * Will try to process the packets received by each NetworkManager, gracefully manage processing failures and cleans
	 * up dead connections
	 */
	public void networkTick() {
		synchronized (this.networkManagers) {
			Iterator<NetworkManager> iterator = this.networkManagers.iterator();

			while (iterator.hasNext()) {
				final NetworkManager networkmanager = iterator.next();

				if (networkmanager.hasNoChannel()) continue;
				if (!networkmanager.isChannelOpen()) {
					iterator.remove();
					networkmanager.checkDisconnected();
					continue;
				}
				try {
					networkmanager.processReceivedPackets();
				} catch (Exception exception) {
					if (networkmanager.isLocalChannel()) {
						CrashReport crashreport = CrashReport.makeCrashReport(exception, "Ticking memory connection");
						CrashReportCategory crashreportcategory = crashreport.makeCategory("Ticking connection");
						crashreportcategory.addCrashSectionCallable("Connection", networkmanager::toString);
						throw new ReportedException(crashreport);
					}

					logger.warn("Failed to handle packet for " + networkmanager.getRemoteAddress(), exception);
					final ChatComponentText chatcomponenttext = new ChatComponentText("Internal server error");
					networkmanager.sendPacket(new S40PacketDisconnect(chatcomponenttext), p_operationComplete_1_ -> networkmanager.closeChannel(chatcomponenttext));
					networkmanager.disableAutoRead();
				}
			}
		}
	}

	public MinecraftServer getServer() {
		return this.mcServer;
	}

}
