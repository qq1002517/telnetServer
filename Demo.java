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
					print("????????????");
					break BIORead;
				}
				try {

					while ((realLen = in.read(contextBytes, 0, maxLen)) != -1) {
						String line = new String(contextBytes, 0, realLen);
						message.append(line);
						print("read:" + line);
						if (message.indexOf("User-Agent") > -1) {
							PrintWriter pout = new PrintWriter(out, true);
							pout.println("HTTP/1.1 200 OK");// ??????????????????,???????????????
							pout.println("Content-Type:text/html;charset=utf-8");
							pout.println();// ?????? HTTP ??????, ????????????????????????
							pout.println("<title> Hello Telnet Server</title>");
							pout.println("<meta charset=\"utf-8\">");
							pout.println("<h1> Hello Telnet Server</h1>");
							pout.println("hello,this is a Java Telnet server demo app.<br>");
							pout.println("pleas use command telnet blog.xuhaobo.cn");
							Demo.remove(this.name);
							break BIORead;
						}
						/*
						 * ????????????????????????over??????????????? ??????????????????????????????????????????????????????????????????
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
							//????????????????????????
							String respone = "res:now there [" + Demo.getCountner() + "] user in room\n\r";
							out.write(respone.getBytes());
						}
						// ??????????????????
						print(addree + "???" + sourcePort + "-->" + message);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// ??????
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
	 * ?????????????????????????????????????????????????????????API??????BufferedReader??????????????????????????????????????????
	 * ??????????????????????????????????????????????????????????????????????????????POST??????????????????????????????????????????????????????
	 * ?????????????????????????????????????????????????????????????????????????????????BufferedReader???????????????POST??????
	 * ????????????????????????POST???????????????????????????????????????Content-Length??????????????????????????????????????????
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
			} while (total < contentLe);// ????????????????????????
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
		 * http?????????header????????????Referer?????????????????????????????????????????????????????????????????????????????????
		 * ??????????????????????????????????????????url??????????????????url???????????????????????????????????????????????????????????????
		 * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		 * ??????????????????Referer????????????????????????UTF-8?????????????????????GBK?????????????????????????????????????????????
		 */
		if (tmpStr.startsWith("Referer")) {// ?????????Referer???????????????UTF-8??????
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
			// ?????????????????????,?????????????????????
			if (!F.exists()) {
				F.createNewFile();
			}
			FileWriter fw = null;
			// writeDate ???????????????
			// ?????????:True,?????????????????????????????????
			fw = new FileWriter(F, true);
			// ???????????????
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
			InputStreamReader read = new InputStreamReader(new FileInputStream(fileName), "UTF-8");// ?????????????????????
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
