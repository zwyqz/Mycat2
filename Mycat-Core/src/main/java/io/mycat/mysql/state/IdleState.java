package io.mycat.mysql.state;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mycat.backend.MySQLBackendConnection;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.packet.MySQLPacket;
import io.mycat.net2.states.NetworkState;
import io.mycat.net2.states.ReadWaitingState;

/**
 * 空闲状态
 *
 * @author ynfeng
 */
public class IdleState extends AbstractMysqlConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdleState.class);

    public static final IdleState INSTANCE = new IdleState();

    private IdleState() {
    }

    @Override
    protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment)throws IOException {
        LOGGER.debug("Frontend in IdleState");
       // processPacketHeader(mySQLFrontConnection);
        processPacketProcedure(mySQLFrontConnection);
        if(mySQLFrontConnection.getDirectTransferMode() == TransferMode.SHORT_HALF_PACKET) {
            //不用去注册的因为当前还是网络状态机还是读状态
            //mySQLFrontConnection.setNextNetworkState(ReadWaitingState.INSTANCE);
            return false;
        }
        int packetType = mySQLFrontConnection.getCurrentPacketType();        
    	boolean returnflag = false;
        switch (packetType) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Frontend receive a COM_QUERY in IdleState");
                mySQLFrontConnection.setNextState(ComQueryState.INSTANCE);
                returnflag = true;
                break;
            case MySQLPacket.COM_QUIT:
            	LOGGER.debug("Frontend receive a COM_QUIT in IdleState");
            	mySQLFrontConnection.setNextState(CloseState.INSTANCE);
            	returnflag = true;
            default:
                break;
        }
        return returnflag;
    }

    @Override
    protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment) throws IOException {
        LOGGER.debug("Backend in IdleState");
        boolean returnflag = false;
        /*processPacketHeader(mySQLBackendConnection); 
        switch (mySQLBackendConnection.getCurrentPacketType()) {
            case MySQLPacket.COM_QUERY:
                LOGGER.debug("Backend receive a COM_QUERY in IdleState");
                mySQLBackendConnection.setNextState(ComQueryState.INSTANCE);
                returnflag = true;
                break;
            default:
                break;
        }*/
        return returnflag;
    }
}
