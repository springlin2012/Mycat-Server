package org.opencloudb.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencloudb.util.TimeUtil;

public class NIOSocketWR extends SocketWR {
	private SelectionKey processKey;
	private static final int OP_NOT_READ = ~SelectionKey.OP_READ;
	private static final int OP_NOT_WRITE = ~SelectionKey.OP_WRITE;
	private final AbstractConnection con;
	private final SocketChannel channel;
	private final AtomicBoolean writing = new AtomicBoolean(false);

	public NIOSocketWR(AbstractConnection con) {
		this.con = con;
		this.channel = (SocketChannel) con.channel;
	}

	public void register(Selector selector) throws IOException {
		try {
			processKey = channel.register(selector, SelectionKey.OP_READ, con);
		} finally {
			if (con.isClosed.get()) {
				clearSelectionKey();
			}
		}
	}

	public void doNextWriteCheck() {
        //检查是否正在写,看CAS更新writing值是否成功
		if (!writing.compareAndSet(false, true)) {
			return;
		}

		try {
            //利用缓存队列和写缓冲记录保证写的可靠性，返回true则为全部写入成功
			boolean noMoreData = write0();
            //因为只有一个线程可以成功CAS更新writing值，所以这里不用再CAS
			writing.set(false);
            //如果全部写入成功而且写入队列为空（有可能在写入过程中又有新的Bytebuffer加入到队列），则取消注册写事件
            //否则，继续注册写事件
			if (noMoreData && con.writeQueue.isEmpty()) {
				if ((processKey.isValid() && (processKey.interestOps() & SelectionKey.OP_WRITE) != 0)) {
					disableWrite();
				}

			} else {

				if ((processKey.isValid() && (processKey.interestOps() & SelectionKey.OP_WRITE) == 0)) {
					enableWrite(false);
				}
			}

		} catch (IOException e) {
			if (AbstractConnection.LOGGER.isDebugEnabled()) {
				AbstractConnection.LOGGER.debug("caught err:", e);
			}
			con.close("err:" + e);
		}

	}

	private boolean write0() throws IOException {

		int written = 0;
		ByteBuffer buffer = con.writeBuffer;
		if (buffer != null) {
            //只要写缓冲记录中还有数据就不停写入，但如果写入字节为0，证明网络繁忙，则退出
			while (buffer.hasRemaining()) {
				written = channel.write(buffer);
				if (written > 0) {
					con.netOutBytes += written;
					con.processor.addNetOutBytes(written);
					con.lastWriteTime = TimeUtil.currentTimeMillis();
				} else {
					break;
				}
			}

            //如果写缓冲中还有数据证明网络繁忙，计数并退出，否则清空缓冲
			if (buffer.hasRemaining()) {
				con.writeAttempts++;
				return false;
			} else {
				con.writeBuffer = null;
				con.recycle(buffer);
			}
		}

        //读取缓存队列并写channel
		while ((buffer = con.writeQueue.poll()) != null) {
			if (buffer.limit() == 0) {
				con.recycle(buffer);
				con.close("quit send");
				return true;
			}

			buffer.flip();
            //如果写缓冲中还有数据证明网络繁忙，计数，记录下这次未写完的数据到写缓冲记录并退出，否则回收缓冲
			while (buffer.hasRemaining()) {
				written = channel.write(buffer);
				if (written > 0) {
					con.lastWriteTime = TimeUtil.currentTimeMillis();
					con.netOutBytes += written;
					con.processor.addNetOutBytes(written);
					con.lastWriteTime = TimeUtil.currentTimeMillis();
				} else {
					break;
				}
			}
			if (buffer.hasRemaining()) {
				con.writeBuffer = buffer;
				con.writeAttempts++;
				return false;
			} else {
				con.recycle(buffer);
			}
		}
		return true;
	}

	private void disableWrite() {
		try {
			SelectionKey key = this.processKey;
			key.interestOps(key.interestOps() & OP_NOT_WRITE);
		} catch (Exception e) {
			AbstractConnection.LOGGER.warn("can't disable write " + e + " con "
					+ con);
		}

	}

	private void enableWrite(boolean wakeup) {
		boolean needWakeup = false;
		try {
			SelectionKey key = this.processKey;
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
			needWakeup = true;
		} catch (Exception e) {
			AbstractConnection.LOGGER.warn("can't enable write " + e);

		}
		if (needWakeup && wakeup) {
			processKey.selector().wakeup();
		}
	}

	public void disableRead() {

		SelectionKey key = this.processKey;
		key.interestOps(key.interestOps() & OP_NOT_READ);
	}

	public void enableRead() {

		boolean needWakeup = false;
		try {
			SelectionKey key = this.processKey;
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			needWakeup = true;
		} catch (Exception e) {
			AbstractConnection.LOGGER.warn("enable read fail " + e);
		}
		if (needWakeup) {
			processKey.selector().wakeup();
		}
	}

	private void clearSelectionKey() {
		try {
			SelectionKey key = this.processKey;
			if (key != null && key.isValid()) {
				key.attach(null);
				key.cancel();
			}
		} catch (Exception e) {
			AbstractConnection.LOGGER.warn("clear selector keys err:" + e);
		}
	}

	@Override
	public void asynRead() throws IOException {
		ByteBuffer theBuffer = con.readBuffer;
		if (theBuffer == null) {
			theBuffer = con.processor.getBufferPool().allocate();
			con.readBuffer = theBuffer;
		}
		int got = channel.read(theBuffer);
		con.onReadData(got);

	}

}
