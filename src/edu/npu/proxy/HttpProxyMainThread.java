package edu.npu.proxy;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import edu.npu.utils.URL;

public class HttpProxyMainThread extends Thread {
	static public int CONNECT_RETRIES = 5; // 尝试与目标主机连接次数
	static public int CONNECT_PAUSE = 5; // 每次建立连接的间隔时间
	static public int TIMEOUT = 50; // 每次尝试连接的最大时间
	
	protected Socket csocket;// 与客户端连接的Socket

	public HttpProxyMainThread(Socket cs) {
		this.csocket = cs;
	}

	public void run() {
		String firstLine = ""; // http请求头第一行
		String urlStr = ""; // 请求的url
		Socket ssocket = null;//与目标服务器连接的socket
		// cis为客户端输入流，sis为目标主机输入流
		InputStream cis = null, sis = null;
		// cos为客户端输出流，sos为目标主机输出流
		OutputStream cos = null, sos = null;
		try {
			csocket.setSoTimeout(TIMEOUT);
			cis = csocket.getInputStream();
			cos = csocket.getOutputStream();
			while (true) {
				int c = cis.read();
				if (c == -1)
					break; // -1为结尾标志
				if (c == '\r' || c == '\n')
					break;// 读入第一行数据,从中获取目标主机url
				firstLine = firstLine + (char) c;
			}
			urlStr = extractUrl(firstLine);
			System.out.println(urlStr);
			URL url = new URL(urlStr);//将url封装成对象，完成一系列转换工作,并在getIP中实现了dns缓存
			firstLine = firstLine.replace(url.getScheme()+"://"+url.getHost(), "");//这一步很重要，把请求头的绝对路径换成相对路径
			int retry = CONNECT_RETRIES;
			while (retry-- != 0) {
				try {
					ssocket = new Socket(url.getIP(), url.getPort()); // 尝试建立与目标主机的连接
					System.out.println("+++++successfully connect to ("+url.getIP()+":"+url.getPort()+")(host:"+url.getHost()+")+++++,get resource("+url.getResource()+")");
					break;
				} catch (Exception e) {
					System.out.println("-----fail connect to ("+url.getIP()+":"+url.getPort()+")(host:"+url.getHost()+")-----");
				}
				// 等待
				Thread.sleep(CONNECT_PAUSE);
			}
			if (ssocket != null) {
				ssocket.setSoTimeout(TIMEOUT);
				sis = ssocket.getInputStream();
				sos = ssocket.getOutputStream();
				sos.write(firstLine.getBytes()); // 将请求头写入
				pipe(cis, sis, sos, cos); // 建立通信管道
			}
		} catch (Exception e) {
			//e.printStackTrace();
		} finally {
			try {
				csocket.close();
				cis.close();
				cos.close();
			} catch (Exception e1) {
			}
			try {
				ssocket.close();
				sis.close();
				sos.close();
			} catch (Exception e2) {
			}
		}
	}
	/**
	 * 从http请求头的第一行提取请求的url
	 * @param firstLine http请求头第一行
	 * @return url
	 */
	public String extractUrl(String firstLine) {
		String[] tokens = firstLine.split(" ");
		String URL = "";
		for (int index = 0; index < tokens.length; index++) {
			if (tokens[index].startsWith("http://")) {
				URL = tokens[index];
				break;
			}
		}
		return URL;
	}

	/**
	 * 为客户机与目标服务器建立通信管道
	 * @param cis 客户端输入流
	 * @param sis 目标主机输入流
	 * @param sos 目标主机输出流
	 * @param cos 客户端输出流
	 */
	public void pipe(InputStream cis, InputStream sis, OutputStream sos, OutputStream cos) {
		Client2ServerThread c2s = new Client2ServerThread(cis, sos);
		Server2ClientThread s2c = new Server2ClientThread(sis, cos);
		c2s.start();
		s2c.start();
		try {
			c2s.join();
			s2c.join();
		} catch (InterruptedException e1) {
			
		}
	}
}
