public class Server {

  private static void parseInventoryFile(String fileName)
  {
    if(fileName == null | fileName.isEmpty())
    {
      System.err.println("Please provide a valid filename");
      System.exit(-1);
    }
  }
  public static void main (String[] args) {
    int tcpPort;
    int udpPort;
    if (args.length != 3) {
      System.out.println("ERROR: Provide 3 arguments");
      System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
      System.out.println("\t(2) <udpPort>: the port number for UDP connection");
      System.out.println("\t(3) <file>: the file of inventory");

      System.exit(-1);
    }
    tcpPort = Integer.parseInt(args[0]);
    udpPort = Integer.parseInt(args[1]);
    String fileName = args[2];

    // parse the inventory file
    parseInventoryFile(fileName);

    // TODO: handle request from clients
  }

  private class Item
  {

  }
}
