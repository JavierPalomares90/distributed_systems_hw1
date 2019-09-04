import java.io.*;
import java.net.ServerSocket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DatagramServer
{
    private static List<Item> inventory;
    private static List<Order> orders;
    private static AtomicInteger orderId = new AtomicInteger(1);
    private static final String PURCHASE = "purchase";
    private static final String CANCEL = "cancel";
    private static final String SEARCH = "search";
    private static final String LIST = "list";

    public synchronized static Order createAndAddOrder(String userName, String productName, int quantity)
    {
        int id = orderId.getAndIncrement();
        Order o = new Order(id,userName,productName,quantity);
        if(orders == null)
        {
            orders = new ArrayList<>();
        }
        orders.add(o);
        return o;
    }

    private static List<Item> parseInventoryFile(String fileName)
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
            List<Item> items = new ArrayList<>();
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
            Collections.sort(items,Item.itemComparator);
            return items;
        }catch(IOException e)
        {
            System.err.println("Unable to read inventory file");
            e.printStackTrace();
        }
        return new ArrayList<>();
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
        inventory = parseInventoryFile(fileName);

        // Parse messages from clients
        ServerThread s = new ServerThread(tcpPort,udpPort,inventory);
        new Thread(s).start();

    }

    private static class Order
    {
        int orderId;
        String userName;
        String productName;
        int productQuantity;
        // Set to false once orders are cancelled
        AtomicBoolean validOrder;

        public Order(int orderId, String userName, String productName, int productQuantity)
        {
            this.orderId = orderId;
            this.userName = userName;
            this.productName = productName;
            this.productQuantity = productQuantity;
            validOrder = new AtomicBoolean(true);
        }

        public String toString()
        {
            return this.orderId + " " + this.userName + " " + this.productName + " " + this.productQuantity;
        }

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
                throw new InvalidParameterException("Not enough items to buy of item " + this.name);
            }
            // Update the new quantity
            quantity = quantity - toPurchase;
            // Release the lock after updating the quantity
            lock.unlock();

        }

        public static Comparator<Item> itemComparator = new Comparator<Item>()
        {
            @Override
            public int compare(Item item1, Item item2)
            {
                if (item1 == null)
                {
                    if (item2 == null)
                    {
                        return 0;
                    }
                    return 1;
                }
                if(item2 == null)
                {
                    return -1;
                }
                return item1.name.compareTo(item2.name);
            }
        };
    }


    private static class ServerThread implements Runnable
    {


        private int tcpPort;
        private int udpPort;
        private List<Item> inventory;
        private AtomicBoolean isRunning = new AtomicBoolean(false);

        public ServerThread(int tcpPort, int udpPort, List<Item> inventory)
        {
            this.tcpPort = tcpPort;
            this.udpPort = udpPort;
            this.inventory = inventory;
        }

        private String purchaseMsg(String[] tokens)
        {
            if(tokens == null || tokens.length < 4)
            {
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
                System.err.println("Unable to parse quantity for purchase");
                e.printStackTrace();
                return null;
            }
            // Search for the item in the inventory
            for (Item i: inventory)
            {
                if (productName.equals(i.name))
                {
                    // try to purchase the given quantity of i
                    try
                    {
                        i.purchaseQuantiy(quantity);
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                        return "Not Available - Not enough items.";
                    }
                    // Create an order and add it to the list
                    //
                    Order o = DatagramServer.createAndAddOrder(userName,productName,quantity);
                    return "Your order has been placed " + o.toString();
                }
            }

            // Did not find the item
            return "Not Available - We do not sell this product.";
        }

        private String cancelMsg(String[] tokens)
        {
            if(tokens == null || tokens.length < 2)
            {
                return null;
            }
            Integer orderId;
            try
            {
                orderId = Integer.parseInt(tokens[1]);
            }catch(NumberFormatException e)
            {
                System.err.println("Unable to parse orderId for cancellation");
                e.printStackTrace();
                return null;
            }
            // Search for the order and mark it as invalid
            for (Order o: orders)
            {
                if(o.orderId == orderId.intValue())
                {
                    int quantity = o.productQuantity;
                    String productName = o.productName;
                    // The index of the product in the inventory list
                    int index  = Collections.binarySearch(inventory, new Item(productName,quantity), Item.itemComparator);
                    if(index < 0)
                    {
                        // product is not in the inventory
                        return "Unable to cancel order with id " + orderId;
                    }
                    // Increase the quantity of the item in the order and cancel the order
                    Item invItem = inventory.get(index);
                    try
                    {
                        invItem.purchaseQuantiy(-1 * quantity);
                        o.validOrder.getAndSet(false);
                        return "Order " + orderId + " is canceled";
                    }catch (Exception e)
                    {
                        System.err.println("Unable to cancel order with id " + orderId);
                        e.printStackTrace();
                        return "Unable to cancel order with id " + orderId;
                    }
                }
            }
            return orderId + " is not found, no such order";
        }

        private String searchMsg(String[] tokens)
        {
            if(tokens == null || tokens.length < 2)
            {
                return null;
            }
            String userName = tokens[1];
            String response = null;
            // Search for the orders with the given username
            for (Order o: orders)
            {
                // Add orders if the user name matches and it's a valid order
                if(o.userName.equals(userName) && o.validOrder.get())
                {
                    // Add it to the response
                    if(response == null)
                    {
                        response = "";
                    }
                    response += o.toString() + "\n";
                }
            }
            // No orders found
            if(response == null)
            {
                return "No order found for " + userName;
            }
            return response;
        }

        private String listMsg(String[] tokens)
        {
            if(tokens == null || tokens.length < 1)
            {
                return null;
            }
            String response = "";
            for (Item i: inventory)
            {
                response += i.name + " " + i.quantity + "\n";;
            }
            return response;
        }

        private String processMessage(String msg)
        {
            String[] tokens = msg.trim().split("\\s+");
            String response = null;
            if(PURCHASE.equals(tokens[0]))
            {
                response = purchaseMsg(tokens);
            } else if (CANCEL.equals(tokens[0]))
            {
                response = cancelMsg(tokens);
            } else if (SEARCH.equals(tokens[0]))
            {
                response = searchMsg(tokens);
            }
            else if (LIST.equals(tokens[0]))
            {
                response = listMsg(tokens);
            } else {
                System.out.println("Invalid command: " + tokens[0]);
            }
            return response;
        }

        public void stop()
        {
            isRunning.getAndSet(false);
        }

        public void run()
        {
            isRunning.getAndSet(true);
            DatagramPacket receivePacket, returnPacket;
            int len = 1024;

            try {
                while(true) {
                    // Assign inbound port and listen
                    DatagramSocket inboundSocket = new DatagramSocket(udpPort);
                    byte[] buf = new byte[len];

                    try {
                        // Receive command and close inbound socket
                        receivePacket = new DatagramPacket(buf, buf.length);
                        inboundSocket.receive(receivePacket);
                        inboundSocket.close();

                        // Parse command and process
                        if (receivePacket.getData() != null) {

                            String msg = new String(receivePacket.getData());
                            String response = processMessage(msg);
                            byte[] byteResponse = response.getBytes();

                            // Send response back to client and close outbound socket
                            if (response != null) {
                                // Allow OS to assign outbound port
                                DatagramSocket outboundSocket = new DatagramSocket();

                                returnPacket = new DatagramPacket(byteResponse,
                                        byteResponse.length,
                                        receivePacket.getAddress(),
                                        receivePacket.getPort());
                                outboundSocket.send(returnPacket);
                                outboundSocket.close();
                            }
                        }
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }

            } catch (Exception e)
            {
                System.err.println("Unable to receive message from client");
                e.printStackTrace();
            }

        }


    }
}