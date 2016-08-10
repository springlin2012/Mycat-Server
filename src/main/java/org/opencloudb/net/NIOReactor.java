/*
 * Copyright (c) 2013, OpenCloudDB/MyCAT and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software;Designed and Developed mainly by many Chinese 
 * opensource volunteers. you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Any questions about this component can be directed to it's project Web address 
 * https://code.google.com/p/opencloudb/.
 *
 */
package org.opencloudb.net;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

/**
 * 网络事件反应器
 * 
 * @author mycat
 * 
 * Catch exceptions such as OOM so that the reactor can keep running for response client!
 * @author Uncle-pan
 * @since 2016-03-30
 */
public final class NIOReactor {
	private static final Logger LOGGER = Logger.getLogger(NIOReactor.class);
	private final String name;
	private final RW reactorR;

	public NIOReactor(String name) throws IOException {
		this.name = name;
		this.reactorR = new RW();
	}

	final void startup() {
		new Thread(reactorR, name + "-RW").start();
	}

	final void postRegister(AbstractConnection c) {
        // 放入RW线程的注册队列
		reactorR.registerQueue.offer(c);
		reactorR.selector.wakeup();
	}

	final Queue<AbstractConnection> getRegisterQueue() {
		return reactorR.registerQueue;
	}

	final long getReactCount() {
		return reactorR.reactCount;
	}

    // RW线程注册队列
	private final class RW implements Runnable {
		private final Selector selector;
		private final ConcurrentLinkedQueue<AbstractConnection> registerQueue;
		private long reactCount;

		private RW() throws IOException {
			this.selector = Selector.open();
			this.registerQueue = new ConcurrentLinkedQueue<AbstractConnection>();
		}

		@Override
		public void run() {
			final Selector selector = this.selector;
			Set<SelectionKey> keys = null;
			for (;;) {
				++reactCount;
				try {
					selector.select(500L);
                    //从注册队列中取出AbstractConnection之后注册读事件
                    //之后做一些列操作，请参考下面注释
					register(selector);
					keys = selector.selectedKeys();
					for (SelectionKey key : keys) {
						AbstractConnection con = null;
						try {
							Object att = key.attachment();
							if (att != null) {
								con = (AbstractConnection) att;
								if (key.isValid() && key.isReadable()) {
									try {
                                        //异步读取数据并处理数据
										con.asynRead();
									} catch (IOException e) {
                                        con.close("program err:" + e.toString());
										continue;
									} catch (Exception e) {
										LOGGER.warn("caught err: ", e);
										con.close("program err:" + e.toString());
										continue;
									}
								}

								if (key.isValid() && key.isWritable()) {
                                    //异步写数据
									con.doNextWriteCheck();
								}
							} else {
								key.cancel();
							}
                        } catch (CancelledKeyException e) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(con + " socket key canceled");
                            }
                        } catch (Exception e) {
                            LOGGER.warn(con + " " + e);
                        } catch (final Throwable e){
                        	// Catch exceptions such as OOM and close connection if exists
                        	//so that the reactor can keep running!
                        	// @author Uncle-pan
                        	// @since 2016-03-30
                        	if(con != null){
                        		con.close("Bad: "+e);
                        	}
                        	LOGGER.error("caught err: ", e);
                        	continue;
                        }
					}
				} catch (Exception e) {
					LOGGER.warn(name, e);
				} catch (final Throwable e){
					// Catch exceptions such as OOM so that the reactor can keep running!
                	// @author Uncle-pan
                	// @since 2016-03-30
					LOGGER.error("caught err: ", e);
				} finally {
					if (keys != null) {
						keys.clear();
					}
				}
			}
		}

		private void register(Selector selector) {
			AbstractConnection c = null;
			if (registerQueue.isEmpty()) {
				return;
			}
			while ((c = registerQueue.poll()) != null) {
				try {
                    //注册读事件
					((NIOSocketWR) c.getSocketWR()).register(selector);
                    //连接注册，对于FrontendConnection是发送HandshakePacket并异步读取响应
                    //响应为AuthPacket，读取其中的信息，验证用户名密码等信息，如果符合条件
                    //则发送OkPacket
					c.register();
				} catch (Exception e) {
					c.close("register err" + e.toString());
				}
			}
		}

	}

}