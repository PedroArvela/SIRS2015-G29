using System;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using WindowsFormsApplication2;

class UDP_Sender
{

    private const int port = 8889;
    private const int listenPort = 8888;
    private const int requestPort = 0;

    public static string GetLocalIPAddress()
    {
        var host = Dns.GetHostEntry(Dns.GetHostName());
        foreach (var ip in host.AddressList)
        {
            if (ip.AddressFamily == AddressFamily.InterNetwork)
            {
                return ip.ToString();
            }
        }
        throw new Exception("Local IP Address Not Found!");
    }

    internal static void sender(String pbkey)
    {
        Socket s = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
            ProtocolType.Udp);

        IPAddress broadcast = IPAddress.Parse("192.168.1.255");
        string serverName = System.Windows.Forms.SystemInformation.ComputerName;


        byte[] sendbuf = Encoding.ASCII.GetBytes(pbkey);
        IPEndPoint ep = new IPEndPoint(broadcast, port);

        s.SendTo(sendbuf, ep);
        s.Close();
    }


    internal static void sendinfo(string pbkey)
    {
        Socket s = new Socket(AddressFamily.InterNetwork, SocketType.Dgram,
            ProtocolType.Udp);

        bool done = false;

        UdpClient listener = new UdpClient(listenPort);
        IPEndPoint groupEP = new IPEndPoint(IPAddress.Any, listenPort);

        IPAddress broadcast = IPAddress.Parse("192.168.1.255");
        string serverName = System.Windows.Forms.SystemInformation.ComputerName;


        try
        {
            while (!done)
            {

                byte[] bytes = listener.Receive(ref groupEP);

                string returnData = Encoding.ASCII.GetString(bytes);
                string[] receivedport = returnData.Split('|');
                string p = receivedport[1];

                byte[] sendbuf = Encoding.ASCII.GetBytes(serverName + "|" + GetLocalIPAddress() + "|" + port);
                IPEndPoint ep = new IPEndPoint(broadcast, port);

                s.SendTo(sendbuf, ep);

                byte[] bytes2 = listener.Receive(ref groupEP);


                byte[] sendbuf2 = Encoding.ASCII.GetBytes(pbkey);

                s.SendTo(sendbuf2, ep);


                done = true;

            }

        }
        catch (Exception e)
        {
            Console.WriteLine(e.ToString());
        }
        finally
        {
            listener.Close();
            s.Close();
        }

    }

}
