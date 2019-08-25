import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {

  private static final String TCP_MODE = "T";
  private static final String UDP_MODE = "U";
  private static String ipProtocol = TCP_MODE;
  private static int BUF_LEN = 1024;

  private static void setMode(String[] tokens)
  {
      if (tokens.length < 2)
      {
        System.out.println("Usage: setmode <T|U>");
        return;
      }
      String mode = tokens[1];
      if(UDP_MODE.equals(mode))
      {
        ipProtocol = UDP_MODE;
      }else
      {
        // default to TCP Protocol
        ipProtocol = TCP_MODE;
      }
      System.out.println("Setmode to " + ipProtocol);
  }


  private static String getPurchaseCmd(String[] tokens) 
  {
      if (tokens.length < 4)
      {
        System.out.println("Usage: purchase <user-name> <product-name> <quantity>");
        return null;
      }
      String userName = tokens[1];
      String productName = tokens[2];
      Integer quantity;
      try
      {
        quantity = Integer.parseInt(tokens[3]);
      }catch(NumberFormatException e)
      {
        System.err.println("Unable to parse quantity for purchase order");
        e.printStackTrace();
        return null;
      }
      String cmd = "purchase " + userName + " " + productName + " " + quantity;
      return cmd;
  }

  private static String getCancelCmd(String[] tokens)
  {
      if (tokens.length < 2)
      {
        System.out.println("Usage: cancel <order-id>");
        return null;
      }
      String orderId = tokens[1];
      String cmd = "cancel " + orderId;
      return cmd;
  }

  private static String getSearchCmd(String[] tokens)
  {
      if (tokens.length < 2)
      {
        System.out.println("Usage: cancel <order-id>");
        return null;
      }
      String userName = tokens[1];
      String cmd = "search" + userName;
      return cmd;

  }

  private static String getListCmd(String[] tokens)
  {
      if (tokens.length < 2)
      {
        System.out.println("Usage: list");
        return null;
      }
      String cmd = "list";
      return cmd;

  }

  private static void sendCmdOverUdp(String command, String hostAddress, int port)
  {
    try
    {
      //Send message
      byte[] payload = new byte[command.length()];
      payload = command.getBytes();
      InetAddress inetAddy = InetAddress.getByName(hostAddress);
      // Let the OS pick an outbound port
      DatagramSocket udpOutboundSocket = new DatagramSocket();
      // Listen on UDP port for inbound
      DatagramSocket udpInboundSocket = new DatagramSocket(port);
      DatagramPacket sPacket = new DatagramPacket(payload, payload.length, inetAddy, port);
      udpOutboundSocket.send(sPacket);
      udpOutboundSocket.close();

      //Receive Reply:
      byte[] udpBuff = new byte[BUF_LEN];
      DatagramPacket rPacket = new DatagramPacket(udpBuff,udpBuff.length);
      udpInboundSocket.receive(rPacket);
      String msgData = new String(rPacket.getData());                            
      msgData = msgData.trim();
      System.out.println(msgData);  
      udpInboundSocket.close();
    } catch(Exception e)
    {
      System.err.println("Unable to send message over udp");
      e.printStackTrace();
    }
  }

  private static void sendCmdOverTcp(String command, String hostAddress, int port)
  {
      // Send the purchase over TCP
      Socket tcpSocket = null;
      try
      {
        // Get the socket
        tcpSocket = new Socket(hostAddress, port);
        PrintWriter outputWriter = new PrintWriter(tcpSocket.getOutputStream(), true);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
        // Write the purchase message
        outputWriter.write(command);
        outputWriter.flush();

        // Wait for the response from the server
        String response = "";
                            
        while(true)
        {
          response = inputReader.readLine();
          if (response == null)
          {
            break;
          }
          // Print the response
          System.out.println(response);
          tcpSocket.close();
        }

      }catch(Exception e)
      {
        System.err.println("Unable to send purchase order");
        e.printStackTrace();
      }finally
      {
        if (tcpSocket != null)
        {
          try
          {
            tcpSocket.close();
          }catch(Exception e)
          {
            System.err.println("Unable to close socket");
            e.printStackTrace();
          }

        }
      }
  }

  public static void main (String[] args) 
  {
    String hostAddress;
    int tcpPort;
    int udpPort;
    // Default protocol is TCP
    String ipProtocol = TCP_MODE;


    if (args.length != 3) {
      System.out.println("ERROR: Provide 3 arguments");
      System.out.println("\t(1) <hostAddress>: the address of the server");
      System.out.println("\t(2) <tcpPort>: the port number for TCP connection");
      System.out.println("\t(3) <udpPort>: the port number for UDP connection");
      System.exit(-1);
    }

    hostAddress = args[0];
    tcpPort = Integer.parseInt(args[1]);
    udpPort = Integer.parseInt(args[2]);

    Scanner sc = new Scanner(System.in);
    while(sc.hasNextLine()) {
      String cmd = sc.nextLine();
      String[] tokens = cmd.split(" ");

      if (tokens[0].equals("setmode")) {
        // Set the ip protocol mode
        setMode(tokens);
      } else
      {
        // Send a command to the server
        String command = null;
        if (tokens[0].equals("purchase")) {
          command = getPurchaseCmd(tokens);
        } else if (tokens[0].equals("cancel")) {
          command = getCancelCmd(tokens);
        } else if (tokens[0].equals("search")) {
          command = getSearchCmd(tokens);
        } else if (tokens[0].equals("list")) {
          command = getListCmd(tokens);
        } else {
          System.out.println("ERROR: No such command");
        }
        // Send the command if it's not null
        if (command != null)
        {
          if(UDP_MODE.equals(ipProtocol))
          {
            sendCmdOverUdp(command,hostAddress,udpPort);
          }else
          {
            sendCmdOverTcp(command,hostAddress,tcpPort);
          }
        }

      }
    }
  }
}
