package io.mycat.mysql.state;

import java.io.IOException;

import io.mycat.SQLEngineCtx;
import io.mycat.backend.MySQLBackendConnection;
import io.mycat.backend.MySQLBackendConnectionFactory;
import io.mycat.backend.MySQLDataSource;
import io.mycat.backend.MySQLReplicatSet;
import io.mycat.beans.DNBean;
import io.mycat.buffer.MycatByteBuffer;
import io.mycat.engine.dataChannel.TransferMode;
import io.mycat.front.MySQLFrontConnection;
import io.mycat.mysql.MySQLConnection;
import io.mycat.mysql.packet.MySQLPacket;

/**
 * 状态基类，封装前后端状态处理分发
 *
 * @author ynfeng
 */
public abstract class AbstractMysqlConnectionState implements MysqlConnectionState {
    @Override
    public boolean handle(MySQLConnection mySQLConnection, Object attachment) throws IOException{
        if (mySQLConnection instanceof MySQLBackendConnection) {
            return backendHandle((MySQLBackendConnection) mySQLConnection, attachment);
        } else if (mySQLConnection instanceof MySQLFrontConnection) {
        	return frontendHandle((MySQLFrontConnection) mySQLConnection, attachment);
        } else {
            throw new IllegalArgumentException("No such type of MySQLConnection:" + mySQLConnection.getClass());
        }
    }

    /**
     * 判断报文头是否为全包
     * @param conn
     * @return true 报文头解析成功，false 报文头没有解析出来
     * @throws IOException
     */
    protected boolean validateCompletePacket(MySQLConnection conn)throws IOException{
        final MycatByteBuffer buffer = conn.getDataBuffer();
        
        int offset = -1;
        int limit = buffer.writeIndex();
        if(conn.isPassthrough()) {
           offset = buffer.writeLimit();
        } else {
            offset = buffer.readIndex();
        }
        
        if (!MySQLConnection.validateHeader(offset, limit)) {
           return false;
        }
        int length = MySQLConnection.getPacketLength(buffer, offset);
        if( length > limit) {
            return false;
        }
        return true;
    }
    
