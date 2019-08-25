import java.util.Scanner;

public class Client {

  private static final String TCP_MODE = "T";
  private static final String UDP_MODE = "U";

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
        if (tokens.length < 2)
        {
          System.out.println("Usage: setmode <T|U>");
          continue;
        }
        String mode = tokens[1];
        if(UDP_MODE.equals(mode))
        {
          ipProtocol = UDP_MODE;
        }
        System.out.println("Setmode to " + ipProtocol);
      }

      else if (tokens[0].equals("purchase")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else if (tokens[0].equals("cancel")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else if (tokens[0].equals("search")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else if (tokens[0].equals("list")) {
        // TODO: send appropriate command to the server and display the
        // appropriate responses form the server
      } else {
        System.out.println("ERROR: No such command");
      }
    }
  }
}
