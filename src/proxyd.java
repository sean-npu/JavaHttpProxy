import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import edu.npu.proxy.HttpProxyMainThread;

public class proxyd {

	public static void main(String[] args) {
		
		if (args.length > 0 && args[0].equals("-port")) {
			int port = Integer.parseInt(args[1]);
			ServerSocket serverSocket = null;
			try {
				serverSocket = new ServerSocket(port);
				System.out.println("在端口" + port + "启动代理服务器\n");
				while (true) {
					Socket socket = null;
					try {
						socket = serverSocket.accept();
						new HttpProxyMainThread(socket).start();//有一个请求就启动一个线程
					} catch (Exception e) {
						System.out.println("线程启动失败");
					}
				}
			} catch (IOException e1) {
				System.out.println("proxyd启动失败\n");
			}finally{
				try {
					serverSocket.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
		}else{
			System.out.println("参数错误");
		}
	}
	
}
