package org.android.filesystem.server;

import java.lang.reflect.Constructor;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.android.filesystem.packet.PacketSender;
import org.android.filesystem.packet.RFSPacket;
import org.android.filesystem.task.AccessTask;
import org.android.filesystem.task.CreateTask;
import org.android.filesystem.task.FileOperation;
import org.android.filesystem.task.GetattrTask;
import org.android.filesystem.task.MkdirTask;
import org.android.filesystem.task.MknodTask;
import org.android.filesystem.task.OpenTask;
import org.android.filesystem.task.ReadTask;
import org.android.filesystem.task.ReaddirTask;
import org.android.filesystem.task.ReleaseTask;
import org.android.filesystem.task.RenameTask;
import org.android.filesystem.task.RmdirTask;
import org.android.filesystem.task.Task;
import org.android.filesystem.task.TruncateTask;
import org.android.filesystem.task.UnlinkTask;
import org.android.filesystem.task.UtimensTask;
import org.android.filesystem.task.WriteTask;

public class TaskDispatcher {

	private ExecutorService _executor;
	private Map<Byte, String> _opTaskMap;
	private PacketSender _sender;
	private static TaskDispatcher _instance;
	
	private TaskDispatcher(){
		_opTaskMap = new HashMap<Byte, String>();
		_opTaskMap.put(FileOperation.READDIR.getOp(), ReaddirTask.class.getName());
		_opTaskMap.put(FileOperation.ACCESS.getOp(), AccessTask.class.getName());
		_opTaskMap.put(FileOperation.GETATTR.getOp(), GetattrTask.class.getName());
		_opTaskMap.put(FileOperation.MKDIR.getOp(), MkdirTask.class.getName());
		_opTaskMap.put(FileOperation.MKNOD.getOp(), MknodTask.class.getName());
		_opTaskMap.put(FileOperation.OPEN.getOp(), OpenTask.class.getName());
		_opTaskMap.put(FileOperation.READ.getOp(), ReadTask.class.getName());
		_opTaskMap.put(FileOperation.RELEASE.getOp(), ReleaseTask.class.getName());
		_opTaskMap.put(FileOperation.RENAME.getOp(), RenameTask.class.getName());
		_opTaskMap.put(FileOperation.RMDIR.getOp(), RmdirTask.class.getName());
		_opTaskMap.put(FileOperation.WRITE.getOp(), WriteTask.class.getName());
		_opTaskMap.put(FileOperation.CREATE.getOp(), CreateTask.class.getName());
		_opTaskMap.put(FileOperation.UNLINK.getOp(), UnlinkTask.class.getName());
		_opTaskMap.put(FileOperation.TRUNCATE.getOp(), TruncateTask.class.getName());
		_opTaskMap.put(FileOperation.UTIMENS.getOp(), UtimensTask.class.getName());
		_sender = null;
	}
	
	public PacketSender getSender() {
		return _sender;
	}

	public void setSender(PacketSender sender) {
		this._sender = sender;
	}

	public void startup(){
		_executor = new ThreadPoolExecutor(10, 50, 1, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
	}
	
	public void shutdown(){
		_executor.shutdown();
		_executor = null;
	}
	
	public synchronized static TaskDispatcher getInstance(){
		if(_instance == null){
			_instance = new TaskDispatcher();
		}
		
		return _instance;
	}
	
	public void dispatch(SelectionKey key, RFSPacket packet) {
		if(_executor == null){
			return;
		}
		
		String taskName = _opTaskMap.get(packet.getOp());
		if(taskName != null){
			try{
				@SuppressWarnings("unchecked")
				Constructor<? extends Task> taskConstructor =  (Constructor<? extends Task>) 
					Class.forName(taskName).getConstructor(new Class<?>[] {RFSPacket.class,
					SelectionKey.class, PacketSender.class});
				Task task = taskConstructor.newInstance(packet, key, _sender);
				_executor.execute(task);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

}
