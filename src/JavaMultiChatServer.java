
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by KBPark on 2016. 5. 28..
 */
public class JavaMultiChatServer
{
    HashMap clients;

    JavaMultiChatServer()
    {
        clients = new HashMap();
        Collections.synchronizedMap(clients);
    }

    public void start()
    {
        ServerSocket serverSocket = null;
        Socket socket = null;

        try
        {
            serverSocket = new ServerSocket(7777);
            System.out.println("서버가 시작되었습니다.");

            // 이 무한루프는 사용자를 계속 받기위한 무한루프 입니다. 사용자마다 소켓 1개씩 할당.
            while(true)
            {
                socket = serverSocket.accept();
                System.out.println("[" + socket.getInetAddress() + "," + socket.getPort() + "]"
                        + "에서 접속하셨습니다.");

                // 새로운 사용자가 잡속을 시도할때마다 계속 thread를 생성합니다. (main thread가 일을 다 처리하기 힘드니까, 각각 thread에 분업시킵니다.)
                ServerReceiver thread = new ServerReceiver(socket);
                thread.start();
            }

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        new JavaMultiChatServer().start();

    }

    /**
     * 모든 사용자에게 msg를 뿌려주는 메소드
     * @param msg
     */
    public void sendToAll(String msg)
    {
        Iterator it = clients.keySet().iterator();

        while(it.hasNext())
        {
            try
            {
                // iterator로 하나씩 조회하면서, client.get() 을 통해 해당 value(저장된 OutputStream 값) 을 빼옵니다.
                DataOutputStream out = (DataOutputStream) clients.get(it.next());

                out.writeUTF(msg); // 해당 outputStream으로 전달받은 msg를 뿌려줍니다.
                out.flush();

            }catch (IOException e) { }
        }
    }



    /**
     *  얘는 새로운 사용자가 접속을 시도할때마다 불리는 thread입니다.
     */
    class ServerReceiver extends Thread
    {
        Socket socket;
        DataInputStream in;
        DataOutputStream out;

        // contsructor에서 받아온 socket정보를 이용하여 새로운 사용자와의 연결을 setting합니다.
        ServerReceiver(Socket socket)
        {
            this.socket = socket;

            try
            {
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

            }catch (IOException e){}
        }

        @Override
        public void run()
        {
            String name = "";
            // 1. 먼저, 사용자의 이름과 outputStream정보를 hashmap에 추가합니다.
            try
            {
                name = in.readUTF(); // 이름을 먼저 받아놓습니다. 왜냐면 hashmap에 먼저 등록을 해놓기 위해서! (Client측에서도 그렇게 해놨습니다. 이름먼저 보내고 내용보내기로.)
                System.out.println("#" + name + "님 접속하셨군요."); // 테스트용
                sendToAll("#"+name+"님이 들어오셨습니다.");

                // hashMap에 name-OutputStream 정보를 추가해줍니다! 왜 하필 OutputStream이냐구요? sendToAll()를 보면 알수 있어요!
                clients.put(name, out);
                System.out.println("현재 접속자 수 : " + clients.size() + "명");



                while(in != null) //여기서 이렇게 무한루프 돌리면서 sendToAll()해도 되나.. ???
                {
                    sendToAll(in.readUTF());
                }

            }catch(IOException e) { }
            finally
            {
                sendToAll("#"+ name + "님이 나가셨습니다.");

                clients.remove(name);

                System.out.println("[" + socket.getInetAddress() + "," + socket.getPort() + "]"
                        + "에서 접속을 종료하셨습니다.");
                System.out.println("현재 접속자 수 : " + clients.size() + "명");


            }

        }

    }
}
