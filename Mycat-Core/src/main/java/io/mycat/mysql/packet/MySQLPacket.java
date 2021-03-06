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
package io.mycat.mysql.packet;

import java.util.HashMap;
import java.util.Map;

import io.mycat.buffer.MycatByteBuffer;

/**
 * 
 * @author wuzhihui
 *
 */
public abstract class MySQLPacket {
    /*
    protected static final Map<Byte,Boolean> completePacketMap = new HashMap<Byte,Boolean>();
    {
        completePacketMap.put(OK_PACKET, true);
        completePacketMap.put(EOF_PACKET, true);
        completePacketMap.put(ERROR_PACKET, true);
        completePacketMap.put(AUTH_PACKET, true);
        completePacketMap.put(ROW_EOF_PACKET, true);
    }
     */
    public static boolean receiveCompletePackete(byte packetType) {
        
        if(packetType == OK_PACKET || 
                packetType == EOF_PACKET ||
                packetType == ERROR_PACKET ||
                packetType == AUTH_PACKET ||
                packetType == ROW_EOF_PACKET ) {
            return true;
        }
        return false;
    }
    public static int packetHeaderSize = 4;
    
    // 后端报文类型
    public static final byte REQUEST_FILE_FIELD_COUNT = (byte) 251;
    public static final byte OK_PACKET = 0;
    public static final byte ERROR_PACKET = (byte) 0xFF;
    public static final byte EOF_PACKET = (byte) 0xFE;
    public static final byte FIELD_EOF_PACKET = (byte) 0xFE;
    public static final byte ROW_EOF_PACKET = (byte) 0xFE;
    public static final byte AUTH_PACKET = 1;
    public static final byte QUIT_PACKET = 2;

    // 前端报文类型
    /**
     * none, this is an internal thread state
     */
    public static final byte COM_SLEEP = 0;

    /**
     * mysql_close
     */
    public static final byte COM_QUIT = 1;

    public static final int COM_QUIT_PACKET_LENGTH = 1;

    /**
     * mysql_select_db
     */
    public static final byte COM_INIT_DB = 2;

    /**
     * mysql_real_query
     */
    public static final byte COM_QUERY = 3;

    /**
     * mysql_list_fields
     */
    public static final byte COM_FIELD_LIST = 4;

    /**
     * mysql_create_db (deprecated)
     */
    public static final byte COM_CREATE_DB = 5;

    /**
     * mysql_drop_db (deprecated)
     */
    public static final byte COM_DROP_DB = 6;

    /**
     * mysql_refresh
     */
    public static final byte COM_REFRESH = 7;

    /**
     * mysql_shutdown
     */
    public static final byte COM_SHUTDOWN = 8;

    /**
     * mysql_stat
     */
    public static final byte COM_STATISTICS = 9;

    /**
     * mysql_list_processes
     */
    public static final byte COM_PROCESS_INFO = 10;

    /**
     * none, this is an internal thread state
     */
    public static final byte COM_CONNECT = 11;

    /**
     * mysql_kill
     */
    public static final byte COM_PROCESS_KILL = 12;

    /**
     * mysql_dump_debug_info
     */
    public static final byte COM_DEBUG = 13;

    /**
     * mysql_ping
     */
    public static final byte COM_PING = 14;

    /**
     * none, this is an internal thread state
     */
    public static final byte COM_TIME = 15;

    /**
     * none, this is an internal thread state
     */
    public static final byte COM_DELAYED_INSERT = 16;

    /**
     * mysql_change_user
     */
    public static final byte COM_CHANGE_USER = 17;

    /**
     * used by slave server mysqlbinlog
     */
    public static final byte COM_BINLOG_DUMP = 18;

    /**
     * used by slave server to get master table
     */
    public static final byte COM_TABLE_DUMP = 19;

    /**
     * used by slave to log connection to master
     */
    public static final byte COM_CONNECT_OUT = 20;

    /**
     * used by slave to register to master
     */
    public static final byte COM_REGISTER_SLAVE = 21;

    /**
     * mysql_stmt_prepare
     */
    public static final byte COM_STMT_PREPARE = 22;

    /**
     * mysql_stmt_execute
     */
    public static final byte COM_STMT_EXECUTE = 23;

    /**
     * mysql_stmt_send_long_data
     */
    public static final byte COM_STMT_SEND_LONG_DATA = 24;

    /**
     * mysql_stmt_close
     */
    public static final byte COM_STMT_CLOSE = 25;

    /**
     * mysql_stmt_reset
     */
    public static final byte COM_STMT_RESET = 26;

    /**
     * mysql_set_server_option
     */
    public static final byte COM_SET_OPTION = 27;

    /**
     * mysql_stmt_fetch
     */
    public static final byte COM_STMT_FETCH = 28;
    
    /**
     * mysql_stmt_fetch
     */
    public static final byte COM_DAEMON     = 29;
    
    /**
     * mysql_stmt_fetch
     */
    public static final byte COM_BINLOG_DUMP_GTID  = 30;
    
    /**
     * mysql_stmt_fetch
     */
    public static final byte COM_RESET_CONNECTION  = 31;

    /**
     * Mycat heartbeat
     */
    public static final byte COM_HEARTBEAT = 64;
    
    
    public int packetLength;
    public byte packetId;

      /**
     * 计算数据包大小，不包含包头长度。
     */
    public abstract int calcPacketSize();

    /**
     * 取得数据包信息
     */
    protected abstract String getPacketInfo();

    @Override
    public String toString() {
        return new StringBuilder().append(getPacketInfo()).append("{length=")
                .append(packetLength).append(",id=").append(packetId)
                .append('}').toString();
    }

    public void write(MycatByteBuffer buffer, int pkgSize) {
		
		
	}
}
