import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server
{
    private Set<Item> inventory;

  private static Set<Item> parseInventoryFile(String fileName)
  {


      if(fileName == null | fileName.isEmpty())
      {
        System.err.println("Please provide a valid filename");
        System.exit(-1);
      }

      // Read the file
      try
      {
          FileReader invFile = new FileReader(fileName);
          BufferedReader invBuff = new BufferedReader(invFile);
          String invLine = null;
          Set<Item> items = new HashSet<>();
          while ((invLine = invBuff.readLine()) != null)
          {
              if (invLine.length() > 0)
              {
                  String[] itemArr = invLine.split("\\s+");
                  if (itemArr.length == 2)
                  {
                      try
                      {
                          Item newItem = new Item(itemArr[0], Integer.parseInt(itemArr[1]));
                          items.add(newItem);
                      }catch (Exception e)
                      {
                          System.err.println("Unable to parse inventory line");
                          e.printStackTrace();
                      }
                  }
              }
          }
          return items;
      }catch(IOException e)
      {
          System.err.println("Unable to read inventory file");
          e.printStackTrace();
      }
      return new HashSet<>();
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
    Set<Item> inventory = parseInventoryFile(fileName);

    // Parse messages from clients
    ServerThread s = new ServerThread(tcpPort,udpPort,inventory);
    new Thread(s).start();

  }

  private static class Item
  {
      String name;
      int quantity;
      Lock lock = new ReentrantLock();


      public Item(String name, int quantity)
      {
          if(name == null || quantity < 0)
          {
            throw new InvalidParameterException("Item must have valid name and non negative quantity");
          }
          this.name = name;
          this.quantity = quantity;
      }

      public int getQuantity()
      {

          lock.lock();
          int value = this.quantity;
          lock.unlock();
          return value;
      }

      // Update the quantity of an item
      // Do it in synchronized block
      public void purchaseQuantiy(int toPurchase) throws InvalidParameterException
      {
          lock.lock();
          int currQuantity = this.quantity;
          if(toPurchase > currQuantity)
          {
              lock.unlock();
              throw new InvalidParameterException("Not enough items to buy purchase");
          }
          // Update the new quantity
          quantity = quantity - toPurchase;
          // Release the lock after updating the quantity
          lock.unlock();

      }
  }

  private static class ClientWorkerThread implements  Runnable
  {
      Socket s;
      Set<Item> inventory;
      public ClientWorkerThread(Socket s,Set<Item> inventory)
      {
          this.s = s;
          this.inventory = inventory;
      }

      private String processMessage(String[] msg)
      {
          //TODO: Complete impl
          return null;

      }

      public void run()
      {
          // Read the message from the client
          try
          {
              //We have received a TCP socket from the client.  Receive message and reply.
              BufferedReader inputReader = new BufferedReader(new InputStreamReader(s.getInputStream()));
              boolean autoFlush = true;
              PrintWriter outputWriter = new PrintWriter(s.getOutputStream(), autoFlush);
              String inputLine = inputReader.readLine();
              if (inputLine != null && inputLine.length() > 0) {
                  String[] msgArray = inputLine.split("\\s+");
                  String response = processMessage(msgArray);
                  if(response != null)
                  {
                      outputWriter.write(response);
                      outputWriter.flush();
                  }
                  outputWriter.close();
              }
          }catch (Exception e)
          {
              System.err.println("Unable to receive message from client");
              e.printStackTrace();
          }finally
          {
              if(s != null)
              {
                  try
                  {
                      s.close();
                  }catch (Exception e)
                  {
                      System.err.println("Unable to close client socket");
                      e.printStackTrace();
                  }
              }

          }

      }

  }


  private static class ServerThread implements Runnable
  {
      private int tcpPort;
      private int udpPort;
      private Set<Item> inventory;
      private AtomicBoolean isRunning = new AtomicBoolean(false);

      public ServerThread(int tcpPort, int udpPort, Set<Item> inventory)
      {
          this.tcpPort = tcpPort;
          this.udpPort = udpPort;
          this.inventory = inventory;
      }

      public void stop()
      {
          isRunning.getAndSet(false);
      }

      public void run()
      {
          isRunning.getAndSet(true);
          ServerSocket tcpServerSocket = null;
          //TODO: Need to also add udp sockets to listen on
          try
          {
              tcpServerSocket = new ServerSocket(this.tcpPort);
              while(isRunning.get() == true)
              {
                  Socket socket = null;
                  try
                  {
                      // Open a new socket with clients
                      socket = tcpServerSocket.accept();
                  }catch(Exception e)
                  {
                      System.err.println("Unable to accept new client connection");
                      e.printStackTrace();
                  }
                  if(socket != null)
                  {
                      // Spawn off a new thread to process messages from this client
                      ClientWorkerThread t = new ClientWorkerThread(socket,inventory);
                      new Thread(t).start();
                  }
              }

          }catch (Exception e)
          {
              System.err.println("Unable to accept client connections");
              e.printStackTrace();
          }finally {
              if (tcpServerSocket != null)
              {
                  try
                  {
                      tcpServerSocket.close();
                  }catch (Exception e)
                  {
                      System.err.println("Unable to close tcp server socket");
                      e.printStackTrace();
                  }
              }
              if (tcpServerSocket != null)
              {
                  try
                  {
                      tcpServerSocket.close();
                  }catch (Exception e)
                  {
                      System.err.println("Unable to close tcp socket");
                      e.printStackTrace();
                  }
              }
          }

      }


  }
}