    /*判断load data local infile 传输数据的时候的结束包 （包为一个长度为4的包）通过判断packet的length为4*/
    protected boolean validateIsLoadEndPacket(MySQLConnection conn)throws IOException {
        final MycatByteBuffer buffer = conn.getDataBuffer();
        int offset = buffer.writeLimit();
        int limit = buffer.writeIndex();
        if(offset + MySQLConnection.msyql_packetHeaderSize == limit) {
            int length = MySQLConnection.getPacketLength(buffer, offset);
            if(length == MySQLConnection.msyql_packetHeaderSize) {
                conn.setCanDrive(true);
                conn.setDealFinish(true);
                conn.setDirectTransferMode(TransferMode.COMPLETE_PACKET);
                conn.setRemainLength(0);
                buffer.writeLimit(limit);
                conn.setDataStartPos(offset);
                conn.setDataEndPos(limit);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理报文的中间层
     * */
    public void processPacketProcedure(MySQLConnection conn) throws IOException {
       // boolean candrive = false;
        final MycatByteBuffer buffer = conn.getDataBuffer();
        int offset = buffer.writeLimit();
        int limit = buffer.writeIndex();
        byte packetType = conn.getCurrentPacketType();
        int remainLength = conn.getRemainLength();  //当前包还需要接受remainLength长度的数据
        
        //还未透传过的，可以获取packet 的相关信息
        if(remainLength == 0) {
            //接受新的包
            if (!MySQLConnection.validateHeader(offset, limit)) {
                conn.setCanDrive(false); //是否可驱动状态机
                conn.setDealFinish(true); //当前的buffer是否处理完毕
                conn.setDirectTransferMode(TransferMode.SHORT_HALF_PACKET);  //短半包透传
               return;
            }
            int length = MySQLConnection.getPacketLength(buffer, offset); //当前packet的长度

            packetType = buffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize); //获取packet的类型
            remainLength = length; //还需要接受的包的长度
            conn.setRemainLength(length);
            conn.setLength(length); //当前包的长度
            conn.setCurrentPacketType(packetType); //当前包的类型
        }
        //当前包全部接受完毕
        if(offset + remainLength <= limit) {
            conn.setCanDrive(true);   //packet已经接受完毕 可以驱动状态机进入下一个状态
            conn.setDataStartPos(offset); //当前包buffer可以处理的开始为止
            conn.setDataEndPos(offset + remainLength); //当前包buffer可以处理的结束为止
            conn.setDealFinish(offset + remainLength >= limit);//当前buffer是否处理完毕，然后可以进行buffer的写出
            if(conn.getLength() == remainLength) {
                conn.setCurrentPacketLength(offset + conn.getLength()); //设置全包的packet的开始位置
                conn.setCurrentPacketStartPos(offset); //设置全包的packet的结束为止
                conn.setDirectTransferMode(TransferMode.COMPLETE_PACKET); //当前包是全包 所有的packet的数据在一个buffer中
            } else {
                conn.setDirectTransferMode(TransferMode.LONG_COMPLETE_PACKET); //之前已经透传过若干次的长半包，当前的数据接受完，当前的包就全部接受完
            }
            conn.setRemainLength(0); //剩余接受长度为零
            buffer.writeLimit(offset + remainLength); //修改可透传的位置
            return ;
        } else if(!MySQLPacket.receiveCompletePackete(packetType)){
            //可以透传半包的数据
            conn.setCanDrive(false);
            conn.setDataStartPos(offset);
            conn.setDataEndPos(limit);
            conn.setDealFinish(offset + remainLength >= limit); //这边应该为true
            conn.setRemainLength(offset + remainLength - limit);
            buffer.writeLimit(limit); //修改可透传的位置
            conn.setDirectTransferMode(TransferMode.LONG_HALF_PACKET);
            return ;
        }
    }
    
    
    /**
     * 处理报文头
     * @param conn
     * @return true 报文头解析成功，false 报文头没有解析出来
     * @throws IOException
     */
 /*   protected void processPacketHeader(MySQLConnection conn)throws IOException{
//    	conn.clearCurrentPacket();
    	final MycatByteBuffer buffer = conn.getDataBuffer();
        int offset = buffer.writeLimit();
    	int limit = buffer.writeIndex();
    	int currentStartPos = conn.getCurrentPacketStartPos();
    	
    	if(currentStartPos < 0){  //完成一次透传后,如果有半包,并且半包已经透传出去的情况.offset 会小于0
    		int currentPacketLength = conn.getCurrentPacketLength();
 			if(currentPacketLength < limit){
 				offset = conn.getCurrentPacketLength() ;  //当前半包剩余部分不做解析,直接开始解析下一个包
 				buffer.writeLimit(offset);
 				//这边是否要驱动一下状态机
 			}else{       // packet 太大,剩余部分仍然超过了缓冲区大小
 				conn.setDirectTransferMode(TransferMode.LONG_HALF_PACKET); 
 				return;
 			}
 		}

    	if (!MySQLConnection.validateHeader(offset, limit)) {
    		conn.setDirectTransferMode(TransferMode.SHORT_HALF_PACKET);  //半包透传
     	   return;
        }

        int length = MySQLConnection.getPacketLength(buffer, offset);
        final byte packetType = buffer.getByte(offset + MySQLConnection.msyql_packetHeaderSize);
        final int pkgStartPos = offset;

        if(length + offset > limit){
        	conn.setDirectTransferMode(TransferMode.LONG_HALF_PACKET);  //半包透传
        }else{
        	conn.setDirectTransferMode(TransferMode.COMPLETE_PACKET);  //完整的包
        }
        
        offset += length;
        conn.setCurrentPacketLength(offset);
        conn.setCurrentPacketStartPos(pkgStartPos);
        conn.setCurrentPacketType(packetType);
    }*/
    
    protected void processPacket(MySQLConnection conn)throws IOException{ 
        
    }
    protected boolean setNextStatue(MySQLFrontConnection mySQLFrontConnection, Object attachment)throws IOException {
        
        return true;
    }
    
    protected MySQLBackendConnection getBackendFrontConnection(MySQLFrontConnection mySQLFrontConnection) throws IOException {
//      // 直接透传（默认）获取连接池
//      MySQLBackendConnection existCon = null;
//      UserSession session = mySQLFrontConnection.getSession();
//      ArrayList<MySQLBackendConnection> allBackCons = session.getBackendCons();
//      if (!allBackCons.isEmpty()) {
//          existCon = allBackCons.get(0);
//      }
//      if (existCon == null || existCon.isClosed()) {
//          if (existCon != null) {
//              session.removeBackCon(existCon);
//          }
          final DNBean dnBean = mySQLFrontConnection.getMycatSchema().getDefaultDN();
          final String replica = dnBean.getMysqlReplica();
          final SQLEngineCtx ctx = SQLEngineCtx.INSTANCE();
          final MySQLReplicatSet repSet = ctx.getMySQLReplicatSet(replica);
          final MySQLDataSource datas = repSet.getCurWriteDH();
//
//          /**
//           * 如果该sql对应后端db，没有连接池，则创建连接池部分
//           */
//      MySQLBackendConnection existCon = datas.getConnection(mySQLFrontConnection.getReactor(), dnBean.getDatabase(), true, mySQLFrontConnection, null);
//      }
      MySQLBackendConnection con = new MySQLBackendConnectionFactory().make(datas, mySQLFrontConnection.getReactor(), dnBean.getDatabase(), mySQLFrontConnection, null);
              mySQLFrontConnection.setBackendConnection(con);
      return con;
  }

    abstract protected boolean frontendHandle(MySQLFrontConnection mySQLFrontConnection, Object attachment)throws IOException;

    abstract protected boolean backendHandle(MySQLBackendConnection mySQLBackendConnection, Object attachment)throws IOException;
}
