import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Demo {
	public static int countner = 0;
	public static Map<String, SocketServerThread> manager = new HashMap<>();
	public static Map<String,String> auth_uidb = new HashMap<>();

	public static void main(String[] args) {
		try {
			
			ServerSocket ss = new ServerSocket(19001);
			System.out.println(ss);
			while (countner < 30) {
				System.out.println("....");
				Socket s = ss.accept();
				SocketServerThread sst = new SocketServerThread(s, countner + "");
				Thread aa = new Thread(sst);
				aa.start();
				manager.put(countner + "", sst);
				Demo.add();
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	public static void add() {
		countner++;
	}

	public static void remove(String name) {
		if(name!=null) {
			if(name.contains("guest")) {
				name = name.replaceAll("guest", "");				
			}else {
				name = auth_uidb.get(name);
			}
			manager.remove(name);
		}
	}

	public static int getCountner() {
		return manager.size();
	}

	public static void noticeAll(String msg) {
		for (String key : manager.keySet()) {
			manager.get(key).notice(msg);
		}
	}
	public static void noticeSb(String to,String from ,String msg) {
		String uid = auth_uidb.get(to);
		if(uid!=null) {
			msg ="["+from +" tell "+to+"]: "+msg;
			manager.get(uid).notice(msg);
		}
	}
	
	public static void onLinePeople() {
		for(String str:manager.keySet()) {
			//TODO INS
			manager.get(str).notice(str);
		}
	}
	
	public static boolean checkLogin(String loginInfo) {
		loginInfo = loginInfo.substring(1);
		List<String> allUser = Txt.getAllLines();
		if (allUser.contains(loginInfo)) {
			return true;
		}
		return false;
	}
	public static boolean addSession(String auth,String uid) {
		auth_uidb.put(auth, uid);
		return true;
	}

	public static String registUser(String loginInfo) {
		List<String> allUser = Txt.getAllLines();
		if (allUser.contains(loginInfo)) {
			return "this user has regist! \n";
		}
		Txt.writeTxt(loginInfo);
		return "regist sucsess !\n";
	}
}

class SocketServerThread implements Runnable {
	private Socket socket;
	private String name;
	private boolean loginStatus = false;
	private String loginInfo = "";
	private int registStatus = 0;
	private String resgitInfo = "";

	InputStream in = null;
	OutputStream out = null;

	public SocketServerThread(Socket socket, String name) {
		System.out.println(socket);
		this.socket = socket;
		this.name = "guest" + name;
		
	}

	public void print(String str) {
		String msg = this.name + "-->" + str;
		System.out.println(msg);
	}

	public void notice(String msg) {

		try {
			out.write(msg.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public void run() {

		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
			if (in.available() == 0) {
				welcome(in, out);
			}
			Integer sourcePort = socket.getPort();
			SocketAddress addree = socket.getRemoteSocketAddress();
			int maxLen = 2048;
			byte[] contextBytes = new byte[maxLen];
			int realLen;
			StringBuffer message = new StringBuffer();
			int cc = 0;
			BIORead: while (true) {
				print("running---");
				cc++;
				if (cc > 2) {
					Demo.remove(this.name);
					print("断开连接");
					break BIORead;
				}
				try {

					while ((realLen = in.read(contextBytes, 0, maxLen)) != -1) {
						String line = new String(contextBytes, 0, realLen);
						message.append(line);
						print("read:" + line);
						if (message.indexOf("User-Agent") > -1) {
							PrintWriter pout = new PrintWriter(out, true);
							pout.println("HTTP/1.1 200 OK");// 返回应答消息,并结束应答
							pout.println("Content-Type:text/html;charset=utf-8");
							pout.println();// 根据 HTTP 协议, 空行将结束头信息
							pout.println("<title> Hello Telnet Server</title>");
							pout.println("<meta charset=\"utf-8\">");
							pout.println("<h1> Hello Telnet Server</h1>");
							pout.println("hello,this is a Java Telnet server demo app.<br>");
							pout.println("pleas use command telnet blog.xuhaobo.cn");
							Demo.remove(this.name);
							break BIORead;
						}
						/*
						 * 我们假设读取到“over”关键字， 表示客户端的所有信息在经过若干次传送后，完成
						 */
						if (line.trim().equals("over")) {
							Demo.remove(this.name);
							break BIORead;
						}
						if(line.trim().equals("who")) {
							out.write("online people is \n".getBytes());
							Demo.onLinePeople();
							continue;
						}
						if (!this.loginStatus) {
							if (line.trim().equals("new")) {
								this.registStatus = 1;
								out.write("input Name:".getBytes());
							}
							if (registStatus > 0) {
								this.resgitInfo = this.resgitInfo +"^"+ line.trim();
								this.registStatus++;
								if (this.registStatus == 4) {
									resgitInfo =  resgitInfo.substring(5);
									String registUser = Demo.registUser(resgitInfo);
									if (registUser.startsWith("this")) {
										this.registStatus = 1;
										this.resgitInfo = "";
										out.write(registUser.getBytes());
										out.write("input Name:".getBytes());
									} else {
										this.registStatus  = 0;
										out.write(registUser.getBytes());										
										out.write("Name:".getBytes());
									}
								}else if(this.registStatus==3) {
									out.write("input PassWord:".getBytes());
								}
							} else {
								int step = 0;
								if (this.loginInfo.equals("")) {
									step = 1;
								} else {
									step = 2;
								}
								this.loginInfo = this.loginInfo + "^"+line.trim();
								boolean check = Demo.checkLogin(this.loginInfo);
								if (check) {
									this.loginStatus = true;
									String[] arrs = this.loginInfo.split("\\^");
									String userName = arrs[2];
									Demo.addSession(userName, this.name.substring(5));
									this.name = userName;
									loginWelcome();
								} else {
									if (step == 1) {
										out.write("PassWord:".getBytes());
									} else {
										out.write("Name:".getBytes());
										this.loginInfo = "";
									}
								}
							}
						}
						if (this.loginStatus) {
							if (line.startsWith(":")) {
								Demo.noticeAll(this.name + line);
							}
							if(line.startsWith("2")&&line.contains(":")) {
								String [] arrs = line.trim().split(":");
								String to  = arrs[0].substring(1);
								String msg = arrs[1];
								Demo.noticeSb(to, this.name, msg);
							}
							//下面开始发送信息
							String respone = "res:now there [" + Demo.getCountner() + "] user in room\n\r";
							out.write(respone.getBytes());
						}
						// 下面打印信息
						print(addree + "：" + sourcePort + "-->" + message);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// 关闭
			out.close();
			in.close();
			this.socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loginWelcome() {
		try {
			out.write("Connected to cubanbar.hochstenbach.net \n".getBytes());
			out.write("--------------------------------------------------------------------------- \n".getBytes());
			out.write("                             o  o o \n".getBytes());
			out.write("                            o o oooo  o \n".getBytes());
			out.write("                            o|ooooo|\\ \n".getBytes());
			out.write("                            o|l l l| | \n".getBytes());
			out.write("                            o|U U U| | \n".getBytes());
			out.write("                        ooooo|_____|/ \n".getBytes());
			out.write("                   Hello my good friends  welcome in the-CuBan-BaR. \n".getBytes());
			out.write(" Have  a drink,  and if  you play horn or piano ..bass .. or if you \n".getBytes());
			out.write(" sing, whatever, please  give  it  a try ..join the Jamsession ..we \n".getBytes());
			out.write(" tried to amplify the vibes..forgive Us if the sound is a bit Wacky. \n".getBytes());
			out.write(" {By the way, peanuts 4 free} \n".getBytes());
			out.write("\n".getBytes());
			out.write("--------------------------------------------------------------------------- \n".getBytes());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void welcome(InputStream in, OutputStream out) {
		try {
			out.write("\033[32;5m   ########88888888888#8888888           8888888 88888888888#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88      _dP\"#####88           88#####\"9b_      88#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88      \"9b######88           88######dP\"      88#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88  _      9b####88           88####dP\"     _  88#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88_d8b_     \"9b##88           88##dP\"     _d8b_88#########  \033[0m  \n"
					.getBytes());
			out.write(
					"\033[32;5m   #########8P\"#\"9b_     \"9#88           8#dP\"     _dP\"#\"98##########  \033[0m  \n"
							.getBytes());
			out.write("\033[32;5m   ######_d8######9b_     \"9P            9P\"     _dP\"#######b_#######  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ####_dP\"88########9b_                       _dP########88\"9b######  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ###dP\"  8888888888888b    W E L C O M E    dP\"88888888888  \"9b####  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   #dP\"                         to  the                         \"9b##  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   #9b_                   C U B A N   B A R                     _dP##  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ###9b_  8888888888888P                   9b_8888888888888  _dP####  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   #####9b_88########dP\"                     \"96#####################  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ######\"98#######dP\"                         \"9b#########8P########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   #########8b###dP\"       _dP           8d_     \"9b#####d8##########  \033[0m  \n"
					.getBytes());
			out.write(
					"\033[32;5m   ########88\"98P\"       _dP\"8           889b_     \"9b_dP\"88#########  \033[0m  \n"
							.getBytes());
			out.write("\033[32;5m   ########88          _dP\"#88           88#\"9b_          88#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88        _dP####88           88####9b_        88#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88       9b######88           88######dP       88#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88888888888#8888888           8888888#88888888888#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88888888888#8888888           8888888#88888888888#########  \033[0m  \n"
					.getBytes());
			out.write("\033[32;5m   ########88888888888#8888888           8888888#88888888888#########  \033[0m  \n"
					.getBytes());
			out.write("   \"O my goodness! *smile*  *kisses*  *hugs*\" We'll never forget.  \n".getBytes());
			out.write("  					Remembering Deanna + 1996\n".getBytes());
			out.write("   For information e-mail Fidel     : patrick.hochstenbach@ugent.be  \n".getBytes());
			out.write("\033[31m   Type 'new' to create a new account, type 'who' to see who is in the  \033[0m \n"
					.getBytes());
			out.write("\033[31m   uban Bar or type 'over' to end this login session.  \033[0m \n".getBytes());
			out.write("\033[31m   type :[msg] to notice all user  \033[0m \n".getBytes());
			out.write("\033[31m   type 2[user]: to notice  user  \033[0m \n".getBytes());
			out.write("\n".getBytes());
			out.write("Name:".getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * 这里我们自己模拟读取一行，因为如果使用API中的BufferedReader时，它是读取到一个回车换行后
	 * 才返回，否则如果没有读取，则一直阻塞，这就导致如果为POST请求时，表单中的元素会以消息体传送，
	 * 这时，消息体最末按标准是没有回车换行的，如果此时还使用BufferedReader来读时，则POST提交
	 * 时会阻塞。如果是POST提交时我们按照消息体的长度Content-Length来截取消息体，这样就不会阻塞
	 */
	@SuppressWarnings("unused")
	private String readLine(InputStream is, int contentLe) throws IOException {
		ArrayList<Byte> lineByteList = new ArrayList<>();
		byte readByte;
		int total = 0;
		if (contentLe != 0) {
			do {
				readByte = (byte) is.read();
				lineByteList.add(Byte.valueOf(readByte));
				total++;
			} while (total < contentLe);// 消息体读还未读完
		} else {
			do {
				readByte = (byte) is.read();
				lineByteList.add(Byte.valueOf(readByte));
			} while (readByte != 10);
		}

		byte[] tmpByteArr = new byte[lineByteList.size()];
		for (int i = 0; i < lineByteList.size(); i++) {
			tmpByteArr[i] = ((Byte) lineByteList.get(i)).byteValue();
		}
		lineByteList.clear();

		String tmpStr = new String(tmpByteArr, "UTF-8");
		/*
		 * http请求的header中有一个Referer属性，这个属性的意思就是如果当前请求是从别的页面链接过
		 * 来的，那个属性就是那个页面的url，如果请求的url是直接从浏览器地址栏输入的就没有这个值。得
		 * 到这个值可以实现很多有用的功能，例如防盗链，记录访问来源以及记住刚才访问的链接等。另外，浏
		 * 览器发送这个Referer链接时好像固定用UTF-8编码的，所以在GBK下出现乱码，我们在这里纠正一下
		 */
		if (tmpStr.startsWith("Referer")) {// 如果有Referer头时，使用UTF-8编码
			tmpStr = new String(tmpByteArr, "UTF-8");
		}
		return tmpStr;
	}

}

class Txt {
	public static String fileName = "db.txt";

	public static boolean writeTxt(String text) {
		boolean result = false;
		try {
			File F = new File(fileName);
			// 如果文件不存在,就动态创建文件
			if (!F.exists()) {
				F.createNewFile();
			}
			FileWriter fw = null;
			// writeDate 写入的内容
			// 设置为:True,表示写入的时候追加数据
			fw = new FileWriter(F, true);
			// 回车并换行
			fw.write(text + "\r\n");
			if (fw != null) {
				fw.close();
			}
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@SuppressWarnings("resource")
	public static List<String> getAllLines() {
		List<String> allLines = new ArrayList<>();
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(fileName), "UTF-8");// 考虑到编码格式
			BufferedReader bufferedReader = new BufferedReader(read);
			String lineTxt = null;
			while ((lineTxt = bufferedReader.readLine()) != null) {
				allLines.add(lineTxt);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return allLines;
	}

}
